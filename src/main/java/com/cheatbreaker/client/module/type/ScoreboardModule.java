package com.cheatbreaker.client.module.type;

import com.cheatbreaker.client.config.Setting;
import com.cheatbreaker.client.event.type.GuiDrawEvent;
import com.cheatbreaker.client.event.type.RenderPreviewEvent;
import com.cheatbreaker.client.module.AbstractModule;
import com.cheatbreaker.client.module.ModuleRule;
import com.cheatbreaker.client.ui.module.CBGuiAnchor;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.Gui;
import net.minecraft.scoreboard.*;
import net.minecraft.util.EnumChatFormatting;
import org.lwjgl.opengl.GL11;

import java.util.Collection;
import java.util.Iterator;

public class ScoreboardModule extends AbstractModule {
    public static ModuleRule rule = ModuleRule.SCOREBOARD;
    public Setting removeNumbers;
    private final Setting hideScoreboard;
    private final Setting textShadow;
    private final Setting border;
    private final Setting borderThickness;
    private final Setting displayToggleMessage;
    private final Setting backgroundColor;
    private final Setting borderColor;
    private ScoreObjective previewObjective;
    private ScoreObjective cachedObjective;
    private FontRenderer cachedFont;
    private int cachedTick = Integer.MIN_VALUE;
    private ScoreboardRenderData cachedData;

    public ScoreboardModule() {
        super("Scoreboard");
        this.setDefaultAnchor(CBGuiAnchor.RIGHT_MIDDLE);
        this.setDefaultTranslations(-1.0f, 0.0f);
        this.removeNumbers = new Setting(this, "Hide Numbers").setValue(true);
        this.hideScoreboard = new Setting(this, "Hide Scoreboard").setValue(false);
        this.textShadow = new Setting(this, "Text Shadow").setValue(false);
        this.borderThickness = new Setting(this, "Border Thickness").setValue(0.5F).setMinMax(0.0F, 4.0F);
        this.border = new Setting(this, "Border").setValue(false);
        new Setting(this, "label").setValue("Toggle");
        this.displayToggleMessage = new Setting(this, "Display Toggle Message").setValue(true);
        new Setting(this, "label").setValue("Color");
        this.backgroundColor = new Setting(this, "Background Color").setValue(0x50000000).setMinMax(Integer.MIN_VALUE, Integer.MAX_VALUE);
        this.borderColor = new Setting(this, "Border Color").setValue(0x9F000000).setMinMax(Integer.MIN_VALUE, Integer.MAX_VALUE);
        this.addEvent(GuiDrawEvent.class, this::renderReal);
        this.addEvent(RenderPreviewEvent.class, this::renderPreview);
        this.addEvent(com.cheatbreaker.client.event.type.TickEvent.class, event -> cachedObjective = null);
        this.setDefaultState(true);
    }

    private void renderPreview(RenderPreviewEvent renderPreviewEvent) {
        if (!this.isRenderHud() || (Boolean)this.hideScoreboard.getValue()) {
            return;
        }
        if (this.minecraft.theWorld.getScoreboard().getObjectiveInDisplaySlot(1) != null) {
            return;
        }
        if (previewObjective == null) {
            Scoreboard scoreboard = new Scoreboard();
            previewObjective = new ScoreObjective(scoreboard, "KazerClient", IScoreObjectiveCriteria.DUMMY);
            previewObjective.setDisplayName(EnumChatFormatting.AQUA + "Kazer" + EnumChatFormatting.WHITE
                    + EnumChatFormatting.BOLD + "Client");
            scoreboard.getValueFromObjective("Steve", previewObjective);
            scoreboard.getValueFromObjective("Alex", previewObjective);
        }
        renderObjective(previewObjective, renderPreviewEvent);
    }

    private void renderReal(GuiDrawEvent guiDrawEvent) {
        if (!this.isRenderHud() || (Boolean)this.hideScoreboard.getValue()) {
            return;
        }
        ScoreObjective objective = this.minecraft.theWorld.getScoreboard().getObjectiveInDisplaySlot(1);
        if (objective != null) renderObjective(objective, guiDrawEvent);
    }

    private void renderObjective(ScoreObjective objective, GuiDrawEvent event) {
        FontRenderer font = this.minecraft.fontRendererObj;
        ScoreboardRenderData data = prepareObjective(objective, font);
        if (data == null || data.names.length == 0) return;
        boolean removeNumbers = isRemoveNumbers();
        this.setDimensions(data.right - (-2 + (removeNumbers ? 14 : 0)),
                data.names.length * font.FONT_HEIGHT + 12);
        GL11.glPushMatrix();
        try {
            this.scaleAndTranslate(event.getResolution());
            GL11.glTranslatef(removeNumbers ? -12F : 2F, this.height, 0F);
            drawObjective(objective, font);
        } finally { GL11.glPopMatrix(); }
    }

    private ScoreboardRenderData prepareObjective(ScoreObjective objective, FontRenderer font) {
        // All server/team/score packets are applied on the client tick thread.
        // A tick boundary, objective replacement or font replacement invalidates the snapshot.
        int tick = this.minecraft.thePlayer == null ? 0 : this.minecraft.thePlayer.ticksExisted;
        if (cachedObjective == objective && cachedFont == font && cachedTick == tick) return cachedData;
        cachedObjective = objective; cachedFont = font; cachedTick = tick;
        Scoreboard board = objective.getScoreboard();
        Collection<Score> scores = board.getSortedScores(objective);
        if (scores.size() > 15) { cachedData = null; return null; }
        String[] names = new String[scores.size()], numbers = new String[scores.size()];
        int row = 0;
        for (Score score : scores) {
            names[row] = ScorePlayerTeam.formatPlayerName(board.getPlayersTeam(score.getPlayerName()), score.getPlayerName());
            numbers[row++] = EnumChatFormatting.RED + "" + score.getScorePoints();
        }
        return cachedData = new ScoreboardRenderData(objective.getDisplayName(), names, numbers, font::getStringWidth);
    }

    private void drawObjective(ScoreObjective objective, FontRenderer fontRenderer) {
        ScoreboardRenderData data = prepareObjective(objective, fontRenderer);
        if (data == null) return;
        boolean removeNumbers = isRemoveNumbers(), shadow = (Boolean)this.textShadow.getValue();
        int background = this.backgroundColor.getColorValue();
        int left = -2 + (removeNumbers ? 14 : 0);
        for (int row = 0; row < data.names.length; row++) {
            int lineY = -(row + 1) * fontRenderer.FONT_HEIGHT;
            cn.kazer.client189.compat.LegacyGui.drawRect(left, lineY, data.right, lineY + fontRenderer.FONT_HEIGHT, background);
            fontRenderer.drawString(data.names[row], removeNumbers ? 16 : 0, lineY, 0xFFFFFFFF, shadow);
            if (!removeNumbers)
                fontRenderer.drawString(data.numbers[row], data.right - data.numberWidths[row] - 2, lineY, 0xFFFFFFFF, shadow);
            if (row != data.names.length - 1) continue;
            cn.kazer.client189.compat.LegacyGui.drawRect(left, lineY - fontRenderer.FONT_HEIGHT - 1, data.right, lineY - 1, background);
            cn.kazer.client189.compat.LegacyGui.drawRect(left, lineY - 1, data.right, lineY, background);
            fontRenderer.drawString(data.title, data.width / 2 - data.titleWidth / 2 + (removeNumbers ? 14 : 0),
                    lineY - fontRenderer.FONT_HEIGHT, 0xFFFFFFFF, shadow);
        }
    }

    private boolean isRemoveNumbers() {
        return rule == ModuleRule.SCOREBOARD ? (Boolean) this.removeNumbers.getValue() : false;
    }
}
