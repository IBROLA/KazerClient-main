package cn.kazer.client189.compat;

import net.minecraft.client.renderer.GlStateManager;
import org.lwjgl.opengl.GL11;

/** Legacy renderers must cooperate with 1.8's cached OpenGL state. */
public final class LegacyGL {
    private static final int[] CAPS = {GL11.GL_BLEND, GL11.GL_ALPHA_TEST, GL11.GL_DEPTH_TEST,
            GL11.GL_CULL_FACE, GL11.GL_LIGHTING, GL11.GL_FOG, 32826};
    // All render calls are on the GL thread; avoid allocating native buffers per nametag/HUD.
    private static final java.nio.FloatBuffer COLOR = org.lwjgl.BufferUtils.createFloatBuffer(16);
    private static final java.nio.ByteBuffer MASK = org.lwjgl.BufferUtils.createByteBuffer(16);
    private LegacyGL() { }
    public static void glEnable(int cap) { capability(cap, true); }
    public static void glDisable(int cap) { capability(cap, false); }
    private static void capability(int cap, boolean enabled) {
        switch (cap) {
            case GL11.GL_BLEND: if (enabled) GlStateManager.enableBlend(); else GlStateManager.disableBlend(); break;
            case GL11.GL_TEXTURE_2D: if (enabled) GlStateManager.enableTexture2D(); else GlStateManager.disableTexture2D(); break;
            case GL11.GL_ALPHA_TEST: if (enabled) GlStateManager.enableAlpha(); else GlStateManager.disableAlpha(); break;
            case GL11.GL_DEPTH_TEST: if (enabled) GlStateManager.enableDepth(); else GlStateManager.disableDepth(); break;
            case GL11.GL_CULL_FACE: if (enabled) GlStateManager.enableCull(); else GlStateManager.disableCull(); break;
            case GL11.GL_LIGHTING: if (enabled) GlStateManager.enableLighting(); else GlStateManager.disableLighting(); break;
            case GL11.GL_FOG: if (enabled) GlStateManager.enableFog(); else GlStateManager.disableFog(); break;
            case 32826: if (enabled) GlStateManager.enableRescaleNormal(); else GlStateManager.disableRescaleNormal(); break;
            default: if (enabled) GL11.glEnable(cap); else GL11.glDisable(cap);
        }
    }
    public static void glColor4f(float r, float g, float b, float a) { GlStateManager.color(r,g,b,a); }
    public static void glColor3f(float r, float g, float b) { GlStateManager.color(r,g,b,1); }
    public static void glBindTexture(int target, int texture) { if (target == GL11.GL_TEXTURE_2D) GlStateManager.bindTexture(texture); else GL11.glBindTexture(target, texture); }
    public static void glDepthMask(boolean value) { GlStateManager.depthMask(value); }
    public static void glBlendFunc(int source, int destination) { GlStateManager.blendFunc(source, destination); }
    public static void glAlphaFunc(int func, float ref) { GlStateManager.alphaFunc(func, ref); }
    public static void glColorMask(boolean r, boolean g, boolean b, boolean a) { GlStateManager.colorMask(r,g,b,a); }
    private static final java.util.List<GlCacheSnapshot> SNAPSHOTS=new java.util.ArrayList<>();
    private static final java.util.List<Integer> MASKS=new java.util.ArrayList<>();
    private static int stackDepth;
    public static void glPushAttrib(int mask) {
        if(stackDepth==SNAPSHOTS.size()){SNAPSHOTS.add(new GlCacheSnapshot());MASKS.add(0);}
        // Stack-depth masks are stable across frames. Reuse their boxed values
        // rather than allocate an Integer for every GL_ALL_ATTRIB_BITS guard.
        if(MASKS.get(stackDepth).intValue()!=mask)MASKS.set(stackDepth,mask);
        if(mask==GL11.GL_ALL_ATTRIB_BITS)SNAPSHOTS.get(stackDepth).capture();
        stackDepth++;GL11.glPushAttrib(mask);
    }
    public static void glPopAttrib() {
        GL11.glPopAttrib();
        if(stackDepth==0){sync();return;}
        int mask=MASKS.get(--stackDepth);
        if(mask==GL11.GL_ALL_ATTRIB_BITS)SNAPSHOTS.get(stackDepth).restore();
        else if(mask!=0)sync(); // Partial attribute masks retain the conservative fallback.
    }
    /** Synchronize the cache after old renderers restore driver state with glPopAttrib. */
    public static void sync() {
        int active=GL11.glGetInteger(org.lwjgl.opengl.GL13.GL_ACTIVE_TEXTURE);
        // ShaderManager touches both diffuse and lightmap units. Driver restores do not update MC's cache.
        GlStateManager.setActiveTexture(org.lwjgl.opengl.GL13.GL_TEXTURE0+7);
        for(int unit=0;unit<3;unit++) {
            GlStateManager.setActiveTexture(org.lwjgl.opengl.GL13.GL_TEXTURE0+unit);
            boolean enabled=GL11.glIsEnabled(GL11.GL_TEXTURE_2D);
            capability(GL11.GL_TEXTURE_2D,!enabled);capability(GL11.GL_TEXTURE_2D,enabled);
            int texture=GL11.glGetInteger(GL11.GL_TEXTURE_BINDING_2D);
            GlStateManager.bindTexture(0);GlStateManager.bindTexture(texture);
        }
        GlStateManager.setActiveTexture(active);
        for (int cap : CAPS) { boolean on = GL11.glIsEnabled(cap); capability(cap, !on); capability(cap, on); }
        java.nio.FloatBuffer color = COLOR; color.clear();
        GL11.glGetFloat(GL11.GL_CURRENT_COLOR, color);
        GlStateManager.resetColor(); GlStateManager.color(color.get(0),color.get(1),color.get(2),color.get(3));
        boolean depth = GL11.glGetBoolean(GL11.GL_DEPTH_WRITEMASK);
        GlStateManager.depthMask(!depth); GlStateManager.depthMask(depth);
        int sr=GL11.glGetInteger(org.lwjgl.opengl.GL14.GL_BLEND_SRC_RGB),dr=GL11.glGetInteger(org.lwjgl.opengl.GL14.GL_BLEND_DST_RGB);
        int sa=GL11.glGetInteger(org.lwjgl.opengl.GL14.GL_BLEND_SRC_ALPHA),da=GL11.glGetInteger(org.lwjgl.opengl.GL14.GL_BLEND_DST_ALPHA);
        GlStateManager.tryBlendFuncSeparate(sr==0?1:0,dr==0?1:0,sa==0?1:0,da==0?1:0);
        GlStateManager.tryBlendFuncSeparate(sr,dr,sa,da);
        int func=GL11.glGetInteger(GL11.GL_DEPTH_FUNC);GlStateManager.depthFunc(func==GL11.GL_ALWAYS?GL11.GL_LEQUAL:GL11.GL_ALWAYS);GlStateManager.depthFunc(func);
        java.nio.ByteBuffer mask=MASK;mask.clear();GL11.glGetBoolean(GL11.GL_COLOR_WRITEMASK,mask);
        boolean r=mask.get(0)!=0,g=mask.get(1)!=0,b=mask.get(2)!=0,a=mask.get(3)!=0;
        GlStateManager.colorMask(!r,!g,!b,!a);GlStateManager.colorMask(r,g,b,a);
    }
}
