package com.cheatbreaker.client.kazerui.module.modules.hud;

import com.cheatbreaker.client.kazerui.feature.HudAnchor;
import com.cheatbreaker.client.kazerui.gui.ui.KazerLang;
import com.cheatbreaker.client.kazerui.module.Module;
import com.cheatbreaker.client.kazerui.module.Page;
import com.cheatbreaker.client.kazerui.module.hud.Draw;
import com.cheatbreaker.client.kazerui.module.hud.TextHud;
import com.cheatbreaker.client.kazerui.module.setting.BoolSetting;
import com.cheatbreaker.client.kazerui.module.setting.ColorSetting;
import com.cheatbreaker.client.kazerui.util.ClickCounter;

public final class ModuleCps extends Module {
    private final BoolSetting rightClick = bool("rightClick", false);
    private final BoolSetting showCPSText = bool("showCPSText", true);
    private final BoolSetting reverseText = bool("reverseText", false);
    private final BoolSetting ignoreCancelledClicks = bool("ignoreCancelledClicks", false);
    private final ColorSetting lineColor = color("lineColor", 0xFF202020);
    private final Hud hud;
    private int lastLeft = -1, lastRight = -1, lastOptions = -1;
    private String cachedText;

    public ModuleCps() {
        super("CPS", false);
        hud = hud(new Hud());
    }

    @Override protected void onDisable() { ClickCounter.clearRight(); }

    @Override protected void layout(Page page) {
        page.section("settings", s -> {
            s.group(rightClick, g -> g.add(lineColor));
            s.add(showCPSText);
            s.add(reverseText);
            s.add(ignoreCancelledClicks);
        });
    }

    static String cpsText() { return KazerLang.get("shared_info", "cps"); }

    private final class Hud extends TextHud {
        Hud() { super(ModuleCps.this, 0, 0, HudAnchor.TOP_LEFT, sizes(10, 18, 22, 40, 56, 62)); }

        @Override protected String text(boolean preview) {
            boolean showRight = rightClick.on(), showLabel = showCPSText.on(), reverse = reverseText.on();
            int leftValue = ClickCounter.left(ignoreCancelledClicks.on()), rightValue = showRight ? ClickCounter.right() : 0;
            int options = (showRight ? 1 : 0) | (showLabel ? 2 : 0) | (reverse ? 4 : 0);
            if (cachedText != null && leftValue == lastLeft && rightValue == lastRight && options == lastOptions) return cachedText;
            String gap = showRight ? "  " : "", space = showLabel ? " " : "";
            String left = Integer.toString(leftValue), right = showRight ? Integer.toString(rightValue) : "";
            String label = showLabel ? cpsText() : "";
            cachedText = reverse ? label + space + left + gap + right : left + gap + right + space + label;
            lastLeft = leftValue; lastRight = rightValue; lastOptions = options;
            return cachedText;
        }

        @Override protected void decorate(float x, float y, boolean preview, boolean after) {
            if (!after || !rightClick.on()) return;
            boolean ignore = ignoreCancelledClicks.on();
            float total = Draw.width(text(preview));
            float left = Draw.width(ClickCounter.left(ignore) + " ");
            float lineX;
            if (reverseText.on()) {
                float label = showCPSText.on() ? Draw.width(cpsText() + " ") : 0;
                lineX = x + width() / 2.04f - total / 2 + label + left;
            } else {
                lineX = x + width() / 2.04f - total / 2 + left;
            }
            if (textShadow.on()) Draw.rect(lineX + 0.25f, y + height() / 2 - 4, 1, 9, 0x6F000000);
            Draw.rect(lineX - 0.5f, y + height() / 2 - 4.5f, 1, 9, lineColor.color(lineX + y));
        }
    }
}
