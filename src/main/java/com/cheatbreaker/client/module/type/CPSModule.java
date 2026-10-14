package com.cheatbreaker.client.module.type;

import com.cheatbreaker.client.config.Setting;
import com.cheatbreaker.client.event.type.ClickEvent;
import com.cheatbreaker.client.event.type.GuiDrawEvent;
import com.cheatbreaker.client.event.type.TickEvent;
import com.cheatbreaker.client.module.AbstractModule;
import com.cheatbreaker.client.ui.module.CBGuiAnchor;
import net.minecraft.client.gui.Gui;
import org.lwjgl.opengl.GL11;

import java.util.ArrayList;
import java.util.List;

public class CPSModule extends AbstractModule {

    private final List<Long> clicks;
    private final Setting showBackground;
    private final Setting rightClickCps;
    private final Setting showCpsText;
    private final Setting reverseText;
    private final Setting ignoreCancelledClicks;
    private final Setting textShadow;
    private final Setting textColor;
    private final Setting backgroundColor;
    private final CounterText counterText = new CounterText("CPS");

    public CPSModule() {
        super("CPS");
        this.setDefaultAnchor(CBGuiAnchor.RIGHT_BOTTOM);
        this.setDefaultTranslations(-170.0f, -2.0f);
        this.setDefaultState(true);
        this.setDefaultRenderHud(false);
        this.clicks = new ArrayList<>();
        this.rightClickCps = new Setting(this, "Right Click CPS").setValue(false);
        this.showCpsText = new Setting(this, "Show CPS Text").setValue(true);
        this.reverseText = new Setting(this, "Reverse Text").setValue(false);
        this.ignoreCancelledClicks = new Setting(this, "Ignore Cancelled Clicks").setValue(false);
        this.textShadow = new Setting(this, "Text Shadow").setValue(false);
        this.showBackground = new Setting(this, "Show Background").setValue(true);
        this.textColor = new Setting(this, "Text Color").setValue(-1).setMinMax(Integer.MIN_VALUE, Integer.MAX_VALUE);
        this.backgroundColor = new Setting(this, "Background Color").setValue(0x6F000000).setMinMax(Integer.MIN_VALUE, Integer.MAX_VALUE);
        this.setPreviewLabel("[9 CPS]", 1.1030303f * 1.2692307f);
        this.addEvent(GuiDrawEvent.class, this::onDraw);
        this.addEvent(TickEvent.class, this::onTick);
        this.addEvent(ClickEvent.class, this::onClick);
    }

    private void onDraw(GuiDrawEvent drawEvent) {
        if (!this.isRenderHud()) {
            return;
        }
        boolean background = (Boolean) this.showBackground.getValue();
        String string = counterText.text(this.clicks.size(), background,
                (Boolean)this.reverseText.getValue(), (Boolean)this.showCpsText.getValue());
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

    private void onTick(TickEvent cBTickEvent) {
        this.clicks.removeIf(l -> l < System.currentTimeMillis() - 1000L);
    }

    private void onClick(ClickEvent cBClickEvent) {
        if (cBClickEvent.getMouseButton() == 0 || (cBClickEvent.getMouseButton() == 1 && (Boolean)this.rightClickCps.getValue())) {
            this.clicks.add(System.currentTimeMillis());
        }
    }

}
