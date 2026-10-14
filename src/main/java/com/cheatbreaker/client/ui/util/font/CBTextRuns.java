package com.cheatbreaker.client.ui.util.font;

import java.util.ArrayList;
import java.util.List;

/**
 * 把字符串切成渲染段：>= U+0100 且非代理对的字符（CJK 等）走 vanilla FontRenderer，
 * 其余字符留在 CB 字体段。代理对（emoji 等）整对留在 CB 段，与 CBFontRenderer
 * 既有整串回退谓词（requiresUnicodeFallback）的逐字判定保持一致。
 */
public final class CBTextRuns {

    public static final class Run {
        public final String text;
        /** true = 该段需 vanilla FontRenderer 渲染。 */
        public final boolean unicode;

        Run(String text, boolean unicode) {
            this.text = text;
            this.unicode = unicode;
        }
    }

    private CBTextRuns() {
    }

    public static boolean isUnicodeChar(char c) {
        return c >= 'Ā' && !Character.isSurrogate(c);
    }

    public static List<Run> split(String text) {
        List<Run> runs = new ArrayList<>();
        if (text == null || text.isEmpty()) {
            return runs;
        }
        StringBuilder current = new StringBuilder();
        boolean currentUnicode = isUnicodeChar(text.charAt(0));
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            boolean unicode = isUnicodeChar(c);
            if (unicode != currentUnicode && current.length() > 0) {
                runs.add(new Run(current.toString(), currentUnicode));
                current.setLength(0);
                currentUnicode = unicode;
            }
            current.append(c);
        }
        if (current.length() > 0) {
            runs.add(new Run(current.toString(), currentUnicode));
        }
        return runs;
    }
}
