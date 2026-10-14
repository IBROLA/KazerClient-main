package com.cheatbreaker.client.config;

import java.util.LinkedHashSet;
import java.util.Set;

/** Global UI preference, independent of module state and the active HUD profile. */
public final class FavoriteModules {
    private final Set<String> names = new LinkedHashSet<>();
    public boolean contains(String name) { return names.contains(name); }
    public void toggle(String name) { if (!names.remove(name)) names.add(name); }
    public String serialize() { return String.join(",", names); }
    public void load(String value) {
        names.clear();
        for (String name : value.split(",")) if (!name.trim().isEmpty()) names.add(name.trim());
    }
}
