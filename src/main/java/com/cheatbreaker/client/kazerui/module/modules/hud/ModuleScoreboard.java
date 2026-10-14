package com.cheatbreaker.client.kazerui.module.modules.hud;

import com.cheatbreaker.client.kazerui.feature.HudAnchor;
import com.cheatbreaker.client.kazerui.module.Module;
import com.cheatbreaker.client.kazerui.module.Page;
import com.cheatbreaker.client.kazerui.module.hud.*;
import com.cheatbreaker.client.kazerui.module.setting.*;
import net.minecraft.scoreboard.*;

import com.cheatbreaker.client.kazerui.event.UiTick;
import java.util.*;

public final class ModuleScoreboard extends Module {
    private final BoolSetting numbers = bool("numbers", false), hide = bool("hideScoreboard", false), shadow = bool("textShadow", false);
    private final BoolSetting border = bool("border", false), message = bool("displayToggleMessage", true);
    private final NumberSetting thickness = decimal("borderThickness", .5f, .5f, 3);
    private final ColorSetting background = color("backgroundColor", 0x50000000), header = color("headerColor", 0x50000000), borderColor = color("borderColor", 0x9F000000);
    private final KeySetting toggle = keyCombo("toggleHideScoreboard");
    private boolean hidden, down;
    public ModuleScoreboard() { super("SCOREBOARD", true); hud(new Hud()); }
    protected void layout(Page p) {
        p.section("generalOptions", s -> s.add(numbers, hide, shadow, thickness, border));
        p.section("toggle", s -> s.add(toggle, message));
        p.section("colorOptions", s -> s.add(background, header, borderColor));
    }
    public static boolean replacesVanilla() {
        Module m = com.cheatbreaker.client.kazerui.module.ModuleManager.get("scoreboard");
        return m != null && m.isEnabled();
    }
     public void tick(UiTick.ClientTickEvent e) {
        if (e.phase != UiTick.Phase.START) return;
        boolean held = isEnabled() && mc().theWorld != null && mc().currentScreen == null && toggle.isDown();
        if (held && !down) {
            hidden = !hidden;
            if (message.on()) com.cheatbreaker.client.kazerui.gui.KazerNotifications.info(
                    com.cheatbreaker.client.kazerui.gui.ui.KazerLang.get("popups", hidden ? "scoreboardHidden" : "scoreboardDisplayed"));
        }
        down = held;
    }
    private final class Hud extends HudElement {
        private String title;
        private final List<String> names = new ArrayList<String>(), scores = new ArrayList<String>();
        private final List<Score> rows = new ArrayList<Score>();
        private final float[] scoreWidths=new float[15];
        private float titleWidth;
        private ScoreObjective cachedObjective;
        private Object cachedWorld,cachedPlayer,cachedFont;
        private int cachedTick=Integer.MIN_VALUE;
        private long cachedEpoch=Long.MIN_VALUE,cachedRevision=Long.MIN_VALUE;
        private boolean cachedPreview,prepared;
        Hud() { super(ModuleScoreboard.this, 0, 0, HudAnchor.MIDDLE_RIGHT); }
        public boolean editable() { return !hide.on() && !hidden; }
        public boolean visible(boolean preview) {
            if (hide.on() || hidden) return false;
            ScoreObjective objective = null;
            if (mc().theWorld != null && mc().thePlayer != null) {
                Scoreboard board = mc().theWorld.getScoreboard();
                ScorePlayerTeam team = board.getPlayersTeam(mc().thePlayer.getName());
                int index = team == null ? -1 : team.getChatFormat().getColorIndex();
                if (index >= 0) objective = board.getObjectiveInDisplaySlot(3 + index);
                if (objective == null) objective = board.getObjectiveInDisplaySlot(1);
            }
            int tick=mc().thePlayer==null?0:mc().thePlayer.ticksExisted;
            long epoch=com.cheatbreaker.client.kazerui.KazerResourceEpoch.current();
            com.cheatbreaker.client.kazerui.gui.ui.UiModel model=com.cheatbreaker.client.kazerui.module.ModuleManager.model();
            long revision=model==null?0:model.revision;
            Object font=Draw.font();
            if(prepared&&cachedObjective==objective&&cachedWorld==mc().theWorld&&cachedPlayer==mc().thePlayer&&cachedFont==font
                    &&cachedTick==tick&&cachedEpoch==epoch&&cachedRevision==revision&&cachedPreview==preview) return objective!=null||preview;
            cachedObjective=objective;cachedWorld=mc().theWorld;cachedPlayer=mc().thePlayer;cachedFont=font;
            cachedTick=tick;cachedEpoch=epoch;cachedRevision=revision;cachedPreview=preview;prepared=true;
            names.clear();scores.clear();rows.clear();
            if (objective == null) {
                if (!preview) return false;
                title = "\u00a7bKazer\u00a7r \u00a7lClient";
                for (String name : Arrays.asList("Steve", "Alex", "Villager", "Enderman")) { names.add(name); scores.add("\u00a7c0"); }
            } else {
                title = objective.getDisplayName(); Scoreboard board = objective.getScoreboard();
                for (Score score : board.getSortedScores(objective)) if (score.getPlayerName() != null && !score.getPlayerName().startsWith("#")) rows.add(score);
                for (int index=Math.max(0,rows.size()-15);index<rows.size();index++) {
                    Score score=rows.get(index);
                    names.add(ScorePlayerTeam.formatPlayerName(board.getPlayersTeam(score.getPlayerName()), score.getPlayerName()));
                    scores.add("\u00a7c" + score.getScorePoints());
                }
            }
            titleWidth=Draw.width(title);
            float width = titleWidth,separator=Draw.width(": ");
            for (int i = 0; i < names.size(); i++) {
                scoreWidths[i]=Draw.width(scores.get(i));
                width = Math.max(width, Draw.width(names.get(i)) + separator + scoreWidths[i]);
            }

            if (numbers.on() && width > titleWidth) width -= Math.min(width - titleWidth, 9);
            else if (numbers.on()) width += 4;
            size(width + 2, names.size() * Draw.fontHeight() + 10); return true;
        }
        public void render(boolean preview) {
            int h = Draw.fontHeight();
            if (!names.isEmpty()) {
                Draw.fill(header, 0, 1, width(), h); Draw.fill(background, 0, h + 1, width(), height() - h);
                if (border.on()) Draw.border(borderColor, 0, 0, width(), height(), thickness.value());
            }
            for (int i = 0; i < names.size(); i++) {
                float y = height() - (i + 1) * h;
                Draw.text(names.get(i), 2, y, -1, shadow.on());
                if (!numbers.on()) Draw.text(scores.get(i), width() - scoreWidths[i], y, -1, shadow.on());
            }
            if (!names.isEmpty()) Draw.text(title, (width() - titleWidth) / 2, height() - (names.size() + 1) * h, -1, shadow.on());
        }
    }
}
