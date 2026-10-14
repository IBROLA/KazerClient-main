package com.cheatbreaker.client.module.type.motionblur;

/**
 * The four motion blur algorithms selectable via the module's Mode setting.
 * Listing order is the product contract (left to right in the dropdown);
 * the persisted value is the display label.
 */
public enum MotionBlurMode {

    /** PolyBlur port: linear lerp trail, mix(prev, curr, Weight). Blurs the hand, like any post-world effect. */
    PHOSPHOR("Phosphor"),

    /** PolyBlur port: double-buffered alpha lerp. Blurs the hand. */
    MOULBERRY("Moulberry"),

    /** PolyBlur port: world-space velocity reprojection using the depth buffer. Never blurs the first-person hand. */
    MONKEY("Monkey"),

    /** CheatBreaker's original glow trail, max(prev * k, curr). Blurs the hand. */
    OLD("Old");

    public static final MotionBlurMode DEFAULT = PHOSPHOR;

    private final String label;

    MotionBlurMode(String label) {
        this.label = label;
    }

    public String label() {
        return this.label;
    }

    public static String[] labelsInOrder() {
        MotionBlurMode[] modes = values();
        String[] labels = new String[modes.length];
        for (int i = 0; i < modes.length; i++) {
            labels[i] = modes[i].label;
        }
        return labels;
    }

    public static MotionBlurMode fromLabel(String label) {
        if (label != null) {
            for (MotionBlurMode mode : values()) {
                if (mode.label.equals(label)) {
                    return mode;
                }
            }
        }
        return DEFAULT;
    }
}
