package com.cheatbreaker.client.ui.util;

import com.cheatbreaker.client.ui.util.font.CBFontRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.opengl.GL11;

/** Menu-only footer; bold KAZER and regular CLIENT share the existing UI typeface. */
public final class KazerMenuBranding {
    private static CBFontRenderer bold, regular;
    private static final ResourceLocation LOGO=new ResourceLocation("client/kazer_logo.png");
    private KazerMenuBranding() {}
    public static void draw(int width,int height) {
        Minecraft mc=Minecraft.getMinecraft();
        if(mc.theWorld==null||mc.currentScreen==null||mc.currentScreen instanceof net.minecraft.client.gui.GuiChat) return;
        if(mc.currentScreen instanceof com.cheatbreaker.client.kazerui.gui.KazerClickGui
                ||mc.currentScreen instanceof com.cheatbreaker.client.kazerui.gui.KazerMovementScreen) {
            com.cheatbreaker.client.CheatBreaker.getInstance().playRegular14px.drawMixedString(
                    com.cheatbreaker.client.util.KazerBuildInfo.footer("1.8.9"),8,height-12,0xFFAAAAAA);
            return;
        }
        String position=com.cheatbreaker.client.kazerui.module.ModuleManager.model()==null ? "above" :
                com.cheatbreaker.client.kazerui.module.ModuleManager.model().value("global","inventoryBrand","above");
        if(position.equals("hidden")) return;
        if(bold==null){bold=new CBFontRenderer(new ResourceLocation("client/font/Ubuntu-B.ttf"),20);regular=new CBFontRenderer(new ResourceLocation("client/font/Ubuntu-R.ttf"),20);}
        float first=bold.getMixedStringWidth("KAZER"), second=regular.getMixedStringWidth(" CLIENT");
        float x=width-first-second-8, y=height-22;
        if(position.equals("above") && mc.currentScreen instanceof com.cheatbreaker.client.kazerui.gui.ContainerBounds) {
            com.cheatbreaker.client.kazerui.gui.ContainerBounds bounds=(com.cheatbreaker.client.kazerui.gui.ContainerBounds)mc.currentScreen;
            // Centre the logo and both words as one group, not just the text.
            x=Math.max(29,Math.min(width-first-second-8,bounds.kazerLeft()+(bounds.kazerWidth()-first-second-29)/2f+29));
            // Creative inventory tabs extend 28 GUI pixels above the base container.
            int tabs = mc.currentScreen instanceof net.minecraft.client.gui.inventory.GuiContainerCreative ? 28 : 0;
            y=Math.max(8,bounds.kazerTop()-tabs-24);
        }
        cn.kazer.client189.compat.LegacyGL.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS);
        GL11.glPushMatrix();
        try {
            cn.kazer.client189.compat.LegacyGL.glDisable(GL11.GL_DEPTH_TEST);cn.kazer.client189.compat.LegacyGL.glDisable(GL11.GL_LIGHTING);cn.kazer.client189.compat.LegacyGL.glDepthMask(false);
            cn.kazer.client189.compat.LegacyGL.glEnable(GL11.GL_BLEND);cn.kazer.client189.compat.LegacyGL.glBlendFunc(GL11.GL_SRC_ALPHA,GL11.GL_ONE_MINUS_SRC_ALPHA);
            cn.kazer.client189.compat.LegacyGL.glColor4f(1,1,1,1);
            RenderUtil.lIIIIlIIllIIlIIlIIIlIIllI(LOGO,x-29,y-7,25,25);
            bold.drawMixedString("KAZER",x,y,0xFFFFFFFF);
            regular.drawMixedString(" CLIENT",x+first,y,0xFFFFFFFF);
            com.cheatbreaker.client.CheatBreaker.getInstance().playRegular14px.drawMixedString(
                    com.cheatbreaker.client.util.KazerBuildInfo.footer("1.8.9"),8,height-12,0xFFAAAAAA);
        } finally {GL11.glPopMatrix();cn.kazer.client189.compat.LegacyGL.glPopAttrib();}
    }
}
