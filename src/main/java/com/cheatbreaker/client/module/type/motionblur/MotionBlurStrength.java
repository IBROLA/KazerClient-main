package com.cheatbreaker.client.module.type.motionblur;

/**
 * Maps the Motion Blur "Amount" setting (0-100 percent) onto the phosphor
 * accumulation factor. The percent scale extends CheatBreaker's original
 * 1-10 levels with one decimal digit: percent / 10 == legacy level, so
 * phosphorFactor(p) = 0.69 + 0.003 * p reproduces k = 0.69 + 0.03 * level.
 * 0% is a special zero dose and behaves exactly like a disabled module.
 */
public final class MotionBlurStrength {

    public static final int MIN_PERCENT = 0;
    public static final int MAX_PERCENT = 100;
    public static final int DEFAULT_PERCENT = 50;

    private MotionBlurStrength() {
    }

    public static int clampPercent(int percent) {
        return Math.max(MIN_PERCENT, Math.min(MAX_PERCENT, percent));
    }

    public static boolean isActive(int percent) {
        return clampPercent(percent) > MIN_PERCENT;
    }

    public static float phosphorFactor(int percent) {
        return 0.69f + 0.3f * clampPercent(percent) / 100.0f;
    }

    /** PolyBlur Phosphor lerp weight (mix fraction of the *current* frame): clamp(1.1 - strength/10, 0.1, 1.0). */
    public static float phosphorMixWeight(int percent) {
        float weight = 1.1f - clampPercent(percent) / 100.0f;
        return Math.max(0.1f, Math.min(1.0f, weight));
    }

    /** Monkey reprojection strength (PolyBlur: configStrength / 2, configStrength = percent/10). */
    public static float monkeyStrength(int percent) {
        return clampPercent(percent) / 20.0f;
    }

    /** Moulberry's blend alpha (PolyBlur: min(0.1 + strength*0.1, 0.9), strength = percent/10). */
    public static float moulberryAlpha(int percent) {
        return Math.min(0.1f + clampPercent(percent) * 0.01f, 0.9f);
    }
}
