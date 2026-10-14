package com.cheatbreaker.client.module.type;

import com.cheatbreaker.client.config.Setting;
import com.cheatbreaker.client.event.type.GuiDrawEvent;
import com.cheatbreaker.client.event.type.RenderPreviewEvent;
import com.cheatbreaker.client.module.AbstractModule;
import com.cheatbreaker.client.ui.module.CBGuiAnchor;
import com.cheatbreaker.client.ui.module.CBModulePlaceGui;
import com.cheatbreaker.client.ui.module.CBModulesGui;
import net.minecraft.client.gui.Gui;
import org.lwjgl.opengl.GL11;

import java.time.Clock;

public final class ClockModule extends AbstractModule {
    private final ClockText clockText;
    private final Setting twentyFourHour;
    private final Setting showSeconds;
    private final Setting brackets;
    private final Setting showBackground;
    private final Setting textColor;
    private final Setting backgroundColor;

    public ClockModule() {
        this(Clock.systemDefaultZone());
    }

    ClockModule(Clock clock) {
        super("Clock");
        this.clockText = new ClockText(clock);
        this.setDefaultAnchor(CBGuiAnchor.RIGHT_TOP);
        this.setDefaultTranslations(-1.0F, 0.0F);
        this.setDefaultState(true);
        this.twentyFourHour = new Setting(this, "24 Hour").setValue(true);
        this.showSeconds = new Setting(this, "Show Seconds").setValue(true);
        this.brackets = new Setting(this, "Brackets").setValue(true);
        this.showBackground = new Setting(this, "Show Background").setValue(false);
        this.textColor = new Setting(this, "Text Color").setValue(0xFFFFFFFF)
                .setMinMax(Integer.MIN_VALUE, Integer.MAX_VALUE);
        this.backgroundColor = new Setting(this, "Background Color").setValue(0x6F000000)
                .setMinMax(Integer.MIN_VALUE, Integer.MAX_VALUE);
        this.setPreviewLabel("[13:05:09]", 1.5F);
        this.addEvent(GuiDrawEvent.class, this::renderHud);
        this.addEvent(RenderPreviewEvent.class, this::renderPreview);
    }

    String getDisplayText() {
        String text = this.clockText.format((Boolean)this.twentyFourHour.getValue(),
                (Boolean)this.showSeconds.getValue());
        return (Boolean)this.brackets.getValue() ? "[" + text + "]" : text;
    }

    float[] getContentDimensions(int textWidth, int fontHeight) {
        boolean background = (Boolean)this.showBackground.getValue();
        return new float[]{
                textWidth + (background ? 4.0F : 0.0F),
                background ? fontHeight + 4.0F : fontHeight
        };
    }

    int getTextOffsetY() {
        return (Boolean)this.showBackground.getValue() ? 3 : 0;
    }

    private void renderHud(GuiDrawEvent event) {
        if (!this.isRenderHud() || this.minecraft.thePlayer == null
                || this.minecraft.currentScreen instanceof CBModulesGui
                || this.minecraft.currentScreen instanceof CBModulePlaceGui) {
            return;
        }
        this.render(event);
    }

    private void renderPreview(RenderPreviewEvent event) {
        if (!this.isRenderHud()) {
            return;
        }
        if (this.minecraft.currentScreen instanceof CBModulePlaceGui
                || this.minecraft.currentScreen instanceof CBModulesGui) {
            this.render(event);
        }
    }

    private void render(GuiDrawEvent event) {
        String text = this.getDisplayText();
        boolean background = (Boolean)this.showBackground.getValue();
        int textWidth = this.minecraft.fontRendererObj.getStringWidth(text);
        float[] dimensions = this.getContentDimensions(textWidth, this.minecraft.fontRendererObj.FONT_HEIGHT);
        float contentWidth = dimensions[0];
        float contentHeight = dimensions[1];
        this.setDimensions(contentWidth, contentHeight);
        float padding = background ? 2.0F : 0.0F;
        int textY = this.getTextOffsetY();
        boolean depthEnabled = GL11.glIsEnabled(GL11.GL_DEPTH_TEST);
        GL11.glPushMatrix();
        cn.kazer.client189.compat.LegacyGL.glDisable(GL11.GL_DEPTH_TEST);
        try {
            this.scaleAndTranslate(event.getResolution());
            if (background) {
                cn.kazer.client189.compat.LegacyGui.drawRect(0.0F, 0.0F, contentWidth, contentHeight,
                        this.backgroundColor.getColorValue());
                this.minecraft.fontRendererObj.drawString(text, (int)padding, textY,
                        this.textColor.getColorValue());
            } else {
                this.minecraft.fontRendererObj.drawStringWithShadow(text, 0, textY,
                        this.textColor.getColorValue());
            }
        } finally {
            GL11.glPopMatrix();
            if (depthEnabled) {
                cn.kazer.client189.compat.LegacyGL.glEnable(GL11.GL_DEPTH_TEST);
            }
        }
    }
}
