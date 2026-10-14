package com.cheatbreaker.client.kazerui.module.modules.mechanic;

/** Shared local chat plane. Drawing and input use the same line pitch and offsets. */
final class ChatGeometry {
    static final int PITCH = 9;
    static float origin(float offset, float anchorOrigin, float screen, float extent) {
        return Math.max(0, Math.min(screen - extent, offset + anchorOrigin));
    }
    static int coordinate(float display, int guiScale, float origin, float scale) {
        return (int)Math.floor((display / guiScale - origin) / scale);
    }
    static int row(int x, int y, int width, int visible, int scroll, int count) {
        if (x < 0 || x >= width || y < 0 || y >= visible * PITCH) return -1;
        int row = y / PITCH + scroll;
        return row >= 0 && row < count ? row : -1;
    }
    private ChatGeometry() { }
}
