package cn.kazer.client189.hud;

import cn.kazer.client189.config.KazerConfigDocument;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiChat;
import net.minecraft.client.gui.inventory.GuiInventory;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.resources.I18n;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;

/** Shared-profile potion effect HUD, using Minecraft's native font and icons. */
public final class PotionEffectsHud {
    private static final ResourceLocation INVENTORY =
            new ResourceLocation("textures/gui/container/inventory.png");
    private boolean enabled = true;
    private boolean renderHud = true;
    private boolean showWhileTyping = true;
    private boolean showName = true;
    private boolean showDuration = true;
    private boolean showAmplifier = true;
    private boolean showInInventory = true;
    private boolean hidePotionStatus;
    private boolean uppercase;
    private boolean reversed;
    private boolean blink = true;
    private boolean showBackground;
    private boolean textShadow = true;
    private boolean colorNameBasedOnEffect;
    private boolean colorInfoBasedOnEffect;
    private int blinkDuration = 10;
    private int nameColor = -1;
    private int durationColor = -1;
    private String mode = "Normal";
    private String anchor = "LEFT_MIDDLE";
    private float xTranslation;
    private float yTranslation = 4;
    private float scale = 1;

    public void load(KazerConfigDocument profile) {
        if (profile == null) return;
        String section = "Potion Effects";
        enabled = bool(profile.get(section, "-State"), enabled);
        renderHud = bool(profile.get(section, "-RenderHUD"), renderHud);
        showWhileTyping = bool(profile.get(section, "Show While Typing"), showWhileTyping);
        showName = bool(profile.get(section, "Show Effect Name"), showName);
        showDuration = bool(profile.get(section, "Show Effect Duration"), showDuration);
        showAmplifier = bool(profile.get(section, "Show Effect Amplifier"), showAmplifier);
        showInInventory = bool(profile.get(section, "Show In Inventory"), showInInventory);
        hidePotionStatus = bool(profile.get(section, "Hide Potion Status"), hidePotionStatus);
        uppercase = bool(profile.get(section, "Uppercase Potion Names"), uppercase);
        reversed = bool(profile.get(section, "Reversed Text"), reversed);
        blink = bool(profile.get(section, "Blink"), blink);
        showBackground = bool(profile.get(section, "Show Background"), showBackground);
        textShadow = bool(profile.get(section, "Text Shadow"), textShadow);
        colorNameBasedOnEffect = bool(profile.get(section, "Color Name Based on Effect"), colorNameBasedOnEffect);
        colorInfoBasedOnEffect = bool(profile.get(section, "Color Info Based on Effect"), colorInfoBasedOnEffect);
        blinkDuration = integer(profile.get(section, "Blink Duration"), blinkDuration);
        nameColor = integer(profile.get(section, "Name Color"), nameColor);
        durationColor = integer(profile.get(section, "Duration Color"), durationColor);
        String configuredMode = profile.get(section, "Mode");
        if (configuredMode != null) mode = configuredMode;
        String configuredAnchor = profile.get(section, "-Anchor");
        if (configuredAnchor != null) anchor = configuredAnchor;
        xTranslation = decimal(profile.get(section, "-xTranslation"), xTranslation);
        yTranslation = decimal(profile.get(section, "-yTranslation"), yTranslation);
        scale = decimal(profile.get(section, "Scale"), scale);
        if (scale <= 0) scale = 1;
    }

    @SubscribeEvent
    public void onOverlay(RenderGameOverlayEvent.Post event) {
        if (event.type != RenderGameOverlayEvent.ElementType.ALL || !enabled || !renderHud || hidePotionStatus) return;
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.thePlayer == null || mc.theWorld == null || mc.gameSettings.hideGUI) return;
        if (!showWhileTyping && mc.currentScreen instanceof GuiChat) return;
        if (!showInInventory && mc.currentScreen instanceof GuiInventory) return;
        Collection<PotionEffect> active = mc.thePlayer.getActivePotionEffects();
        if (active.isEmpty()) return;
        List<EffectLine> lines = new ArrayList<EffectLine>();
        int width = 18;
        for (PotionEffect effect : active) {
            Potion potion = Potion.potionTypes[effect.getPotionID()];
            if (potion == null) continue;
            String name = I18n.format(effect.getEffectName());
            if (uppercase) name = name.toUpperCase(Locale.ROOT);
            if (showAmplifier && effect.getAmplifier() > 0) name += " " + roman(effect.getAmplifier() + 1);
            String duration = Potion.getDurationString(effect);
            if (!showName) name = "";
            if (!showDuration) duration = "";
            if (reversed) {
                String old = name;
                name = duration;
                duration = old;
            }
            width = Math.max(width, 20 + Math.max(mc.fontRendererObj.getStringWidth(name),
                    mc.fontRendererObj.getStringWidth(duration)));
            lines.add(new EffectLine(potion, effect, name, duration));
        }
        if (lines.isEmpty()) return;
        int rowHeight = mode.equalsIgnoreCase("Compact") ? 18 : 22;
        int height = lines.size() * rowHeight;
        float[] point = HudPlacement.position(anchor, event.resolution.getScaledWidth(),
                event.resolution.getScaledHeight(), width, height, scale, xTranslation, yTranslation);
        boolean right = anchor.toUpperCase(Locale.ROOT).startsWith("RIGHT");
        GlStateManager.pushMatrix();
        GlStateManager.translate(point[0], point[1], 0);
        GlStateManager.scale(scale, scale, 1);
        if (showBackground) Gui.drawRect(0, 0, width, height, 0x6F000000);
        for (int i = 0; i < lines.size(); i++) {
            EffectLine line = lines.get(i);
            int y = i * rowHeight;
            if (line.potion.hasStatusIcon()) {
                GlStateManager.color(1, 1, 1, 1);
                mc.getTextureManager().bindTexture(INVENTORY);
                int icon = line.potion.getStatusIconIndex();
                Gui.drawModalRectWithCustomSizedTexture(right ? width - 18 : 0, y,
                        icon % 8 * 18, 198 + icon / 8 * 18, 18, 18, 256, 256);
            }
            int color = 0xFF000000 | line.potion.getLiquidColor();
            int firstColor = colorNameBasedOnEffect ? color : nameColor;
            int secondColor = colorInfoBasedOnEffect ? color : durationColor;
            int textX = right ? width - 20 - mc.fontRendererObj.getStringWidth(line.name) : 20;
            if (!line.name.isEmpty()) mc.fontRendererObj.drawString(line.name, textX, y, firstColor, textShadow);
            if (!line.duration.isEmpty() && visibleDuration(line.effect)) {
                int durationX = right ? width - 20 - mc.fontRendererObj.getStringWidth(line.duration) : 20;
                mc.fontRendererObj.drawString(line.duration, durationX,
                        y + (line.name.isEmpty() ? 4 : 10), secondColor, textShadow);
            }
        }
        GlStateManager.popMatrix();
    }

    private boolean visibleDuration(PotionEffect effect) {
        if (!blink || effect.getDuration() > blinkDuration * 20) return true;
        return (System.currentTimeMillis() / 400L) % 2L == 0L;
    }

    private static String roman(int level) {
        String[] values = {"", "I", "II", "III", "IV", "V", "VI", "VII", "VIII", "IX", "X"};
        return level < values.length ? values[level] : Integer.toString(level);
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

    private static final class EffectLine {
        private final Potion potion;
        private final PotionEffect effect;
        private final String name;
        private final String duration;

        private EffectLine(Potion potion, PotionEffect effect, String name, String duration) {
            this.potion = potion;
            this.effect = effect;
            this.name = name;
            this.duration = duration;
        }
    }
}
