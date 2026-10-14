package com.cheatbreaker.client.kazerui.module;

import com.cheatbreaker.client.config.GlobalSettings;
import com.cheatbreaker.client.config.Setting;
import com.cheatbreaker.client.kazerui.event.UiTick;
import com.cheatbreaker.client.kazerui.gui.ui.OptionCatalog;
import com.cheatbreaker.client.kazerui.gui.ui.UiModel;
import java.util.*;

/** Keeps existing native rendering/input controls reachable through the owned UI.
 * Global controls are stored once, not changed implicitly when profiles switch. */
public final class NativeGlobalSettings extends Module {
    private static NativeGlobalSettings instance;
    private final List<NativeSettingsAdapter.Value> values = new ArrayList<NativeSettingsAdapter.Value>();
    private final Set<String> performance = new HashSet<String>();
    private final GlobalSettings nativeSettings;
    private long appliedRevision = Long.MIN_VALUE;
    private final com.cheatbreaker.client.kazerui.module.setting.BoolSetting borderless=bool("borderlessFullscreen",true);
    private final com.cheatbreaker.client.kazerui.module.setting.BoolSetting cloaks=bool("showCloaks",true);
    private final com.cheatbreaker.client.kazerui.module.setting.BoolSetting headwear=bool("showHeadwear",true);
    private final com.cheatbreaker.client.kazerui.module.setting.BoolSetting pets=bool("showPets",true);
    private final com.cheatbreaker.client.kazerui.module.setting.BoolSetting tabIcon=bool("showTabIcon",true),nametagIcon=bool("showNametagIcon",true);
    private final com.cheatbreaker.client.kazerui.module.setting.BoolSetting toggleNotification=bool("modToggleNotification",true);
    private final com.cheatbreaker.client.kazerui.module.setting.BoolSetting disableSorting=bool("disableModSorting",false);
    private final com.cheatbreaker.client.kazerui.module.setting.BoolSetting transparency=bool("uiTransparency",true);
    private final com.cheatbreaker.client.kazerui.module.setting.BoolSetting previews=bool("packPreviews",true);
    private final com.cheatbreaker.client.kazerui.module.setting.BoolSetting raw=bool("rawInput",false),debug=bool("hideDebug",true);
    private final GlobalHudStyle styles=new GlobalHudStyle(this);
    public NativeGlobalSettings(GlobalSettings settings, Map<String,Object> defaults) {
        super("global", true);
        nativeSettings = settings;
        instance=this;
        borderless.label(() -> "Borderless Fullscreen");
        borderless.onChange(() -> applyWindowMode());
        cloaks.label(() -> "Show Cloaks");headwear.label(() -> "Show Headwear");pets.label(() -> "Show Pets");
        tabIcon.label(()->"Show Tab Icon");nametagIcon.label(()->"Show Nametag Icon");
        toggleNotification.label(()->"Mod Toggle Notification");disableSorting.label(()->"Disable Mod Sorting");transparency.label(()->"UI Transparency");
        previews.label(()->"Pack Previews");
        raw.label(()->"Raw Mouse Input");debug.label(()->"Show HUD in F3");
        UiModel model = ModuleManager.model();
        if(!"true".equals(model.store.get("migration.badgeControls.v1",null))) {
            for(String profile:model.profiles) {
                String prefix="profile."+profile+".";
                if("true".equals(model.store.get(prefix+"tab.enabled","false"))
                    &&"false".equals(model.store.get(prefix+"tab.tabLogo","true")))
                    model.store.put(prefix+"nametags.legacy_show_kazer_icon_in_tab","false");
            }
            model.store.put("migration.badgeControls.v1","true");
        }
        boolean seed = !"true".equals(model.store.get("migration.globalSettings.v1",null));
        for (Setting setting : settings.settingsList) {
            if (setting==settings.showHudInDebug || setting.getLabel().equalsIgnoreCase("label") || setting.getValue()==null || setting.getValue() instanceof String[]) continue;
            Object fallback = defaults.get(setting.getLabel());
            if (fallback == null) fallback = setting.getValue();
            NativeSettingsAdapter.Value value = add(new NativeSettingsAdapter.Value(setting, fallback));
            value.label(setting::getDisplayLabel); value.onChange(value::apply);
            if (setting.getParent()!=null) value.hideIf(() -> !setting.isVisible());
            values.add(value);
            String label = setting.getLabel().toLowerCase(Locale.ROOT);
            if (label.contains("fps") || label.contains("chunk") || label.contains("particle") || label.contains("distance")) performance.add(value.key);
            if (seed && model.store.get(model.key("global",value.key),null)==null)
                model.store.put(model.key("global",value.key),value.format(setting.getValue()));
        }
        if (seed && model.store.get(model.key("global","hideDebug"),null)==null)
            model.store.put(model.key("global","hideDebug"),String.valueOf(!(Boolean)settings.showHudInDebug.getValue()));
        model.store.put("migration.globalSettings.v1","true");
        com.cheatbreaker.client.kazerui.module.modules.visual.ModuleOverlay.migrateLegacy(settings);
        model.revision++;
    }
    public List<OptionCatalog.Node> options(boolean perf) {
        Page page = new Page();
        if(!perf)page.section("General",s -> {
            s.add(borderless,raw);
            addNative(s,"legacy_keep_inventory_centered","legacy_disable_hotbar_scrolling");
        });
        if(!perf)page.section("HUD Options",s -> {s.add(debug);addNative(s,"legacy_show_crosshair_in_third_person");});
        if(!perf)page.section("Notification Options",s -> addNative(s,"legacy_show_achievements"));
        if(!perf)page.section("Resource Pack Options",s -> {addNative(s,"legacy_transparent_background");s.add(previews);});
        if(!perf)page.section("Cosmetics",s -> s.add(cloaks,headwear,pets,tabIcon,nametagIcon));
        if(!perf)page.section("Mod Options",s -> s.add(toggleNotification,disableSorting));
        if(!perf)page.section("Interface",s -> addNative(s,"legacy_scrolling_speed_"));
        if(!perf)page.section("Interface",s -> s.add(transparency));
        if(!perf)styles.layout(page);
        page.section(perf?"Performance":"Other Options",s -> {
            for (NativeSettingsAdapter.Value value : values) if (performance.contains(value.key)==perf && !hasModulePage(value.key)&&!organized(value.key)) s.add(value);
        });
        return page.nodes();
    }
    @Override protected void layout(Page page) {
        for(com.cheatbreaker.client.kazerui.module.setting.Setting<?> setting:settings())page.add(setting);
    }
    private void addNative(Page page,String...keys) {
        for(String key:keys)for(NativeSettingsAdapter.Value value:values)if(value.key.equals(key))page.add(value);
    }
    private static boolean organized(String key) {
        return key.equals("legacy_keep_inventory_centered")||key.equals("legacy_disable_hotbar_scrolling")
            ||key.equals("legacy_show_crosshair_in_third_person")||key.equals("legacy_show_achievements")
            ||key.equals("legacy_transparent_background")||key.equals("legacy_scrolling_speed_");
    }
    private static void applyWindowMode() {
        cn.kazer.client189.input.FullscreenTransition.applyCurrent();
    }
    public static boolean cosmeticVisible(com.cheatbreaker.client.kazerui.cosmetics.CosmeticCategory category) {
        NativeGlobalSettings global=instance;
        return global==null|| (category==com.cheatbreaker.client.kazerui.cosmetics.CosmeticCategory.CLOAKS?global.cloaks:
            category==com.cheatbreaker.client.kazerui.cosmetics.CosmeticCategory.PETS?global.pets:global.headwear).on();
    }
    public static boolean packPreviews(){return instance==null||instance.previews.on();}
    private static boolean hasModulePage(String key) {
        // Keep stored aliases and native synchronization, but expose each control once.
        String label=key.replace("legacy_", "").replace("_", "").toLowerCase(Locale.ROOT);
        if (label.equals("mutekazersounds") || label.equals("mutecheatbreakersounds")) return true;
        if(ModuleManager.get("overlay")!=null&&(label.equals("enchantmentglint")||label.equals("clearglass")||label.equals("redstring")))return true;
        if (label.equals("worldtime") && ModuleManager.get("world_time")!=null) return true;
        for (Module module : ModuleManager.modules())
            if (!module.id.equals("global") && label.equals(module.id.replace("_","").toLowerCase(Locale.ROOT))) return true;
        return label.equals("fullbright") || label.equals("shinypots") || label.equals("guiblur")
            || label.equals("menublur") || label.equals("showchatbackground")
            || label.startsWith("crosshair") && ModuleManager.get("crosshair")!=null;
    }
    public void apply() {
        if(org.lwjgl.opengl.Display.isCreated()&&net.minecraft.client.Minecraft.getMinecraft().isFullScreen()
            &&borderless.on()!=com.cheatbreaker.client.kazerui.input.WindowMode.active())applyWindowMode();
        for (NativeSettingsAdapter.Value value : values) value.apply();
        boolean show = !ModuleManager.model().flag("global","hideDebug",true);
        if (!Boolean.valueOf(show).equals(nativeSettings.showHudInDebug.getValue())) nativeSettings.showHudInDebug.setValue(show,false);
        appliedRevision = ModuleManager.model().revision;
    }
    @Override public void tick(UiTick.ClientTickEvent event) {
        if (event.phase==UiTick.Phase.START && appliedRevision!=ModuleManager.model().revision) apply();
    }
}
