package com.cheatbreaker.client.module.type.autotexthotkey;

import java.util.ArrayList;
import java.util.List;

/**
 * 绑定捕获协议状态机（纯逻辑，无 MC/LWJGL 依赖；事件由视图层喂入）。
 * 语义：捕获期间按下的每个键累积入集合（捕获序）；集合中任一成员松开即定稿，
 * 绑定 = 捕获期间按过的全部键（含刚松开者）；ESC 按下 = 清除绑定并结束；
 * 左键（-100）/右键（-99）按下 = 取消并恢复原绑定；二者永不入集合。
 * 定稿后的事件一律忽略。
 */
public final class BindCapture {
    /** vanilla 键码约定：鼠标键 = 按钮序号 - 100。 */
    public static final int MOUSE_LEFT = -100;
    public static final int MOUSE_RIGHT = -99;
    public static final int KEY_ESCAPE = 1;

    public enum Phase { IDLE, CAPTURING, FINISHED, CLEARED, CANCELLED }

    private Phase phase = Phase.IDLE;
    private final List<Integer> captured = new ArrayList<>();
    private final List<Integer> original = new ArrayList<>();

    /** 进入捕获；original 为该行当前绑定（取消时恢复用），可为空。 */
    public void begin(List<Integer> originalKeys) {
        this.phase = Phase.CAPTURING;
        this.captured.clear();
        this.original.clear();
        if (originalKeys != null) {
            this.original.addAll(originalKeys);
        }
    }

    public Phase phase() {
        return this.phase;
    }

    /** 定稿/清除/取消后的结果集合；CAPTURING 期间为已累积成员。 */
    public List<Integer> resultKeys() {
        switch (this.phase) {
            case FINISHED: return new ArrayList<>(this.captured);
            case CLEARED:  return new ArrayList<>();
            case CANCELLED: return new ArrayList<>(this.original);
            default: return new ArrayList<>(this.captured);
        }
    }

    public void keyPressed(int code) {
        if (this.phase != Phase.CAPTURING) {
            return;
        }
        if (code == KEY_ESCAPE) {
            this.captured.clear();
            this.phase = Phase.CLEARED;
            return;
        }
        if (!this.captured.contains(code)) {
            this.captured.add(code);
        }
    }

    public void mousePressed(int button) {
        if (this.phase != Phase.CAPTURING) {
            return;
        }
        if (button == MOUSE_LEFT || button == MOUSE_RIGHT) {
            this.captured.clear();
            this.phase = Phase.CANCELLED;
            return;
        }
        if (!this.captured.contains(button)) {
            this.captured.add(button);
        }
    }

    public void keyReleased(int code) {
        releaseOf(code);
    }

    public void mouseReleased(int button) {
        releaseOf(button);
    }

    private void releaseOf(int code) {
        if (this.phase != Phase.CAPTURING) {
            return;
        }
        if (this.captured.contains(code)) {
            this.phase = Phase.FINISHED;
        }
    }
}
