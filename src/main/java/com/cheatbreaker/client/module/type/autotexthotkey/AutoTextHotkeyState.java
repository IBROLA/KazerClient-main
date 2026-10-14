package com.cheatbreaker.client.module.type.autotexthotkey;

import java.util.ArrayList;
import java.util.List;

/**
 * Ordered row list for Auto Text Hotkey. List order equals management-page order (top first)
 * and the send order for rows sharing a key set. Hard cap of {@link #MAX_ITEMS} applies to both
 * UI-driven adds and codec-driven loads.
 */
public final class AutoTextHotkeyState {
    public static final int MAX_ITEMS = 32;

    public final List<AutoTextHotkeyItem> items = new ArrayList<>();
    private int nextId = 1;

    /** Returns the created item, or null when the row cap is reached. */
    public AutoTextHotkeyItem add() {
        if (this.items.size() >= MAX_ITEMS) {
            return null;
        }
        AutoTextHotkeyItem item = new AutoTextHotkeyItem();
        item.id = "ath" + this.nextId++;
        this.items.add(item);
        return item;
    }

    public boolean remove(String id) {
        AutoTextHotkeyItem item = byId(id);
        return item != null && this.items.remove(item);
    }

    public AutoTextHotkeyItem byId(String id) {
        for (AutoTextHotkeyItem item : this.items) {
            if (item.id.equals(id)) {
                return item;
            }
        }
        return null;
    }

    /** Moves the item to the target management-list index (clamped). */
    public void move(String id, int targetIndex) {
        AutoTextHotkeyItem item = byId(id);
        if (item == null) {
            return;
        }
        this.items.remove(item);
        int clamped = Math.max(0, Math.min(targetIndex, this.items.size()));
        this.items.add(clamped, item);
    }

    int getNextId() {
        return this.nextId;
    }

    void setNextId(int nextId) {
        this.nextId = nextId;
    }
}
