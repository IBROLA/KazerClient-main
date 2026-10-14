package com.cheatbreaker.client.kazerui.module.modules.hud;

/** Pure monotonic state machine. No wall-clock jumps, disk writes or game-time changes. */
public final class StopwatchClock {
    private long accumulated, started;
    private boolean running;
    public long elapsed(long now) { return accumulated + (running ? Math.max(0, now - started) : 0); }
    public boolean running() { return running; }
    public void toggle(long now) {
        if (running) { accumulated = elapsed(now); running = false; }
        else { started = now; running = true; }
    }
    public void pause(long now) { if (running) toggle(now); }
    public void reset(long now) { accumulated = 0; started = now; running = false; }
    public static String format(long millis, boolean tenths) {
        long value = Math.max(0, millis), seconds = value / 1000;
        long hours = seconds / 3600, minutes = seconds / 60 % 60;
        StringBuilder out = new StringBuilder(12);
        if (hours > 0) { out.append(hours).append(':'); }
        if (minutes < 10) out.append('0');
        out.append(minutes).append(':');
        if (seconds % 60 < 10) out.append('0');
        out.append(seconds % 60);
        if (tenths) out.append('.').append(value / 100 % 10);
        return out.toString();
    }
}
