package com.cheatbreaker.client.kazerui;

import com.cheatbreaker.client.CheatBreaker;
import com.cheatbreaker.client.config.ConfigManager;
import com.cheatbreaker.client.config.Setting;
import com.cheatbreaker.client.event.type.ClickEvent;
import com.cheatbreaker.client.event.type.GuiDrawEvent;
import com.cheatbreaker.client.event.type.TickEvent;
import com.cheatbreaker.client.event.type.KeyboardEvent;
import com.cheatbreaker.client.kazerui.feature.FeatureManager;
import com.cheatbreaker.client.kazerui.gui.KazerClickGui;
import com.cheatbreaker.client.kazerui.gui.KazerMovementScreen;
import com.cheatbreaker.client.kazerui.gui.home.KazerHomeScreen;
import com.cheatbreaker.client.kazerui.module.ModuleManager;
import com.cheatbreaker.client.kazerui.util.ClickCounter;
import com.cheatbreaker.client.module.AbstractModule;
import java.io.File;
import java.util.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.settings.KeyBinding;

/** Native lifecycle owner; no runtime reflection and no duplicate HUD consumers. */
public final class KazerUiRuntime {
    private final CheatBreaker core;
    private final FeatureManager features;
    private final List<AbstractModule> replaced = new ArrayList<AbstractModule>();
    private long appliedRevision = Long.MIN_VALUE;
    private long cosmeticPermissionRevision=Long.MIN_VALUE;
    private final com.cheatbreaker.client.kazerui.input.KazerMouseHelper mouse=new com.cheatbreaker.client.kazerui.input.KazerMouseHelper();
    private static final boolean NATIVE_1710=KazerUiBrand.MINECRAFT.equals("1.7.10");
    private final com.cheatbreaker.client.kazerui.module.modules.hud.armorstatus.ArmorBars armorBars;
    public static Map<String,Object> globalDefaults(CheatBreaker core) {
        Map<String,Object> result = new HashMap<String,Object>();
        for (Setting setting : core.globalSettings.settingsList) result.put(setting.getLabel(),setting.getValue());
        return result;
    }
    public KazerUiRuntime(CheatBreaker core, Map<String,Object> defaults) {
        this.core = core;
        Minecraft.getMinecraft().mouseHelper=mouse;
        ((net.minecraft.client.resources.IReloadableResourceManager)Minecraft.getMinecraft().getResourceManager())
            .registerReloadListener(resources -> {
                KazerResourceEpoch.reloaded();
                com.cheatbreaker.client.kazerui.module.modules.visual.ModuleMotionBlur.resourceReload();
            });
        features = new FeatureManager(new File(ConfigManager.configDir,"owned-ui.properties"));
        com.cheatbreaker.client.kazerui.cosmetics.Loadout.initialize(features.ui().store);
        com.cheatbreaker.client.kazerui.cosmetics.KazerCdn.preload();
        features.retainNativeSettings(core);
        ModuleManager.retainGlobalSettings(core.globalSettings,defaults);
        armorBars=(com.cheatbreaker.client.kazerui.module.modules.hud.armorstatus.ArmorBars)ModuleManager.get("armorstatus_bars_child");
        for (AbstractModule module : core.moduleManager.modules) {
            String id = com.cheatbreaker.client.kazerui.config.LegacyProfileMigration.moduleId(module.getName());
            com.cheatbreaker.client.kazerui.module.Module imported = id == null ? null : ModuleManager.get(id);
            if (imported != null && !(imported instanceof com.cheatbreaker.client.kazerui.module.NativeSettingsAdapter)) {
                module.retireEventsForSharedUi(); replaced.add(module);
            }
        }
        String raw = features.get("menuKey",null);
        if (raw != null) try {
            int key = Integer.parseInt(raw);
            if (key>=0 && key<256) { core.globalSettings.openMenu.setKeyCode(key); KeyBinding.resetKeyBindingArrayAndHash(); }
        } catch (NumberFormatException ignored) { }
        for(String id:new String[]{"freelook","zoom"}) {
            KeyBinding binding=core.globalSettings.keyBindings.binding(id.equals("freelook")?"Freelook":"Zoom");
            String saved=features.get(id+"Key",null);
            if(binding!=null && saved!=null) try {
                int key=Integer.parseInt(saved);if(key>=-100 && key<256)binding.setKeyCode(key);
            }catch(NumberFormatException ignored){}
        }
        KeyBinding.resetKeyBindingArrayAndHash();
        core.eventBus.addEvent(TickEvent.class,event -> tick());
        core.eventBus.addEvent(GuiDrawEvent.class,event -> {
            com.cheatbreaker.client.kazerui.module.modules.mechanic.ModuleDamageTint.render(event.getResolution());
            if (NATIVE_1710 && features.isEnabled("bossbar") && net.minecraft.entity.boss.BossStatus.statusBarTime>0)
                --net.minecraft.entity.boss.BossStatus.statusBarTime;
            if (!(Minecraft.getMinecraft().currentScreen instanceof KazerMovementScreen))
                features.renderHud(event.getResolution());
            if (Minecraft.getMinecraft().currentScreen==null) com.cheatbreaker.client.kazerui.gui.KazerNotifications.render();
        });
        core.eventBus.addEvent(ClickEvent.class,event -> {
            ClickCounter.click(event.getMouseButton(),Minecraft.getMinecraft().thePlayer!=null && Minecraft.getMinecraft().thePlayer.isUsingItem());
            toggleBinding(event.getMouseButton()-100);
        });
        core.eventBus.addEvent(KeyboardEvent.class,event -> toggleBinding(event.getKeyboardKey()));
        tick(); features.save();
    }
    private void toggleBinding(int key) {
        if (key==0 || Minecraft.getMinecraft().currentScreen!=null) return;
        for (com.cheatbreaker.client.kazerui.gui.ui.ModuleCatalog.Module module : com.cheatbreaker.client.kazerui.gui.ui.ModuleCatalog.ALL) {
            String binding=features.ui().value(module.id,"toggleKeybind","NONE");
            if (!binding.equals("NONE") && com.cheatbreaker.client.kazerui.module.setting.KeySetting.code(binding)==key)
                features.toggle(module.id);
        }
    }
    private void tick() {
        if(home!=null && Minecraft.getMinecraft().theWorld!=null) {home.releaseResources();home=null;}
        com.cheatbreaker.client.kazerui.cosmetics.Loadout.refreshPermissions();
        long permissions=com.cheatbreaker.client.kazerui.cosmetics.CosmeticPermissions.revision;
        if(cosmeticPermissionRevision!=permissions){cosmeticPermissionRevision=permissions;features.ui().revision++;}
        ModuleManager.tick();
        if (appliedRevision==features.ui().revision) return;
        mouse.enabled(features.ui().flag("global","rawInput",false));
        for (AbstractModule module : replaced) {
            String id = com.cheatbreaker.client.kazerui.config.LegacyProfileMigration.moduleId(module.getName());
            module.applySharedUiState(features.isEnabled(id));
        }
        appliedRevision=features.ui().revision;
    }
    public FeatureManager features() { return features; }
    public static boolean isOwnedScreen(GuiScreen screen) {
        return screen instanceof KazerHomeScreen || screen instanceof com.cheatbreaker.client.kazerui.gui.home.KazerAccountScreen
                || screen instanceof KazerClickGui || screen instanceof KazerMovementScreen;
    }
    public boolean bossPre() {
        if (!features.isEnabled("bossbar")) return false;
        if (net.minecraft.entity.boss.BossStatus.statusBarTime>0) --net.minecraft.entity.boss.BossStatus.statusBarTime;
        return true;
    }
    public boolean hasArmorOverlay() { return armorBars!=null && armorBars.isEnabled(); }
    public boolean renderArmor(net.minecraft.client.gui.ScaledResolution resolution,int baseline) {
        if (!hasArmorOverlay()) return false;
        com.cheatbreaker.client.kazerui.event.UiOverlayMetrics.left_height=baseline;
        com.cheatbreaker.client.kazerui.event.UiOverlay.Pre event=new com.cheatbreaker.client.kazerui.event.UiOverlay.Pre(
                com.cheatbreaker.client.kazerui.event.UiOverlay.ElementType.ARMOR,resolution);
        armorBars.onOverlay(event);return event.isCanceled();
    }
    private KazerHomeScreen home;
    public GuiScreen home() {
        if(home==null)home=new KazerHomeScreen(features,core.globalSettings.openMenu);
        return home;
    }
    public GuiScreen menu(GuiScreen parent) { return new KazerClickGui(features,core.globalSettings.openMenu,parent); }
    public GuiScreen hudEditor(GuiScreen parent) { return new KazerMovementScreen(features,core.globalSettings.openMenu,parent); }
    public void save() { features.save(); }
}
