package com.cheatbreaker.client.kazerui.module.modules.mechanic;

import net.minecraft.client.gui.ChatLine;
import net.minecraft.client.gui.GuiNewChat;
import net.minecraft.util.IChatComponent;

/** Native signatures only; wrapping/rendering policy belongs to the owned Chat. */
final class ChatNative {
    static ChatLine line(int counter, IChatComponent text, int deletionId, IChatComponent logical) {
        return new ChatLine(counter, text, deletionId);
    }
    static void scroll(GuiNewChat gui, int amount) { gui.scroll(amount); }
    private ChatNative() { }
}
