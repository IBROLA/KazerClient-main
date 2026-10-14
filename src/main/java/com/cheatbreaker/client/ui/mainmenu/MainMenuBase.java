package com.cheatbreaker.client.ui.mainmenu;

import com.cheatbreaker.client.ui.AbstractGui;
import net.minecraft.client.renderer.OpenGlHelper;
import cn.kazer.client189.compat.Tessellator;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.util.MathHelper;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.opengl.GL11;
import org.lwjgl.util.glu.Project;


public class MainMenuBase extends AbstractGui {
    private static int IIIllIllIlIlllllllIlIlIII = 4100;
    private final ResourceLocation[] IllIlIlIllllIlIIllllIIlll = new ResourceLocation[]{new ResourceLocation("client/panorama/0.png"), new ResourceLocation("client/panorama/1.png"), new ResourceLocation("client/panorama/2.png"), new ResourceLocation("client/panorama/3.png"), new ResourceLocation("client/panorama/4.png"), new ResourceLocation("client/panorama/5.png")};
    private ResourceLocation panoramaBackgroundLocation;
    private int lastPanoramaTick = -1;

    @Override
    public void handleMouseInput() throws java.io.IOException {
        super.handleMouseInput();
    }

    @Override
    public void updateScreen() {
        super.updateScreen();
        ++IIIllIllIlIlllllllIlIlIII;
    }

    @Override
    public void initGui() {
        super.initGui();
        if (this.panoramaBackgroundLocation == null) {
            DynamicTexture texture = new DynamicTexture(256, 256);
            this.panoramaBackgroundLocation = this.mc.getTextureManager().getDynamicTextureLocation("background", texture);
        }
        lastPanoramaTick = -1;
    }

    @Override
    public void drawScreen(int n, int n2, float f) {
        cn.kazer.client189.compat.LegacyGL.glDisable(0xbc0);
        this.renderSkybox(n, n2, 1.0f);
        cn.kazer.client189.compat.LegacyGL.glEnable(3008);
        super.drawScreen(n, n2, f);
    }

    @Override
    public void drawMenu(float f, float f2) {
        cn.kazer.client189.compat.LegacyGui.drawGradientRect(0.0f, 0.0f, this.getScaledWidth(), this.getScaledHeight(), 0x54000000, 0x25000000);
        cn.kazer.client189.compat.LegacyGui.drawGradientRect(0.0f, 0.0f, this.getScaledWidth(), 100.0f, 0x70000000, 0x00000000);
    }

    @Override
    public void onMouseReleased(float mouseX, float mouseY, int mouseButton) {

    }

    @Override
    public void onMouseClicked(float mouseX, float mouseY, int button) {
        // Controls are owned by the concrete menu. Authentication remains
        // launcher-owned; this screen never reads cached credentials.
    }

    private void drawPanorama(int p_73970_1_, int p_73970_2_, float p_73970_3_) {
        Tessellator var4 = Tessellator.instance;
        GL11.glMatrixMode(GL11.GL_PROJECTION);
        GL11.glPushMatrix();
        GL11.glLoadIdentity();
        Project.gluPerspective(120.0F, 1.0F, 0.05F, 10.0F);
        GL11.glMatrixMode(GL11.GL_MODELVIEW);
        GL11.glPushMatrix();
        GL11.glLoadIdentity();
        cn.kazer.client189.compat.LegacyGL.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
        GL11.glRotatef(180.0F, 1.0F, 0.0F, 0.0F);
        GL11.glRotatef(90.0F, 0.0F, 0.0F, 1.0F);
        cn.kazer.client189.compat.LegacyGL.glEnable(GL11.GL_BLEND);
        cn.kazer.client189.compat.LegacyGL.glDisable(GL11.GL_ALPHA_TEST);
        cn.kazer.client189.compat.LegacyGL.glDisable(GL11.GL_CULL_FACE);
        cn.kazer.client189.compat.LegacyGL.glDepthMask(false);
        OpenGlHelper.glBlendFunc(770, 771, 1, 0);
        byte var5 = 8;

        for (int var6 = 0; var6 < var5 * var5; ++var6) {
            GL11.glPushMatrix();
            float var7 = ((float) (var6 % var5) / (float) var5 - 0.5F) / 64.0F;
            float var8 = ((float) (var6 / var5) / (float) var5 - 0.5F) / 64.0F;
            float var9 = 0.0F;
            GL11.glTranslatef(var7, var8, var9);
            GL11.glRotatef(MathHelper.sin(((float) IIIllIllIlIlllllllIlIlIII + p_73970_3_) / 400.0F) * 25.0F + 20.0F, 1.0F, 0.0F, 0.0F);
            GL11.glRotatef(-((float) IIIllIllIlIlllllllIlIlIII + p_73970_3_) * 0.1F, 0.0F, 1.0F, 0.0F);

            for (int var10 = 0; var10 < 6; ++var10) {
                GL11.glPushMatrix();

                if (var10 == 1) {
                    GL11.glRotatef(90.0F, 0.0F, 1.0F, 0.0F);
                }

                if (var10 == 2) {
                    GL11.glRotatef(180.0F, 0.0F, 1.0F, 0.0F);
                }

                if (var10 == 3) {
                    GL11.glRotatef(-90.0F, 0.0F, 1.0F, 0.0F);
                }

                if (var10 == 4) {
                    GL11.glRotatef(90.0F, 1.0F, 0.0F, 0.0F);
                }

                if (var10 == 5) {
                    GL11.glRotatef(-90.0F, 1.0F, 0.0F, 0.0F);
                }

                this.mc.getTextureManager().bindTexture(IllIlIlIllllIlIIllllIIlll[var10]);
                var4.startDrawingQuads();
                var4.setColorRGBA_I(16777215, 255 / (var6 + 1));
                float var11 = 0.0F;
                var4.addVertexWithUV(-1.0D, -1.0D, 1.0D, 0.0F + var11, 0.0F + var11);
                var4.addVertexWithUV(1.0D, -1.0D, 1.0D, 1.0F - var11, 0.0F + var11);
                var4.addVertexWithUV(1.0D, 1.0D, 1.0D, 1.0F - var11, 1.0F - var11);
                var4.addVertexWithUV(-1.0D, 1.0D, 1.0D, 0.0F + var11, 1.0F - var11);
                var4.draw();
                GL11.glPopMatrix();
            }

            GL11.glPopMatrix();
            cn.kazer.client189.compat.LegacyGL.glColorMask(true, true, true, false);
        }

        var4.setTranslation(0.0D, 0.0D, 0.0D);
        cn.kazer.client189.compat.LegacyGL.glColorMask(true, true, true, true);
        GL11.glMatrixMode(GL11.GL_PROJECTION);
        GL11.glPopMatrix();
        GL11.glMatrixMode(GL11.GL_MODELVIEW);
        GL11.glPopMatrix();
        cn.kazer.client189.compat.LegacyGL.glDepthMask(true);
        cn.kazer.client189.compat.LegacyGL.glEnable(GL11.GL_CULL_FACE);
        cn.kazer.client189.compat.LegacyGL.glEnable(GL11.GL_DEPTH_TEST);
    }

    /**
     * Rotate and blurs the skybox view in the main menu
     */
    private void rotateAndBlurSkybox(float p_73968_1_) {
        this.mc.getTextureManager().bindTexture(this.panoramaBackgroundLocation);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR);
        GL11.glCopyTexSubImage2D(GL11.GL_TEXTURE_2D, 0, 0, 0, 0, 0, 256, 256);
        cn.kazer.client189.compat.LegacyGL.glEnable(GL11.GL_BLEND);
        OpenGlHelper.glBlendFunc(770, 771, 1, 0);
        cn.kazer.client189.compat.LegacyGL.glColorMask(true, true, true, false);
        Tessellator var2 = Tessellator.instance;
        var2.startDrawingQuads();
        cn.kazer.client189.compat.LegacyGL.glDisable(GL11.GL_ALPHA_TEST);
        byte var3 = 3;

        for (int var4 = 0; var4 < var3; ++var4) {
            var2.setColorRGBA_F(1.0F, 1.0F, 1.0F, 1.0F / (float) (var4 + 1));
            int var5 = this.width;
            int var6 = this.height;
            float var7 = (float) (var4 - var3 / 2) / 256.0F;
            var2.addVertexWithUV(var5, var6, zLevel, 0.0F + var7, 1.0D);
            var2.addVertexWithUV(var5, 0.0D, zLevel, 1.0F + var7, 1.0D);
            var2.addVertexWithUV(0.0D, 0.0D, zLevel, 1.0F + var7, 0.0D);
            var2.addVertexWithUV(0.0D, var6, zLevel, 0.0F + var7, 0.0D);
        }

        var2.draw();
        cn.kazer.client189.compat.LegacyGL.glEnable(GL11.GL_ALPHA_TEST);
        cn.kazer.client189.compat.LegacyGL.glColorMask(true, true, true, true);
    }

    /**
     * Renders the skybox in the main menu
     */
    private void renderSkybox(int p_73971_1_, int p_73971_2_, float p_73971_3_) {
        // The original angle is tick-based (partial is fixed at 1); reuse identical frames.
        if (lastPanoramaTick != IIIllIllIlIlllllllIlIlIII) {
        lastPanoramaTick = IIIllIllIlIlllllllIlIlIII;
        this.mc.getFramebuffer().unbindFramebuffer();
        GL11.glViewport(0, 0, 256, 256);
        this.drawPanorama(p_73971_1_, p_73971_2_, p_73971_3_);
        this.rotateAndBlurSkybox(p_73971_3_);
        this.rotateAndBlurSkybox(p_73971_3_);
        this.rotateAndBlurSkybox(p_73971_3_);
        this.rotateAndBlurSkybox(p_73971_3_);
        this.rotateAndBlurSkybox(p_73971_3_);
        this.rotateAndBlurSkybox(p_73971_3_);
        this.rotateAndBlurSkybox(p_73971_3_);
        this.mc.getFramebuffer().bindFramebuffer(true);
        GL11.glViewport(0, 0, this.mc.displayWidth, this.mc.displayHeight);
        }
        cn.kazer.client189.compat.LegacyGL.glEnable(GL11.GL_TEXTURE_2D);
        cn.kazer.client189.compat.LegacyGL.glColor4f(1,1,1,1);
        this.mc.getTextureManager().bindTexture(this.panoramaBackgroundLocation);
        Tessellator var4 = Tessellator.instance;
        var4.startDrawingQuads();
        float var5 = this.width > this.height ? 120.0F / (float) this.width : 120.0F / (float) this.height;
        float var6 = (float) this.height * var5 / 256.0F;
        float var7 = (float) this.width * var5 / 256.0F;
        var4.setColorRGBA_F(1.0F, 1.0F, 1.0F, 1.0F);
        int var8 = this.width;
        int var9 = this.height;
        var4.addVertexWithUV(0.0D, var9, zLevel, 0.5F - var6, 0.5F + var7);
        var4.addVertexWithUV(var8, var9, zLevel, 0.5F - var6, 0.5F - var7);
        var4.addVertexWithUV(var8, 0.0D, zLevel, 0.5F + var6, 0.5F - var7);
        var4.addVertexWithUV(0.0D, 0.0D, zLevel, 0.5F + var6, 0.5F + var7);
        var4.draw();
    }

}
