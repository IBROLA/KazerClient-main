package com.cheatbreaker.client.kazerui.module.modules.hud;

import com.cheatbreaker.client.kazerui.feature.HudAnchor;
import com.cheatbreaker.client.kazerui.module.Module;
import com.cheatbreaker.client.kazerui.module.Page;
import com.cheatbreaker.client.kazerui.module.hud.TextHud;
import com.cheatbreaker.client.kazerui.module.setting.*;
import net.minecraft.entity.Entity;
import net.minecraft.util.Vec3;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

public final class ModuleReachDisplay extends Module {
    private final BoolSetting reverse = bool("reverseOrder", false), hideZero = bool("hideZero", false);
    private final BoolSetting highlight = bool("highlightAttackablePlayers", false);
    private final ColorSetting highlightColor = color("highlightColor", 1291910912);
    private final DecimalFormat format = new DecimalFormat("#.##", new DecimalFormatSymbols(Locale.ENGLISH));
    private int target = -1, attackTick = -100;
    private double pending, distance;
    private double displayed = Double.NaN;
    private boolean displayedReverse;
    private String cached, blocks, label;
    public ModuleReachDisplay() {
        super("REACH_DISPLAY", false);
        hud(new TextHud(this, 0, 0, HudAnchor.TOP_LEFT, TextHud.sizes(10,18,22,40,65,72)) {
            protected String text(boolean preview) {
                if (hideZero.on() && distance == 0) return null;
                if (displayed != distance || displayedReverse != reverse.on()) {
                    if (blocks == null) {
                        blocks = com.cheatbreaker.client.kazerui.gui.ui.KazerLang.get("shared_info", "blocks");
                        String raw = blocks.trim();
                        label = raw.isEmpty() ? "Blocks" : Character.toUpperCase(raw.charAt(0)) + raw.substring(1);
                    }
                    displayed = distance; displayedReverse = reverse.on();
                    String n = format.format(distance);
                    cached = displayedReverse ? label + ": " + n : n + blocks;
                }
                return cached;
            }
        });
    }
    protected void layout(Page p) { p.section("generalOptions", s -> { s.add(reverse, hideZero); s.group(highlight, c -> c.add(highlightColor)); }); }
    public void attack(Entity targetEntity) {
        if (!isEnabled() || mc().thePlayer == null) return;
        Vec3 eye = mc().thePlayer.getPositionEyes(1);

        if (mc().objectMouseOver == null || mc().objectMouseOver.entityHit != targetEntity || mc().objectMouseOver.hitVec == null) return;
        target = targetEntity.getEntityId(); attackTick = mc().thePlayer.ticksExisted;
        pending = eye.distanceTo(mc().objectMouseOver.hitVec);
    }
    public void status(Entity e, byte status) {
        if (isEnabled() && mc().thePlayer != null && status == 2 && e.getEntityId() == target
                && mc().thePlayer.ticksExisted - attackTick <= 4) distance = Math.min(3, pending);
    }
    public void tick(com.cheatbreaker.client.kazerui.event.UiTick.ClientTickEvent e) {
        if (e.phase == com.cheatbreaker.client.kazerui.event.UiTick.Phase.END && (mc().thePlayer == null || !isEnabled() || mc().thePlayer.ticksExisted - attackTick >= 80)) { distance = 0; target = -1; }
    }
}
