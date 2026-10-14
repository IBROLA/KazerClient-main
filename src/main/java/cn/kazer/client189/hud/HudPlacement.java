package cn.kazer.client189.hud;

/** 1.7 anchor and translation rules, in scaled screen coordinates. */
public final class HudPlacement {
    private HudPlacement() { }

    public static float[] position(String anchor, int screenWidth, int screenHeight,
                                   float itemWidth, float itemHeight, float scale,
                                   float xTranslation, float yTranslation) {
        float width = itemWidth * scale;
        float height = itemHeight * scale;
        float x;
        float y;
        switch (anchor == null ? "LEFT_TOP" : anchor.toUpperCase(java.util.Locale.ROOT)) {
            case "LEFT_MIDDLE": x = 2; y = screenHeight / 2.0F - height / 2.0F; break;
            case "LEFT_BOTTOM": x = 2; y = screenHeight - height - 2; break;
            case "MIDDLE_TOP": x = screenWidth / 2.0F - width / 2.0F; y = 2; break;
            case "MIDDLE_MIDDLE": x = screenWidth / 2.0F - width / 2.0F;
                y = screenHeight / 2.0F - height / 2.0F; break;
            case "MIDDLE_BOTTOM_LEFT": x = screenWidth / 2.0F - width; y = screenHeight - height - 2; break;
            case "MIDDLE_BOTTOM_RIGHT": x = screenWidth / 2.0F; y = screenHeight - height - 2; break;
            case "RIGHT_TOP": x = screenWidth - width - 2; y = 2; break;
            case "RIGHT_MIDDLE": x = screenWidth - width; y = screenHeight / 2.0F - height / 2.0F; break;
            case "RIGHT_BOTTOM": x = screenWidth - width; y = screenHeight - height; break;
            case "LEFT_TOP": default: x = 2; y = 2; break;
        }
        return new float[] { x + xTranslation, y + yTranslation };
    }
}
