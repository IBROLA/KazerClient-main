package com.cheatbreaker.client.config;

/** Pure visual policy; never changes world ticks, collisions or combat. */
public final class RenderSettingsPolicy {
    private RenderSettingsPolicy() { }
    public static boolean keepParticle(int percent, int sample) {
        return Math.max(0, Math.min(100, percent)) > Math.floorMod(sample, 100);
    }
    public static double distanceSquared(int blocks) {
        return blocks <= 0 ? Double.POSITIVE_INFINITY : (double)blocks * blocks;
    }
    public static int frameLimit(int normalLimit, boolean focused, boolean limitUnfocused, int unfocusedLimit) {
        return !focused && limitUnfocused ? Math.min(normalLimit, Math.max(5, unfocusedLimit)) : normalLimit;
    }
}
