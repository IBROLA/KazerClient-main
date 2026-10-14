package com.cheatbreaker.client.ui.element.module;

import com.cheatbreaker.client.CheatBreaker;
import com.cheatbreaker.client.ui.element.AbstractModulesGuiElement;
import com.cheatbreaker.client.ui.element.AbstractScrollableElement;
import com.cheatbreaker.client.ui.module.CBModulesGui;
import com.cheatbreaker.client.ui.util.RenderUtil;
import com.cheatbreaker.client.ui.util.font.CBFontRenderer;
import net.minecraft.client.gui.Gui;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.opengl.GL11;

public class ModulesGuiButtonElement
        extends AbstractModulesGuiElement {
    public int lIIIIlIIllIIlIIlIIIlIIllI;
    public String displayString;
    public final AbstractScrollableElement lIIIIllIIlIlIllIIIlIllIlI;
    private final CBFontRenderer fontRenderer;
    public boolean IlllIllIlIIIIlIIlIIllIIIl = true;
    private int llIIlllIIIIlllIllIlIlllIl = 0;
    /**
     * Corner radius for the hover highlight. 0 keeps the legacy square highlight.
     * A button flush with a rounded container corner (e.g. the back arrow) sets the
     * container radius so the highlight's corner arc is concentric with the panel
     * edge and the rounding stays visible while hovered.
     */
    private int hoverCornerRadius = 0;
    private boolean kazerLandingStyle;
    private float opacity = 1.0F;

    public ModulesGuiButtonElement useKazerLandingStyle() {
        this.kazerLandingStyle = true;
        return this;
    }

    public ModulesGuiButtonElement setOpacity(float opacity) {
        this.opacity = Math.max(0.0F, Math.min(1.0F, opacity));
        return this;
    }

    private int withOpacity(int color) {
        int alpha = Math.round((color >>> 24) * this.opacity);
        return color & 0x00FFFFFF | alpha << 24;
    }

    public ModulesGuiButtonElement setHoverCornerRadius(int radius) {
        this.hoverCornerRadius = radius;
        return this;
    }

    /** Square hover fill inset 2px in every direction; 4 numbers in drawRect order. */
    static float[] hoverFill(float x, float y, int width, int height) {
        return new float[] { x - 2.0F, y - 2.0F, x + (float) width + 2.0F, y + (float) height + 2.0F };
    }

    public int getHoverCornerRadius() {
        return this.hoverCornerRadius;
    }

    public ModulesGuiButtonElement(CBFontRenderer fontRenderer, AbstractScrollableElement lllIllIllIlIllIlIIllllIIl2, String displayString, int n, int n2, int n3, int n4, int n5, float f) {
        super(f);
        this.displayString = displayString;
        this.setDimensions(n, n2, n3, n4);
        this.lIIIIlIIllIIlIIlIIIlIIllI = n5;
        this.lIIIIllIIlIlIllIIIlIllIlI = lllIllIllIlIllIlIIllllIIl2;
        this.fontRenderer = fontRenderer;
    }

    public ModulesGuiButtonElement(AbstractScrollableElement lllIllIllIlIllIlIIllllIIl2, String string, int n, int n2, int n3, int n4, int n5, float f) {
        this(CheatBreaker.getInstance().playBold22px, lllIllIllIlIllIlIIllllIIl2, string, n, n2, n3, n4, n5, f);
    }

    @Override
    public void handleDrawElement(int mouseX, int mouseY, float partialTicks) {
        float f2;
        boolean bl = this.isMouseInside(mouseX, mouseY);
        if (this.kazerLandingStyle) {
            RenderUtil.lIIIIlIIllIIlIIlIIIlIIllI(this.x, this.y, this.x + this.width,
                    this.y + this.height, 5.0D, withOpacity(bl ? 0xCC666A67 : 0xAA4A4E4B));
            RenderUtil.lIIIIlIIllIIlIIlIIIlIIllI(this.x + 1, this.y + 1, this.x + this.width - 1,
                    this.y + this.height - 1, 4.0D, withOpacity(bl ? 0xC72A302D : 0xB0151A17));
            this.fontRenderer.drawCenteredInRect(this.displayString.toUpperCase(),
                    this.x, this.y, this.width, this.height,
                    withOpacity(0xFFFFFFFF));
            return;
        }
        int n3 = 120;
        if (bl && this.IlllIllIlIIIIlIIlIIllIIIl) {
            float[] hoverFill = hoverFill(this.x, this.y, this.width, this.height);
            if (this.hoverCornerRadius > 0) {
                RenderUtil.lIIIIlIIllIIlIIlIIIlIIllI((double)hoverFill[0], (double)hoverFill[1], (double)hoverFill[2], (double)hoverFill[3], (double)this.hoverCornerRadius, -854025);
            } else {
                cn.kazer.client189.compat.LegacyGui.drawRect(hoverFill[0], hoverFill[1], hoverFill[2], hoverFill[3], -854025);
            }
            f2 = CBModulesGui.getSmoothFloat(790);
            this.llIIlllIIIIlllIllIlIlllIl = (float)this.llIIlllIIIIlllIllIlIlllIl + f2 < (float)n3 ? (int)((float)this.llIIlllIIIIlllIllIlIlllIl + f2) : n3;
        } else if (this.llIIlllIIIIlllIllIlIlllIl > 0) {
            f2 = CBModulesGui.getSmoothFloat(790);
            this.llIIlllIIIIlllIllIlIlllIl = (float)this.llIIlllIIIIlllIllIlIlllIl - f2 < 0.0f ? 0 : (int)((float)this.llIIlllIIIIlllIllIlIlllIl - f2);
        }
        if (this.IlllIllIlIIIIlIIlIIllIIIl) {
            cn.kazer.client189.compat.LegacyGui.drawRect(this.x, this.y, this.x + this.width, this.y + this.height, -723724);
        } else {
            cn.kazer.client189.compat.LegacyGui.drawRect(this.x, this.y, this.x + this.width, this.y + this.height, -1611336460);
        }
        if (this.llIIlllIIIIlllIllIlIlllIl > 0) {
            f2 = (float)this.llIIlllIIIIlllIllIlIlllIl / (float)n3 * (float)100;
            cn.kazer.client189.compat.LegacyGui.drawRect(this.x, (int)((float)this.y + ((float)this.height - (float)this.height * f2 / (float)100)), this.x + this.width, this.y + this.height, this.lIIIIlIIllIIlIIlIIIlIIllI);
        }
        if (this.displayString.contains(".png")) {
            GL11.glPushMatrix();
            cn.kazer.client189.compat.LegacyGL.glColor4f(0.0f, 0.0f, 0.0f, 0.47368422f * 0.9499999f);
            RenderUtil.drawIcon(new ResourceLocation("client/icons/" + this.displayString), (float)8, (float)(this.x + 6), (float)(this.y + 6));
            GL11.glPopMatrix();
        } else {
            // IIIllIllIlIlllllllIlIlIII = Fontrenderer
            f2 = this.fontRenderer == CheatBreaker.getInstance().playBold22px ? 2.0f : 0.54545456f * 0.9166667f;
            this.fontRenderer.drawCenteredString(this.displayString.toUpperCase(), this.x + this.width / 2, (float)(this.y + this.height / 2 - this.fontRenderer.getHeight()) + f2, 0x6F000000);
        }
    }

    @Override
    public void handleMouseClick(int mouseX, int mouseY, int button) {
    }

    public int lIIIIIIIIIlIllIIllIlIIlIl() {
        return this.width;
    }

    public void lIIIIlIIllIIlIIlIIIlIIllI(boolean bl) {
        this.IlllIllIlIIIIlIIlIIllIIIl = bl;
    }
}
