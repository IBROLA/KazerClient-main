package com.cheatbreaker.client.kazerui.module.modules.hud;

import com.cheatbreaker.client.kazerui.feature.HudAnchor;
import com.cheatbreaker.client.kazerui.module.Module;
import com.cheatbreaker.client.kazerui.module.Page;
import com.cheatbreaker.client.kazerui.module.hud.TextHud;
import com.cheatbreaker.client.kazerui.module.setting.BoolSetting;

public final class ModuleDayCounter extends Module {
    private final BoolSetting useWorldType = bool("useWorldType", false);

    public ModuleDayCounter() {
        super("DAY_COUNTER", false);
        hud(new Hud());
    }

    @Override protected void layout(Page page) {
        page.section("settings", s -> s.add(useWorldType));
    }

    private final class Hud extends TextHud {
        private long lastDay=Long.MIN_VALUE;
        private String cachedDay;
        Hud() { super(ModuleDayCounter.this, 0, 0, HudAnchor.TOP_CENTER, sizes(10, 18, 22, 46, 56, 62)); }

        @Override protected Boolean staticWidthFor(String text) { return Boolean.FALSE; }

        @Override protected String text(boolean preview) {
            if (mc().theWorld == null || mc().theWorld.getWorldInfo() == null) {
                return preview ? lang("days", 21) : null;
            }
            long day = useWorldType.on()
                    ? mc().theWorld.getWorldTime() / 24000L
                    : mc().theWorld.getWorldInfo().getWorldTotalTime() / 24000L;
            if(day!=lastDay || cachedDay==null){lastDay=day;cachedDay=lang(day != 1L ? "days" : "day", day);}
            return cachedDay;
        }
    }
}
