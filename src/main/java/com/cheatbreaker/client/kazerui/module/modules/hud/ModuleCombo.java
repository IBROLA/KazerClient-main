package com.cheatbreaker.client.kazerui.module.modules.hud;

import com.cheatbreaker.client.kazerui.feature.HudAnchor;
import com.cheatbreaker.client.kazerui.module.Module;
import com.cheatbreaker.client.kazerui.module.hud.TextHud;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;

public final class ModuleCombo extends Module {
    private int combo, target = -1, attackTick = -100, confirmedTick = -100;
    private int displayed = Integer.MIN_VALUE;
    private String cached, example;
    public ModuleCombo() {
        super("COMBO", false);
        hud(new TextHud(this, 0, 0, HudAnchor.TOP_RIGHT, TextHud.sizes(10,18,22,46,56,62)) {
            protected String text(boolean preview) {
                if (preview) { if (example == null) example = lang("combo", 3); return example; }
                if (displayed != combo) { displayed = combo; cached = lang(combo == 0 ? "noCombo" : "combo", combo); }
                return cached;
            }
        });
    }
    public void attack(Entity targetEntity) {
        if (!isEnabled() || mc().thePlayer == null) return;
        target = targetEntity.getEntityId(); attackTick = mc().thePlayer.ticksExisted;
    }
    public void status(Entity entity, byte status) {
        if (!isEnabled() || mc().thePlayer == null || status != 2) return;
        if (entity == mc().thePlayer) { combo = 0; return; }
        if (entity instanceof EntityPlayer && entity.getEntityId() == target
                && mc().thePlayer.ticksExisted - attackTick <= 4 && attackTick != confirmedTick) {
            confirmedTick = attackTick; combo++;
        }
    }
    public void tick(com.cheatbreaker.client.kazerui.event.UiTick.ClientTickEvent e) {
        if (e.phase != com.cheatbreaker.client.kazerui.event.UiTick.Phase.END) return;
        if (!isEnabled() || mc().thePlayer == null) { combo = 0; target = -1; return; }
        if (mc().thePlayer.hurtTime > 0 && mc().thePlayer.hurtTime == mc().thePlayer.maxHurtTime || mc().thePlayer.ticksExisted - attackTick >= 80) combo = 0;
    }
}
