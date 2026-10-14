package com.cheatbreaker.client.module.type.motionblur;

import java.nio.ByteBuffer;
import javax.vecmath.Matrix4f;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.OpenGlHelper;
import cn.kazer.client189.compat.Tessellator;
import net.minecraft.client.renderer.texture.TextureUtil;
import net.minecraft.client.shader.Framebuffer;
import net.minecraft.client.shader.ShaderManager;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL14;

/**
 * Self-owned accumulation pipeline for Motion Blur.
 *
 * Deliberately bypasses the vanilla {@code FrameBuffer}/{@code ShaderGroup}
 * path: OptiFine's Fast Render forces {@code OpenGlHelper.isFramebufferEnabled()}
 * to false, which silences every method of {@code FrameBuffer}. All FBO work
 * here goes through capability-level {@code OpenGlHelper} wrappers instead, so
 * the module works with Fast Render on or off (ADR-0004).
 *
 * Per frame, right after the world render and before the GUI pass:
 *  - Fast Render OFF (vanilla FBO path): mix the main FBO texture with the
 *    accumulated previous frame into a swap buffer, then blit the result both
 *    into the accumulation buffer and back into the main FBO.
 *  - Fast Render ON (world already sits in the default framebuffer): snapshot
 *    the back buffer, draw the mix straight to the screen, snapshot the result
 *    as next frame's "previous".
 *
 * The shader is the vanilla phosphor program ({@code max(prev * k, curr)}),
 * loaded through the standard {@code ShaderManager} from
 * {@code assets/minecraft/shaders/program/motionblur.json}.
 */
public final class MotionBlurPipeline {

    private static final Logger LOGGER = LogManager.getLogger(MotionBlurPipeline.class);

    private boolean unsupported;
    private ShaderManager shaderMax;   // Old mode: max(prev * k, curr)
    private ShaderManager shaderMix;   // Phosphor mode: mix(prev, curr, Weight)
    private int prevTexture = -1;
    private int prevFramebuffer = -1;
    private int swapTexture = -1;
    private int swapFramebuffer = -1;
    private int currentTexture = -1;

    /**
     * Fixed-function GL state save/restore for our post-world passes
     * (spec: client-ui-contracts 渲染后处理状态契约). Everything the passes touch
     * returns to its entry value before the caller continues into the GUI pass.
     */
    static final class GlFrameStateGuard {
        private final int framebuffer=GL11.glGetInteger(36006);
        private final int matrixMode=GL11.glGetInteger(GL11.GL_MATRIX_MODE);
        private final boolean blend = GL11.glIsEnabled(GL11.GL_BLEND);
        private final boolean alphaTest = GL11.glIsEnabled(GL11.GL_ALPHA_TEST);
        private final boolean depthTest = GL11.glIsEnabled(GL11.GL_DEPTH_TEST);
        private final boolean lighting = GL11.glIsEnabled(GL11.GL_LIGHTING);
        private final boolean cullFace = GL11.glIsEnabled(GL11.GL_CULL_FACE);
        private final boolean depthMask = GL11.glGetBoolean(GL11.GL_DEPTH_WRITEMASK);
        private final int blendSrcRgb = GL11.glGetInteger(GL14.GL_BLEND_SRC_RGB);
        private final int blendDstRgb = GL11.glGetInteger(GL14.GL_BLEND_DST_RGB);
        private final int blendSrcAlpha = GL11.glGetInteger(GL14.GL_BLEND_SRC_ALPHA);
        private final int blendDstAlpha = GL11.glGetInteger(GL14.GL_BLEND_DST_ALPHA);
        private final int activeTexture = GL11.glGetInteger(GL13.GL_ACTIVE_TEXTURE);

        GlFrameStateGuard() {
            cn.kazer.client189.compat.LegacyGL.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS);
            GL11.glMatrixMode(GL11.GL_PROJECTION);GL11.glPushMatrix();
            GL11.glMatrixMode(GL11.GL_MODELVIEW);GL11.glPushMatrix();
            net.minecraft.client.renderer.GlStateManager.setActiveTexture(OpenGlHelper.defaultTexUnit);
        }

        void restore() {
            OpenGlHelper.glBlendFunc(this.blendSrcRgb, this.blendDstRgb, this.blendSrcAlpha, this.blendDstAlpha);
            cn.kazer.client189.compat.LegacyGL.glDepthMask(this.depthMask);
            set(GL11.GL_BLEND, this.blend);
            set(GL11.GL_ALPHA_TEST, this.alphaTest);
            set(GL11.GL_DEPTH_TEST, this.depthTest);
            set(GL11.GL_LIGHTING, this.lighting);
            set(GL11.GL_CULL_FACE, this.cullFace);
            GL11.glMatrixMode(GL11.GL_PROJECTION);GL11.glPopMatrix();
            GL11.glMatrixMode(GL11.GL_MODELVIEW);GL11.glPopMatrix();GL11.glMatrixMode(matrixMode);
            cn.kazer.client189.compat.LegacyGL.glPopAttrib();
            OpenGlHelper.glBindFramebuffer(OpenGlHelper.GL_FRAMEBUFFER,framebuffer);
            net.minecraft.client.renderer.GlStateManager.setActiveTexture(this.activeTexture);
        }

        private static void set(int capability, boolean enabled) {
            if (enabled) {
                cn.kazer.client189.compat.LegacyGL.glEnable(capability);
            } else {
                cn.kazer.client189.compat.LegacyGL.glDisable(capability);
            }
        }
    }

    public void render(Minecraft mc, MotionBlurAccumulation accumulation, MotionBlurMode mode, int percent) {
        // TEMP 036-038 fallback: Monkey lands in 038; render Old until then.
        if (mode == MotionBlurMode.MONKEY) {
            mode = MotionBlurMode.OLD;
        }
        if (this.unsupported) {
            return;
        }
        if (!OpenGlHelper.framebufferSupported || !OpenGlHelper.shadersSupported) {
            this.unsupported = true;
            return;
        }
        int width = mc.displayWidth;
        int height = mc.displayHeight;
        if (width <= 0 || height <= 0) {
            return;
        }
        GlFrameStateGuard glState = new GlFrameStateGuard();
        try {
            if (accumulation.onFramebufferSize(width, height)) {
                this.deleteBuffers();
            }
            if (this.prevTexture < 0) {
                this.createBuffers(width, height);
            }
            if (this.shaderMax == null) {
                this.shaderMax = new ShaderManager(mc.getResourceManager(), "motionblur");
                this.shaderMix = new ShaderManager(mc.getResourceManager(), "motionblur_mix");
            }
            if (accumulation.isResetPending()) {
                // Pass-through frame: output stays untouched and "previous" becomes the
                // current frame, so every mode starts from a clean baseline.
                this.captureCurrentFrameAsPrevious(mc, width, height);
                accumulation.consumeReset();
                return;
            }
            if (mode == MotionBlurMode.MOULBERRY) {
                this.renderMoulberry(mc, width, height, MotionBlurStrength.moulberryAlpha(percent));
            } else {
                boolean mix = mode == MotionBlurMode.PHOSPHOR;
                float weight = mix
                        ? MotionBlurStrength.phosphorMixWeight(percent)
                        : MotionBlurStrength.phosphorFactor(percent);
                if (OpenGlHelper.isFramebufferEnabled()) {
                    this.renderAcrossMainFramebuffer(mc, width, height, mix, weight);
                } else {
                    this.renderAcrossBackBuffer(width, height, mix, weight);
                }
            }
        } catch (Throwable throwable) {
            LOGGER.warn("Motion blur disabled after a render failure.", throwable);
            this.unsupported = true;
            this.deleteBuffers();
            this.shaderMax = null;
            this.shaderMix = null;
        } finally {
            // Contract: hand the GUI pass exactly the state the world render left us.
            glState.restore();
        }
    }

    /** Fast Render off: the world lives in the main FBO texture. */
    private void renderAcrossMainFramebuffer(Minecraft mc, int width, int height, boolean mix, float weight) {
        Framebuffer main = mc.getFramebuffer();
        this.bindTarget(this.swapFramebuffer, width, height);
        this.runAccumulationPass(main.framebufferTexture, width, height, mix, weight);
        this.bindTarget(this.prevFramebuffer, width, height);
        blitFullscreen(this.swapTexture, width, height, false, 1.0F);
        OpenGlHelper.glBindFramebuffer(OpenGlHelper.GL_FRAMEBUFFER, main.framebufferObject);
        GL11.glViewport(0, 0, width, height);
        blitFullscreen(this.swapTexture, width, height, false, 1.0F);
    }

    /** Fast Render on: the world was drawn straight into the back buffer. */
    private void renderAcrossBackBuffer(int width, int height, boolean mix, float weight) {
        OpenGlHelper.glBindFramebuffer(OpenGlHelper.GL_FRAMEBUFFER, 0);
        GL11.glViewport(0, 0, width, height);
        cn.kazer.client189.compat.LegacyGL.glBindTexture(GL11.GL_TEXTURE_2D, this.currentTexture);
        GL11.glCopyTexSubImage2D(GL11.GL_TEXTURE_2D, 0, 0, 0, 0, 0, width, height);
        this.runAccumulationPass(this.currentTexture, width, height, mix, weight);
        cn.kazer.client189.compat.LegacyGL.glBindTexture(GL11.GL_TEXTURE_2D, this.prevTexture);
        GL11.glCopyTexSubImage2D(GL11.GL_TEXTURE_2D, 0, 0, 0, 0, 0, width, height);
        cn.kazer.client189.compat.LegacyGL.glBindTexture(GL11.GL_TEXTURE_2D, 0);
    }

    /** The ShaderGroup ortho-projection for a pixel-space fullscreen quad. */
    static Matrix4f quadProjection(int width, int height) {
        Matrix4f projection = new Matrix4f();
        projection.setIdentity();
        projection.m00 = 2.0F / (float) width;
        projection.m11 = 2.0F / (float) (-height);
        projection.m22 = -0.0020001999F;
        projection.m33 = 1.0F;
        projection.m03 = -1.0F;
        projection.m13 = 1.0F;
        projection.m23 = -1.0001999F;
        return projection;
    }

    /** Position-attribute quad spanning (0,0)-(width,height); shaders map it via sobel.vsh. */
    static void drawFullscreenQuad(int width, int height) {
        net.minecraft.client.renderer.Tessellator tessellator=net.minecraft.client.renderer.Tessellator.getInstance();
        net.minecraft.client.renderer.WorldRenderer vertices=tessellator.getWorldRenderer();
        vertices.begin(GL11.GL_QUADS,net.minecraft.client.renderer.vertex.DefaultVertexFormats.POSITION_COLOR);
        vertices.pos(0,height,500).color(255,255,255,255).endVertex();
        vertices.pos(width,height,500).color(255,255,255,255).endVertex();
        vertices.pos(width,0,500).color(255,255,255,255).endVertex();
        vertices.pos(0,0,500).color(255,255,255,255).endVertex();tessellator.draw();
    }

    private void runAccumulationPass(int currentTexture, int width, int height, boolean mix, float weight) {
        ShaderManager shader = mix ? this.shaderMix : this.shaderMax;
        Matrix4f projection = quadProjection(width, height);

        shader.addSamplerTexture("DiffuseSampler", Integer.valueOf(currentTexture));
        shader.addSamplerTexture("PrevSampler", Integer.valueOf(this.prevTexture));
        shader.getShaderUniformOrDefault("ProjMat").set(cn.kazer.client189.compat.KazerCompat.shaderMatrix(projection));
        shader.getShaderUniformOrDefault("InSize").set((float) width, (float) height);
        shader.getShaderUniformOrDefault("OutSize").set((float) width, (float) height);
        if (mix) {
            shader.getShaderUniformOrDefault("Weight").set(weight);
        } else {
            shader.getShaderUniformOrDefault("Phosphor").set(weight, weight, weight);
        }

        cn.kazer.client189.compat.LegacyGL.glColorMask(true, true, true, false);
        cn.kazer.client189.compat.LegacyGL.glDisable(GL11.GL_DEPTH_TEST);
        cn.kazer.client189.compat.LegacyGL.glDepthMask(false);
        GL11.glViewport(0, 0, width, height);
        shader.useShader();
        drawFullscreenQuad(width, height);
        shader.endShader();
        net.minecraft.client.renderer.GlStateManager.setActiveTexture(OpenGlHelper.lightmapTexUnit);
        net.minecraft.client.renderer.GlStateManager.disableTexture2D();
        net.minecraft.client.renderer.GlStateManager.setActiveTexture(OpenGlHelper.defaultTexUnit);
        cn.kazer.client189.compat.LegacyGL.glDepthMask(true);
        cn.kazer.client189.compat.LegacyGL.glColorMask(true, true, true, true);
    }

    /** Fixed-function 1:1 copy of {@code texture} onto the currently bound target. */
    private static void blitFullscreen(int texture, int width, int height, boolean alphaBlend, float alpha) {
        net.minecraft.client.renderer.GlStateManager.setActiveTexture(OpenGlHelper.lightmapTexUnit);
        net.minecraft.client.renderer.GlStateManager.disableTexture2D();
        net.minecraft.client.renderer.GlStateManager.setActiveTexture(OpenGlHelper.defaultTexUnit);
        cn.kazer.client189.compat.LegacyGL.glColorMask(true, true, true, false);
        cn.kazer.client189.compat.LegacyGL.glDisable(GL11.GL_DEPTH_TEST);
        cn.kazer.client189.compat.LegacyGL.glDepthMask(false);
        GL11.glMatrixMode(GL11.GL_PROJECTION);
        GL11.glLoadIdentity();
        GL11.glOrtho(0.0D, width, height, 0.0D, 1000.0D, 3000.0D);
        GL11.glMatrixMode(GL11.GL_MODELVIEW);
        GL11.glLoadIdentity();
        GL11.glTranslatef(0.0F, 0.0F, -2000.0F);
        GL11.glViewport(0, 0, width, height);
        cn.kazer.client189.compat.LegacyGL.glDisable(GL11.GL_CULL_FACE);
        cn.kazer.client189.compat.LegacyGL.glEnable(GL11.GL_TEXTURE_2D);
        cn.kazer.client189.compat.LegacyGL.glDisable(GL11.GL_LIGHTING);
        cn.kazer.client189.compat.LegacyGL.glDisable(GL11.GL_ALPHA_TEST);
        if (alphaBlend) {
            cn.kazer.client189.compat.LegacyGL.glEnable(GL11.GL_BLEND);
            // The quad carries per-vertex colors (setColorOpaque_I), which make GL 1.x
            // ignore glColor4f's alpha; the texture alpha channel is unreliable too.
            // The trail weight therefore rides on the constant blend color so that
            // SRC_ALPHA can never degenerate into "previous frame x 1.0" (frozen screen).
            GL14.glBlendColor(1.0F, 1.0F, 1.0F, alpha);
            OpenGlHelper.glBlendFunc(moulberryBlendSrcFactor(), moulberryBlendDstFactor(), 1, 1);
            cn.kazer.client189.compat.LegacyGL.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
        } else {
            cn.kazer.client189.compat.LegacyGL.glDisable(GL11.GL_BLEND);
            cn.kazer.client189.compat.LegacyGL.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
        }
        cn.kazer.client189.compat.LegacyGL.glBindTexture(GL11.GL_TEXTURE_2D, texture);

        Tessellator tessellator = Tessellator.instance;
        tessellator.startDrawingQuads();
        tessellator.setColorOpaque_I(-1);
        tessellator.addVertexWithUV(0.0D, height, 0.0D, 0.0D, 0.0D);
        tessellator.addVertexWithUV(width, height, 0.0D, 1.0D, 0.0D);
        tessellator.addVertexWithUV(width, 0.0D, 0.0D, 1.0D, 1.0D);
        tessellator.addVertexWithUV(0.0D, 0.0D, 0.0D, 0.0D, 1.0D);
        tessellator.draw();

        cn.kazer.client189.compat.LegacyGL.glBindTexture(GL11.GL_TEXTURE_2D, 0);
        cn.kazer.client189.compat.LegacyGL.glDisable(GL11.GL_BLEND);
        cn.kazer.client189.compat.LegacyGL.glEnable(GL11.GL_CULL_FACE);
        cn.kazer.client189.compat.LegacyGL.glDepthMask(true);
        cn.kazer.client189.compat.LegacyGL.glColorMask(true, true, true, true);
    }

    /**
     * Blend factors for the trail overlay (spec: client-ui-contracts 顶点色陷阱).
     * Contract: never SRC_ALPHA here — see the Moulberry freeze regression.
     */
    /** OpenGL core GL_CONSTANT_ALPHA (0x8003); LWJGL 2.9.1's GL14 misses the named constant. */
    static final int GL_CONSTANT_ALPHA_BLEND = 0x8003;
    /** OpenGL core GL_ONE_MINUS_CONSTANT_ALPHA (0x8004)。 */
    static final int GL_ONE_MINUS_CONSTANT_ALPHA_BLEND = 0x8004;

    static int moulberryBlendSrcFactor() {
        return GL_CONSTANT_ALPHA_BLEND;
    }

    static int moulberryBlendDstFactor() {
        return GL_ONE_MINUS_CONSTANT_ALPHA_BLEND;
    }

    /** Pass-through helper: make "previous" identical to the freshly rendered frame. */
    private void captureCurrentFrameAsPrevious(Minecraft mc, int width, int height) {
        if (OpenGlHelper.isFramebufferEnabled()) {
            this.bindTarget(this.prevFramebuffer, width, height);
            blitFullscreen(mc.getFramebuffer().framebufferTexture, width, height, false, 1.0F);
        } else {
            OpenGlHelper.glBindFramebuffer(OpenGlHelper.GL_FRAMEBUFFER, 0);
            cn.kazer.client189.compat.LegacyGL.glBindTexture(GL11.GL_TEXTURE_2D, this.prevTexture);
            GL11.glCopyTexSubImage2D(GL11.GL_TEXTURE_2D, 0, 0, 0, 0, 0, width, height);
            cn.kazer.client189.compat.LegacyGL.glBindTexture(GL11.GL_TEXTURE_2D, 0);
        }
    }

    private void renderMoulberry(Minecraft mc, int width, int height, float alpha) {
        boolean mainFbo = OpenGlHelper.isFramebufferEnabled();
        int currentTextureId;
        if (mainFbo) {
            currentTextureId = mc.getFramebuffer().framebufferTexture;
        } else {
            OpenGlHelper.glBindFramebuffer(OpenGlHelper.GL_FRAMEBUFFER, 0);
            GL11.glViewport(0, 0, width, height);
            cn.kazer.client189.compat.LegacyGL.glBindTexture(GL11.GL_TEXTURE_2D, this.currentTexture);
            GL11.glCopyTexSubImage2D(GL11.GL_TEXTURE_2D, 0, 0, 0, 0, 0, width, height);
            currentTextureId = this.currentTexture;
        }
        // Accumulate: new frame into the offscreen buffer, previous accumulation
        // layered over it with the trail alpha (Poly's blurBufferInto/blurBufferMain dance).
        this.bindTarget(this.swapFramebuffer, width, height);
        blitFullscreen(currentTextureId, width, height, false, 1.0F);
        blitFullscreen(this.prevTexture, width, height, true, alpha);
        if (mainFbo) {
            OpenGlHelper.glBindFramebuffer(OpenGlHelper.GL_FRAMEBUFFER, mc.getFramebuffer().framebufferObject);
            GL11.glViewport(0, 0, width, height);
        } else {
            OpenGlHelper.glBindFramebuffer(OpenGlHelper.GL_FRAMEBUFFER, 0);
            GL11.glViewport(0, 0, width, height);
        }
        // Poly draws the result with color alpha = trail + 1.0 under
        // blendFunc(srcAlpha, 1-srcAlpha); alpha > 1 clamps to an opaque copy.
        blitFullscreen(this.swapTexture, width, height, false, 1.0F);
        // Fresh accumulation becomes next frame's "previous".
        int texture = this.prevTexture;
        this.prevTexture = this.swapTexture;
        this.swapTexture = texture;
        int framebuffer = this.prevFramebuffer;
        this.prevFramebuffer = this.swapFramebuffer;
        this.swapFramebuffer = framebuffer;
    }

    private void bindTarget(int framebuffer, int width, int height) {
        OpenGlHelper.glBindFramebuffer(OpenGlHelper.GL_FRAMEBUFFER, framebuffer);
        GL11.glViewport(0, 0, width, height);
    }

    private void createBuffers(int width, int height) {
        this.prevTexture = createColorTexture(width, height);
        this.prevFramebuffer = createFramebufferFor(this.prevTexture);
        this.swapTexture = createColorTexture(width, height);
        this.swapFramebuffer = createFramebufferFor(this.swapTexture);
        this.currentTexture = createColorTexture(width, height);
        OpenGlHelper.glBindFramebuffer(OpenGlHelper.GL_FRAMEBUFFER, 0);
    }

    private static int createColorTexture(int width, int height) {
        int texture = TextureUtil.glGenTextures();
        cn.kazer.client189.compat.LegacyGL.glBindTexture(GL11.GL_TEXTURE_2D, texture);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_NEAREST);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_NEAREST);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, 10496);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, 10496);
        GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0, GL11.GL_RGBA8, width, height, 0, GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, (ByteBuffer) null);
        cn.kazer.client189.compat.LegacyGL.glBindTexture(GL11.GL_TEXTURE_2D, 0);
        return texture;
    }

    private static int createFramebufferFor(int texture) {
        int framebuffer = OpenGlHelper.glGenFramebuffers();
        OpenGlHelper.glBindFramebuffer(OpenGlHelper.GL_FRAMEBUFFER, framebuffer);
        OpenGlHelper.glFramebufferTexture2D(OpenGlHelper.GL_FRAMEBUFFER, OpenGlHelper.GL_COLOR_ATTACHMENT0, GL11.GL_TEXTURE_2D, texture, 0);
        int status = OpenGlHelper.glCheckFramebufferStatus(OpenGlHelper.GL_FRAMEBUFFER);
        if (status != OpenGlHelper.GL_FRAMEBUFFER_COMPLETE) {
            throw new IllegalStateException("Motion blur framebuffer incomplete, status " + status);
        }
        return framebuffer;
    }

    private void deleteBuffers() {
        if (this.prevFramebuffer >= 0) {
            OpenGlHelper.glDeleteFramebuffers(this.prevFramebuffer);
            this.prevFramebuffer = -1;
        }
        if (this.swapFramebuffer >= 0) {
            OpenGlHelper.glDeleteFramebuffers(this.swapFramebuffer);
            this.swapFramebuffer = -1;
        }
        if (this.prevTexture >= 0) {
            TextureUtil.deleteTexture(this.prevTexture);
            this.prevTexture = -1;
        }
        if (this.swapTexture >= 0) {
            TextureUtil.deleteTexture(this.swapTexture);
            this.swapTexture = -1;
        }
        if (this.currentTexture >= 0) {
            TextureUtil.deleteTexture(this.currentTexture);
            this.currentTexture = -1;
        }
    }
}
