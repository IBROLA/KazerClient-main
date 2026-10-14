package com.cheatbreaker.client.ui.element.type;

import com.cheatbreaker.client.CheatBreaker;
import com.cheatbreaker.client.config.Setting;
import com.cheatbreaker.client.ui.element.AbstractModulesGuiElement;
import com.cheatbreaker.client.ui.module.CBModulesGui;
import com.cheatbreaker.client.ui.util.RenderUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.GL11;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class SliderElement extends AbstractModulesGuiElement {
    private static final float TRACK_START_OFFSET = 181.25F;
    private static final float TRACK_WIDTH = 130.0F;
    private static final float MARKER_SNAP_RADIUS = 6.0F;
    private static final float VALUE_LEFT_OFFSET = 130.0F;
    private static final float VALUE_RIGHT_OFFSET = 178.0F;
    private static final SliderDrawer OPENGL_SLIDER_DRAWER = new SliderDrawer() {
        @Override
        public void drawRect(double left, double top, double right, double bottom, int color) {
            cn.kazer.client189.compat.LegacyGui.drawRect((float)left, (float)top, (float)right, (float)bottom, color);
        }

        @Override
        public void drawCircle(double x, double y, double radius, int color) {
            cn.kazer.client189.compat.LegacyGL.glColor4f((color >> 16 & 255) / 255.0F, (color >> 8 & 255) / 255.0F,
                    (color & 255) / 255.0F, (color >>> 24) / 255.0F);
            RenderUtil.lIIIIlIIllIIlIIlIIIlIIllI(x, y, radius);
        }
    };

    private Setting setting;
    private float IllIIIIIIIlIlIllllIIllIII = -1;
    private float val;
    private boolean IlllIllIlIIIIlIIlIIllIIIl = false;
    private boolean editingValue;
    private boolean selectAllValue;
    private String valueBuffer;

    public SliderElement(Setting cBSetting, float f) {
        super(f);
        this.setting = cBSetting;
        this.height = 14;
        this.IllIIIIIIIlIlIllllIIllIII = Float.parseFloat("" + cBSetting.getValue());
    }

    @Override
    public void handleMouseClick(int mouseX, int mouseY, int button) {
        if (button == 0 && this.isValueHovered(mouseX, mouseY)) {
            this.beginValueEditing();
            return;
        }
        int offset = 170;
        int width = 170;
        boolean bl2 = (float) mouseX > (float)(this.x + offset) * this.scale && (float) mouseX < (float)(this.x + offset + width - 2) * this.scale && (float) mouseY > (float)(this.y + 4 + this.yOffset) * this.scale && (float) mouseY < (float)(this.y + 10 + this.yOffset) * this.scale;
        if (button == 0 && bl2) {
            this.IlllIllIlIIIIlIIlIIllIIIl = true;
            if (this.setting == CheatBreaker.getInstance().getGlobalSettings().containerVignetteStrength
                    && Minecraft.getMinecraft().currentScreen instanceof CBModulesGui) {
                cn.kazer.client189.compat.KazerCompat.vignette().previewPressed(Minecraft.getSystemTime());
            }
        }
    }

    @Override
    public void handleDrawElement(int mouseX, int mouseY, float partialTicks) {
        float f2;
        float f3;
        int n3 = 148;
        CheatBreaker.getInstance().ubuntuMedium16px.drawString(this.setting.getDisplayLabel().toUpperCase(), this.x + 10, (float)(this.y + 2), -1895825408);
        if (this.IlllIllIlIIIIlIIlIIllIIIl && !Mouse.isButtonDown(0)) {
            this.IlllIllIlIIIIlIIlIIllIIIl = false;
        }
        boolean valueHovered = this.isValueHovered(mouseX, mouseY);
        String string = this.editingValue ? this.valueBuffer : this.setting.getValue() + "";
        float textWidth = CheatBreaker.getInstance().ubuntuMedium16px.getStringWidth(string);
        double[] valueBounds = valueBounds(154.0D, textWidth);
        drawValueEditorChrome(valueHovered, this.editingValue, this.selectAllValue,
                this.x + valueBounds[0], this.y, this.x + valueBounds[1], this.y + 13,
                this.x + 154 - textWidth / 2.0F, this.x + 154 + textWidth / 2.0F,
                OPENGL_SLIDER_DRAWER);
        CheatBreaker.getInstance().ubuntuMedium16px.drawCenteredString(string, this.x + 154, (float)(this.y + 2), -1895825408);
        if (this.editingValue && !this.selectAllValue && System.currentTimeMillis() / 500L % 2L == 0L) {
            float cursorX = this.x + 154 + textWidth / 2.0F + 1.0F;
            cn.kazer.client189.compat.LegacyGui.drawRect(cursorX, this.y + 2, cursorX + 1.0F, this.y + 11, 0x7F000000);
        }
        boolean bl = (float) mouseX > (float)(this.x + 172) * this.scale && (float) mouseX < (float)(this.x + 172 + n3 - 2) * this.scale && (float) mouseY > (float)(this.y + 4 + this.yOffset) * this.scale && (float) mouseY < (float)(this.y + 10 + this.yOffset) * this.scale;
        RenderUtil.lIIIIlIIllIIlIIlIIIlIIllI((double)(this.x + 174), (double)(this.y + 6), (double)(this.x + 170 + n3 - 4), (double)(this.y + 8), 1.0, bl ? -1895825408 : 0x6F000000);
        double d = TRACK_WIDTH;
        float minVal = Float.parseFloat("" + this.setting.getMinimumValue());
        float maxVal = Float.parseFloat("" + this.setting.getMaximumValue());
        if (this.IlllIllIlIIIIlIIlIIllIIIl) {
            this.val = (float)Math.round(valueForMouseX(mouseX, this.x, this.scale, minVal, maxVal,
                    this.setting.getSliderMarker()) * 100.0D) / 100.0F;
            if (this.setting.getType().equals(Setting.Type.INTEGER)) {
                this.val = Math.round(this.val);
            }
            if (this.val < minVal) {
                this.val = minVal;
            } else if (this.val > maxVal) {
                this.val = maxVal;
            }
            switch (this.setting.getType()) {
                case INTEGER: {
                    this.setting.setValue(Integer.parseInt((int)this.val + ""));
                    break;
                }
                case FLOAT: {
                    this.setting.setValue(this.val);
                    break;
                }
                case DOUBLE: {
                    this.setting.setValue(Double.parseDouble(this.val + ""));
                }
            }
            CheatBreaker.getInstance().moduleManager.keyStrokes.initialize();
        }
        f3 = (f3 = Float.parseFloat(this.setting.getValue() + "")) < this.IllIIIIIIIlIlIllllIIllIII ? this.IllIIIIIIIlIlIllllIIllIII - f3 : (f3 -= this.IllIIIIIIIlIlIllllIIllIII);
        float f6 = ((maxVal - minVal) / (float)20 + f3 * (float)8) / (float)(Minecraft.getDebugFPS() + 1);
        if ((double)f6 < 43.5 * 2.2988505747126437E-6) {
            f6 = 1.9523809f * 5.121951E-5f;
        }
        if (this.IllIIIIIIIlIlIllllIIllIII < (f2 = Float.parseFloat(this.setting.getValue() + ""))) {
            this.IllIIIIIIIlIlIllllIIllIII = this.IllIIIIIIIlIlIllllIIllIII + f6 <= f2 ? (this.IllIIIIIIIlIlIllllIIllIII += f6) : f2;
        } else if (this.IllIIIIIIIlIlIllllIIllIII > f2) {
            this.IllIIIIIIIlIlIllllIIllIII = this.IllIIIIIIIlIlIllllIIllIII - f6 >= f2 ? (this.IllIIIIIIIlIlIllllIIllIII -= f6) : f2;
        }
        double d2 = (float)100 * ((this.IllIIIIIIIlIlIllllIIllIII - minVal) / (maxVal - minVal));
        RenderUtil.lIIIIlIIllIIlIIlIIIlIIllI((double)(this.x + 174), (double)(this.y + 6), (double)(this.x + 180) + d * d2 / (double)100, (double)(this.y + 8), (double)4, -12418828);
        Double markerX = this.setting.getSliderMarker() == null ? null
                : this.x + TRACK_START_OFFSET + d * this.setting.getSliderMarker();
        double thumbX = this.x + TRACK_START_OFFSET + d * d2 / 100.0D;
        drawMarkerAndThumb(markerX, thumbX, this.y + 7.25D, OPENGL_SLIDER_DRAWER);
    }

    @Override
    public boolean handleKeyTyped(char character, int keyCode) {
        if (!this.editingValue) {
            return false;
        }
        if (keyCode == Keyboard.KEY_LEFT || keyCode == Keyboard.KEY_RIGHT
                || keyCode == Keyboard.KEY_UP || keyCode == Keyboard.KEY_DOWN) {
            boolean increase = keyCode == Keyboard.KEY_RIGHT || keyCode == Keyboard.KEY_UP;
            boolean major = keyCode == Keyboard.KEY_UP || keyCode == Keyboard.KEY_DOWN;
            Number current = parseEditorValue(this.valueBuffer, this.setting.getType(),
                    this.getMinimum(), this.getMaximum());
            if (current == null) {
                current = (Number)this.setting.getValue();
            }
            Number stepped = stepEditorValue(current, this.setting.getType(), increase, major,
                    this.getMinimum(), this.getMaximum());
            this.applyValue(stepped);
            this.valueBuffer = formatEditorValue(stepped, this.setting.getType());
            this.selectAllValue = false;
            return true;
        }
        if (keyCode == Keyboard.KEY_RETURN || keyCode == Keyboard.KEY_NUMPADENTER) {
            this.commitValueEditing();
            return true;
        }
        if (keyCode == Keyboard.KEY_BACK) {
            if (this.selectAllValue) {
                this.valueBuffer = "";
                this.selectAllValue = false;
            } else if (!this.valueBuffer.isEmpty()) {
                this.valueBuffer = this.valueBuffer.substring(0, this.valueBuffer.length() - 1);
            }
            return true;
        }
        if (keyCode == Keyboard.KEY_DELETE) {
            this.valueBuffer = "";
            this.selectAllValue = false;
            return true;
        }
        if (keyCode == Keyboard.KEY_ESCAPE || keyCode == Keyboard.KEY_TAB) {
            this.commitValueEditing();
            return false;
        }
        if (character == 1) {
            this.selectAllValue = true;
            return true;
        }
        if (isAllowedEditorCharacter(character, this.setting.getType(), this.valueBuffer,
                this.selectAllValue)) {
            this.valueBuffer = this.selectAllValue ? Character.toString(character)
                    : this.valueBuffer + character;
            this.selectAllValue = false;
            return true;
        }
        return true;
    }

    @Override
    public void handleGlobalMouseClick(int mouseX, int mouseY, int button) {
        if (this.editingValue && !this.isValueHovered(mouseX, mouseY)) {
            this.commitValueEditing();
        }
    }

    @Override
    public void onFocusLost() {
        this.commitValueEditing();
    }

    private void beginValueEditing() {
        this.editingValue = true;
        this.selectAllValue = true;
        this.valueBuffer = this.setting.getValue().toString();
    }

    private void commitValueEditing() {
        if (!this.editingValue) {
            return;
        }
        Number parsed = parseEditorValue(this.valueBuffer, this.setting.getType(),
                this.getMinimum(), this.getMaximum());
        if (parsed != null) {
            this.applyValue(parsed);
        }
        this.editingValue = false;
        this.selectAllValue = false;
        this.valueBuffer = null;
    }

    private void applyValue(Number number) {
        switch (this.setting.getType()) {
            case INTEGER:
                this.setting.setValue(number.intValue());
                break;
            case FLOAT:
                this.setting.setValue(number.floatValue());
                break;
            case DOUBLE:
                this.setting.setValue(number.doubleValue());
                break;
        }
        CheatBreaker.getInstance().moduleManager.keyStrokes.initialize();
    }

    private double getMinimum() {
        return this.setting.getMinimumValue() == null ? -Double.MAX_VALUE
                : this.setting.getMinimumValue().doubleValue();
    }

    private double getMaximum() {
        return this.setting.getMaximumValue() == null ? Double.MAX_VALUE
                : this.setting.getMaximumValue().doubleValue();
    }

    private boolean isValueHovered(int mouseX, int mouseY) {
        String displayedValue = this.editingValue ? this.valueBuffer : this.setting.getValue().toString();
        float textWidth = CheatBreaker.getInstance().ubuntuMedium16px.getStringWidth(displayedValue);
        double[] bounds = valueBounds(154.0D, textWidth);
        return mouseX >= (this.x + bounds[0]) * this.scale
                && mouseX <= (this.x + bounds[1]) * this.scale
                && mouseY >= (this.y + this.yOffset) * this.scale
                && mouseY <= (this.y + this.yOffset + 13) * this.scale;
    }

    static double[] valueBounds(double center, double textWidth) {
        double halfWidth = textWidth / 2.0D + 2.0D;
        return new double[]{
                Math.max(VALUE_LEFT_OFFSET, center - halfWidth),
                Math.min(VALUE_RIGHT_OFFSET, center + halfWidth)
        };
    }

    static Number parseEditorValue(String text, Setting.Type type, double minimum, double maximum) {
        if (text == null || text.isEmpty() || "-".equals(text) || ".".equals(text)
                || "-.".equals(text)) {
            return null;
        }
        try {
            if (type == Setting.Type.INTEGER) {
                return (int)Math.max(minimum, Math.min(maximum, Integer.parseInt(text)));
            }
            double parsed = Math.max(minimum, Math.min(maximum, Double.parseDouble(text)));
            return type == Setting.Type.FLOAT ? (float)parsed : parsed;
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    static Number stepEditorValue(Number current, Setting.Type type, boolean increase, boolean major,
                                  double minimum, double maximum) {
        BigDecimal step = type == Setting.Type.INTEGER
                ? BigDecimal.valueOf(major ? 10L : 1L)
                : BigDecimal.valueOf(major ? 0.1D : 0.01D);
        BigDecimal value = new BigDecimal(current.toString());
        value = increase ? value.add(step) : value.subtract(step);
        double clamped = Math.max(minimum, Math.min(maximum, value.doubleValue()));
        if (type == Setting.Type.INTEGER) {
            return (int)Math.round(clamped);
        }
        return type == Setting.Type.FLOAT ? (float)clamped : clamped;
    }

    static String formatEditorValue(Number value, Setting.Type type) {
        if (type == Setting.Type.INTEGER) {
            return Integer.toString(value.intValue());
        }
        return BigDecimal.valueOf(value.doubleValue()).setScale(2, RoundingMode.HALF_UP)
                .stripTrailingZeros().toPlainString();
    }

    static boolean isAllowedEditorCharacter(char character, Setting.Type type, String current,
                                            boolean replacing) {
        if (character >= '0' && character <= '9') {
            return true;
        }
        String value = replacing ? "" : current;
        if (character == '-') {
            return value.isEmpty();
        }
        return character == '.' && type != Setting.Type.INTEGER && !value.contains(".");
    }

    static double valueForMouseX(float mouseX, float elementX, float scale, float minValue, float maxValue,
                                 Float markerPercentage) {
        float scaledTrackStart = (elementX + TRACK_START_OFFSET) * scale;
        float pointerOffset = mouseX - scaledTrackStart;
        double percentage = pointerOffset / (TRACK_WIDTH * (double)scale);

        if (markerPercentage != null) {
            float markerX = (elementX + TRACK_START_OFFSET + TRACK_WIDTH * markerPercentage) * scale;
            if (Math.abs(mouseX - markerX) <= MARKER_SNAP_RADIUS * scale) {
                percentage = markerPercentage;
            }
        }

        double value = (double)minValue + (double)(maxValue - minValue) * percentage;
        return Math.max(minValue, Math.min(maxValue, value));
    }

    static void drawMarkerAndThumb(Double markerX, double thumbX, double y, SliderDrawer drawer) {
        if (markerX != null) {
            drawer.drawRect(markerX - 0.5D, y - 9.25D, markerX + 0.5D, y - 3.25D, 0x6F000000);
        }
        drawer.drawCircle(thumbX, y, 4.5D, 0xFF4073FF);
        boolean centered = markerX != null && Math.abs(markerX - thumbX) < 0.001D;
        drawer.drawCircle(thumbX, y, 2.7D, centered ? 0xFF4073FF : 0xFFFFFFFF);
    }

    static void drawValueEditorChrome(boolean hovered, boolean editing, boolean selectAll,
                                      double left, double top, double right, double bottom,
                                      double textLeft, double textRight, SliderDrawer drawer) {
        if (!hovered && !editing) {
            return;
        }
        drawer.drawRect(left, top, right, bottom, editing ? 0x24000000 : 0x20000000);
        if (editing && selectAll) {
            drawer.drawRect(textLeft - 1.0D, top + 1.0D, textRight + 1.0D, bottom - 1.0D,
                    0x384073FF);
        }
    }

    interface SliderDrawer {
        void drawRect(double left, double top, double right, double bottom, int color);
        void drawCircle(double x, double y, double radius, int color);
    }

    @Override
    public com.cheatbreaker.client.config.Setting getSetting() {
        return this.setting;
    }
}
