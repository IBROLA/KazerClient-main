package com.cheatbreaker.client.kazerui.module.modules.visual;

import com.cheatbreaker.client.kazerui.module.Module;
import com.cheatbreaker.client.kazerui.module.Page;
import com.cheatbreaker.client.kazerui.module.setting.*;
import com.cheatbreaker.client.kazerui.module.render.TooltipHooks;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.item.ItemStack;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.GL11;

/** Own item-tip viewport. No wheel polling from render, inventory mutations or cached NBT. */
public final class ModuleScrollableTooltips extends Module {
    private static ModuleScrollableTooltips instance;
    private final NumberSetting tooltipScale = decimal("tooltipScale", 1, 0.25f, 2.5f);
    private final NumberSetting scrollSpeed = integer("scrollSpeed", 20, 5, 60);
    private final BoolSetting startAtTop = bool("startAtTop", true);
    private GuiScreen screen;
    private ItemStack stack;
    private int cursorX, cursorY;
    private float offsetX, offsetY, overflowX, overflowY;
    private long lastDraw;
    public ModuleScrollableTooltips() { super("SCROLLABLE_TOOLTIPS", false); instance = this; labels(); }
    private void labels() {
        for (Setting<?> setting : settings()) {
            switch (setting.key) {
                case "tooltipScale": setting.label(() -> "Tooltip Scale (Ctrl + Wheel)"); break;
                case "scrollSpeed": setting.label(() -> "Scroll Step"); break;
                case "startAtTop": setting.label(() -> "Start Long Tooltips at Top"); break;
                default: break;
            }
        }
    }
    @Override protected void layout(Page p) { p.add(tooltipScale, scrollSpeed, startAtTop); }
    public static float clampOffset(float value, float overflow) {
        return Math.max(0, Math.min(Math.max(0, overflow), value));
    }
    public static boolean wheel(GuiScreen gui, int x, int y, int delta) {
        ModuleScrollableTooltips m = instance;
        if (m == null || !m.isEnabled() || delta == 0 || !(gui instanceof GuiContainer)
            || m.screen != gui || System.nanoTime() - m.lastDraw > 150000000L
            || Math.abs(x - m.cursorX) > 8 || Math.abs(y - m.cursorY) > 8) return false;
        int direction = delta > 0 ? -1 : 1;
        if (Keyboard.isKeyDown(Keyboard.KEY_LCONTROL) || Keyboard.isKeyDown(Keyboard.KEY_RCONTROL)) {
            m.tooltipScale.set(Math.max(.25f, Math.min(2.5f, m.tooltipScale.value() - direction * .1f)));
            return true;
        }
        if (Keyboard.isKeyDown(Keyboard.KEY_LSHIFT) || Keyboard.isKeyDown(Keyboard.KEY_RSHIFT)) {
            if (m.overflowX <= 0) return false;
            m.offsetX = clampOffset(m.offsetX + direction * m.scrollSpeed.intValue(), m.overflowX);
        } else {
            if (m.overflowY <= 0) return false;
            m.offsetY = clampOffset(m.offsetY + direction * m.scrollSpeed.intValue(), m.overflowY);
        }
        return true;
    }
    public static boolean draw(GuiScreen gui, ItemStack item, List<String> lines, int x, int y, FontRenderer font) {
        ModuleScrollableTooltips m = instance;
        if (m == null || !m.isEnabled() || lines.isEmpty() || !(gui instanceof GuiContainer)) return false;
        if (m.screen != gui || m.stack != item || System.nanoTime() - m.lastDraw > 150000000L) {
            m.offsetX = m.offsetY = 0;
            m.screen = gui; m.stack = item;
        }
        m.cursorX = x; m.cursorY = y; m.lastDraw = System.nanoTime();
        int width = 0;
        for (String line : lines) width = Math.max(width, font.getStringWidth(line));
        int height = lines.size() == 1 ? 8 : 10 + (lines.size() - 1) * 10;
        float scale = m.tooltipScale.value(), availableW = Math.max(4, gui.width - 12), availableH = Math.max(4, gui.height - 12);
        m.overflowX = Math.max(0, width * scale - availableW);
        m.overflowY = Math.max(0, height * scale - availableH);
        m.offsetX = clampOffset(m.offsetX, m.overflowX);
        m.offsetY = clampOffset(m.offsetY, m.overflowY);
        if (scale == 1 && m.overflowX == 0 && m.overflowY == 0) return false;
        float viewW = Math.min(width * scale, availableW), viewH = Math.min(height * scale, availableH);
        int left = Math.round(Math.max(6, Math.min(gui.width - viewW - 6, x + 12)));
        int top = Math.round(Math.max(6, Math.min(gui.height - viewH - 6, y - 12)));
        if (m.overflowY > 0 && m.startAtTop.on()) top = 6;
        int right = Math.round(left + viewW), bottom = Math.round(top + viewH);
        cn.kazer.client189.compat.LegacyGL.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS);
        GL11.glPushMatrix();
        try {
            GL11.glDisable(GL11.GL_LIGHTING); GL11.glDisable(GL11.GL_DEPTH_TEST); GL11.glDepthMask(false);
            GL11.glTranslatef(0, 0, 300);
            Gui.drawRect(left - 4, top - 4, right + 4, bottom + 4, 0xFF100010);
            Gui.drawRect(left - 4, top - 4, right + 4, top - 3, 0xFF5050A0);
            Gui.drawRect(left - 4, bottom + 3, right + 4, bottom + 4, 0xFF282850);
            Minecraft mc = Minecraft.getMinecraft();
            double sx = mc.displayWidth / (double)gui.width, sy = mc.displayHeight / (double)gui.height;
            GL11.glEnable(GL11.GL_SCISSOR_TEST);
            GL11.glScissor((int)(left * sx), (int)((gui.height - bottom) * sy),
                Math.max(1, (int)(viewW * sx)), Math.max(1, (int)(viewH * sy)));
            GL11.glTranslatef(left - m.offsetX, top - m.offsetY, 0);
            GL11.glScalef(scale, scale, 1);
            for (int i = 0; i < lines.size(); i++) {
                int lineY = i * 10 + (i > 0 ? 2 : 0);
                float screenY = lineY * scale - m.offsetY;
                if (screenY + 10 * scale >= 0 && screenY <= viewH)
                    TooltipHooks.drawLine(font, lines.get(i), 0, lineY, -1);
            }
        } finally { GL11.glPopMatrix(); cn.kazer.client189.compat.LegacyGL.glPopAttrib(); }
        return true;
    }
}
