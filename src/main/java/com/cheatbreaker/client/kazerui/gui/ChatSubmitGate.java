package com.cheatbreaker.client.kazerui.gui;

import java.util.WeakHashMap;

/** One submit per chat screen. Reopening chat intentionally permits the same message. */
public final class ChatSubmitGate {
    private static final WeakHashMap<Object,Boolean> SENT = new WeakHashMap<Object,Boolean>();
    private ChatSubmitGate() {}
    public static void opened(Object screen) { SENT.remove(screen); }
    public static boolean submit(Object screen) { return SENT.put(screen,Boolean.TRUE)==null; }
}
