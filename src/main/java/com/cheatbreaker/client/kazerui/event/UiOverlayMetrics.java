package com.cheatbreaker.client.kazerui.event;

/** The host supplies its armor-overlay baseline before dispatch. */
public final class UiOverlayMetrics {
    public static int left_height;
    public static int extent;
    public static void dimensions(net.minecraft.client.gui.ScaledResolution resolution) {
        extent=resolution.getScaledWidth()+resolution.getScaledHeight();
    }
    private UiOverlayMetrics() { }
}
