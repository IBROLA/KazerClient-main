package com.cheatbreaker.client.module.type.customtext;

import java.util.ArrayList;
import java.util.List;

/**
 * Single-line codec for the Custom Text profile entry. Entries are `|`-separated records of
 * `~`-separated positional fields; `\` escapes both separators. Malformed entries are skipped,
 * loads cap at {@link CustomTextState#MAX_ITEMS} and keep id monotonicity so a truncated or
 * edited config can never resurrect colliding ids.
 */
public final class CustomTextStateCodec {
    private CustomTextStateCodec() {
    }

    public static String encode(CustomTextState state) {
        StringBuilder out = new StringBuilder();
        for (CustomTextItem item : state.items) {
            out.append(escape(item.id))
                    .append("~").append(escape(item.content))
                    .append("~").append(item.color)
                    .append("~").append(item.showBackground ? 1 : 0)
                    .append("~").append(item.backgroundColor)
                    .append("~").append(item.scale)
                    .append("~").append(escape(item.anchor))
                    .append("~").append(item.xTranslation)
                    .append("~").append(item.yTranslation)
                    .append("~").append(item.enabled ? 1 : 0)
                    .append("~").append(item.renderHud ? 1 : 0)
                    .append("|");
        }
        out.append("next=").append(state.getNextId());
        return out.toString();
    }

    public static CustomTextState decode(String encoded) {
        CustomTextState state = new CustomTextState();
        if (encoded == null || encoded.isEmpty()) {
            return state;
        }
        for (String record : split(encoded, '|')) {
            if (record.startsWith("next=")) {
                state.setNextId(parseInt(record.substring(5), 1));
                continue;
            }
            List<String> fields = split(record, '~');
            if (fields.size() < 2) {
                continue;
            }
            if (state.items.size() >= CustomTextState.MAX_ITEMS) {
                continue;
            }
            CustomTextItem item = state.add();
            if (item == null) {
                continue;
            }
            item.id = unescape(fields.get(0));
            item.setContent(unescape(fields.get(1)));
            if (fields.size() > 2) item.color = parseColor(fields.get(2));
            if (fields.size() > 3) item.showBackground = "1".equals(fields.get(3));
            if (fields.size() > 4) item.backgroundColor = parseColor(fields.get(4));
            if (fields.size() > 5) item.scale = parseFloat(fields.get(5), CustomTextItem.DEFAULT_SCALE);
            if (fields.size() > 6) item.anchor = unescape(fields.get(6));
            if (fields.size() > 7) item.xTranslation = parseFloat(fields.get(7), 0.0F);
            if (fields.size() > 8) item.yTranslation = parseFloat(fields.get(8), 0.0F);
            if (fields.size() > 9) item.enabled = !"0".equals(fields.get(9));
            if (fields.size() > 10) item.renderHud = !"0".equals(fields.get(10));
        }
        return state;
    }

    private static int parseColor(String value) {
        try {
            return value.startsWith("0x") ? (int) Long.parseLong(value.substring(2), 16)
                    : (int) Long.parseLong(value);
        } catch (NumberFormatException ignored) {
            return CustomTextItem.DEFAULT_COLOR;
        }
    }

    private static int parseInt(String value, int fallback) {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException ignored) {
            return fallback;
        }
    }

    private static float parseFloat(String value, float fallback) {
        try {
            return Float.parseFloat(value);
        } catch (NumberFormatException ignored) {
            return fallback;
        }
    }

    private static String escape(String value) {
        StringBuilder out = new StringBuilder();
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if (c == '\\' || c == '~' || c == '|') {
                out.append('\\');
            }
            out.append(c);
        }
        return out.toString();
    }

    private static String unescape(String value) {
        StringBuilder out = new StringBuilder();
        boolean escaped = false;
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if (escaped) {
                out.append(c);
                escaped = false;
            } else if (c == '\\') {
                escaped = true;
            } else {
                out.append(c);
            }
        }
        return out.toString();
    }

    private static List<String> split(String input, char separator) {
        // Keep escape sequences intact at this level so nested splitting and final
        // unescape each see exactly their own layer of encoding.
        List<String> tokens = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean escaped = false;
        for (int i = 0; i < input.length(); i++) {
            char c = input.charAt(i);
            if (escaped) {
                current.append(c);
                escaped = false;
            } else if (c == '\\') {
                current.append(c);
                escaped = true;
            } else if (c == separator) {
                tokens.add(current.toString());
                current.setLength(0);
            } else {
                current.append(c);
            }
        }
        tokens.add(current.toString());
        return tokens;
    }
}
