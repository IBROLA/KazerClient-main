package com.cheatbreaker.client.module.type;

public enum ItemCountType {
    HEALING_POT("healing_pot"),
    MUSHROOM_SOUP("mushroom_soup"),
    ARROW("arrow"),
    GOLDEN_APPLE("golden_apple"),
    ENCHANTED_GOLDEN_APPLE("enchanted_golden_apple");

    private final String id;

    ItemCountType(String id) {
        this.id = id;
    }

    public String id() {
        return this.id;
    }

    public static ItemCountType fromId(String id) {
        for (ItemCountType type : values()) {
            if (type.id.equalsIgnoreCase(id)) {
                return type;
            }
        }
        return null;
    }
}
