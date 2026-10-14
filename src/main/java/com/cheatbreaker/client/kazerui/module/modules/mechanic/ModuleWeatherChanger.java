package com.cheatbreaker.client.kazerui.module.modules.mechanic;

import com.cheatbreaker.client.kazerui.module.Module;
import com.cheatbreaker.client.kazerui.module.Page;
import com.cheatbreaker.client.kazerui.module.setting.Setting;
import com.cheatbreaker.client.kazerui.module.setting.BoolSetting;
import com.cheatbreaker.client.kazerui.module.setting.ChoiceSetting;
import com.cheatbreaker.client.kazerui.module.setting.NumberSetting;

/** Only renderer call sites use these helpers. Never overrides World weather state. */
public final class ModuleWeatherChanger extends Module {
    public enum Type implements ChoiceSetting.Option {
        NATURAL, CLEAR, RAIN, SNOW;
        public String langId() { return null; }
    }
    private static ModuleWeatherChanger instance;
    private final ChoiceSetting<Type> weatherMode = choice("weatherMode", Type.CLEAR);
    private final NumberSetting rainStrength = decimal("rainStrength", 1, 0, 1);
    private final BoolSetting weatherSounds = bool("weatherSounds", true);
    public ModuleWeatherChanger() { super("WEATHER_CHANGER", false); instance = this; labels(); }
    private void labels() {
        for (Setting<?> setting : settings()) {
            switch (setting.key) {
                case "weatherMode": setting.label(() -> "Weather Mode"); break;
                case "rainStrength": setting.label(() -> "Precipitation Strength"); break;
                case "weatherSounds": setting.label(() -> "Weather Sounds"); break;
                default: break;
            }
        }
    }
    @Override protected void layout(Page p) {
        p.add(weatherMode);
        p.add(rainStrength).hideIf(() -> weatherMode.get() == Type.CLEAR || weatherMode.get() == Type.NATURAL);
        p.add(weatherSounds);
    }
    private static Type mode() {
        return instance == null || !instance.isEnabled() ? Type.NATURAL : instance.weatherMode.get();
    }
    public static float strength(float natural) {
        Type type = mode();
        return type == Type.NATURAL ? natural : type == Type.CLEAR ? 0 : instance.rainStrength.value();
    }
    public static boolean precipitation(boolean natural) {
        Type type = mode();
        return type == Type.NATURAL ? natural : type != Type.CLEAR;
    }
    public static float temperature(float natural) {
        Type type = mode();
        return type == Type.SNOW ? 0 : type == Type.RAIN ? 0.5f : natural;
    }
    public static boolean sounds() {
        return instance == null || !instance.isEnabled() || instance.weatherSounds.on();
    }
    public static float thunder(float natural) { return mode() == Type.NATURAL ? natural : 0; }
    public static boolean muteSound(String path) {
        return instance != null && instance.isEnabled() && (!instance.weatherSounds.on() || mode() == Type.CLEAR)
            && path != null && path.startsWith("ambient.weather.");
    }
}
