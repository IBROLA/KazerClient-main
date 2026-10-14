package com.cheatbreaker.client.module.type;

import com.cheatbreaker.client.config.Setting;
import com.cheatbreaker.client.event.type.GuiDrawEvent;
import com.cheatbreaker.client.event.type.RenderPreviewEvent;
import com.cheatbreaker.client.module.AbstractModule;
import com.cheatbreaker.client.ui.module.CBGuiAnchor;
import com.cheatbreaker.client.ui.module.CBModulePlaceGui;
import com.cheatbreaker.client.ui.module.CBModulesGui;
import net.minecraft.client.gui.Gui;
import net.minecraft.util.FoodStats;
import org.lwjgl.opengl.GL11;

public final class SaturationModule extends AbstractModule {
    private static final float SAMPLE_SATURATION = 5.76F;

    private final Setting showDecimals;
    private final Setting showBackground;
    private final Setting textColor;
    private final Setting backgroundColor;

    public SaturationModule() {
        super("Saturation");
        this.setDefaultAnchor(CBGuiAnchor.RIGHT_BOTTOM);
        this.setDefaultTranslations(-240.70001F, -13.600006F);
        this.setDefaultState(true);
        this.scale.setDefaultValue(0.6f);
        this.showDecimals = new Setting(this, "Show Decimals").setValue(false);
        this.showBackground = new Setting(this, "Show Background").setValue(false);
        this.textColor = new Setting(this, "Text Color").setValue(-11141121)
                .setMinMax(Integer.MIN_VALUE, Integer.MAX_VALUE);
        this.backgroundColor = new Setting(this, "Background Color").setValue(0x6F000000)
                .setMinMax(Integer.MIN_VALUE, Integer.MAX_VALUE);
        this.setPreviewLabel("6", 2.0F);
        this.addEvent(GuiDrawEvent.class, this::renderHud);
        this.addEvent(RenderPreviewEvent.class, this::renderPreview);
    }

    private void renderHud(GuiDrawEvent event) {
        if (!this.isRenderHud() || this.minecraft.thePlayer == null
                || this.minecraft.currentScreen instanceof CBModulesGui
                || this.minecraft.currentScreen instanceof CBModulePlaceGui) {
            return;
        }
        this.render(event, SaturationText.fromFoodStats(this.minecraft.thePlayer.getFoodStats(),
                (Boolean)this.showDecimals.getValue()));
    }

    private void renderPreview(RenderPreviewEvent event) {
        if (!this.isRenderHud()) {
            return;
        }
        boolean placementPreview = this.minecraft.currentScreen instanceof CBModulePlaceGui;
        FoodStats foodStats;
        if (placementPreview) {
            foodStats = new FoodStats();
            foodStats.setFoodSaturationLevel(SAMPLE_SATURATION);
        } else if (this.minecraft.currentScreen instanceof CBModulesGui
                && this.minecraft.thePlayer != null) {
            foodStats = this.minecraft.thePlayer.getFoodStats();
        } else {
            return;
        }
        this.render(event, SaturationText.fromFoodStats(foodStats,
                (Boolean)this.showDecimals.getValue()));
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

    float getPositionalWidth(int referenceTextWidth, int fontHeight) {
        return this.getContentDimensions(referenceTextWidth, fontHeight)[0];
    }

    @Override
    protected float getPositionalWidth() {
        String reference = SaturationText.referenceText((Boolean)this.showDecimals.getValue());
        int referenceWidth = this.minecraft.fontRendererObj.getStringWidth(reference);
        return this.getPositionalWidth(referenceWidth, this.minecraft.fontRendererObj.FONT_HEIGHT);
    }

    private void render(GuiDrawEvent event, String text) {
        if (text == null) {
            return;
        }
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
        this.scaleAndTranslate(event.getResolution());
        if (background) {
            cn.kazer.client189.compat.LegacyGui.drawRect(0.0F, 0.0F, contentWidth, contentHeight,
                    this.backgroundColor.getColorValue());
        }
        this.minecraft.fontRendererObj.drawStringWithShadow(text, (int)padding, textY,
                this.textColor.getColorValue());
        GL11.glPopMatrix();
        if (depthEnabled) {
            cn.kazer.client189.compat.LegacyGL.glEnable(GL11.GL_DEPTH_TEST);
        }
    }
}
