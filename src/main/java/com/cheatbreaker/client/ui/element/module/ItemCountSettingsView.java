package com.cheatbreaker.client.ui.element.module;

import com.cheatbreaker.client.module.type.ItemCountModule;
import com.cheatbreaker.client.module.type.ItemCountType;
import com.cheatbreaker.client.ui.element.AbstractModulesGuiElement;
import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.PositionedSoundRecord;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.renderer.RenderHelper;
import cn.kazer.client189.compat.RenderItem;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.opengl.GL11;

/**
 * Items Counter chooser page, rendered as the module's whole settings page. Five item icons in
 * the order POTION / SOUP / GAPPLE / ENCHANTED GAPPLE / ARROW, edge-aligned (first icon flush
 * left, last flush right, evenly spaced between) and drawn at 2x scale. Icons have no background
 * at rest; hovering one (or its state bar) reveals a coloured background (blue for an enabled
 * child, green for a disabled one). The state bar below each icon stays green (ON) / white (OFF)
 * and shows ON/OFF text; clicking the bar toggles the child's enabled flag while clicking the
 * icon selects that child's standard settings page. Works regardless of the module's own enabled
 * state, so children can be configured while disabled.
 */
public class ItemCountSettingsView extends AbstractModulesGuiElement {
    private static final int CELL = 32;
    private static final int ICON = 16;
    private static final int ICON_DRAW = 2 * ICON;
    private static final int BAR_HEIGHT = 10;
    private static final int ICON_TOP = 22;

    private static final int COLOUR_ENABLED = -13916106;
    private static final int COLOUR_DISABLED = -723724;
    private static final int COLOUR_HOVER_ENABLED = -12418828;
    private static final int COLOUR_HOVER_DISABLED = -13916106;

    private static final ItemCountType[] ORDER = {
            ItemCountType.HEALING_POT,
            ItemCountType.MUSHROOM_SOUP,
            ItemCountType.GOLDEN_APPLE,
            ItemCountType.ENCHANTED_GOLDEN_APPLE,
            ItemCountType.ARROW
    };

    private final ItemCountModule module;
    private final ModuleListElement listElement;
    private final RenderItem renderItem = new RenderItem();

    public ItemCountSettingsView(ItemCountModule module, ModuleListElement listElement, float f) {
        super(f);
        this.module = module;
        this.listElement = listElement;
    }

    @Override
    public int getHeight() {
        return ICON_TOP + ICON_DRAW + 4 + BAR_HEIGHT + 8;
    }

    private int firstCellX() {
        return this.x + 14;
    }

    private float cellStep() {
        return (float) (this.width - 28 - CELL) / (ORDER.length - 1);
    }

    private int cellX(int i) {
        return (int) (this.firstCellX() + i * this.cellStep());
    }

    private int iconY() {
        return this.y + ICON_TOP;
    }

    private int barX(int i) {
        return this.cellX(i);
    }

    private int barY() {
        return this.y + ICON_TOP + ICON_DRAW + 4;
    }

    private boolean inside(int mouseX, int mouseY, int x, int y, int w, int h) {
        return (float) mouseX > (float) x * this.scale
                && (float) mouseX < (float) (x + w) * this.scale
                && (float) mouseY > (float) (y + this.yOffset) * this.scale
                && (float) mouseY < (float) (y + h + this.yOffset) * this.scale;
    }

    private int hoverColour(boolean enabled) {
        return enabled ? COLOUR_HOVER_ENABLED : COLOUR_HOVER_DISABLED;
    }

    private int barColour(boolean enabled, boolean hover) {
        return hover ? this.hoverColour(enabled) : (enabled ? COLOUR_ENABLED : COLOUR_DISABLED);
    }

    @Override
    public void handleDrawElement(int mouseX, int mouseY, float partialTicks) {
        boolean depthOn = GL11.glIsEnabled(GL11.GL_DEPTH_TEST);
        cn.kazer.client189.compat.LegacyGL.glEnable(GL11.GL_DEPTH_TEST);
        cn.kazer.client189.compat.LegacyGL.glDepthMask(true);
        RenderHelper.enableGUIStandardItemLighting();
        try {
            int iconY = this.iconY();
            int barY = this.barY();
            for (int i = 0; i < ORDER.length; i++) {
                ItemCountType type = ORDER[i];
                int cellX = this.cellX(i);
                int barX = this.barX(i);
                boolean enabled = this.module.isChildEnabled(type);
                boolean hover = this.inside(mouseX, mouseY, cellX, iconY, ICON_DRAW, ICON_DRAW)
                        || this.inside(mouseX, mouseY, barX, barY, CELL, BAR_HEIGHT);
                if (hover) {
                    cn.kazer.client189.compat.LegacyGui.drawRect(cellX, iconY, cellX + ICON_DRAW, iconY + ICON_DRAW, this.hoverColour(enabled));
                }
                GL11.glPushMatrix();
                GL11.glTranslatef((float) cellX, (float) iconY, 0.0F);
                GL11.glScalef(2.0F, 2.0F, 1.0F);
                this.renderItem.renderItemAndEffectIntoGUI(Minecraft.getMinecraft().fontRendererObj,
                        Minecraft.getMinecraft().getTextureManager(), ItemCountModule.displayStack(type), 0, 0);
                GL11.glPopMatrix();
                int barColour = this.barColour(enabled, hover);
                cn.kazer.client189.compat.LegacyGui.drawRect(barX, barY, barX + CELL, barY + BAR_HEIGHT, barColour);
                String label = enabled ? "ON" : "OFF";
                int labelWidth = Minecraft.getMinecraft().fontRendererObj.getStringWidth(label);
                Minecraft.getMinecraft().fontRendererObj.drawStringWithShadow(label, barX + (CELL - labelWidth) / 2, barY + 1, 0xFFFFFFFF);
            }
        } finally {
            RenderHelper.disableStandardItemLighting();
            if (depthOn) {
                cn.kazer.client189.compat.LegacyGL.glEnable(GL11.GL_DEPTH_TEST);
            } else {
                cn.kazer.client189.compat.LegacyGL.glDisable(GL11.GL_DEPTH_TEST);
            }
            cn.kazer.client189.compat.LegacyGL.glDepthMask(true);
        }
    }

    @Override
    public void handleMouseClick(int mouseX, int mouseY, int button) {
        if (button != 0) {
            return;
        }
        int iconY = this.iconY();
        int barY = this.barY();
        for (int i = 0; i < ORDER.length; i++) {
            ItemCountType type = ORDER[i];
            int cellX = this.cellX(i);
            int barX = this.barX(i);
            if (this.inside(mouseX, mouseY, cellX, iconY, ICON_DRAW, ICON_DRAW)) {
                playClick();
                this.listElement.llIlIIIlIIIIlIlllIlIIIIll = false;
                this.listElement.module = this.module.childUnitForView(type);
                return;
            }
            if (this.inside(mouseX, mouseY, barX, barY, CELL, BAR_HEIGHT)) {
                playClick();
                this.module.setChildEnabled(type, !this.module.isChildEnabled(type));
                return;
            }
        }
    }

    private static void playClick() {
        Minecraft.getMinecraft().getSoundHandler()
                .playSound(PositionedSoundRecord.create(new ResourceLocation("gui.button.press"), 1.0f));
    }
}
