package com.cheatbreaker.client.kazerui.gui.ui;

import com.cheatbreaker.client.kazerui.KazerUiBrand;
import java.util.*;

/** Authoritative catalog of real adapted modules. Hypixel-specific modules are excluded. */
public final class ModuleCatalog {
    public static final class Module {
        public final String id, name, category, icon;
        public final boolean implemented = true;
        Module(String id, String name, String category, String icon) {
            this.id = id; this.name = name; this.category = category; this.icon = icon;
        }
    }
    public static final List<String> PORTED = Collections.emptyList();
    public static final List<Module> ALL;
    public static final List<String> CODE;
    static {
        List<Module> modules = new ArrayList<Module>();
        modules.add(new Module("armorstatus", "Armor Status", "HUD", "armorstatus"));
        modules.add(new Module("toggle_sneak", "ToggleSprint", "MECHANIC", "toggle_sneak"));
        modules.add(new Module("potion_effects", "Potion Effects", "HUD", "potion_effects"));
        modules.add(new Module("keystrokes", "Key Strokes", "HUD", "keystrokes"));
        modules.add(new Module("fps", "FPS", "HUD", "fps"));
        modules.add(new Module("cps", "CPS", "HUD", "cps"));
        modules.add(new Module("nametags", "Nametags", "MECHANIC", "nametag"));
        modules.add(new Module("bossbar", "Boss bar", "HUD", "bossbar"));
        modules.add(new Module("motion_blur", "Motion Blur", "MECHANIC", "motion_blur"));
        modules.add(new Module("scoreboard", "Scoreboard", "HUD", "scoreboard"));
        modules.add(new Module("chat", "Chat", "MECHANIC", "chat"));
        modules.add(new Module("clock", "Clock", "HUD", "clock"));
        modules.add(new Module("memory", "Memory", "HUD", "memory"));
        modules.add(new Module("day_counter", "Day Counter", "HUD", "day_counter"));
        modules.add(new Module("potion_counter", "Potion Counter", "HUD", "potion_counter"));
        modules.add(new Module("server_address", "Server Address", "HUD", "server_address"));
        modules.add(new Module("direction_hud", "Direction HUD", "HUD", "direction_hud"));
        modules.add(new Module("coordinates", "Coordinates", "HUD", "coordinates"));
        modules.add(new Module("combo", "Combo", "HUD", "combo"));
        modules.add(new Module("reach_display", "Reach Display", "HUD", "reach_display"));
        modules.add(new Module("freelook", "Freelook", "MECHANIC", "freelook"));
        modules.add(new Module("ping", "Ping", "HUD", "ping"));
        modules.add(new Module("saturation", "Saturation", "HUD", "saturation"));
        modules.add(new Module("3d_skins", "3D Skins", "MECHANIC", "3d_skins"));
        modules.add(new Module("2d_items", "2D Items", "MECHANIC", "2d_items"));
        modules.add(new Module("overlay", "Overlay", "MECHANIC", "overlay"));
        modules.add(new Module("particle_changer", "Particle Changer", "MECHANIC", "particle_changer"));
        modules.add(new Module("glint_colorizer", "Glint Colorizer", "MECHANIC", "glint_colorizer"));
        modules.add(new Module("scrollable_tooltips", "Scrollable Tooltips", "MECHANIC", "scrollable_tooltips"));
        modules.add(new Module("stopwatch", "Stopwatch", "HUD", "stopwatch"));
        modules.add(new Module("item_physics", "Item Physics", "MECHANIC", "item_physics"));
        modules.add(new Module("light_overlay", "Light Overlay", "MECHANIC", "light_overlay"));
        modules.add(new Module("weather_changer", "Weather Changer", "MECHANIC", "weather_changer"));
        modules.add(new Module("shiny_pots", "Shiny Pots", "MECHANIC", "shiny_pots"));
        modules.add(new Module("menu_blur", "Menu Blur", "MECHANIC", "menu_blur"));
        modules.add(new Module("lighting", "Lighting", "MECHANIC", "lighting"));
        modules.add(new Module("damage_tint", "Damage Tint", "MECHANIC", "damage_tint"));
        modules.add(new Module("hit_color", "Hit Color", "MECHANIC", "hit_color"));
        modules.add(new Module("fov", "Fov Changer", "MECHANIC", "fov"));
        modules.add(new Module("zoom", "Zoom", "MECHANIC", "zoom"));
        modules.add(new Module("color_saturation", "Color Saturation", "MECHANIC", "color_saturation"));
        modules.add(new Module("block_outline", "Block Outline", "MECHANIC", "block_outline"));
        modules.add(new Module("hitbox", "Hitbox", "MECHANIC", "hitbox"));
        modules.add(new Module("pvp_info", "PvP Info", "HUD", "pvp_info"));
        modules.add(new Module("tab", "Tab Editor", "MECHANIC", "tab"));
        modules.add(new Module("titles", "Title", "HUD", "titles"));
        if (KazerUiBrand.MINECRAFT.equals("1.8.9"))
            modules.add(new Module("one_seven_visuals", "1.7 Visuals", "MECHANIC", "one_seven_visuals"));
        ALL = Collections.unmodifiableList(modules);
        List<String> ids = new ArrayList<String>();
        for (Module module : modules) ids.add(module.id);
        CODE = Collections.unmodifiableList(ids);
    }
    public static boolean defaultEnabled(String id) {
        com.cheatbreaker.client.kazerui.module.Module module = com.cheatbreaker.client.kazerui.module.ModuleManager.get(id);
        return module != null && module.defaultEnabled();
    }
    public static Module find(String id) {
        for (Module module : ALL) if (module.id.equals(id)) return module;
        throw new IllegalArgumentException("Unknown module: " + id);
    }
    private ModuleCatalog() { }
}
