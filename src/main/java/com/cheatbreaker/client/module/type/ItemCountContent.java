package com.cheatbreaker.client.module.type;

public final class ItemCountContent {
    private ItemCountContent() {
    }

    public static String classicText(ItemCountType type, int count) {
        String noun = noun(type);
        return count + " " + noun + (count == 0 || count == 1 ? "" : "s");
    }

    public static String countText(int count) {
        return String.valueOf(count);
    }

    public static ItemCountDisplayMode defaultDisplayMode(ItemCountType type) {
        return (type == ItemCountType.HEALING_POT || type == ItemCountType.MUSHROOM_SOUP)
                ? ItemCountDisplayMode.CLASSIC
                : ItemCountDisplayMode.ICON;
    }

    private static String noun(ItemCountType type) {
        switch (type) {
            case HEALING_POT: return "Pot";
            case MUSHROOM_SOUP: return "Soup";
            case ARROW: return "Arrow";
            case GOLDEN_APPLE: return "Gapple";
            case ENCHANTED_GOLDEN_APPLE: return "Enchanted Gapple";
            default: return "Item";
        }
    }

    public static float[] dimensions(ItemCountDisplayMode mode, int iconSize, int textWidth, int fontHeight, boolean background) {
        if (mode == ItemCountDisplayMode.ICON) {
            // Icon 16x16 plus the count overlay extending to y = 9 + 9 = 18: the declared
            // bounding box must cover icon + number (vanilla overlay position). The background
            // box is exactly this bounding box, so the size is the same with or without it.
            return new float[]{iconSize, iconSize + 2.0F};
        }
        if (background) {
            // FPS-style padded background: a few px of spare area around the text.
            return new float[]{textWidth + 4.0F, fontHeight + 4.0F};
        }
        return new float[]{textWidth, fontHeight};
    }
}
