package cn.kazer.client189.hud;

import cn.kazer.client189.config.KazerConfigDocument;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

/** 1.8.9 renderer for the shared 1.7 Key Strokes profile section. */
public final class KeystrokesHud {
    private boolean enabled = true;
    private boolean renderHud;
    private boolean showClicks = true;
    private boolean showMovementKeys = true;
    private boolean showSpacebar;
    private boolean arrows;
    private boolean textShadow;
    private boolean border;
    private int borderThickness = 1;
    private int boxSize = 18;
    private int gap = 1;
    private int textColor = -1;
    private int textColorPressed = -1;
    private int backgroundColor = 0x6F000000;
    private int backgroundColorPressed = 0x6FFFFFFF;
    private String anchor = "LEFT_TOP";
    private float xTranslation = 1.0F;
    private float yTranslation = 18.5F;
    private float scale = 1.0F;

    public void load(KazerConfigDocument profile) {
        if (profile == null) return;
        final String section = "Key Strokes";
        enabled = bool(profile.get(section, "-State"), enabled);
        renderHud = bool(profile.get(section, "-RenderHUD"), renderHud);
        showClicks = bool(profile.get(section, "Show clicks"), showClicks);
        showMovementKeys = bool(profile.get(section, "Show movement keys"), showMovementKeys);
        showSpacebar = bool(profile.get(section, "Show spacebar"), showSpacebar);
        arrows = bool(profile.get(section, "Replace names with arrows"), arrows);
        textShadow = bool(profile.get(section, "Text Shadow"), textShadow);
        border = bool(profile.get(section, "Border"), border);
        borderThickness = clamp(integer(profile.get(section, "Border Thickness"), borderThickness), 0, 4);
        boxSize = clamp(integer(profile.get(section, "Box size"), boxSize), 10, 32);
        gap = clamp(integer(profile.get(section, "Gap"), gap), 1, 4);
        textColor = integer(profile.get(section, "Text Color"), textColor);
        textColorPressed = integer(profile.get(section, "Text Color (Pressed)"), textColorPressed);
        backgroundColor = integer(profile.get(section, "Background Color"), backgroundColor);
        backgroundColorPressed = integer(profile.get(section, "Background Color (Pressed)"), backgroundColorPressed);
        String configuredAnchor = profile.get(section, "-Anchor");
        if (configuredAnchor != null) anchor = configuredAnchor;
        xTranslation = decimal(profile.get(section, "-xTranslation"), xTranslation);
        yTranslation = decimal(profile.get(section, "-yTranslation"), yTranslation);
        scale = decimal(profile.get(section, "Scale"), scale);
        if (scale <= 0) scale = 1.0F;
    }

    @SubscribeEvent
    public void onOverlay(RenderGameOverlayEvent.Post event) {
        if (event.type != RenderGameOverlayEvent.ElementType.ALL || !enabled || !renderHud) return;
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.thePlayer == null || mc.theWorld == null || mc.gameSettings.hideGUI) return;
        int rowWidth = boxSize * 3 + gap * 2;
        int height = 0;
        if (showMovementKeys) height += boxSize * 2 + gap;
        if (showClicks) height += (height == 0 ? 0 : gap) + boxSize;
        if (showSpacebar) height += (height == 0 ? 0 : gap) + boxSize / 2;
        if (height == 0) return;
        float[] point = HudPlacement.position(anchor, event.resolution.getScaledWidth(),
                event.resolution.getScaledHeight(), rowWidth, height, scale, xTranslation, yTranslation);
        GlStateManager.pushMatrix();
        GlStateManager.translate(point[0], point[1], 0);
        GlStateManager.scale(scale, scale, 1);
        int y = 0;
        if (showMovementKeys) {
            drawKey(mc, mc.gameSettings.keyBindForward.getKeyCode(), arrows ? "▲" : keyName(mc.gameSettings.keyBindForward.getKeyCode()),
                    boxSize + gap, y, boxSize, boxSize);
            y += boxSize + gap;
            drawKey(mc, mc.gameSettings.keyBindLeft.getKeyCode(), arrows ? "◀" : keyName(mc.gameSettings.keyBindLeft.getKeyCode()),
                    0, y, boxSize, boxSize);
            drawKey(mc, mc.gameSettings.keyBindBack.getKeyCode(), arrows ? "▼" : keyName(mc.gameSettings.keyBindBack.getKeyCode()),
                    boxSize + gap, y, boxSize, boxSize);
            drawKey(mc, mc.gameSettings.keyBindRight.getKeyCode(), arrows ? "▶" : keyName(mc.gameSettings.keyBindRight.getKeyCode()),
                    (boxSize + gap) * 2, y, boxSize, boxSize);
            y += boxSize + gap;
        }
        if (showClicks) {
            int leftWidth = (rowWidth - gap) / 2;
            drawKey(mc, mc.gameSettings.keyBindAttack.getKeyCode(), boxSize < 14 ? "L" : "LMB",
                    0, y, leftWidth, boxSize);
            drawKey(mc, mc.gameSettings.keyBindUseItem.getKeyCode(), boxSize < 14 ? "R" : "RMB",
                    leftWidth + gap, y, rowWidth - leftWidth - gap, boxSize);
            y += boxSize + gap;
        }
        if (showSpacebar) {
            drawKey(mc, mc.gameSettings.keyBindJump.getKeyCode(), "", 0, y, rowWidth, boxSize / 2);
        }
        GlStateManager.popMatrix();
    }

    private void drawKey(Minecraft mc, int keyCode, String label, int x, int y, int width, int height) {
        boolean pressed = mc.currentScreen == null && isDown(keyCode);
        int background = pressed ? backgroundColorPressed : backgroundColor;
        if (border && borderThickness > 0) {
            Gui.drawRect(x, y, x + width, y + height, textColor);
            Gui.drawRect(x + borderThickness, y + borderThickness,
                    x + width - borderThickness, y + height - borderThickness, background);
        } else {
            Gui.drawRect(x, y, x + width, y + height, background);
        }
        int foreground = pressed ? textColorPressed : textColor;
        if (label.isEmpty()) {
            Gui.drawRect(x + width * 3 / 8, y + height / 2,
                    x + width * 5 / 8, y + height / 2 + 1, foreground);
        } else {
            int labelWidth = mc.fontRendererObj.getStringWidth(label);
            mc.fontRendererObj.drawString(label, x + (width - labelWidth) / 2,
                    y + (height - mc.fontRendererObj.FONT_HEIGHT) / 2, foreground, textShadow);
        }
    }

    private static boolean isDown(int keyCode) {
        if (keyCode < 0) {
            int button = keyCode + 100;
            return button >= 0 && Mouse.isButtonDown(button);
        }
        return keyCode > 0 && Keyboard.isKeyDown(keyCode);
    }

    private static String keyName(int keyCode) {
        String name = keyCode < 0 ? "M" + (keyCode + 101) : Keyboard.getKeyName(keyCode);
        return name == null || name.isEmpty() ? "?" : name.length() > 2 ? name.substring(0, 1) : name;
    }

    private static boolean bool(String value, boolean fallback) {
        return value == null ? fallback : Boolean.parseBoolean(value);
    }

    private static int integer(String value, int fallback) {
        if (value == null) return fallback;
        try { return (int) Float.parseFloat(value.split(";", 2)[0]); }
        catch (NumberFormatException ignored) { return fallback; }
    }

    private static float decimal(String value, float fallback) {
        if (value == null) return fallback;
        try { return Float.parseFloat(value); }
        catch (NumberFormatException ignored) { return fallback; }
    }

    private static int clamp(int value, int minimum, int maximum) {
        return Math.max(minimum, Math.min(maximum, value));
    }
}
