package com.cheatbreaker.client.nethandler.apollo;

import com.google.gson.*;
import java.util.*;

/** Adventure formatting to the 1.7 renderer; unsupported RGB is mapped to the nearest legacy color. */
public final class AdventureText {
    private static final String[] NAMES = {"black", "dark_blue", "dark_green", "dark_aqua", "dark_red",
            "dark_purple", "gold", "gray", "dark_gray", "blue", "green", "aqua", "red", "light_purple", "yellow", "white"};
    private static final int[] RGB = {0x000000,0x0000aa,0x00aa00,0x00aaaa,0xaa0000,0xaa00aa,0xffaa00,0xaaaaaa,
            0x555555,0x5555ff,0x55ff55,0x55ffff,0xff5555,0xff55ff,0xffff55,0xffffff};
    private static final String[] FLAGS = {"obfuscated", "bold", "strikethrough", "underlined", "italic"};

    private AdventureText() { }
    public static String legacy(String json) {
        if (json.length() > 8192) throw new IllegalArgumentException("Oversized component");
        StringBuilder result = new StringBuilder();
        append(ApolloProtocol.parseJson(json), new Style(), result, 0);
        if (result.length() > 8192) throw new IllegalArgumentException("Oversized text");
        return result.toString();
    }

    static char color(String color) {
        for (int i = 0; i < NAMES.length; i++) if (NAMES[i].equalsIgnoreCase(color)) return "0123456789abcdef".charAt(i);
        if (color != null && color.matches("#[0-9a-fA-F]{6}")) {
            int rgb = Integer.parseInt(color.substring(1), 16), best = 15, distance = Integer.MAX_VALUE;
            for (int i = 0; i < RGB.length; i++) {
                int r = (rgb >> 16 & 255) - (RGB[i] >> 16 & 255);
                int g = (rgb >> 8 & 255) - (RGB[i] >> 8 & 255);
                int b = (rgb & 255) - (RGB[i] & 255);
                int d = r*r + g*g + b*b;
                if (d < distance) { distance = d; best = i; }
            }
            return "0123456789abcdef".charAt(best);
        }
        return 'f';
    }

    private static void append(JsonElement e, Style parent, StringBuilder out, int depth) {
        if (depth > 32 || out.length() > 8192) throw new IllegalArgumentException("Component nesting limit");
        if (e == null || e.isJsonNull()) return;
        if (e.isJsonArray()) {
            // Adventure array's first component is the parent of subsequent components.
            JsonArray array = e.getAsJsonArray();
            Style inherited = parent;
            for (int i = 0; i < array.size(); i++) {
                JsonElement child = array.get(i);
                append(child, inherited, out, depth + 1);
                if (i == 0 && child.isJsonObject()) inherited = parent.with(child.getAsJsonObject());
            }
            return;
        }
        Style style = parent;
        String text = "";
        JsonObject object = null;
        if (e.isJsonPrimitive()) text = e.getAsString();
        else {
            object = e.getAsJsonObject();
            style = parent.with(object);
            if (object.has("text")) text = object.get("text").getAsString();
            else if (object.has("translate")) text = object.has("fallback") ? object.get("fallback").getAsString() : object.get("translate").getAsString();
            else if (object.has("keybind")) text = object.get("keybind").getAsString();
            else if (object.has("score") && object.getAsJsonObject("score").has("value")) text = object.getAsJsonObject("score").get("value").getAsString();
        }
        if (!text.isEmpty()) {
            out.append('\u00a7').append('r').append('\u00a7').append(style.color);
            for (int i = 0; i < style.flags.length; i++) if (style.flags[i]) out.append('\u00a7').append("klmno".charAt(i));
            out.append(text);
        }
        if (object != null && object.has("extra"))
            for (JsonElement child : object.getAsJsonArray("extra")) append(child, style, out, depth + 1);
    }

    private static class Style {
        char color = 'f';
        boolean[] flags = new boolean[5];
        Style with(JsonObject o) {
            Style next = new Style();
            next.color = o.has("color") ? AdventureText.color(o.get("color").getAsString()) : color;
            next.flags = flags.clone();
            for (int i = 0; i < FLAGS.length; i++) if (o.has(FLAGS[i])) next.flags[i] = o.get(FLAGS[i]).getAsBoolean();
            return next;
        }
    }
}
