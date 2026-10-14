package com.cheatbreaker.client.kazerui.gui.ui;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class KazerLang {
    private static final Pattern ARG = Pattern.compile("(?<raw>[$%](?<id>\\d+)?(?<i18n>\\$[sdf])?(\\{(?<name>[a-zA-Z0-9]+)})?)");
    private static Map<String, Object> root;

    private KazerLang() {}

    @SuppressWarnings("unchecked")
    private static synchronized Map<String, Object> root() {
        if (root == null) {
            try (InputStream in = KazerLang.class.getResourceAsStream("/assets/kazerui/lang/kazer/en_US.json")) {
                ByteArrayOutputStream bytes = new ByteArrayOutputStream();
                byte[] buffer = new byte[8192];
                for (int n; (n = in.read(buffer)) > 0; ) bytes.write(buffer, 0, n);
                root = (Map<String, Object>)Json.parse(new String(bytes.toByteArray(), StandardCharsets.UTF_8));
            } catch (Exception e) {
                throw new IllegalStateException("Cannot load Kazer's en_US.json", e);
            }
        }
        return root;
    }

    @SuppressWarnings("unchecked")
    public static String get(String path, String key, Object... args) {
        Map<String, Object> node = root();
        for (String segment : path.split("\\.")) {
            if (segment.isEmpty()) continue;
            Object child = node.get(segment);
            if (child instanceof Map) node = (Map<String, Object>)child;
        }
        Object value = node.get(key);
        if (!(value instanceof String) || ((String)value).isEmpty()) return key;
        return format((String)value, args);
    }

    public static boolean has(String path, String key) { return !get(path, key).equals(key); }

    static String format(String text, Object... args) {
        if (!text.contains("$")) return text;
        Matcher m = ARG.matcher(text);
        String out = text;
        while (m.find()) {
            if (m.group("id") == null) continue;
            int index = Integer.parseInt(m.group("id")) - (m.group("i18n") != null ? 1 : 0);
            if (index >= 0 && index < args.length) out = out.replace(m.group("raw"), String.valueOf(args[index]));
        }
        return out;
    }

    private static final java.util.Map<String, java.util.function.Supplier<String>> NAMES = new java.util.HashMap<String, java.util.function.Supplier<String>>();

    public static void registerName(String id, java.util.function.Supplier<String> name) { NAMES.put(id.toUpperCase(java.util.Locale.ROOT), name); }

    public static String featureName(String id, String fallback) {
        java.util.function.Supplier<String> own = NAMES.get(id.toUpperCase(java.util.Locale.ROOT));
        if (own != null) return own.get();
        String name = get("features." + id.toUpperCase(java.util.Locale.ROOT) + ".details", "name");
        return name.equals("name") ? fallback : name;
    }

    public static String featureDescription(String id) {
        if(id.equalsIgnoreCase("particle_changer"))return "Adjust particle size, density, visible types and color without changing combat.";
        if(id.equalsIgnoreCase("glint_colorizer"))return "Set item and armor glint colors; respect Overlay visibility and native depth coverage.";
        if(id.equalsIgnoreCase("scrollable_tooltips"))return "Scroll long item tips. Shift + Wheel moves horizontally; Ctrl + Wheel changes scale.";
        if(id.equalsIgnoreCase("stopwatch"))return "A movable stopwatch or countdown HUD with start, pause and reset controls.";
        if(id.equalsIgnoreCase("item_physics"))return "Dropped-item model rotation and ground display; takes priority over 2D Items without changing item physics.";
        if(id.equalsIgnoreCase("light_overlay"))return "Show nearby floor brightness markers using bounded checks of loaded blocks. Markers are light hints, not exact mob-spawn predictions.";
        if(id.equalsIgnoreCase("weather_changer"))return "Choose local clear, rain or snow visuals and weather sounds. Server weather stays unchanged.";
        if(id.equalsIgnoreCase("overlay"))return "Adjust texture overlays, first-person effects and visible entity layers.";
        String d = get("features." + id.toUpperCase(java.util.Locale.ROOT) + ".details", "description");
        return d.equals("description") ? null : d;
    }
}
