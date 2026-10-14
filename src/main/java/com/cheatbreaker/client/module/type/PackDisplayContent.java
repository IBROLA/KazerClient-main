package com.cheatbreaker.client.module.type;

import java.util.List;
import java.util.regex.Pattern;

/**
 * Pure layout and selection rules for {@link PackDisplayModule}.
 *
 * The Resource Packs screen builds its selected list as Lists.reverse(repositoryEntries),
 * so the entry shown at the top of that list is the LAST repository entry.
 * Background-on layout follows the Lunar-style flush rule: the icon touches the
 * background left/top/bottom edges with no padding rim.
 * Background-off follows the FPS/CPS convention: the name is wrapped in brackets
 * and drawn with the vanilla FontRenderer shadow.
 */
public final class PackDisplayContent {
    public static final String DEFAULT_NAME = "Default";
    public static final int ICON_SIZE = 32;
    public static final int ICON_TEXT_GAP = 4;
    public static final int TEXT_PADDING = 2;
    public static final int TEXT_BOX_HEIGHT = 13;
    public static final int TEXT_BOX_TEXT_Y = 3;

    private static final Pattern ZIP_SUFFIX = Pattern.compile("\\.zip$", Pattern.CASE_INSENSITIVE);

    private PackDisplayContent() {
    }

    public static <T> T topOrNull(List<T> entries) {
        return entries.isEmpty() ? null : entries.get(entries.size() - 1);
    }

    public static String resolveName(String packName) {
        if (packName == null) {
            return DEFAULT_NAME;
        }
        return ZIP_SUFFIX.matcher(packName).replaceAll("");
    }

    public static boolean showsName(boolean showImage, boolean showName) {
        return showName || !showImage;
    }

    public static float[] computeDimensions(boolean showImage, boolean showName, int textWidth, int fontHeight) {
        return computeDimensions(showImage, showName, true, textWidth, fontHeight);
    }

    public static float[] computeDimensions(boolean showImage, boolean showName, boolean background,
            int textWidth, int fontHeight) {
        if (!showImage) {
            return background
                    ? new float[]{textWidth + 2.0F * TEXT_PADDING, TEXT_BOX_HEIGHT}
                    : new float[]{textWidth, fontHeight};
        }
        return background
                ? new float[]{ICON_SIZE + ICON_TEXT_GAP + textWidth + TEXT_PADDING, ICON_SIZE}
                : new float[]{ICON_SIZE + ICON_TEXT_GAP + textWidth, ICON_SIZE};
    }

    public static int textX(boolean showImage, boolean background) {
        if (showImage) {
            return ICON_SIZE + ICON_TEXT_GAP;
        }
        return background ? TEXT_PADDING : 0;
    }

    public static int textY(boolean showImage, int fontHeight, boolean background) {
        if (showImage) {
            return (ICON_SIZE - fontHeight) / 2;
        }
        return background ? TEXT_BOX_TEXT_Y : 0;
    }
}
