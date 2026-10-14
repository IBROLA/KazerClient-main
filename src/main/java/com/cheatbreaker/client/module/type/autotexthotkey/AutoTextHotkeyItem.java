package com.cheatbreaker.client.module.type.autotexthotkey;

import java.util.LinkedList;
import java.util.List;

/**
 * One Auto Text Hotkey row: the text to send plus its bound key set. Key codes follow the
 * vanilla convention (keyboard codes as-is, mouse buttons as button index minus 100, so left
 * click is -100 and the side buttons are -97/-96); capture ordering is
 * preserved so display matches how the user pressed the combo. Rows have no display settings —
 * hidden only masks the management-page label.
 */
public final class AutoTextHotkeyItem {
    public String id;
    public String content = "Text";
    public boolean enabled = true;
    public boolean hidden = false;
    /** Capture-ordered key set; never null. Empty means unbound. */
    public final List<Integer> keys = new LinkedList<>();

    public void setContent(String content) {
        this.content = sanitize(content);
    }

    /** Quick-input text is single-line: CR/LF are stripped, never line-wrapped. */
    static String sanitize(String content) {
        if (content == null || content.isEmpty()) {
            return "Text";
        }
        return content.replace("\r", "").replace("\n", "");
    }

    @Override
    public String toString() {
        return this.id + "(" + this.content + ")";
    }
}
