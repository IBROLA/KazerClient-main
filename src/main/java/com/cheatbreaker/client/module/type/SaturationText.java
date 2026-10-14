package com.cheatbreaker.client.module.type;

import net.minecraft.util.FoodStats;

import java.util.Locale;

public final class SaturationText {
    private SaturationText() {
    }

    /** 定位用参考文本：饱和度上限 20，取当前显示模式下的最宽形态。 */
    public static String referenceText(boolean showDecimals) {
        return showDecimals ? "20.0" : "20";
    }

    public static String fromFoodStats(FoodStats stats, boolean showDecimals) {
        if (stats == null) {
            return null;
        }
        if (showDecimals) {
            return String.format(Locale.ROOT, "%.1f", stats.getSaturationLevel());
        }
        return Integer.toString(Math.round(stats.getSaturationLevel()));
    }
}
