package com.cheatbreaker.client.module.type;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.util.HashMap;
import java.util.Map;

public final class ItemCountStateCodec {
    private static final Gson GSON = new Gson();

    private ItemCountStateCodec() {
    }

    public static String encode(ItemCountState state) {
        return GSON.toJson(state == null ? new ItemCountState() : state);
    }

    public static ItemCountState decode(String encoded) {
        ItemCountState defaults = new ItemCountState();
        if (encoded == null || encoded.trim().isEmpty()) {
            return defaults;
        }
        try {
            JsonElement rootElement = new JsonParser().parse(encoded);
            if (!rootElement.isJsonObject()) {
                return defaults;
            }
            JsonObject root = rootElement.getAsJsonObject();
            if (!root.has("version") || root.get("version").getAsInt() != 1
                    || !root.has("children") || !root.get("children").isJsonArray()) {
                return defaults;
            }
            ItemCountState decoded = GSON.fromJson(root, ItemCountState.class);
            Map<String, ItemCountChildState> byId = new HashMap<>();
            if (decoded.children != null) {
                for (ItemCountChildState child : decoded.children) {
                    if (ItemCountType.fromId(child.id) != null) {
                        byId.put(child.id.toLowerCase(), child);
                    }
                }
            }
            defaults.children.clear();
            for (ItemCountType type : ItemCountType.values()) {
                ItemCountChildState child = byId.get(type.id());
                defaults.children.add(child == null ? new ItemCountChildState(type) : sanitize(child, type));
            }
            return defaults;
        } catch (Exception ignored) {
            return defaults;
        }
    }

    private static ItemCountChildState sanitize(ItemCountChildState child, ItemCountType type) {
        child.id = type.id();
        if (child.anchor == null) {
            child.anchor = new ItemCountChildState(type).anchor;
        }
        if (child.scale <= 0.0F || Float.isNaN(child.scale) || Float.isInfinite(child.scale)) {
            child.scale = 1.0F;
        }
        ItemCountDisplayMode defaultMode = ItemCountContent.defaultDisplayMode(type);
        if (child.displayMode == null) {
            child.displayMode = defaultMode.name();
        } else {
            try {
                ItemCountDisplayMode.valueOf(child.displayMode);
            } catch (IllegalArgumentException ex) {
                child.displayMode = defaultMode.name();
            }
        }
        if (child.textColor == null) {
            child.textColor = 0xFFFFFFFF;
        }
        if (child.showBackground == null) {
            child.showBackground = false;
        }
        if (child.backgroundColor == null) {
            child.backgroundColor = 0x6F000000;
        }
        return child;
    }
}
