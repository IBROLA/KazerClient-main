package com.cheatbreaker.client.kazerui.module.modules.visual;

import com.cheatbreaker.client.kazerui.module.Module;
import com.cheatbreaker.client.kazerui.module.Page;
import com.cheatbreaker.client.kazerui.module.setting.Setting;
import com.cheatbreaker.client.kazerui.module.setting.BoolSetting;
import com.cheatbreaker.client.kazerui.module.setting.ColorSetting;

/** Color only: the host owns glint geometry, depth coverage and blend restoration. */
public final class ModuleGlintColorizer extends Module {
    private static ModuleGlintColorizer instance;
    private final BoolSetting showGlint = bool("showGlint", true);
    private final BoolSetting overrideItemGlint = bool("overrideItemGlint", true);
    private final BoolSetting overrideArmorGlint = bool("overrideArmorGlint", true);
    private final ColorSetting itemColor = color("itemColor", 0xFF8040CC).noAlpha();
    private final ColorSetting armorColor = color("armorColor", 0xFF8040CC).noAlpha();

    public ModuleGlintColorizer() { super("GLINT_COLORIZER", false); instance = this; labels(); }
    private void labels() {
        for (Setting<?> setting : settings()) {
            switch (setting.key) {
                case "showGlint": setting.label(() -> "Show Glint"); break;
                case "overrideItemGlint": setting.label(() -> "Override Item Glint"); break;
                case "overrideArmorGlint": setting.label(() -> "Override Armor Glint"); break;
                case "itemColor": setting.label(() -> "Item Glint Color"); break;
                case "armorColor": setting.label(() -> "Armor Glint Color"); break;
                default: break;
            }
        }
    }
    @Override protected void layout(Page p) {
        p.add(showGlint);
        p.group(overrideItemGlint, g -> g.add(itemColor)).hideIf(() -> !showGlint.on());
        p.group(overrideArmorGlint, g -> g.add(armorColor)).hideIf(() -> !showGlint.on());
    }
    public static boolean visible(boolean original) {
        return original && (instance == null || !instance.isEnabled() || instance.showGlint.on());
    }
    public static boolean overrides(boolean armor) {
        return instance != null && instance.isEnabled() && instance.showGlint.on()
            && (armor ? instance.overrideArmorGlint.on() : instance.overrideItemGlint.on());
    }
    public static int color(int original, boolean armor) {
        return overrides(armor) ? (armor ? instance.armorColor : instance.itemColor).color() : original;
    }
    public static float channel(float original, boolean armor, int shift) {
        return overrides(armor) ? ((color(0, armor) >>> shift) & 255) / 255f * 0.76f : original;
    }
    public static int itemColor(int original) { return color(original, false); }
    public static float guiChannel(float original, int shift) {
        return overrides(false) ? ((color(0,false) >>> shift) & 255) / 255f : original;
    }
}
