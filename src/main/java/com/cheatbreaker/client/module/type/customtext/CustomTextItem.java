package com.cheatbreaker.client.module.type.customtext;

/**
 * One Custom Text instance: a single-line text or local-image HUD. Positional fields are plain
 * decimals so the management page, codec, and renderer share one source of truth; GUI adapters
 * (detached Setting objects) write back through the owning module.
 */
public final class CustomTextItem {
    public static final int DEFAULT_COLOR = 0xFFFFFFFF;
    public static final int DEFAULT_BACKGROUND_COLOR = 0x60000000;
    public static final float DEFAULT_SCALE = 1.0F;
    public static final String DEFAULT_ANCHOR = "MIDDLE_MIDDLE";

    public String id;
    public String content = "Text";
    public int color = DEFAULT_COLOR;
    public boolean showBackground;
    public int backgroundColor = DEFAULT_BACKGROUND_COLOR;
    public float scale = DEFAULT_SCALE;
    public String anchor = DEFAULT_ANCHOR;
    public float xTranslation;
    public float yTranslation;
    public boolean enabled = true;
    public boolean renderHud = true;

    public void setContent(String content) {
        this.content = sanitize(content);
    }

    /** Text instances are single-line: CR/LF are stripped, never line-wrapped. */
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
