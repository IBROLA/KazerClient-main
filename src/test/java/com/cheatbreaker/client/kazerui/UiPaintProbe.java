package com.cheatbreaker.client.kazerui;

import com.cheatbreaker.client.kazerui.gui.ui.*;
import java.lang.management.ManagementFactory;
import java.util.*;

/** Headless painter allocation probe, not a game-FPS benchmark. */
public final class UiPaintProbe {
    public static void main(String[] args) throws Exception {
        final Map<String,String> values = new HashMap<String,String>();
        UiModel model = new UiModel(new UiStore() {
            public String get(String key,String fallback) { return values.containsKey(key) ? values.get(key) : fallback; }
            public void put(String key,String value) { values.put(key,value); }
            public void remove(String key) { values.remove(key); }
            public Collection<String> keys() { return new ArrayList<String>(values.keySet()); }
            public void save() { }
        });
        UiRenderer painter = new UiRenderer();
        int scale = Integer.parseInt(args[0]), samples = 80;
        for (int i = 0; i < 20; i++) painter.render(model,-1,-1,scale,false);
        com.sun.management.ThreadMXBean bean = (com.sun.management.ThreadMXBean)ManagementFactory.getThreadMXBean();
        if (!bean.isThreadAllocatedMemorySupported()) throw new IllegalStateException("Allocation counter unavailable");
        bean.setThreadAllocatedMemoryEnabled(true);
        long id = Thread.currentThread().getId();
        long startBytes = bean.getThreadAllocatedBytes(id);
        long[] nanos = new long[samples];
        long checksum = 0;
        for (int i = 0; i < samples; i++) {
            long start = System.nanoTime();
            checksum += painter.render(model,-1,-1,scale,false).getRGB(60,60);
            nanos[i] = System.nanoTime() - start;
        }
        long allocated = bean.getThreadAllocatedBytes(id) - startBytes;
        Arrays.sort(nanos);
        System.out.println("{\"scale\":"+scale+",\"samples\":"+samples+",\"bytesPerPaint\":"+(allocated / samples)
            +",\"medianMs\":"+(nanos[samples / 2] / 1e6)+",\"p95Ms\":"+(nanos[(int)(samples * .95)] / 1e6)
            +",\"checksum\":"+checksum+"}");
    }
    private UiPaintProbe() { }
}
