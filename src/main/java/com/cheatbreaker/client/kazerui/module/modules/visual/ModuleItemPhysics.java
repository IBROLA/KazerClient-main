package com.cheatbreaker.client.kazerui.module.modules.visual;

import com.cheatbreaker.client.kazerui.module.Module;
import com.cheatbreaker.client.kazerui.module.Page;
import com.cheatbreaker.client.kazerui.module.setting.Setting;
import com.cheatbreaker.client.kazerui.module.setting.NumberSetting;
import net.minecraft.entity.item.EntityItem;

/** Stateless model transforms. Reads entity age/onGround only; no entity state changes. */
public final class ModuleItemPhysics extends Module {
    private static ModuleItemPhysics instance;
    private final NumberSetting speed = decimal("rotationSpeed", 1, .1f, 4);
    public ModuleItemPhysics() { super("ITEM_PHYSICS", false); instance = this; labels(); }
    private void labels() {
        for (Setting<?> setting : settings()) {
            switch (setting.key) {
                case "rotationSpeed": setting.label(() -> "Rotation Speed"); break;
                default: break;
            }
        }
    }
    @Override protected void layout(Page page) { page.add(speed); }
    public static boolean active() { return instance != null && instance.isEnabled(); }
    public static float bob(float original) { return active() ? .025f : original; }
    public static float angle(EntityItem item, float age) {
        if (!active()) return age;
        return item.onGround ? 90 : (age * instance.speed.value() * 12) % 360;
    }
}
