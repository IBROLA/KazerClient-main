package cn.kazer.client189.compat;

public final class VignetteSession {
    public static final long FADE_DURATION_MILLIS = 170L;

    private long worldGuiStartedAt = -1L;
    private long previewPhaseStartedAt = -1L;
    private long previewPositionAtPhaseStart;
    private boolean previewActive;
    private boolean previewHeld;

    public float worldGuiProgress(long now) {
        if (this.worldGuiStartedAt < 0L) {
            this.worldGuiStartedAt = now;
        }
        return easeOut(now - this.worldGuiStartedAt);
    }

    public void returnedToWorld() {
        this.worldGuiStartedAt = -1L;
    }

    public void previewPressed(long now) {
        this.previewPositionAtPhaseStart = this.previewPosition(now);
        this.previewPhaseStartedAt = now;
        this.previewActive = true;
        this.previewHeld = true;
    }

    public void previewReleased(long now) {
        this.previewPositionAtPhaseStart = this.previewPosition(now);
        this.previewPhaseStartedAt = now;
        this.previewHeld = false;
        this.previewActive = this.previewPositionAtPhaseStart > 0L;
    }

    public boolean isPreviewVisible(long now) {
        if (this.previewActive && !this.previewHeld && this.previewPosition(now) <= 0L) {
            this.previewActive = false;
        }
        return this.previewActive;
    }

    public float previewProgress(long now) {
        return this.isPreviewVisible(now) ? easeOut(this.previewPosition(now)) : 0.0F;
    }

    public void cancelPreview() {
        this.previewPhaseStartedAt = -1L;
        this.previewPositionAtPhaseStart = 0L;
        this.previewActive = false;
        this.previewHeld = false;
    }

    private long previewPosition(long now) {
        if (!this.previewActive || this.previewPhaseStartedAt < 0L) {
            return 0L;
        }

        long elapsed = Math.max(0L, now - this.previewPhaseStartedAt);
        return this.previewHeld
                ? Math.min(FADE_DURATION_MILLIS, this.previewPositionAtPhaseStart + elapsed)
                : Math.max(0L, this.previewPositionAtPhaseStart - elapsed);
    }

    private static float easeOut(long elapsedMillis) {
        float linear = Math.max(0.0F, Math.min(1.0F,
                elapsedMillis / (float) FADE_DURATION_MILLIS));
        return 1.0F - (1.0F - linear) * (1.0F - linear);
    }
}
