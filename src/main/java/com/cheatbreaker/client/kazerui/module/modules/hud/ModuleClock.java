package com.cheatbreaker.client.kazerui.module.modules.hud;

import com.cheatbreaker.client.kazerui.feature.HudAnchor;
import com.cheatbreaker.client.kazerui.module.Module;
import com.cheatbreaker.client.kazerui.module.Page;
import com.cheatbreaker.client.kazerui.module.hud.TextHud;
import com.cheatbreaker.client.kazerui.module.setting.BoolSetting;
import java.text.SimpleDateFormat;
import java.util.Date;

import com.cheatbreaker.client.kazerui.event.UiTick;

public final class ModuleClock extends Module {
    private final SimpleDateFormat hourMinute = new SimpleDateFormat("h:mm",java.util.Locale.ENGLISH);
    private final SimpleDateFormat hourMinuteAmPm = new SimpleDateFormat("h:mm a",java.util.Locale.ENGLISH);
    private final SimpleDateFormat military = new SimpleDateFormat("HH:mm",java.util.Locale.ENGLISH);
    private final BoolSetting militaryTime = bool("militaryTime", false);
    private final BoolSetting showAmPm = bool("showAmPm", true).onChange(this::update);

    private final Date date = new Date();
    private String text;
    private long second = -1;

    public ModuleClock() {
        super("CLOCK", false);
        hud(new Hud());
        militaryTime.onChange(this::update);
    }

    @Override protected void layout(Page page) {
        page.section("settings", s -> {
            s.add(militaryTime);
            s.add(showAmPm).hideIf(militaryTime::on);
        });
    }

    private void update() {
        date.setTime(System.currentTimeMillis());
        text = militaryTime.on() ? military.format(date) : showAmPm.on() ? hourMinuteAmPm.format(date) : hourMinute.format(date);
    }

    
    public void tick(UiTick.ClientTickEvent event) {
        if (event.phase != UiTick.Phase.END || !isEnabled()) return;
        long now = System.currentTimeMillis() / 1000L;
        if (now == second) return;
        second = now;
        update();
    }

    private final class Hud extends TextHud {
        Hud() { super(ModuleClock.this, 0, 0, HudAnchor.TOP_RIGHT, sizes(10, 18, 22, 46, 56, 62)); }

        @Override protected String text(boolean preview) { return text == null ? "" : text; }
    }
}
