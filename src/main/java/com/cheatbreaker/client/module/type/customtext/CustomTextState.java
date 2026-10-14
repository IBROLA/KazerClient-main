package com.cheatbreaker.client.module.type.customtext;

import java.util.ArrayList;
import java.util.List;

/**
 * Ordered instance list for Custom Text. List order equals management-page order (top first);
 * the renderer draws from the end backwards so the topmost list row ends up on top visually.
 * Hard cap of {@link #MAX_ITEMS} applies to both UI-driven adds and codec-driven loads.
 */
public final class CustomTextState {
    public static final int MAX_ITEMS = 32;

    public final List<CustomTextItem> items = new ArrayList<>();
    private int nextId = 1;

    /** Returns the created item, or null when the instance cap is reached. */
    public CustomTextItem add() {
        if (this.items.size() >= MAX_ITEMS) {
            return null;
        }
        CustomTextItem item = new CustomTextItem();
        item.id = "ct" + this.nextId++;
        this.items.add(item);
        return item;
    }

    public boolean remove(String id) {
        CustomTextItem item = byId(id);
        return item != null && this.items.remove(item);
    }

    public CustomTextItem byId(String id) {
        for (CustomTextItem item : this.items) {
            if (item.id.equals(id)) {
                return item;
            }
        }
        return null;
    }

    /** Moves the item to the target management-list index (clamped). */
    public void move(String id, int targetIndex) {
        CustomTextItem item = byId(id);
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
