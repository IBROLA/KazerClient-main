package com.cheatbreaker.client.kazerui.module.modules.mechanic;

import com.cheatbreaker.client.kazerui.feature.HudAnchor;
import com.cheatbreaker.client.kazerui.gui.ui.UiModel;
import com.cheatbreaker.client.kazerui.module.ChatEvent;
import com.cheatbreaker.client.kazerui.module.ModuleManager;
import com.cheatbreaker.client.kazerui.module.hud.HudElement;
import com.cheatbreaker.client.kazerui.module.render.ChatLineTag;
import com.cheatbreaker.client.kazerui.module.render.KazerChatAccess;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ChatLine;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiNewChat;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.IChatComponent;

/** Owns message insertion, wrapping, rendering and its exact inverse hit-test.
 * Native storage/deletion IDs and native screen/IME handling remain native. */
public final class ChatRuntime {
    private static int nextId = 1;
    private static long positionRevision = Long.MIN_VALUE;
    private static UiModel positionModel;
    private static float offsetX, offsetY, x, y, height;
    private static HudAnchor anchor;
    private static final String[] EXAMPLE = {"Welcome to Kazer Client!", "Drag this chat to reposition it.", "Chat messages appear here."};

    public static boolean active() { return ModuleChat.instance != null; }
    private static Minecraft mc() { return Minecraft.getMinecraft(); }
    private static KazerChatAccess access(GuiNewChat gui) { return (KazerChatAccess)gui; }
    static int id(ChatLine line) { return ((ChatLineTag)(Object)line).kazerui$id(); }
    private static void tag(ChatLine line, int id) { ((ChatLineTag)(Object)line).kazerui$setId(id); }

    public static boolean insert(Object nativeGui, IChatComponent component, int deletionId, int counter, boolean displayOnly) {
        if (!active()) return false;
        GuiNewChat gui = (GuiNewChat)nativeGui;
        if (displayOnly) {
            // Other native refresh callers are handled too. Identity lookup avoids text-equality
            // collisions between identical messages with different click/hover actions.
            int saved = 0;
            for (ChatLine line : access(gui).kazerChatLines()) if (line.getChatComponent() == component) { saved = id(line); break; }
            append(gui, component, deletionId, counter, true, saved == 0 ? allocateId() : saved);
            return true;
        }
        int messageId = allocateId();
        ChatEvent event = ChatEvent.fire(component, messageId);
        if (event.cancelled) return true;
        if (event.removeLast) removeLast(gui);
        ChatHooks.messageAdded(append(gui, event.component(), deletionId, counter, false, messageId));
        return true;
    }
    private static int allocateId() {
        // At least 2 billion messages between wraps; never alias a resident ID.
        if (nextId == Integer.MAX_VALUE) nextId = 1;
        return nextId++;
    }
    private static int append(GuiNewChat gui, IChatComponent component, int deletionId, int counter, boolean displayOnly, int messageId) {
        KazerChatAccess access = access(gui);
        List<ChatLine> drawn = access.kazerDrawnLines(), history = access.kazerChatLines();
        if (!displayOnly && deletionId != 0) {
            drawn.removeIf(line -> line.getChatLineID() == deletionId);
            history.removeIf(line -> line.getChatLineID() == deletionId);
        }
        int width = Math.max(1, (int)(gui.getChatWidth() / gui.getChatScale()));
        if (ModuleChat.instance.headsOn() && ModuleChat.instance.heads.skin(messageId) != null) width = Math.max(1, width - 12);
        List<IChatComponent> wrapped = ChatSplitBridge.split(component, width, mc().fontRendererObj, false, false);
        for (IChatComponent part : wrapped) {
            if (!displayOnly && gui.getChatOpen() && access.kazerChatScroll() > 0) {
                access.kazerChatScrolled(true);ChatNative.scroll(gui, 1);
            }
            ChatLine line = ChatNative.line(counter, part, deletionId, component); tag(line, messageId); drawn.add(0, line);
        }
        trim(drawn, ChatHooks.maxLines(100));
        if (!displayOnly) {
            ChatLine line = ChatNative.line(counter, component, deletionId, component); tag(line, messageId); history.add(0, line);
            trim(history, ChatHooks.maxLines(100));
        }
        return wrapped.size();
    }
    static <T> void trim(List<T> list, int limit) { if (list.size() > limit) list.subList(limit, list.size()).clear(); }
    private static void removeLast(GuiNewChat gui) {
        List<ChatLine> history = access(gui).kazerChatLines();
        if (history.isEmpty()) return;
        int id = id(history.remove(0));
        access(gui).kazerDrawnLines().removeIf(line -> id(line) == id);
    }
    public static boolean refresh(Object nativeGui) {
        if (!active()) return false;
        GuiNewChat gui = (GuiNewChat)nativeGui;
        KazerChatAccess access = access(gui); access.kazerDrawnLines().clear(); gui.resetScroll();
        List<ChatLine> history = access.kazerChatLines();
        for (int index = history.size() - 1; index >= 0; index--) {
            ChatLine line = history.get(index);
            append(gui, line.getChatComponent(), line.getChatLineID(), line.getUpdatedCounter(), true, id(line));
        }
        return true;
    }
    static int visibleLines(GuiNewChat gui) {
        List<ChatLine> drawn = access(gui).kazerDrawnLines();
        int scroll = access(gui).kazerChatScroll(), count = 0, counter = mc().ingameGUI.getUpdateCounter();
        for (int index = 0; index + scroll < drawn.size() && index < gui.getLineCount(); index++)
            if (gui.getChatOpen() || ChatHooks.peek() || counter - drawn.get(index + scroll).getUpdatedCounter() < 200) count = index + 1;
        return count;
    }
    private static void position(GuiNewChat gui, ScaledResolution resolution, boolean preview) {
        ModuleChat module = ModuleChat.instance;
        module.hud.visible(preview);
        height = module.hud.height();
        UiModel model = ModuleManager.model();
        if (positionModel != model || positionRevision != model.revision) {
            offsetX = model.number("chat", "x", 0); offsetY = model.number("chat", "y", -26);
            anchor = module.hud.currentAnchor(); positionModel = model; positionRevision = model.revision;
        }
        float scale = module.hud.scale(), sw = resolution.getScaledWidth(), sh = resolution.getScaledHeight();
        float w = module.hud.width() * scale, h = height * scale;
        x = ChatGeometry.origin(offsetX, anchor.originX(sw, w), sw, w);
        y = ChatGeometry.origin(offsetY + module.hud.visualOffsetY() * scale, anchor.originY(sh, h), sh, h);
    }
    /** Called in native chat's existing (screenHeight-48) plane. */
    public static boolean draw(Object nativeGui, int counter) {
        if (!active()) return false;
        GuiNewChat gui = (GuiNewChat)nativeGui;
        ModuleChat module = ModuleChat.instance;
        if (mc().gameSettings.chatVisibility == net.minecraft.entity.player.EntityPlayer.EnumChatVisibility.HIDDEN
                || module.hidden() || module.isEnabled() && !module.isHudEnabled()
                || mc().currentScreen instanceof com.cheatbreaker.client.kazerui.gui.KazerMovementScreen) return true;
        if (access(gui).kazerDrawnLines().isEmpty()) return true;
        Minecraft mc = mc(); ScaledResolution resolution = new ScaledResolution(mc); position(gui, resolution, false);
        GlStateManager.pushMatrix();
        try {
            float scale = module.isEnabled() ? module.hud.scale() : 1;
            float left = module.isEnabled() ? x : 2;
            float bottom = module.isEnabled() ? y + height * scale : resolution.getScaledHeight() - 28;
            GlStateManager.translate(left, bottom - (resolution.getScaledHeight() - 48), 0);
            GlStateManager.scale(scale, scale, 1);
            drawLocal(gui, counter);
        } finally { GlStateManager.color(1, 1, 1, 1); GlStateManager.popMatrix(); }
        return true;
    }
    static void preview(GuiNewChat gui, float height) {
        GlStateManager.pushMatrix();
        try {
            GlStateManager.translate(0, height, 0);
            if (!access(gui).kazerDrawnLines().isEmpty()) drawLocal(gui, mc().ingameGUI.getUpdateCounter());
            else {
                float scale = gui.getChatScale(); GlStateManager.scale(scale, scale, 1);
                for (int row = 0; row < EXAMPLE.length; row++) {
                    int top = (row - EXAMPLE.length) * ChatGeometry.PITCH;
                    ChatHooks.lineBackground(0, top, (int)(gui.getChatWidth() / scale) + 4, top + 9, 0x80000000);
                    mc().fontRendererObj.drawString(EXAMPLE[row], 0, top + 1, -1, ModuleChat.instance.chatShadow.on());
                }
            }
        } finally { GlStateManager.popMatrix(); }
    }
    private static void drawLocal(GuiNewChat gui, int counter) {
        if (ChatHooks.skipDraw()) return;
        float scale = gui.getChatScale();
        ChatHooks.translate(0, 0, 0);
        GlStateManager.scale(scale, scale, 1);
        List<ChatLine> lines = access(gui).kazerDrawnLines(); int scroll = access(gui).kazerChatScroll();
        boolean open = gui.getChatOpen() || ChatHooks.peek();
        float opacity = mc().gameSettings.chatOpacity * .9f + .1f;
        int width = (int)Math.ceil(gui.getChatWidth() / scale);
        for (int row = 0; row + scroll < lines.size() && row < gui.getLineCount(); row++) {
            ChatLine line = lines.get(row + scroll); int age = counter - line.getUpdatedCounter();
            if (age >= 200 && !open) continue;
            float fade = Math.max(0, Math.min(1, (1 - age / 200f) * 10));
            int alpha = (int)((open ? 1 : fade * fade) * opacity * 255);
            if (alpha <= 3) continue;
            int bottom = -row * ChatGeometry.PITCH;
            ChatHooks.lineBackground(0, bottom - 9, width + 4, bottom, (alpha / 2) << 24);
            ChatHooks.drawLine(mc().fontRendererObj, line.getChatComponent().getFormattedText(), 0, bottom - 8, 0xFFFFFF | alpha << 24);
        }
        if (gui.getChatOpen() && !lines.isEmpty()) {
            int shown=Math.min(gui.getLineCount(),lines.size()-scroll);
            int viewport=shown*ChatGeometry.PITCH,total=lines.size()*ChatGeometry.PITCH;
            if(total>viewport) {
                int offset=scroll*viewport/lines.size(),thumb=viewport*viewport/total;
                int alpha=offset>0?170:96,color=access(gui).kazerChatScrolled()?13382451:3355562;
                Gui.drawRect(-3,-offset,-1,-offset-thumb,color|alpha<<24);
                Gui.drawRect(-1,-offset,-2,-offset-thumb,13421772|alpha<<24);
            }
        }
        GlStateManager.disableBlend(); GlStateManager.disableAlpha();
    }
    public static IChatComponent component(Object nativeGui, int displayX, int displayY) {
        if (!active()) return null;
        ModuleChat.lastHovered = -1;
        GuiNewChat gui = (GuiNewChat)nativeGui; ModuleChat module = ModuleChat.instance;
        if (!gui.getChatOpen() || module.hidden() || module.isEnabled() && !module.isHudEnabled()) return null;
        Minecraft mc = mc(); ScaledResolution resolution = new ScaledResolution(mc); position(gui, resolution, false);
        float hudScale = module.isEnabled() ? module.hud.scale() : 1;
        float left = module.isEnabled() ? x : 2;
        float bottom = module.isEnabled() ? y + height * hudScale : resolution.getScaledHeight() - 28;
        float combined = gui.getChatScale() * hudScale;
        int rx = ChatGeometry.coordinate(displayX, resolution.getScaleFactor(), left, combined);
        int ry = ChatGeometry.coordinate(displayY, resolution.getScaleFactor(), resolution.getScaledHeight() - bottom, combined);
        List<ChatLine> lines = access(gui).kazerDrawnLines();
        int row = ChatGeometry.row(rx, ry, (int)(gui.getChatWidth() / gui.getChatScale()) + 4,
                gui.getLineCount(), access(gui).kazerChatScroll(), lines.size());
        if (row < 0) return null;
        ChatLine line = lines.get(row); ModuleChat.lastHovered = id(line);
        if (module.headsOn() && module.heads.skin(id(line)) != null) rx -= 12;
        if (rx < 0) return null;
        int width = 0;
        for (Object raw : line.getChatComponent()) {
            IChatComponent part = (IChatComponent)raw;
            if (!(part instanceof ChatComponentText)) continue;
            String text = ((ChatComponentText)part).getChatComponentText_TextValue();
            if (!mc().gameSettings.chatColours) text = net.minecraft.util.EnumChatFormatting.getTextWithoutFormattingCodes(text);
            width += mc().fontRendererObj.getStringWidth(text);
            if (width > rx) return part;
        }
        return null;
    }
    private ChatRuntime() { }
}
