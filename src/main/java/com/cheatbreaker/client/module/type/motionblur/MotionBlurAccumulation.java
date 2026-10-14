package com.cheatbreaker.client.module.type.motionblur;

/**
 * Reset contract for the motion-blur animation trail (the accumulated
 * "previous frame"). Without a reset, stale bright pixels from another
 * world, another window size, or the pre-enable era would ghost into the
 * next frames. Pure state machine; the GL side consumes it.
 */
public final class MotionBlurAccumulation {

    private int framebufferWidth = -1;
    private int framebufferHeight = -1;
    private boolean resetPending = true;

    public void onModuleEnabled() {
        this.resetPending = true;
    }

    public void onWorldLoad() {
        this.resetPending = true;
    }

    public void onModeChanged() {
        this.resetPending = true;
    }

    /** @return true when the size actually changed (GL buffers must be rebuilt). */
    public boolean onFramebufferSize(int width, int height) {
        if (width == this.framebufferWidth && height == this.framebufferHeight) {
            return false;
        }
        this.framebufferWidth = width;
        this.framebufferHeight = height;
        this.resetPending = true;
        return true;
    }

    public boolean isResetPending() {
        return this.resetPending;
    }

    public void consumeReset() {
        this.resetPending = false;
    }
}
