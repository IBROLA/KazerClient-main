package com.cheatbreaker.client.module.type;

import java.util.function.ToIntFunction;

/** Immutable text/geometry for one scoreboard tick; drawing does no sorting or measuring. */
public final class ScoreboardRenderData {
    public final String title;
    public final String[] names, numbers;
    public final int[] numberWidths;
    public final int width, right, titleWidth;
    public ScoreboardRenderData(String title, String[] names, String[] numbers, ToIntFunction<String> measure) {
        this.title = title; this.names = names; this.numbers = numbers;
        this.numberWidths = new int[numbers.length];
        titleWidth = measure.applyAsInt(title);
        int widest = titleWidth;
        for (int i = 0; i < names.length; i++) {
            widest = Math.max(widest, measure.applyAsInt(names[i] + ": " + numbers[i]));
            numberWidths[i] = measure.applyAsInt(numbers[i]);
        }
        width = widest;
        right = Math.max(width + 9, titleWidth + 16);
    }
}
