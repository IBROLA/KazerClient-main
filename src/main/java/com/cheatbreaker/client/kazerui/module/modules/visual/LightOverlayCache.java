package com.cheatbreaker.client.kazerui.module.modules.visual;

/** Tick-owned double buffer with a strict query budget. Render only reads published arrays. */
public final class LightOverlayCache {
    public interface Reader { int light(int x, int y, int z, boolean sky); }
    public static final int CAPACITY = 4096, TICK_BUDGET = 128;
    private final int[][] xs = new int[2][CAPACITY], ys = new int[2][CAPACITY], zs = new int[2][CAPACITY], lights = new int[2][CAPACITY];
    private Object world;
    private int front, count, pending, index, cx, cy, cz, radius;
    private boolean sky, scanning;
    public int checkedLastTick;
    public void clear() { world = null; count = pending = index = 0; scanning = false; }
    public int count() { return count; }
    public int x(int i) { return xs[front][i]; }
    public int y(int i) { return ys[front][i]; }
    public int z(int i) { return zs[front][i]; }
    public int light(int i) { return lights[front][i]; }
    public void update(Object identity, int x, int y, int z, int range, boolean includeSky, Reader reader) {
        checkedLastTick = 0;
        if (identity == null) { clear(); return; }
        range = Math.max(1, Math.min(16, range));
        boolean change = world != identity || radius != range || sky != includeSky
            || Math.abs(cx - x) > Math.max(1, range / 2) || Math.abs(cz - z) > Math.max(1, range / 2) || Math.abs(cy - y) > 2;
        if (change) {
            if (world != identity || radius != range || sky != includeSky) count = 0;
            world = identity; cx = x; cy = y; cz = z; radius = range; sky = includeSky;
            index = pending = 0; scanning = true;
        }
        if (!scanning) { index = pending = 0; scanning = true; }
        int side = radius * 2 + 1, columns = side * side, total = columns * 9, back = 1 - front;
        for (int n = 0; n < TICK_BUDGET && index < total; n++, index++) {
            int px = cx - radius + index % side;
            int pz = cz - radius + index / side % side;
            int py = cy - 4 + index / columns;
            checkedLastTick++;
            if (py < 1 || py > 254) continue;
            int light = reader.light(px, py, pz, sky);
            if (light < 0 || pending >= CAPACITY) continue;
            xs[back][pending] = px; ys[back][pending] = py; zs[back][pending] = pz; lights[back][pending++] = Math.min(15, light);
        }
        if (index >= total) { front = back; count = pending; scanning = false; }
    }
}
