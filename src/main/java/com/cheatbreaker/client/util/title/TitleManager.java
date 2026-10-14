package com.cheatbreaker.client.util.title;

import com.cheatbreaker.client.CheatBreaker;
import com.cheatbreaker.client.event.type.GuiDrawEvent;
import com.cheatbreaker.client.event.type.TickEvent;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.renderer.OpenGlHelper;
import org.lwjgl.opengl.GL13;
import com.google.common.collect.Lists;
import net.minecraft.client.Minecraft;
import org.lwjgl.opengl.GL11;

import java.awt.*;
import java.util.List;

public class TitleManager {
    private final Minecraft lIIIIlIIllIIlIIlIIIlIIllI = Minecraft.getMinecraft();
    private final CheatBreaker lIIIIIIIIIlIllIIllIlIIlIl = CheatBreaker.getInstance();
    private final List<Title> IlllIIIlIlllIllIlIIlllIlI = Lists.newArrayList();

    public void lIIIIlIIllIIlIIlIIIlIIllI(GuiDrawEvent lIllIllIlIIllIllIlIlIIlIl2) {
        CheatBreaker client = CheatBreaker.getInstance();
        if (this.IlllIIIlIlllIllIlIIlllIlI.isEmpty() && client.getNetHandler().getApollo().titles().isEmpty()) return;
        List<Title> visible = new java.util.ArrayList<>(this.IlllIIIlIlllIllIlIIlllIlI);
        visible.addAll(client.getNetHandler().getApollo().titles());
        if (visible.isEmpty()) return;
        int matrixMode = GL11.glGetInteger(GL11.GL_MATRIX_MODE);
        int activeTexture = GL11.glGetInteger(GL13.GL_ACTIVE_TEXTURE);
        cn.kazer.client189.compat.LegacyGL.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS);
        GL11.glMatrixMode(GL11.GL_MODELVIEW);
        try {
            cn.kazer.client189.compat.LegacyGL.glEnable(GL11.GL_BLEND);
            cn.kazer.client189.compat.LegacyGL.glDisable(GL11.GL_ALPHA_TEST);
            cn.kazer.client189.compat.LegacyGL.glDisable(GL11.GL_DEPTH_TEST);
            cn.kazer.client189.compat.LegacyGL.glDepthMask(false);
            cn.kazer.client189.compat.LegacyGL.glDisable(GL11.GL_LIGHTING);
            OpenGlHelper.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA, 1, 0);
            // The Minecraft glyph atlas stays crisp at title scale; a small TTF atlas did not.
            FontRenderer font = lIIIIlIIllIIlIIlIIIlIIllI.fontRendererObj;
            long now = System.currentTimeMillis();
            for (Title title : visible) {
                boolean primary=title.getTitleEnum()==Title.TitleType.title;
                if(!com.cheatbreaker.client.kazerui.module.modules.hud.ModuleTitles.show(primary))continue;
                float alpha = title.alphaAt(now);
                float scale = title.scaleAt(now) * (title.getTitleEnum() == Title.TitleType.title ? 4F : 1.5F);
                scale*=com.cheatbreaker.client.kazerui.module.modules.hud.ModuleTitles.multiplier(primary);
                if (alpha <= 0 || scale <= 0 || Float.isNaN(scale) || Float.isInfinite(scale)) continue;
                // Vanilla treats alpha 0..3 as an unspecified (opaque) color.
                int color = (Math.max(4, (int)(255 * alpha)) << 24) | 0xFFFFFF;
                float offset = title.getTitleEnum() == Title.TitleType.title ? -30F : 10F;
                GL11.glPushMatrix();
                try {
                    GL11.glTranslatef(lIllIllIlIIllIllIlIlIIlIl2.getResolution().getScaledWidth() / 2F,
                            lIllIllIlIIllIllIlIlIIlIl2.getResolution().getScaledHeight() / 2F + offset, 0F);
                    GL11.glScalef(scale, scale, scale);
                    font.drawStringWithShadow(title.getMessage(), -font.getStringWidth(title.getMessage()) / 2F,
                            -font.FONT_HEIGHT / 2F, color);
                } finally { GL11.glPopMatrix(); }
            }
        } finally {
            cn.kazer.client189.compat.LegacyGL.glPopAttrib();
            OpenGlHelper.setActiveTexture(activeTexture);
            GL11.glMatrixMode(matrixMode);
        }
    }

    public void lIIIIlIIllIIlIIlIIIlIIllI(TickEvent cBTickEvent) {
        if (!this.IlllIIIlIlllIllIlIIlllIlI.isEmpty()) {
            this.IlllIIIlIlllIllIlIIlllIlI.removeIf(title -> title.expired(System.currentTimeMillis()));
        }
        CheatBreaker.getInstance().getNetHandler().getApollo().tick(System.currentTimeMillis());
    }

    public List<Title> lIIIIlIIllIIlIIlIIIlIIllI() {
        return this.IlllIIIlIlllIllIlIIlllIlI;
    }
}
 
