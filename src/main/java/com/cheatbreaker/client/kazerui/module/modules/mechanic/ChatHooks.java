package com.cheatbreaker.client.kazerui.module.modules.mechanic;

import com.cheatbreaker.client.kazerui.module.ChatEvent;
import com.cheatbreaker.client.kazerui.module.hud.Draw;
import com.cheatbreaker.client.kazerui.module.render.ChatLineTag;
import com.cheatbreaker.client.kazerui.module.render.KazerChatAccess;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ChatLine;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiChat;
import net.minecraft.client.gui.GuiNewChat;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.event.ClickEvent;
import net.minecraft.util.IChatComponent;
import net.minecraft.util.MathHelper;
import net.minecraft.util.ResourceLocation;

public final class ChatHooks {

    private static float percent;
    private static long lastUpdate = -1L;
    private static float eased;
    private static int newLines;
    private static int idCounter = 1;

    private static boolean hoverHasHead, hoverPendingWidth;

    static void reset() { percent=1;eased=1;lastUpdate=-1;newLines=0; }

    private ChatHooks() {}

    private static ModuleChat chat() { return ModuleChat.instance; }

    private static Minecraft mc() { return Minecraft.getMinecraft(); }

    private static GuiNewChat gui() { return mc().ingameGUI == null ? null : mc().ingameGUI.getChatGUI(); }

    @SuppressWarnings("unchecked")
    private static List<ChatLine> drawn(GuiNewChat gui) { return ((KazerChatAccess)gui).kazerDrawnLines(); }

    @SuppressWarnings("unchecked")
    private static List<ChatLine> lines(GuiNewChat gui) { return ((KazerChatAccess)gui).kazerChatLines(); }

    private static int scrollPos(GuiNewChat gui) { return ((KazerChatAccess)gui).kazerChatScroll(); }

    private static boolean scrolled(GuiNewChat gui) { return ((KazerChatAccess)gui).kazerChatScrolled(); }

    private static int id(Object line) { return line instanceof ChatLineTag ? ((ChatLineTag)line).kazerui$id() : 0; }

    public static IChatComponent message(GuiNewChat gui, IChatComponent component, boolean displayOnly) {
        IChatComponent out = component;
        if (!displayOnly) {
            ChatEvent e = ChatEvent.fire(component, idCounter);
            if (e.cancelled) return null;
            percent = 0.0f;
            if (e.removeLast) removeLast(gui);
            if (e.changed) out = e.component();
        }
        int width = MathHelper.floor_float(gui.getChatWidth() / gui.getChatScale());
        newLines = ChatSplitBridge.split(out, width, mc().fontRendererObj, false, false).size() - 1;
        return out;
    }

    public static void lineDone() { idCounter++; }

    public static void messageAdded(GuiNewChat gui, IChatComponent component) {
        percent = 0;
        int width = Math.max(1, MathHelper.floor_float(gui.getChatWidth() / gui.getChatScale()));
        newLines = ChatSplitBridge.split(component, width, mc().fontRendererObj, false, false).size() - 1;
    }
    public static void messageAdded(int wrappedLines) { percent=0;newLines=Math.max(0,wrappedLines-1); }

    public static int maxLines(int n) {
        ModuleChat c = chat();
        return c != null && c.isEnabled() && c.unlimitedChat.on() ? 1000 : n;
    }

    public static void tag(Object line) {
        if (line instanceof ChatLineTag) ((ChatLineTag)line).kazerui$setId(idCounter);
    }

    public static List<IChatComponent> splitText(IChatComponent c, int width, FontRenderer font, boolean a, boolean b) {
        ModuleChat chat = chat();
        if (chat != null && chat.headsOn() && chat.heads.skin(idCounter) != null) width -= 12;
        return ChatSplitBridge.split(c, width, font, a, b);
    }

    private static void removeLast(GuiNewChat gui) {
        List<ChatLine> lines = lines(gui), drawn = drawn(gui);
        if (lines.isEmpty() || drawn.isEmpty()) return;
        int id = id(lines.get(0));
        lines.remove(0);
        while (!drawn.isEmpty() && id(drawn.get(0)) == id) drawn.remove(0);
    }

    static void deleteMessage(int id) {
        GuiNewChat gui = gui();
        if (gui == null) return;
        lines(gui).removeIf(l -> id(l) == id);
        drawn(gui).removeIf(l -> id(l) == id);
    }

    static IChatComponent messageById(int id) {
        GuiNewChat gui = gui();
        if (gui == null) return null;
        for (ChatLine l : lines(gui)) if (id(l) == id) return l.getChatComponent();
        return null;
    }

    public static boolean skipDraw() {
        ModuleChat c = chat();
        if (c != null && c.hidden()) return true;
        long now = Minecraft.getSystemTime();
        if (lastUpdate == -1L) {
            lastUpdate = now;
            percent = 1;
        }
        long elapsed = now - lastUpdate;
        lastUpdate = now;
        if (percent < 1.0f) percent += (c != null ? c.smoothSpeed() : ModuleChat.smoothSpeed(3)) * elapsed;
        percent = Math.max(0.0f, Math.min(1.0f, percent));
        float v = percent - 1.0f;
        eased = Math.max(0.0f, Math.min(1.0f, 1.0f - v*v*v*v));
        return false;
    }

    private static boolean smooth() { ModuleChat c = chat(); return c != null && c.smoothOn(); }

    private static boolean lifted() { ModuleChat c = chat(); return c != null && c.isEnabled() && c.chatHeight.on(); }

    private static float opacity() { ModuleChat c = chat(); return c != null && c.isEnabled() ? c.chatBackgroundOpacity.value() : 1.0f; }

    public static void translate(float x, float y, float z) {
        GlStateManager.translate(x, y, z);
        GuiNewChat gui = gui();
        if (gui != null && smooth() && !gui.getChatOpen() && !peek() && !scrolled(gui)) GlStateManager.translate(0.0f, (9.0f - 9.0f * percent) * gui.getChatScale(), 0.0f);
    }

    public static void scale(float x, float y, float z, int counter) {
        GlStateManager.scale(x, y, z);
        GuiNewChat gui = gui();
        if (gui == null) return;
        int lines = gui.getLineCount();
        boolean open = gui.getChatOpen();
        int width = MathHelper.ceiling_float_int(gui.getChatWidth() / gui.getChatScale());
        float opacity = opacity();
        boolean lifted = lifted();
        if (opacity <= 0.0f) return;
        int height = 0;
        List<ChatLine> drawn = drawn(gui);
        int scroll = scrollPos(gui);
        for (int i = 0; i + scroll < drawn.size() && i < lines; i++) {
            ChatLine line = drawn.get(i + scroll);
            if (line == null) continue;
            double d = (counter - line.getUpdatedCounter()) / 200.0;
            d = MathHelper.clamp_double((1.0 - d) * 10.0, 0.0, 1.0);
            d *= d;
            if ((int)(255.0 * d) >= 254 || open) height -= mc().fontRendererObj.FONT_HEIGHT;
        }
        Gui.drawRect(0, lifted ? -12 : 0, width + 4, height + (lifted ? -12 : 0), (int)(127.0f * opacity) << 24);
    }

    public static void lineBackground(int x1, int y1, int x2, int y2, int color) {
        ModuleChat module = chat();
        if (module != null && module.isEnabled() && !module.showBackground.on()) return;
        float opacity = opacity();
        if (opacity <= 0.0f) return;
        int alpha = (byte)(color >> 24) & 255;
        Gui.drawRect(x1, y1, x2, y2, (int)(opacity * alpha) << 24);
    }

    public static int drawLine(FontRenderer font, String text, float x, float y, int color) {
        GuiNewChat gui = gui();
        ModuleChat chat = chat();
        float index = (y + 8.0f) / -9.0f;
        boolean scrolled = gui != null && scrolled(gui);
        if (smooth() && gui != null && !gui.getChatOpen() && !peek() && index <= newLines && !scrolled) color = color & 0xFFFFFF | ((int)((color >>> 24) * eased) << 24);
        if ((color >>> 24) <= 3) return 0; // Native fonts treat tiny alpha as fully opaque.
        boolean lifted = lifted();
        boolean shadow = chat == null || !chat.isEnabled() || chat.chatShadow.on();
        if (chat != null && chat.headsOn() && gui != null) {
            int line = Math.round(index) + scrollPos(gui);
            ResourceLocation skin = headSkin(gui, line);
            if (skin != null) {
                if (topLine(gui, line)) {
                    float hy = y - 1.0f;
                    if (shadow) head(skin, x + 1.0f, hy + 1.0f, Draw.shadow(color));
                    head(skin, x, hy, 0xFFFFFF | color & 0xFF000000);
                }
                x += 12.0f;
            }
        }
        GlStateManager.enableBlend();
        return font.drawString(text, x, y, color, shadow);
    }

    private static void head(ResourceLocation skin, float x, float y, int color) {
        Draw.blit(skin, (int)x, (int)y, 8, 8, 8, 8, 64, 64, color);
        Draw.blit(skin, (int)x, (int)y, 40, 8, 8, 8, 64, 64, color);
    }

    private static ResourceLocation headSkin(GuiNewChat gui, int line) {
        List<ChatLine> drawn = drawn(gui);
        if (line < 0 || line >= drawn.size()) return null;
        return chat().heads.skin(id(drawn.get(line)));
    }

    private static boolean topLine(GuiNewChat gui, int line) {
        List<ChatLine> drawn = drawn(gui);
        return line + 1 >= drawn.size() || id(drawn.get(line + 1)) != id(drawn.get(line));
    }

    public static void hoverStart() { ModuleChat.lastHovered = -1; }

    public static int hoverY(float f) { return MathHelper.floor_float(f) - (lifted() ? 12 : 0); }

    public static Object hoverLine(List<?> list, int index) {
        ModuleChat chat = chat();
        GuiNewChat gui = gui();
        hoverHasHead = chat != null && chat.headsOn() && gui != null && headSkin(gui, index) != null;
        hoverPendingWidth = true;
        return list.get(index);
    }

    public static int hoverWidth(FontRenderer font, String s) {
        int w = font.getStringWidth(s);
        if (hoverPendingWidth) {
            hoverPendingWidth = false;
            if (hoverHasHead) w += 12;
        }
        return w;
    }

    public static void hovered(Object line) { ModuleChat.lastHovered = id(line); }

    public static boolean peek() { ModuleChat c = chat(); return c != null && c.peeking(); }

    public static void inputBackground(int x1, int y1, int x2, int y2, int color) {
        ModuleChat c = chat();
        float f = c != null ? c.inputOpacity() : 1.0f;
        if (f != 1.0f) color = Math.max(0, Math.min(255, (int)((color >>> 24) * f))) << 24 | color & 0xFFFFFF;
        Gui.drawRect(x1, y1, x2, y2, color);
    }

    public static IChatComponent hoverComponent(IChatComponent component) {
        ModuleChat c = chat();
        if (c == null) return component;
        if (c.isEnabled() && c.hoverImagePreview.on() && component != null) {
            ClickEvent click = component.getChatStyle() == null ? null : component.getChatStyle().getChatClickEvent();
            if (click != null && click.getAction() == ClickEvent.Action.OPEN_URL) {
                return c.preview.show(c, click.getValue(), null) ? null : component;
            }
            return c.preview.show(c, null, messageById(ModuleChat.lastHovered)) ? null : component;
        }
        c.preview.reset();
        return component;
    }

    public static int maxInput(int n) {
        ModuleChat c = chat();
        return c == null ? n : c.maxLength(mc().isSingleplayer(), n);
    }

    public static String trimOutgoing(String text, int from, int to) {
        ModuleChat c = chat();
        int limit = c == null ? to : c.maxLength(mc().isSingleplayer(), to);
        return text.length() > limit ? text.substring(0, limit) : text;
    }

    public static boolean keepChatOpen() {
        // A submitted native chat always closes, including migrated no-close values.
        return false;
    }
    public static boolean mouse(net.minecraft.client.gui.GuiScreen screen) { ModuleChat c = chat(); return c != null && c.onMouse(screen); }
    public static void key(net.minecraft.client.gui.GuiScreen screen) { ModuleChat c = chat(); if (c != null) c.onKey(screen); }
    public static void screen(net.minecraft.client.gui.GuiScreen screen, int x, int y) {
        ModuleChat c = chat(); if (c == null) return;
        if (screen instanceof GuiChat) hoverComponent(component(org.lwjgl.input.Mouse.getX(), org.lwjgl.input.Mouse.getY()));
        c.onDrawScreen(screen,x,y);
    }

    public static IChatComponent component(int x,int y) { return gui()==null?null:ChatRuntime.component(gui(),x,y); }
}
