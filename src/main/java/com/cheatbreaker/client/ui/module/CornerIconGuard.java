package com.cheatbreaker.client.ui.module;

/**
 * 画布角标点击守卫：RShift 编辑器画布上 HUD 框左下齿轮（进设置）与右下叉号（关闭）
 * 的唯一几何来源与挂起判定纯函数。契约见 .docs/client-ui-contracts.md。
 * 图标为模块本地 6x6 矩形：齿轮 (GEAR_LOCAL_X, height - ICON_TOP_OFFSET) 起，
 * 叉号 (width - CLOSE_RIGHT_INSET, height - ICON_TOP_OFFSET) 起；屏幕坐标 = (origin + local) * scale。
 */
public final class CornerIconGuard {

    public static final float ICON_SIZE = 6.0f;
    public static final float GEAR_LOCAL_X = 2.0f;
    public static final float CLOSE_RIGHT_INSET = 8.0f;
    public static final float ICON_TOP_OFFSET = 7.5f;
    public static final int DRAG_THRESHOLD_PX = 4;
    public static final long HOLD_DRAG_MS = 1000L;

    public enum Icon {
        GEAR,
        CLOSE
    }

    public enum ReleaseOutcome {
        EXECUTE,
        CANCEL
    }

    private CornerIconGuard() {
    }

    public static boolean hitsGear(float originX, float originY, float scale, float width, float height, int mouseX, int mouseY) {
        return inside(originX, originY, scale, GEAR_LOCAL_X, height - ICON_TOP_OFFSET, mouseX, mouseY);
    }

    public static boolean hitsClose(float originX, float originY, float scale, float width, float height, int mouseX, int mouseY) {
        return inside(originX, originY, scale, width - CLOSE_RIGHT_INSET, height - ICON_TOP_OFFSET, mouseX, mouseY);
    }

    private static boolean inside(float originX, float originY, float scale, float localX, float localY, int mouseX, int mouseY) {
        return mouseX >= (originX + localX) * scale
                && mouseX <= (originX + localX + ICON_SIZE) * scale
                && mouseY >= (originY + localY) * scale
                && mouseY <= (originY + localY + ICON_SIZE) * scale;
    }

    /** 位移是否已逾拖动阈值（> 4px，dist^2 > 16）。 */
    public static boolean exceedsDragThreshold(int pressX, int pressY, int mouseX, int mouseY) {
        int dx = mouseX - pressX;
        int dy = mouseY - pressY;
        return dx * dx + dy * dy > DRAG_THRESHOLD_PX * DRAG_THRESHOLD_PX;
    }

    /** 按住时长是否已逾长按阈值（> 1000ms）。 */
    public static boolean exceedsHold(long pressTimeMs, long nowMs) {
        return nowMs - pressTimeMs > HOLD_DRAG_MS;
    }

    /**
     * 左键挂起后的松开结算：未超时、未逾位移阈值且指针仍在图标内才执行，
     * 其余情况一律取消（已转拖动或视为无效点击），不产生图标动作。
     */
    public static ReleaseOutcome resolveRelease(long pressTimeMs, long nowMs,
                                                int pressX, int pressY, int mouseX, int mouseY,
                                                boolean insideIcon) {
        if (!insideIcon || exceedsHold(pressTimeMs, nowMs) || exceedsDragThreshold(pressX, pressY, mouseX, mouseY)) {
            return ReleaseOutcome.CANCEL;
        }
        return ReleaseOutcome.EXECUTE;
    }
}
