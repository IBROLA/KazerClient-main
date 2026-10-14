package com.cheatbreaker.client.kazerui.module.modules.hud;

import com.cheatbreaker.client.kazerui.feature.HudAnchor;
import com.cheatbreaker.client.kazerui.module.Module;
import com.cheatbreaker.client.kazerui.module.Page;
import com.cheatbreaker.client.kazerui.module.hud.TextHud;
import com.cheatbreaker.client.kazerui.module.setting.BoolSetting;
import net.minecraft.client.Minecraft;

public final class ModuleFps extends Module {
    private final BoolSetting reverseOrder = bool("reverseOrder", false);
    private int lastFps = Integer.MIN_VALUE;
    private boolean lastReverse;
    private String cachedText;

    public ModuleFps() {
        super("FPS", false);
        hud(new TextHud(this, 0, 0, HudAnchor.TOP_LEFT, TextHud.sizes(10, 18, 22, 50, 56, 62)) {
            @Override protected String text(boolean preview) {
                int fps = Minecraft.getDebugFPS();
                boolean reverse = reverseOrder.on();
                if (cachedText == null || fps != lastFps || reverse != lastReverse) {
                    cachedText = lang(reverse ? "reverse" : "fps", fps);
                    lastFps = fps; lastReverse = reverse;
                }
                return cachedText;
            }
        });
    }

    @Override protected void layout(Page page) {
        page.add(reverseOrder);
    }
}
