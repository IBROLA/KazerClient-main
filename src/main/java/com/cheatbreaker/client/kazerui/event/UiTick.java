package com.cheatbreaker.client.kazerui.event;

/** Native, immutable tick input shared by the two independent client tracks. */
public final class UiTick {
    public enum Phase { START, END }
    public static final class ClientTickEvent {
        public final Phase phase;
        public ClientTickEvent(Phase phase) { this.phase = phase; }
    }
    public static final ClientTickEvent START = new ClientTickEvent(Phase.START);
    public static final ClientTickEvent END = new ClientTickEvent(Phase.END);
    private UiTick() { }
}
