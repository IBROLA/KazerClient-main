package com.cheatbreaker.client.kazerui.module;

import com.cheatbreaker.client.kazerui.gui.ui.*;
import com.cheatbreaker.client.kazerui.module.hud.HudElement;
import java.util.*;

/** Imported module registry. Only explicitly mapped Kazer modules can register. */
public final class ModuleManager {
    private static final Map<String,Module> BY_KEY = new LinkedHashMap<String,Module>();
    private static final List<Module> MODULES = new ArrayList<Module>();
    private static UiModel model;
    public interface HudListener { void added(HudElement hud); void removed(HudElement hud); }
    public static HudListener hudListener;
    public static void init(UiModel ui) {
        model = ui;
        if (!MODULES.isEmpty()) return;
        register(new com.cheatbreaker.client.kazerui.module.modules.hud.ModuleFps());
        register(new com.cheatbreaker.client.kazerui.module.modules.hud.ModuleCps());
        register(new com.cheatbreaker.client.kazerui.module.modules.hud.ModuleArmorStatus());
        register(new com.cheatbreaker.client.kazerui.module.modules.hud.ModuleKeystrokes());
        register(new com.cheatbreaker.client.kazerui.module.modules.hud.ModulePotionEffects());
        register(new com.cheatbreaker.client.kazerui.module.modules.hud.ModuleScoreboard());
        register(new com.cheatbreaker.client.kazerui.module.modules.hud.ModuleBossbar());
        register(new com.cheatbreaker.client.kazerui.module.modules.visual.ModuleMotionBlur());
        register(new com.cheatbreaker.client.kazerui.module.modules.mechanic.ModuleToggleSneak());
        register(new com.cheatbreaker.client.kazerui.module.modules.mechanic.ModuleChat());
        register(new com.cheatbreaker.client.kazerui.module.modules.hud.ModuleClock());
        register(new com.cheatbreaker.client.kazerui.module.modules.hud.ModuleMemory());
        register(new com.cheatbreaker.client.kazerui.module.modules.hud.ModuleDayCounter());
        register(new com.cheatbreaker.client.kazerui.module.modules.hud.ModulePotionCounter());
        register(new com.cheatbreaker.client.kazerui.module.modules.hud.ModuleServerAddress());
        register(new com.cheatbreaker.client.kazerui.module.modules.hud.ModuleDirectionHud());
        register(new com.cheatbreaker.client.kazerui.module.modules.hud.ModuleCoordinates());
        register(new com.cheatbreaker.client.kazerui.module.modules.hud.ModulePing());
        register(new com.cheatbreaker.client.kazerui.module.modules.hud.ModuleSaturation());
        register(new com.cheatbreaker.client.kazerui.module.modules.visual.Module3dSkins());
        register(new com.cheatbreaker.client.kazerui.module.modules.visual.ModuleItems2d());
        register(new com.cheatbreaker.client.kazerui.module.modules.visual.ModuleOverlay());
        register(new com.cheatbreaker.client.kazerui.module.modules.visual.ModuleParticleChanger());
        register(new com.cheatbreaker.client.kazerui.module.modules.visual.ModuleGlintColorizer());
        register(new com.cheatbreaker.client.kazerui.module.modules.visual.ModuleScrollableTooltips());
        register(new com.cheatbreaker.client.kazerui.module.modules.hud.ModuleStopwatch());
        register(new com.cheatbreaker.client.kazerui.module.modules.visual.ModuleItemPhysics());
        register(new com.cheatbreaker.client.kazerui.module.modules.visual.ModuleLightOverlay());
        register(new com.cheatbreaker.client.kazerui.module.modules.mechanic.ModuleWeatherChanger());
        register(new GlobalToggleModule("SHINY_POTS","legacy_shiny_pots",false));
        register(new GlobalToggleModule("MENU_BLUR","legacy_gui_blur",false));
        register(new com.cheatbreaker.client.kazerui.module.modules.mechanic.ModuleLighting());
        register(new com.cheatbreaker.client.kazerui.module.modules.mechanic.ModuleDamageTint());
        register(new com.cheatbreaker.client.kazerui.module.modules.visual.ModuleHitColor());
        register(new com.cheatbreaker.client.kazerui.module.modules.mechanic.ModuleFov());
        register(new com.cheatbreaker.client.kazerui.module.modules.mechanic.ModuleZoom());
        register(new com.cheatbreaker.client.kazerui.module.modules.visual.ModuleColorSaturation());
        register(new com.cheatbreaker.client.kazerui.module.modules.visual.ModuleBlockOutline());
        register(new com.cheatbreaker.client.kazerui.module.modules.visual.ModuleHitbox());
        register(new com.cheatbreaker.client.kazerui.module.modules.hud.ModulePvpInfo());
        register(new com.cheatbreaker.client.kazerui.module.modules.mechanic.ModuleTab());
        register(new com.cheatbreaker.client.kazerui.module.modules.hud.ModuleTitles());
        com.cheatbreaker.client.kazerui.module.modules.hud.ModuleCombo combo=new com.cheatbreaker.client.kazerui.module.modules.hud.ModuleCombo();
        com.cheatbreaker.client.kazerui.module.modules.hud.ModuleReachDisplay reach=new com.cheatbreaker.client.kazerui.module.modules.hud.ModuleReachDisplay();
        register(combo);register(reach);
        com.cheatbreaker.client.kazerui.module.modules.hud.CombatHud.initialize(combo,reach);
        OptionCatalog.provider = id -> { Module module = get(id); return module == null ? null : module.page(); };
        OptionCatalog.buttons = (id,key) -> {
            Module module = get(id);
            if (module == null) return;
            for (com.cheatbreaker.client.kazerui.module.setting.Setting<?> setting : module.settings())
                if (setting.key.equals(key) && setting instanceof com.cheatbreaker.client.kazerui.module.setting.ButtonSetting)
                    ((com.cheatbreaker.client.kazerui.module.setting.ButtonSetting)setting).press();
        };
    }
    private static void register(Module module) { MODULES.add(module); index(module); }
    public static void retainNativeSettings(com.cheatbreaker.client.module.AbstractModule module) {
        String id = com.cheatbreaker.client.kazerui.config.LegacyProfileMigration.moduleId(module.getName());
        if (id != null && get(id) == null) register(new NativeSettingsAdapter(module));
    }
    public static void retainGlobalSettings(com.cheatbreaker.client.config.GlobalSettings settings, Map<String,Object> defaults) {
        if (get("global") == null) register(new NativeGlobalSettings(settings, defaults));
    }
    private static void index(Module module) {
        BY_KEY.put(module.key(),module);
        for (Module child : module.children()) index(child);
    }
    static void attached(Module module) { index(module); if (hudListener != null) for (HudElement hud : module.huds()) hudListener.added(hud); }
    static void detached(Module module) {
        for (Module child : module.children()) detached(child);
        BY_KEY.remove(module.key());
        if (hudListener != null) for (HudElement hud : module.huds()) hudListener.removed(hud);
    }
    public static List<Module> modules() { return Collections.unmodifiableList(MODULES); }
    public static Module get(String id) { return BY_KEY.get(id.toLowerCase(Locale.ROOT)); }
    public static void tick() {
        for (Module module : BY_KEY.values()) module.tick(com.cheatbreaker.client.kazerui.event.UiTick.START);
        for (Module module : BY_KEY.values()) module.tick(com.cheatbreaker.client.kazerui.event.UiTick.END);
    }
    public static List<HudElement> huds() {
        List<HudElement> result = new ArrayList<HudElement>();
        for (Module module : BY_KEY.values()) result.addAll(module.huds());
        return result;
    }
    public static HudElement hud(String id) { Module module = get(id); return module == null || module.huds().isEmpty() ? null : module.huds().get(0); }
    public static UiModel model() { return model; }
    public static String store(String id,String option,String fallback) { return model == null ? fallback : model.value(id,option,fallback); }
    public static void put(String id,String option,String value) { if (model != null) model.set(id,option,value); }
    public static void remove(String id,String option) {
        if (model == null) return;
        model.store.remove(model.key(id,option)); model.revision++; changed(id,option);
    }
    public static void changed(String id,String option) {
        Module module = get(id);
        if (module == null) return;
        for (com.cheatbreaker.client.kazerui.module.setting.Setting<?> setting : module.settings()) if (setting.key.equals(option)) setting.changed();
    }
    private ModuleManager() { }
}
