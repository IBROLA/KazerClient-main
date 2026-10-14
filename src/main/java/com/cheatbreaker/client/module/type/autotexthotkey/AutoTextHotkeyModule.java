package com.cheatbreaker.client.module.type.autotexthotkey;

import com.cheatbreaker.client.event.type.TickEvent;
import com.cheatbreaker.client.module.AbstractModule;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

import java.util.HashSet;
import java.util.Set;

/**
 * Functional module: quick-input rows fired from bound key sets (ADR-0003 self-managed engine).
 * No HUD, no anchor, no display settings — instances live in {@link AutoTextHotkeyState}
 * (management-page order = send order) and persist through {@link AutoTextHotkeyStateCodec}
 * under the profile's -AutoTextHotkeyState= line.
 *
 * Trigger shell (TickEvent): only while fully in-game (no screen open), this module gathers
 * the held set for codes that any bound row references, feeds {@link HotkeyTrigger}, and sends
 * each fired row's content verbatim via the chat channel in list order. All matching logic is
 * testable through {@link #pollAndSend(HeldKeyProbe, SendSink)}; the shell only injects the
 * LWJGL probe and the chat sink.
 */
public final class AutoTextHotkeyModule extends AbstractModule {
    private final AutoTextHotkeyState state = new AutoTextHotkeyState();
    private final HotkeyTrigger trigger = new HotkeyTrigger();

    public AutoTextHotkeyModule() {
        super("Auto Text Hotkey");
        this.setDefaultState(true);
        this.setDefaultRenderHud(false);
        this.isEditable = false;
        this.setPreviewLabel("Auto Text Hotkey", 1.0F);
        this.addEvent(TickEvent.class, this::onTick);
    }

    public AutoTextHotkeyState getState() {
        return this.state;
    }

    // --- row management (management-page API) ---

    public AutoTextHotkeyItem addInstance() {
        return this.state.add();
    }

    public boolean removeInstance(String id) {
        return this.state.remove(id);
    }

    public void setInstanceContent(String id, String content) {
        AutoTextHotkeyItem item = this.state.byId(id);
        if (item != null) {
            item.setContent(content);
        }
    }

    public void setInstanceEnabled(String id, boolean enabled) {
        AutoTextHotkeyItem item = this.state.byId(id);
        if (item != null) {
            item.enabled = enabled;
        }
    }

    public void setInstanceHidden(String id, boolean hidden) {
        AutoTextHotkeyItem item = this.state.byId(id);
        if (item != null) {
            item.hidden = hidden;
        }
    }

    /** 覆盖该行的键集合（空 = 解绑）。 */
    public void setInstanceKeys(String id, java.util.List<Integer> keys) {
        AutoTextHotkeyItem item = this.state.byId(id);
        if (item != null) {
            item.keys.clear();
            item.keys.addAll(keys);
        }
    }

    public void moveInstance(String id, int targetIndex) {
        this.state.move(id, targetIndex);
    }

    // --- persistence ---

    public String serializeState() {
        return AutoTextHotkeyStateCodec.encode(this.state);
    }

    public void loadState(String encoded) {
        AutoTextHotkeyState decoded = AutoTextHotkeyStateCodec.decode(encoded);
        this.state.items.clear();
        this.state.items.addAll(decoded.items);
        this.state.setNextId(decoded.getNextId());
    }

    public void resetState() {
        loadState("");
    }

    // --- trigger ---

    private void onTick(TickEvent event) {
        if (this.minecraft.currentScreen != null || this.minecraft.thePlayer == null) {
            return;
        }
        this.pollAndSend(code -> code >= 0 ? Keyboard.isKeyDown(code) : Mouse.isButtonDown(code + 100),
                this.minecraft.thePlayer::sendChatMessage);
    }

    /**
     * 每 tick 的匹配与派发：held 集合只覆盖被绑定行实际引用的键码；disabled / 未绑定行跳过；
     * 命中行按列表序依次经 sink 原样发送（同 tick、无节流）。
     */
    public void pollAndSend(HeldKeyProbe probe, SendSink sink) {
        Set<Integer> referenced = new HashSet<>();
        java.util.List<AutoTextHotkeyItem> candidates = new java.util.ArrayList<>();
        for (AutoTextHotkeyItem item : this.state.items) {
            if (item.enabled && !item.keys.isEmpty()) {
                candidates.add(item);
                referenced.addAll(item.keys);
            }
        }
        if (candidates.isEmpty()) {
            return;
        }
        Set<Integer> held = new HashSet<>();
        for (Integer code : referenced) {
            if (probe.isDown(code)) {
                held.add(code);
            }
        }
        for (AutoTextHotkeyItem item : this.trigger.poll(candidates, held)) {
            sink.send(item.content);
        }
    }

    /** 按住判定供给（生产 = LWJGL；测试 = 合成表）。 */
    public interface HeldKeyProbe {
        boolean isDown(int code);
    }

    /** 发送末端（生产 = EntityClientPlayerMP.sendChatMessage；测试 = 收集器）。 */
    public interface SendSink {
        void send(String content);
    }
}
