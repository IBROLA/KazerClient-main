package com.cheatbreaker.client.ui.element.type;

import com.cheatbreaker.client.CheatBreaker;
import com.cheatbreaker.client.config.Setting;
import com.cheatbreaker.client.ui.element.AbstractModulesGuiElement;
import com.cheatbreaker.client.ui.module.CBModulesGui;
import com.cheatbreaker.client.ui.util.RenderUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.PositionedSoundRecord;
import net.minecraft.client.gui.Gui;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.opengl.GL11;

public class ToggleElement
        extends AbstractModulesGuiElement {
    private Setting setting;
    private ResourceLocation IllIIIIIIIlIlIllllIIllIII = new ResourceLocation("client/icons/left.png");
    private ResourceLocation lIIIIllIIlIlIllIIIlIllIlI = new ResourceLocation("client/icons/right.png");
    private int IlllIllIlIIIIlIIlIIllIIIl = 0;
    private float IlIlllIIIIllIllllIllIIlIl = 0.0f;
    private String displayString;

    public ToggleElement(Setting cBSetting, float f) {
        super(f);
        this.setting = cBSetting;
        this.height = 12;
    }

    @Override
    public void handleDrawElement(int mouseX, int mouseY, float partialTicks) {
        int switchX = this.x + this.width - 40;
        boolean hovered = (float)mouseX >= switchX * this.scale
                && (float)mouseX < (switchX + 30) * this.scale
                && (float)mouseY >= (this.y + this.yOffset) * this.scale
                && (float)mouseY < (this.y + this.yOffset + 10) * this.scale;
        CheatBreaker.getInstance().ubuntuMedium16px.drawString(this.setting.getDisplayLabel(), this.x + 10,
                this.y + 2, hovered ? 0xFFEDF2EF : 0xFFB6BDB8);
        boolean enabled = (Boolean)this.setting.getValue();
        int color = enabled ? 0xFF36AD72 : 0xFFB52E55;
        if (hovered) color = enabled ? 0xFF42C982 : 0xFFCF3862;
        RenderUtil.lIIIIlIIllIIlIIlIIIlIIllI(switchX, this.y + 1, switchX + 30, this.y + 12, 2D, 0xFF787D79);
        RenderUtil.lIIIIlIIllIIlIIlIIIlIIllI(switchX + 1, this.y + 2, switchX + 29, this.y + 11, 2D, 0xFF454C47);
        int thumbX = switchX + (enabled ? 10 : 1);
        RenderUtil.lIIIIlIIllIIlIIlIIIlIIllI(thumbX, this.y + 2, thumbX + 19, this.y + 11, 2D, color);
        CheatBreaker.getInstance().robotoBold14px.drawCenteredInRect(enabled ? "ON" : "OFF",
                thumbX, this.y + 2, 19, 9, 0xFFFFFFFF);
    }

    @Override
    public void handleMouseClick(int mouseX, int mouseY, int button) {
        int switchX = this.x + this.width - 40;
        boolean hit = (float)mouseX >= switchX * this.scale
                && (float)mouseX < (switchX + 30) * this.scale
                && (float)mouseY >= (this.y + this.yOffset) * this.scale
                && (float)mouseY < (this.y + this.yOffset + 10) * this.scale;
        if (hit) {
            Minecraft.getMinecraft().getSoundHandler().playSound(PositionedSoundRecord.create(new ResourceLocation("gui.button.press"), 1.0f));
            this.setting.setValue(!((Boolean) this.setting.getValue()));
            if (this.setting == CheatBreaker.getInstance().moduleManager.keyStrokes.replaceNamesWithArrows) {
                CheatBreaker.getInstance().moduleManager.keyStrokes.initialize();
            } else if (this.setting == CheatBreaker.getInstance().globalSettings.enableTeamView && !(Boolean) CheatBreaker.getInstance().globalSettings.enableTeamView.getValue()) {
                CheatBreaker.getInstance().globalSettings.enableTeamView.setValue(false);
            }
        }
    }

    @Override
    public com.cheatbreaker.client.config.Setting getSetting() {
        return this.setting;
    }
}
