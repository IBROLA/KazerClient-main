package com.cheatbreaker.client.module.type;

import com.cheatbreaker.client.config.Setting;
import com.cheatbreaker.client.event.type.GuiDrawEvent;
import com.cheatbreaker.client.module.AbstractModule;
import com.cheatbreaker.client.ui.module.CBGuiAnchor;
import net.minecraft.util.MathHelper;
import org.lwjgl.opengl.GL11;

public class CoordinatesModule extends AbstractModule {

    private final Setting mode;
    private final Setting showWhileTyping;
    private final Setting showCoordinates;
    private final Setting hideYCoordinate;
    private final Setting showDirection;
    private final Setting coordinatesColor;
    private final Setting customLine;
    private final Setting directionColor;
    private static final String[] DIRECTIONS = {"N", "NE", "E", "SE", "S", "SW", "W", "NW"};
    private int lastX, lastY, lastZ;
    private boolean lastHideY;
    private String cachedHorizontal, cachedX, cachedY, cachedZ;

    public CoordinatesModule() {
        super("Coordinates");
        this.setDefaultAnchor(CBGuiAnchor.LEFT_TOP);
        this.setDefaultTranslations(158.0f, 0.0f);
        this.setDefaultState(true);
        this.setDefaultRenderHud(false);
        new Setting(this, "label").setValue("General Options");
        this.showWhileTyping = new Setting(this, "Show While Typing").setValue(true);
        this.mode = new Setting(this, "Mode").setValue("Horizontal").acceptedValues("Horizontal", "Vertical");
        this.hideYCoordinate = new Setting(this, "Hide Y Coordinate").setValue(false);
        this.showCoordinates = new Setting(this, "Show Coordinates").setValue(true);
        this.showDirection = new Setting(this, "Direction").setValue(true);
        this.customLine = new Setting(this, "Custom Line").setValue("");
        new Setting(this, "label").setValue("Color Settings");
        this.coordinatesColor = new Setting(this, "Coordinates Color").setValue(-1).setMinMax(Integer.MIN_VALUE, Integer.MAX_VALUE);
        this.directionColor = new Setting(this, "Direction Color").setValue(-1).setMinMax(Integer.MIN_VALUE, Integer.MAX_VALUE);
        this.setPreviewLabel("(16, 65, 120) NW", 1.0f);
        this.addEvent(GuiDrawEvent.class, this::onRender);
    }

    public void onRender(GuiDrawEvent event) {
        if (!this.isRenderHud()) {
            return;
        }

        int n = MathHelper.floor_double(this.minecraft.thePlayer.posX);
        int n2 = (int) this.minecraft.thePlayer.getEntityBoundingBox().minY;
        int n3 = MathHelper.floor_double(this.minecraft.thePlayer.posZ);

        if (!this.minecraft.ingameGUI.getChatGUI().getChatOpen() || ((Boolean) this.showWhileTyping.getValue())) {
            boolean horizontal = this.mode.getValue().equals("Horizontal");
            boolean hideY = (Boolean) this.hideYCoordinate.getValue();
            boolean showCoordinates = (Boolean) this.showCoordinates.getValue();
            float f = horizontal ? 4 : (hideY ? 8.066038f * 1.1777778f : (float) 16);

            if (cachedHorizontal == null || lastX != n || lastY != n2 || lastZ != n3 || lastHideY != hideY) {
                cachedHorizontal = hideY ? String.format("(%1$d, %2$d) ", n, n3)
                        : String.format("(%1$d, %2$d, %3$d) ", n, n2, n3);
                cachedX = "X: " + n; cachedY = "Y: " + n2; cachedZ = "Z: " + n3;
                lastX = n; lastY = n2; lastZ = n3; lastHideY = hideY;
            }
            String horizontalText = showCoordinates ? cachedHorizontal : "";
            String xLine = cachedX, yLine = cachedY, zLine = cachedZ;

            int coordinatesWidth;
            if (horizontal) {
                coordinatesWidth = this.minecraft.fontRendererObj.getStringWidth(horizontalText);
            } else {
                coordinatesWidth = Math.max(this.minecraft.fontRendererObj.getStringWidth(xLine),
                        this.minecraft.fontRendererObj.getStringWidth(zLine));
                if (!hideY) {
                    coordinatesWidth = Math.max(coordinatesWidth,
                            this.minecraft.fontRendererObj.getStringWidth(yLine));
                }
            }

            String direction = (Boolean) this.showDirection.getValue() ? this.currentDirection() : null;
            int directionX = horizontal ? coordinatesWidth : 50;
            float contentWidth = direction == null
                    ? coordinatesWidth
                    : Math.max(coordinatesWidth, directionX + this.minecraft.fontRendererObj.getStringWidth(direction));
            this.setDimensions(contentWidth,
                    contentHeight(horizontal, hideY, this.minecraft.fontRendererObj.FONT_HEIGHT));

            GL11.glPushMatrix();
            this.scaleAndTranslate(event.getResolution());
            if (horizontal) {
                this.minecraft.fontRendererObj.drawStringWithShadow(horizontalText, 0, 0, this.coordinatesColor.getColorValue());
            } else {
                this.minecraft.fontRendererObj.drawStringWithShadow(xLine, 0, 0, this.coordinatesColor.getColorValue());
                if (!hideY) {
                    this.minecraft.fontRendererObj.drawStringWithShadow(yLine, 0, 12, this.coordinatesColor.getColorValue());
                }
                this.minecraft.fontRendererObj.drawStringWithShadow(zLine, 0, hideY ? 12 : 24, this.coordinatesColor.getColorValue());
            }
            if (direction != null) {
                this.minecraft.fontRendererObj.drawStringWithShadow(direction, directionX, (int) f - 4, this.directionColor.getColorValue());
            }
            GL11.glPopMatrix();
        }
    }

    static float contentHeight(boolean horizontal, boolean hideYCoordinate, int fontHeight) {
        int lines = horizontal ? 1 : (hideYCoordinate ? 2 : 3);
        return (lines - 1) * 12.0F + fontHeight;
    }

    private String currentDirection() {
        double d = MathHelper.wrapAngleTo180_double(this.minecraft.thePlayer.rotationYaw) + (double) 180;
        d += 11.682692039868188 * (double) 1.925926f;
        d %= 360;
        return DIRECTIONS[MathHelper.floor_double(d /= (double) 45)];
    }

}
