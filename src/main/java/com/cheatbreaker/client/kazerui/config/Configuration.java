package com.cheatbreaker.client.kazerui.config;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;

/** Track-independent UI store. Atomic saves never replace legacy config files. */
public final class Configuration {
    private final File file;
    private final Properties values = new Properties();
    private boolean changed;
    public Configuration(File file) { this.file = file; }
    public void load() {
        if (!file.isFile()) return;
        try (Reader reader = new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8)) {
            values.load(reader);
        } catch (IOException error) { throw new IllegalStateException("Cannot read Kazer UI settings", error); }
    }
    public boolean hasKey(String category, String key) { return values.containsKey(category + "." + key); }
    public boolean hasCategory(String category) {
        for (String key : values.stringPropertyNames()) if (key.startsWith(category + ".")) return true;
        return false;
    }
    public Category getCategory(String category) { return new Category(category); }
    public Value get(String category, String key, String fallback) {
        String full = category + "." + key;
        if (!values.containsKey(full)) { values.setProperty(full, fallback); changed = true; }
        return new Value(full);
    }
    public boolean getBoolean(String key, String category, boolean fallback, String comment) {
        return Boolean.parseBoolean(get(category,key,Boolean.toString(fallback)).getString());
    }
    public boolean hasChanged() { return changed; }
    public void save() {
        if (!changed) return;
        Path destination = file.toPath().toAbsolutePath();
        Path temporary = null;
        try {
            Files.createDirectories(destination.getParent());
            temporary = Files.createTempFile(destination.getParent(), "kazer-ui-", ".tmp");
            try (Writer writer = new OutputStreamWriter(Files.newOutputStream(temporary), StandardCharsets.UTF_8)) {
                values.store(writer,"Kazer UI schema 1 - shared by Minecraft 1.7.10 and 1.8.9");
            }
            try { Files.move(temporary,destination,StandardCopyOption.ATOMIC_MOVE,StandardCopyOption.REPLACE_EXISTING); }
            catch (AtomicMoveNotSupportedException error) { Files.move(temporary,destination,StandardCopyOption.REPLACE_EXISTING); }
            changed = false;
        } catch (IOException error) { throw new IllegalStateException("Cannot save Kazer UI settings",error); }
        finally {
            if (temporary != null) try { Files.deleteIfExists(temporary); } catch (IOException ignored) { }
        }
    }
    public final class Value {
        private final String key;
        private Value(String key) { this.key = key; }
        public String getString() { return values.getProperty(key); }
        public void set(String value) {
            if (!Objects.equals(values.getProperty(key),value)) { values.setProperty(key,value); changed = true; }
        }
    }
    public final class Category {
        private final String prefix;
        private Category(String category) { prefix = category + "."; }
        public Value get(String key) { return new Value(prefix + key); }
        public Set<String> keySet() {
            Set<String> keys = new LinkedHashSet<String>();
            for (String key : values.stringPropertyNames()) if (key.startsWith(prefix)) keys.add(key.substring(prefix.length()));
            return keys;
        }
        public void remove(String key) { if (values.remove(prefix + key) != null) changed = true; }
    }
}
