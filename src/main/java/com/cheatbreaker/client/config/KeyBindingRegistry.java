package com.cheatbreaker.client.config;

import net.minecraft.client.settings.KeyBinding;

import java.util.ArrayList;
import java.util.List;

/**
 * Single source of truth for the key bindings CheatBreakerZ owns.
 *
 * <p>Registered bindings are stored in {@code global.cfg} and are deliberately excluded from
 * vanilla's {@code options.txt} round trip, so a binding can no longer be reset by the two
 * config paths disagreeing about who owns it. Values already present in {@code options.txt}
 * are migrated once, on the first startup that finds no {@code global.cfg} entry.
 */
public final class KeyBindingRegistry {
    private static final class Entry {
        private final String configName;
        private final KeyBinding binding;
        private final String[] optionNames;

        private Entry(String configName, KeyBinding binding, String[] optionNames) {
            this.configName = configName;
            this.binding = binding;
            this.optionNames = optionNames;
        }
    }

    private final List<Entry> entries = new ArrayList<Entry>();

    public void register(String configName, KeyBinding binding) {
        this.register(configName, binding, new String[0]);
    }

    /**
     * @param legacyOptionNames options.txt names this binding used to be stored under, tried after
     *                          its current key description when migrating.
     */
    public void register(String configName, KeyBinding binding, String... legacyOptionNames) {
        if (configName == null || binding == null) {
            return;
        }
        String[] optionNames = new String[legacyOptionNames.length + 1];
        optionNames[0] = binding.getKeyDescription();
        System.arraycopy(legacyOptionNames, 0, optionNames, 1, legacyOptionNames.length);
        this.entries.add(new Entry(configName, binding, optionNames));
    }

    /** True when this binding's value lives in global.cfg rather than options.txt. */
    public boolean manages(KeyBinding binding) {
        if (binding == null) {
            return false;
        }
        for (Entry entry : this.entries) {
            if (entry.binding == binding) {
                return true;
            }
        }
        return false;
    }

    public KeyBinding binding(String configName) {
        for (Entry entry : entries) if (entry.configName.equals(configName)) return entry.binding;
        return null;
    }

    /**
     * Applies stored codes to every registered binding. A binding absent from both sources keeps
     * whatever code it currently holds, which is its declared default.
     */
    public void load(List<String> globalLines, List<String> optionLines) {
        for (Entry entry : this.entries) {
            entry.binding.setKeyCode(KeyBindingConfig.resolveOrMigrate(globalLines, optionLines,
                    entry.configName, entry.optionNames, entry.binding.getKeyCode()));
        }
        KeyBinding.resetKeyBindingArrayAndHash();
    }

    public List<String> serialize() {
        List<String> lines = new ArrayList<String>(this.entries.size());
        for (Entry entry : this.entries) {
            lines.add(KeyBindingConfig.serialize(entry.configName, entry.binding.getKeyCode()));
        }
        return lines;
    }
}
