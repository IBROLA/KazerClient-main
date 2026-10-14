package com.cheatbreaker.client.module.type;

import com.cheatbreaker.client.config.Setting;
import com.cheatbreaker.client.event.type.GuiDrawEvent;
import com.cheatbreaker.client.module.AbstractModule;
import com.cheatbreaker.client.ui.module.CBGuiAnchor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import org.lwjgl.opengl.GL11;

public class FPSModule extends AbstractModule {

    private final Setting showBackground;
    private final Setting reverseOrder;
    private final Setting textShadow;
    private final Setting textColor;
    private final Setting backgroundColor;
    private final CounterText counterText = new CounterText("FPS");

    public FPSModule() {
        super("FPS");
        this.setDefaultAnchor(CBGuiAnchor.MIDDLE_TOP);
        this.setDefaultTranslations(0.0f, 12.0f);
        this.setDefaultState(true);
        this.reverseOrder = new Setting(this, "Reverse Order").setValue(false);
        this.textShadow = new Setting(this, "Text Shadow").setValue(true);
        this.showBackground = new Setting(this, "Show Background").setValue(true);
        this.textColor = new Setting(this, "Text Color").setValue(-1).setMinMax(Integer.MIN_VALUE, Integer.MAX_VALUE);
        this.backgroundColor = new Setting(this, "Background Color").setValue(0x6F000000).setMinMax(Integer.MIN_VALUE, Integer.MAX_VALUE);
        this.setPreviewLabel("[144 FPS]", 1.6978723f * 0.8245614f);
        this.addEvent(GuiDrawEvent.class, this::onRender);
    }
    private void onRender(GuiDrawEvent drawEvent) {
        if (!this.isRenderHud()) {
            return;
        }
        boolean background = (Boolean) this.showBackground.getValue();
        String string = counterText.text(Minecraft.getDebugFPS(), background,
                (Boolean)this.reverseOrder.getValue(), true);
        int textWidth = this.minecraft.fontRendererObj.getStringWidth(string);
        this.setDimensions(background ? 56F : textWidth,
                this.minecraft.fontRendererObj.FONT_HEIGHT + (background ? 4F : 0F));
        int textY = this.getTextOffsetY();
        GL11.glPushMatrix();
        this.scaleAndTranslate(drawEvent.getResolution());
        if (background) {
            cn.kazer.client189.compat.LegacyGui.drawRect(0.0f, 0.0f, this.width, this.height, this.backgroundColor.getColorValue());
            this.minecraft.fontRendererObj.drawString(string, (int)(this.width / 2.0f - (float)(textWidth / 2)), textY, this.textColor.getColorValue());
        } else {
            this.minecraft.fontRendererObj.drawString(string, 0, textY, this.textColor.getColorValue(), (Boolean)this.textShadow.getValue());
        }
        GL11.glPopMatrix();
    }

    float[] getContentDimensions(int textWidth, int fontHeight) {
        return (Boolean) this.showBackground.getValue()
                ? new float[]{56.0F, fontHeight + 4.0F}
                : new float[]{textWidth, fontHeight};
    }

    int getTextOffsetY() {
        return (Boolean) this.showBackground.getValue() ? 3 : 0;
    }
}
