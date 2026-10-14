package com.cheatbreaker.client.kazerui.module.render;
import java.util.List;
import net.minecraft.client.gui.ChatLine;
/** Direct native fields in 1.7; the existing extension transformer adds the same typed getters in 1.8. */
public interface KazerChatAccess {
    List<ChatLine> kazerDrawnLines();
    List<ChatLine> kazerChatLines();
    int kazerChatScroll();
    boolean kazerChatScrolled();
    void kazerChatScrolled(boolean scrolled);
}
