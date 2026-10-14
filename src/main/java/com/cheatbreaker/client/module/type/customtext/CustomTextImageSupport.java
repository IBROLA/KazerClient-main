package com.cheatbreaker.client.module.type.customtext;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Locale;

/**
 * Image-channel rules for Custom Text: a content string that is an absolute local path (or a
 * URL) ending in an image-ish extension is treated as an image reference; every load failure —
 * URL, WebP, SVG, over-limit, missing, corrupt — resolves to null, mapped by the renderer to the
 * 16x16 missing-image. Text that merely looks path-like but has no absolute root stays text.
 */
public final class CustomTextImageSupport {
    static final long MAX_FILE_BYTES = 4L * 1024 * 1024;
    static final int MAX_DIMENSION = 512;

    private static final String[] IMAGE_EXTENSIONS = {".png", ".jpg", ".jpeg", ".bmp", ".gif", ".webp", ".svg"};
    private static final String[] DECODABLE_EXTENSIONS = {".png", ".jpg", ".jpeg", ".bmp", ".gif"};

    private CustomTextImageSupport() {
    }

    public static boolean isImageReference(String content) {
        if (content == null || content.isEmpty() || content.length() > 1024) {
            return false;
        }
        String lower = content.toLowerCase(Locale.ROOT);
        if (!hasImageExtension(lower)) {
            return false;
        }
        if (lower.contains("://")) {
            return true;
        }
        return lower.matches("^[a-z]:[\\\\/].*") || lower.startsWith("/") || lower.startsWith("\\\\");
    }

    private static boolean hasImageExtension(String lower) {
        for (String extension : IMAGE_EXTENSIONS) {
            if (lower.endsWith(extension)) {
                return true;
            }
        }
        return false;
    }

    private static boolean isDecodable(String lowerPath) {
        for (String extension : DECODABLE_EXTENSIONS) {
            if (lowerPath.endsWith(extension)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Size- then format- then dimension-gated decode. GIF returns its first frame via ImageIO's
     * default reader. Returns null for anything that must render as the missing-image.
     */
    public static BufferedImage validateAndDecode(Path path) {
        java.io.File file = path.toFile();
        if (!file.isFile() || file.length() > MAX_FILE_BYTES) {
            return null;
        }
        if (!isDecodable(file.getName().toLowerCase(Locale.ROOT))) {
            return null;
        }
        BufferedImage decoded;
        try {
            decoded = ImageIO.read(file);
        } catch (Exception ignored) {
            return null;
        }
        if (decoded == null
                || decoded.getWidth() > MAX_DIMENSION
                || decoded.getHeight() > MAX_DIMENSION
                || decoded.getWidth() <= 0
                || decoded.getHeight() <= 0) {
            return null;
        }
        return decoded;
    }

    public static Path toPath(String reference) {
        return Paths.get(reference);
    }
}
