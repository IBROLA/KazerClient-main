package cn.kazer.client189.hud;

import cn.kazer.client189.config.KazerConfigDocument;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

/** FPS module uses the same profile key names, defaults, and geometry as 1.7.10. */
public final class FpsHud {
    private boolean enabled = true;
    private boolean renderHud = true;
    private boolean showBackground = true;
    private boolean reverseOrder;
    private boolean textShadow = true;
    private int textColor = -1;
    private int backgroundColor = 0x6F000000;
    private String anchor = "MIDDLE_TOP";
    private float xTranslation;
    private float yTranslation = 12.0F;
    private float scale = 1.0F;

    public void load(KazerConfigDocument profile) {
        if (profile == null) return;
        enabled = bool(profile.get("FPS", "-State"), enabled);
        renderHud = bool(profile.get("FPS", "-RenderHUD"), renderHud);
        showBackground = bool(profile.get("FPS", "Show Background"), showBackground);
        reverseOrder = bool(profile.get("FPS", "Reverse Order"), reverseOrder);
        textShadow = bool(profile.get("FPS", "Text Shadow"), textShadow);
        textColor = integer(profile.get("FPS", "Text Color"), textColor);
        backgroundColor = integer(profile.get("FPS", "Background Color"), backgroundColor);
        String configuredAnchor = profile.get("FPS", "-Anchor");
        if (configuredAnchor != null) anchor = configuredAnchor;
        xTranslation = decimal(profile.get("FPS", "-xTranslation"), xTranslation);
        yTranslation = decimal(profile.get("FPS", "-yTranslation"), yTranslation);
        scale = decimal(profile.get("FPS", "Scale"), scale);
        if (scale <= 0) scale = 1.0F;
    }

    @SubscribeEvent
    public void onOverlay(RenderGameOverlayEvent.Post event) {
        if (event.type != RenderGameOverlayEvent.ElementType.ALL || !enabled || !renderHud) return;
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.thePlayer == null || mc.theWorld == null || mc.gameSettings.hideGUI) return;
        String value = reverseOrder ? "FPS " + Minecraft.getDebugFPS() : Minecraft.getDebugFPS() + " FPS";
        String label = showBackground ? value : "[" + value + "]";
        int textWidth = mc.fontRendererObj.getStringWidth(label);
        float width = showBackground ? 56.0F : textWidth;
        float height = showBackground ? mc.fontRendererObj.FONT_HEIGHT + 4.0F : mc.fontRendererObj.FONT_HEIGHT;
        float[] point = HudPlacement.position(anchor, event.resolution.getScaledWidth(),
                event.resolution.getScaledHeight(), width, height, scale, xTranslation, yTranslation);
        GlStateManager.pushMatrix();
        GlStateManager.translate(point[0], point[1], 0.0F);
        GlStateManager.scale(scale, scale, 1.0F);
        if (showBackground) {
            Gui.drawRect(0, 0, (int) width, (int) height, backgroundColor);
            mc.fontRendererObj.drawString(label, (int) (width / 2.0F - textWidth / 2.0F), 3, textColor);
        } else {
            mc.fontRendererObj.drawString(label, 0, 0, textColor, textShadow);
        }
        GlStateManager.popMatrix();
    }

    private static boolean bool(String value, boolean fallback) {
        return value == null ? fallback : Boolean.parseBoolean(value);
    }

    private static int integer(String value, int fallback) {
        if (value == null) return fallback;
        try { return Integer.parseInt(value.split(";", 2)[0]); }
        catch (NumberFormatException ignored) { return fallback; }
    }

    private static float decimal(String value, float fallback) {
        if (value == null) return fallback;
        try { return Float.parseFloat(value); }
        catch (NumberFormatException ignored) { return fallback; }
    }
}
