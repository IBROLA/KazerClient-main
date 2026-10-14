package com.cheatbreaker.client.config;

import com.cheatbreaker.client.CheatBreaker;
import com.cheatbreaker.client.ui.element.type.ColorPickerColorElement;
import lombok.Getter;
import net.minecraft.client.Minecraft;
import net.minecraft.client.settings.GameSettings;
import net.minecraft.client.settings.KeyBinding;
import org.apache.commons.lang3.ArrayUtils;

import java.util.ArrayList;
import java.util.List;

public class GlobalSettings {

    public static final boolean DEFAULT_CONTAINER_VIGNETTE_ENABLED = true;
    public static final int DEFAULT_CONTAINER_VIGNETTE_STRENGTH_PERCENT = 50;

    public KeyBinding openMenu;
    public KeyBinding dragLook;
    public final KeyBindingRegistry keyBindings = new KeyBindingRegistry();
    public final List<Setting> settingsList = new ArrayList<>();
    public final FavoriteModules favoriteModules = new FavoriteModules();
    public List<String[]> pinnedServers;
    public List<String> warnedServers;
    public boolean isDebug = true;
    public String crashReportURL = "";
    public String debugUploadURL = "";
    public String cosmeticApiURL = "";
    public String cosmeticAllApiURL = "";
    public String mojangStatusURL = "https://status.mojang.com/check";
    public int reconnectTime = 60;
    public boolean IIIlllIIIllIllIlIIIIIIlII = true;
    public Setting muteCheatBreakerSounds;
    private Setting fpsBoostLabel;
    public Setting enableFpsBoost;
    public Setting slowChunkLoading;
    public Setting fullBright;
    public Setting enchantmentGlint;
    private Setting teamViewLabel;
    public Setting enableTeamView;
    public Setting showDistance;
    public Setting showOffScreenMarker;
    private Setting generalSettingsLabel;
    public Setting guiBlur;
    public Setting worldTime;
    public Setting snapModules;
    private Setting renderSettingsLabel;
    public Setting showHudInDebug;
    public Setting showChatBackground;
    public Setting shinyPots;
    public Setting showPotionInfo;
    public Setting clearGlass;
    public Setting redString;
    public Setting transparentBackground;
    public Setting containerVignette;
    public Setting containerVignetteStrength;
    @Getter private Setting crosshairSettingsLabel;
    public Setting customCrosshair;
    public Setting crosshairOutline;
    public Setting crosshairColor;
    public Setting crosshairThickness;
    public Setting crosshairSize;
    public Setting crosshairGap;
    private Setting colorOptionsLabel;
    public Setting defaultColor;
    public Setting particleAmount;
    public Setting limitUnfocusedFps;
    public Setting maxUnfocusedFps;
    public Setting maxMainMenuFps;
    public Setting entityDistance;
    public Setting tileEntityDistance;
    public Setting keepInventoryCentered;
    public Setting disableHotbarScrolling;
    public Setting showAchievements;
    public Setting showCrosshairThirdPerson;
    public Setting scrollingSpeed;
    // what is this xd
    // Nox: apparently, CheatBreaker code, Decencies.
    public List<ColorPickerColorElement> favouriteColors = new ArrayList<>();
    public List<ColorPickerColorElement> IlIIlIIlIllIIIIllIIllIlIl = new ArrayList<>();

//    public boolean lIIIIlIIllIIlIIlIIIlIIllI() {
//        return (Boolean)this.IlIIlIIIIlIIIIllllIIlIllI.getValue() == false || (Boolean)this.lIllIllIlIIllIllIlIlIIlIl.getValue() != false;
//    }

    public boolean isFavouriteColor(int n) {
        for (ColorPickerColorElement colorPickerColorElement : this.favouriteColors) {
            if (colorPickerColorElement.color != n) continue;
            return true;
        }
        return false;
    }

    public void removeFavouriteColor(int n) {
        this.favouriteColors.removeIf(colorPickerColorElement -> colorPickerColorElement.color == n);
    }

    public GlobalSettings() {
        this.muteCheatBreakerSounds = (new Setting(this.settingsList, "Mute Kazer sounds")).setValue(false);
        this.fpsBoostLabel = new Setting(this.settingsList, "label").setValue("FPS Boost");
        this.enableFpsBoost = new Setting(this.settingsList, "Enable FPS Boost").setValue(true);
        this.slowChunkLoading = new Setting(this.settingsList, "Slow chunk loading (%)").setMinMax(5, 100).setValue(30);
        this.fullBright = new Setting(this.settingsList, "Fullbright").setValue(true);
        this.enchantmentGlint = new Setting(this.settingsList, "Enchantment Glint").setValue(true);
        this.teamViewLabel = new Setting("label").setValue("Team View Settings");
        this.enableTeamView = new Setting("Enable Team View").setValue(false);
        this.showOffScreenMarker = new Setting("Show off-screen marker").setValue(false);
        this.showDistance = new Setting("Show distance").setValue(false);
        this.generalSettingsLabel = new Setting(this.settingsList, "label").setValue("General Settings");
        this.guiBlur = new Setting(this.settingsList, "GUI Blur").setValue(false);
        this.worldTime = new Setting(this.settingsList, "World Time").setValue(-14490).setMinMax(-22880, -6100);
        this.snapModules = new Setting(this.settingsList, "Snap mods to other mods (GUI)").setValue(true);
        this.renderSettingsLabel = new Setting(this.settingsList, "label").setValue("Render Settings");
        this.showPotionInfo = new Setting(this.settingsList, "Show Potion info in inventory").setValue(false);
        this.showChatBackground = new Setting(this.settingsList, "Show chat background").setValue(true);
        this.showHudInDebug = new Setting(this.settingsList, "Show HUD while in debug view").setDisplayLabel("Show HUD in F3").setValue(false);
        this.shinyPots = new Setting(this.settingsList, "Shiny Pots").setValue(false);
        this.clearGlass = new Setting(this.settingsList, "Clear Glass").setValue("REGULAR").acceptedValues("OFF", "REGULAR", "ALL");
        this.redString = new Setting(this.settingsList, "Red String").setValue(true);
        this.transparentBackground = new Setting(this.settingsList, "Transparent background").setValue(true);
        this.containerVignette = new Setting(this.settingsList, "Vignette")
                .setValue(DEFAULT_CONTAINER_VIGNETTE_ENABLED);
        this.containerVignetteStrength = new Setting(this.settingsList, "Vignette Strength")
                .setValue(DEFAULT_CONTAINER_VIGNETTE_STRENGTH_PERCENT).setMinMax(0, 100)
                .setSliderMarker(DEFAULT_CONTAINER_VIGNETTE_STRENGTH_PERCENT / 100.0F);
        this.crosshairSettingsLabel = new Setting(this.settingsList, "label").setValue("Crosshair Settings");
        this.customCrosshair = new Setting(this.settingsList, "Custom crosshair").setValue(false);
        this.crosshairOutline = new Setting(this.settingsList, "Outline").setValue(false);
        this.crosshairColor = new Setting(this.settingsList, "Color").setValue(-1).setMinMax(Integer.MIN_VALUE, Integer.MAX_VALUE);
        this.crosshairThickness = new Setting(this.settingsList, "Thickness").setValue(2.0F).setMinMax(1.0F, 1.8478261F * 1.3529412F);
        this.crosshairSize = new Setting(this.settingsList, "Size").setValue((float)4).setMinMax(1.0F, (float)10);
        this.crosshairGap = new Setting(this.settingsList, "Gap").setValue(4.4722223F * 0.39130434F).setMinMax(0.0F, 1.0493827F * 7.147059F);
        this.colorOptionsLabel = new Setting(this.settingsList, "label").setValue("Color Options");
        this.defaultColor = new Setting(this.settingsList, "Default color").setValue(-1).setMinMax(Integer.MIN_VALUE, Integer.MAX_VALUE);
        this.particleAmount = new Setting(this.settingsList, "Particle Amount (%)").setValue(100).setMinMax(0, 100);
        this.limitUnfocusedFps = new Setting(this.settingsList, "Limit Unfocused FPS").setValue(false);
        this.maxUnfocusedFps = new Setting(this.settingsList, "Max Unfocused FPS").setValue(15).setMinMax(5, 120).setParent(this.limitUnfocusedFps);
        this.maxMainMenuFps = new Setting(this.settingsList, "Max Main Menu FPS").setValue(90).setMinMax(30, 240);
        // Zero preserves vanilla distance rules rather than silently hiding distant entities.
        this.entityDistance = new Setting(this.settingsList, "Entity Distance")
                .setDisplayLabel("Entity Dist. (0=MC)").setValue(0).setMinMax(0, 256);
        this.tileEntityDistance = new Setting(this.settingsList, "Tile Entity Distance")
                .setDisplayLabel("Tile Dist. (0=MC)").setValue(0).setMinMax(0, 256);
        this.keepInventoryCentered = new Setting(this.settingsList, "Keep Inventory Centered").setValue(false);
        this.disableHotbarScrolling = new Setting(this.settingsList, "Disable Hotbar Scrolling").setValue(false);
        this.showAchievements = new Setting(this.settingsList, "Show Achievements").setValue(true);
        this.showCrosshairThirdPerson = new Setting(this.settingsList, "Show Crosshair in Third Person").setValue(true);
        this.scrollingSpeed = new Setting(this.settingsList, "Scrolling Speed (%)").setValue(100).setMinMax(25, 200);
        this.pinnedServers = new ArrayList<>();
        this.pinnedServers.add(new String[]{"Kazer Network", "kazer.cn"});
        this.warnedServers = new ArrayList<>();
        this.warnedServers.add("xyz.com");
        GameSettings var2 = Minecraft.getMinecraft().gameSettings;
        // These privately managed bindings do not belong to vanilla's list.
        // A new global category leaves null rows in GuiKeyBindingList's array.
        this.openMenu = new KeyBinding("Open Menu", 54, "key.categories.misc");
        this.dragLook = new KeyBinding("Drag to look", 56, "key.categories.misc");
        // Kazer-owned bindings are configured in Settings / Controls, not vanilla Controls.
        this.keyBindings.register("Open Menu", this.openMenu);
        this.keyBindings.register("Freelook", this.dragLook, "Freelook");
        this.keyBindings.register("Zoom", cn.kazer.client189.compat.KazerCompat.zoomKey(), "of.key.zoom");
    }
}
