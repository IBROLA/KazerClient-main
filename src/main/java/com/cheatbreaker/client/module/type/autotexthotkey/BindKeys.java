package com.cheatbreaker.client.module.type.autotexthotkey;

import org.lwjgl.input.Keyboard;

/**
 * 键名显示：键盘键用 LWJGL 键名，鼠标键（负码 = 按钮序号 - 100）用 1 基 BUTTON N；
 * 组合按捕获序以 + 拼接；BIND 条三态文案（未绑定 BIND / 捕获中 ... / 已绑定键名）。
 */
public final class BindKeys {
    private BindKeys() {
    }

    public static String keyName(int code) {
        if (code < 0) {
            return "BUTTON " + (code + 101);
        }
        return Keyboard.getKeyName(code).toUpperCase();
    }

    public static String displayName(int[] codes) {
        StringBuilder out = new StringBuilder();
        for (int code : codes) {
            if (out.length() > 0) {
                out.append('+');
            }
            out.append(keyName(code));
        }
        return out.toString();
    }

    public static String barLabel(int[] codes, boolean capturing) {
        if (capturing) {
            return "...";
        }
        return codes.length == 0 ? "BIND" : displayName(codes);
    }
}
