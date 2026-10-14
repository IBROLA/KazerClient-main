package com.cheatbreaker.client.kazerui.module.modules.visual;

import com.cheatbreaker.client.kazerui.module.Module;
import com.cheatbreaker.client.kazerui.module.Page;
import com.cheatbreaker.client.kazerui.module.setting.*;
import net.minecraft.client.particle.*;

/** Native particle emission filters and vertex-only styling; never modifies combat state. */
public final class ModuleParticleChanger extends Module {
    private static ModuleParticleChanger instance;
    private final NumberSetting size = decimal("particleSize", 1, 0.25f, 2);
    private final NumberSetting amount = integer("particleAmount", 100, 0, 100);
    private final BoolSetting critical = bool("showCritical", true), sharpness = bool("showSharpness", true);
    private final BoolSetting blocks = bool("showBlockParticles", true), smoke = bool("showSmoke", true);
    private final BoolSetting flame = bool("showFlame", true), potion = bool("showPotionParticles", true);
    private final BoolSetting portal = bool("showPortal", true), other = bool("showOtherParticles", true);
    private final BoolSetting recolor = bool("overrideParticleColor", false);
    private final ColorSetting tint = color("particleColor", 0xFFFFFFFF).noAlpha().noChroma();
    private final int[] emission = new int[8];
    private final BoolSetting[] visible = {critical, sharpness, blocks, smoke, flame, potion, portal, other};
    public ModuleParticleChanger() { super("PARTICLE_CHANGER", false); instance = this; labels(); }
    private void labels() {
        for (Setting<?> setting : settings()) {
            switch (setting.key) {
                case "particleSize": setting.label(() -> "Particle Size"); break;
                case "particleAmount": setting.label(() -> "Particle Amount (%)"); break;
                case "showCritical": setting.label(() -> "Show Critical / Sharpness"); break;
                case "showSharpness": setting.label(() -> "Show Enchanting Particles"); break;
                case "showBlockParticles": setting.label(() -> "Show Block / Item Particles"); break;
                case "showSmoke": setting.label(() -> "Show Smoke"); break;
                case "showFlame": setting.label(() -> "Show Flame"); break;
                case "showPotionParticles": setting.label(() -> "Show Potion Particles"); break;
                case "showPortal": setting.label(() -> "Show Portal Particles"); break;
                case "showOtherParticles": setting.label(() -> "Show Other Particles"); break;
                case "overrideParticleColor": setting.label(() -> "Override Particle Color"); break;
                case "particleColor": setting.label(() -> "Particle Color"); break;
                default: break;
            }
        }
    }
    @Override protected void layout(Page p) {
        p.add(size, amount);
        p.add(critical, sharpness, blocks, smoke, flame, potion, portal, other);
        p.group(recolor, g -> g.add(tint));
    }
    // Stable_22's EntityCritFX is large smoke; actual crit/magic-crit use EntityCrit2FX.
    private int kind(Object particle) {
        if (particle instanceof EntityCrit2FX) return 0;
        if (particle instanceof EntityEnchantmentTableParticleFX) return 1;
        if (particle instanceof EntityDiggingFX || particle instanceof EntityBreakingFX) return 2;
        if (particle instanceof EntitySmokeFX) return 3;
        if (particle instanceof EntityFlameFX) return 4;
        if (particle instanceof EntitySpellParticleFX) return 5;
        if (particle instanceof EntityPortalFX) return 6;
        return 7;
    }
    public static boolean keep(Object particle) {
        ModuleParticleChanger m = instance;
        if (particle == null) return false;
        if (m == null || !m.isEnabled()) return true;
        int kind = m.kind(particle);
        if (!m.visible[kind].on()) return false;
        int percent = m.amount.intValue();
        if (percent >= 100) return true;
        // Deterministic evenly spaced thinning: no shared game RNG, allocations or copies.
        m.emission[kind] = (m.emission[kind] + percent) % 100;
        return percent > 0 && m.emission[kind] < percent;
    }
    public static boolean active() { return instance != null && instance.isEnabled(); }
    public static float scale(float original, Object particle) {
        ModuleParticleChanger m = instance;
        return m == null || !m.isEnabled() ? original : original * m.size.value();
    }
    public static float channel(float original, Object particle, int shift) {
        ModuleParticleChanger m = instance;
        return m == null || !m.isEnabled() || !m.recolor.on() ? original : ((m.tint.argb() >>> shift) & 255) / 255f;
    }
}
