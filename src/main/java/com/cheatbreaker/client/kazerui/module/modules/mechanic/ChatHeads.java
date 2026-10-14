package com.cheatbreaker.client.kazerui.module.modules.mechanic;

import com.cheatbreaker.client.kazerui.module.ChatEvent;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.minecraft.client.Minecraft;
import net.minecraft.client.network.NetHandlerPlayClient;
import net.minecraft.util.ResourceLocation;

final class ChatHeads {
    private static final Pattern NAME = Pattern.compile("[A-Za-z0-9_]{3,16}");
    private static final String SEPARATORS = "»›:>";

    private final LinkedHashMap<Integer, String> byMessage = new LinkedHashMap<Integer, String>();
    void clear() { byMessage.clear(); }

    void register(ChatEvent message) {
        String info = find(message.plain);
        if (info == null) return;
        byMessage.put(message.id, info);
        Iterator<Integer> it = byMessage.keySet().iterator();
        while (byMessage.size() > 1200 && it.hasNext()) { it.next(); it.remove(); }
    }

    private String find(String text) {
        Matcher m = NAME.matcher(text);
        while (m.find()) {
            if (m.start() > 40) return null;
            if (!(bracketed(text, m) || separator(text, m) != '\0') || !ChatPlayers.known(m.group())) continue;
            return m.group();
        }
        return null;
    }

    ResourceLocation skin(int id) {
        String name = byMessage.get(id);
        return name == null ? null : ChatPlayers.skin(name);
    }

    private static char separator(String s, Matcher m) {
        int i = skipSpaces(s, m.end());
        if (i < s.length() && s.charAt(i) == '[') {
            int close = s.indexOf(']', i);
            if (close == -1) return '\0';
            i = skipSpaces(s, close + 1);
        }
        return i < s.length() && SEPARATORS.indexOf(s.charAt(i)) != -1 ? s.charAt(i) : '\0';
    }

    private static boolean bracketed(String s, Matcher m) {
        return m.start() > 0 && s.charAt(m.start() - 1) == '<' && m.end() < s.length() && s.charAt(m.end()) == '>';
    }

    private static int skipSpaces(String s, int i) {
        while (i < s.length() && s.charAt(i) == ' ') i++;
        return i;
    }
}
