package com.cheatbreaker.client.module.type.autotexthotkey;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 键集合匹配引擎（纯逻辑，ADR-0003 自管路径；事件流由调用方按 tick 喂入）。
 * 每 tick 传入「候选行列表 + 当前按住集合」，返回本拍应触发的行（保持传入列表序）。
 * 规则：行绑定非空 ∧ 集合内全部键当前按住 ∧ 上一拍该集合未全按住（按下沿）→ 触发一次。
 * 按住不重复；完全松开后任一集合再次出现按下沿才再触发。门控（模块/行开关、作用域）
 * 由调用方在喂入前完成——引擎只认「此刻参与判定」的行。
 */
public final class HotkeyTrigger {
    /** 上一拍处于「全按住」态的行 id（用于按下沿判定）。 */
    private final Set<String> wasFiredUp = new HashSet<>();

    /**
     * @param rows  本拍参与判定的行（已过滤门控），列表序即触发序
     * @param held  当前按住的键码集合（键盘码原值，鼠标键按钮序号-100）
     * @return 本拍应触发的行（保持 rows 顺序，可能为空）
     */
    public List<AutoTextHotkeyItem> poll(List<AutoTextHotkeyItem> rows, Set<Integer> held) {
        List<AutoTextHotkeyItem> fired = new ArrayList<>();
        Set<String> nowFiredUp = new HashSet<>();
        for (AutoTextHotkeyItem row : rows) {
            if (row.keys.isEmpty()) {
                continue;
            }
            boolean allHeld = true;
            for (Integer key : row.keys) {
                if (!held.contains(key)) {
                    allHeld = false;
                    break;
                }
            }
            if (allHeld) {
                if (!this.wasFiredUp.contains(row.id)) {
                    fired.add(row);
                }
                nowFiredUp.add(row.id);
            }
        }
        this.wasFiredUp.clear();
        this.wasFiredUp.addAll(nowFiredUp);
        return fired;
    }
}
