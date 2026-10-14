package com.cheatbreaker.client.kazerui.module.modules.hud;

import com.cheatbreaker.client.kazerui.event.UiTick;
import com.cheatbreaker.client.kazerui.feature.HudAnchor;
import com.cheatbreaker.client.kazerui.module.Module;
import com.cheatbreaker.client.kazerui.module.Page;
import com.cheatbreaker.client.kazerui.module.hud.TextHud;
import com.cheatbreaker.client.kazerui.module.setting.*;

public final class ModuleStopwatch extends Module {
    public enum Mode implements ChoiceSetting.Option {
        STOPWATCH, COUNTDOWN;
        public String langId() { return null; }
    }
    private final StopwatchClock clock = new StopwatchClock();
    private final ChoiceSetting<Mode> mode = choice("timerMode", Mode.STOPWATCH);
    private final NumberSetting seconds = integer("countdownSeconds", 60, 1, 86400);
    private final BoolSetting tenths = bool("showTenths", true);
    private final BoolSetting pauseInMenu = bool("pauseInMenu", false);
    private final KeySetting startKey = keybind("startPauseKey"), resetKey = keybind("resetKey");
    private final ButtonSetting start = add(new ButtonSetting("startPause", () -> clock.toggle(System.nanoTime())).width(100));
    private final ButtonSetting reset = add(new ButtonSetting("resetTimer", () -> clock.reset(System.nanoTime())).width(90));
    private String displayed = "00:00.0";
    private long displayedUnit = Long.MIN_VALUE;
    private String profile;
    public ModuleStopwatch() {
        super("STOPWATCH", false); labels();
        mode.onChange(() -> clock.reset(System.nanoTime()));
        hud(new Hud());
    }
    private void labels() {
        for (Setting<?> setting : settings()) {
            switch (setting.key) {
                case "timerMode": setting.label(() -> "Timer Mode"); break;
                case "countdownSeconds": setting.label(() -> "Countdown Seconds"); break;
                case "showTenths": setting.label(() -> "Show Tenths"); break;
                case "pauseInMenu": setting.label(() -> "Pause in Menus"); break;
                case "startPauseKey": setting.label(() -> "Start / Pause Key"); break;
                case "resetKey": setting.label(() -> "Reset Key"); break;
                case "startPause": setting.label(() -> "Start / Pause"); break;
                case "resetTimer": setting.label(() -> "Reset Timer"); break;
                default: break;
            }
        }
    }
    @Override protected void layout(Page p) {
        p.add(mode, tenths, pauseInMenu);
        p.add(seconds).hideIf(() -> mode.get() != Mode.COUNTDOWN);
        p.add(startKey, resetKey, start, reset);
    }
    @Override protected void onDisable() { clock.pause(System.nanoTime()); }
    @Override public void tick(UiTick.ClientTickEvent event) {
        if (event.phase != UiTick.Phase.END) return;
        boolean active = isEnabled() && mc().theWorld != null && mc().currentScreen == null;
        boolean toggle = startKey.pressed(active), clear = resetKey.pressed(active);
        long now = System.nanoTime();
        String current = com.cheatbreaker.client.kazerui.module.ModuleManager.model().profile();
        if (!current.equals(profile)) { clock.reset(now); displayedUnit = Long.MIN_VALUE; profile = current; }
        if (!isEnabled()) { clock.pause(now); return; }
        if (mc().theWorld == null) clock.pause(now);
        if (toggle) clock.toggle(now);
        if (clear) clock.reset(now);
        if (pauseInMenu.on() && mc().currentScreen != null) clock.pause(now);
        long elapsed = clock.elapsed(now) / 1000000L;
        long value = mode.get() == Mode.COUNTDOWN ? Math.max(0, seconds.intValue() * 1000L - elapsed) : elapsed;
        if (mode.get() == Mode.COUNTDOWN && value == 0) clock.pause(now);
        long unit = tenths.on() ? value / 100 : value / 1000;
        long key = unit * 2 + (tenths.on() ? 1 : 0);
        if (key != displayedUnit) { displayedUnit = key; displayed = StopwatchClock.format(value, tenths.on()); }
    }
    private final class Hud extends TextHud {
        Hud() { super(ModuleStopwatch.this, 0, 40, HudAnchor.TOP_RIGHT, sizes(9, 18, 30, 40, 72, 110)); }
        @Override protected String text(boolean preview) { return preview ? "01:23.4" : displayed; }
        @Override protected Boolean staticWidthFor(String text) { return false; }
        @Override protected Boolean heightCondition() { return false; }
    }
}
