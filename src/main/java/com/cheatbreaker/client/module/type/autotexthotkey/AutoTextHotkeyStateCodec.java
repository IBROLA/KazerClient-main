package com.cheatbreaker.client.module.type.autotexthotkey;

import java.util.ArrayList;
import java.util.List;

/**
 * Single-line codec for the Auto Text Hotkey profile entry. Same wire style as Custom Text:
 * entries are `|`-separated records of `~`-separated fields; `\` escapes both separators.
 * Field layout: id ~ content ~ enabled ~ hidden ~ keys ( comma-separated, capture order ).
 * Malformed entries are skipped, loads cap at {@link AutoTextHotkeyState#MAX_ITEMS}, and a
 * corrupt keys field drops that whole set while leaving the row's other fields intact.
 */
public final class AutoTextHotkeyStateCodec {
    private AutoTextHotkeyStateCodec() {
    }

    public static String encode(AutoTextHotkeyState state) {
        StringBuilder out = new StringBuilder();
        for (AutoTextHotkeyItem item : state.items) {
            out.append(escape(item.id))
                    .append("~").append(escape(item.content))
                    .append("~").append(item.enabled ? 1 : 0)
                    .append("~").append(item.hidden ? 1 : 0)
                    .append("~").append(joinKeys(item.keys))
                    .append("|");
        }
        out.append("next=").append(state.getNextId());
        return out.toString();
    }

    public static AutoTextHotkeyState decode(String encoded) {
        AutoTextHotkeyState state = new AutoTextHotkeyState();
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
            if (state.items.size() >= AutoTextHotkeyState.MAX_ITEMS) {
                continue;
            }
            AutoTextHotkeyItem item = state.add();
            if (item == null) {
                continue;
            }
            item.id = unescape(fields.get(0));
            item.setContent(unescape(fields.get(1)));
            if (fields.size() > 2) item.enabled = !"0".equals(fields.get(2));
            if (fields.size() > 3) item.hidden = "1".equals(fields.get(3));
            if (fields.size() > 4) parseKeys(fields.get(4), item.keys);
        }
        return state;
    }

    private static String joinKeys(List<Integer> keys) {
        StringBuilder out = new StringBuilder();
        for (Integer code : keys) {
            if (out.length() > 0) {
                out.append(',');
            }
            out.append(code.intValue());
        }
        return out.toString();
    }

    /** Any unparseable token invalidates the whole set; an empty field is simply unbound. */
    static void parseKeys(String field, List<Integer> out) {
        if (field == null || field.isEmpty()) {
            return;
        }
        List<Integer> parsed = new ArrayList<>();
        for (String token : field.split(",")) {
            try {
                parsed.add(Integer.valueOf(Integer.parseInt(token)));
            } catch (NumberFormatException invalid) {
                return;
            }
        }
        out.addAll(parsed);
    }

    private static int parseInt(String value, int fallback) {
        try {
            return Integer.parseInt(value);
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
