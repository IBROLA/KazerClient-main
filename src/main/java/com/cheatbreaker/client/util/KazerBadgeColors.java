package com.cheatbreaker.client.util;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Shared nametag/TAB palette. A configured UUID is not proof of online presence. */
public final class KazerBadgeColors {
    public static final UUID CANRX_UUID = UUID.fromString("9df8fe7d-81c4-423e-aaa5-96921b9b017f");
    public static final int WHITE = 0xFFFFFF;
    public static final int CANRX_DEFAULT = 0xAA0000; // Minecraft &4, matching the 1.7 template.
    private static final int[] MC = {0x000000,0x0000AA,0x00AA00,0x00AAAA,
            0xAA0000,0xAA00AA,0xFFAA00,0xAAAAAA,0x555555,0x5555FF,0x55FF55,
            0x55FFFF,0xFF5555,0xFF55FF,0xFFFF55,0xFFFFFF};
    private KazerBadgeColors() {}
    public static int defaultColor(UUID uuid) {
        return CANRX_UUID.equals(uuid) ? CANRX_DEFAULT : WHITE;
    }
    public static Integer parse(String raw) {
        if (raw == null) return null;
        String color = raw.trim();
        if (color.length() == 2 && (color.charAt(0) == '&' || color.charAt(0) == '\u00a7')) {
            int index = Character.digit(color.charAt(1), 16);
            return index < 0 ? null : MC[index];
        }
        if (color.startsWith("#")) color = color.substring(1);
        if (!color.matches("[0-9a-fA-F]{6}")) return null;
        return Integer.parseInt(color, 16);
    }
    public static String normalizeUuid(String raw) {
        if (raw == null) return null;
        String value = raw.replace("-", "").trim().toLowerCase(java.util.Locale.ROOT);
        return value.matches("[0-9a-f]{32}") ? value : null;
    }
    /** Protocol 1 UUID-only lines remain valid; protocol 2 adds a server-owned RGB value. */
    public static void acceptLine(Map<String, Integer> output, String line) {
        String[] parts = line.split("\\t", -1);
        String id = normalizeUuid(parts[0]);
        if (id == null || parts.length > 2) return;
        UUID uuid = UUID.fromString(id.substring(0,8)+"-"+id.substring(8,12)+"-"
                +id.substring(12,16)+"-"+id.substring(16,20)+"-"+id.substring(20));
        Integer color = parts.length == 2 ? parse(parts[1]) : Integer.valueOf(defaultColor(uuid));
        if (color != null) output.put(id, color);
    }
}
