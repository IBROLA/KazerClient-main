package com.cheatbreaker.client.kazerui.module.modules.mechanic;

import com.cheatbreaker.client.kazerui.module.Module;
import com.cheatbreaker.client.kazerui.module.ModuleManager;
import com.cheatbreaker.client.kazerui.module.Page;
import com.cheatbreaker.client.kazerui.module.setting.BoolSetting;
import com.cheatbreaker.client.kazerui.module.setting.NumberSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.potion.Potion;
import net.minecraft.util.MathHelper;

public final class ModuleFov extends Module {
    private static ModuleFov instance;
    private final BoolSetting staticFOV = bool("staticFOV", false);

    private final NumberSetting defaultFov = integer("defaultFov", 70, 30, 110);

    private final BoolSetting smoothFov = bool("smoothFov", true);

    private final BoolSetting dynamicFlying = bool("dynamicFlying", true);

    private final NumberSetting flyingFov = integer("flyingFov", 70, 30, 110);

    private final NumberSetting flyingModifier = decimal("flyingModifier", 1.0f, 0.0f, 5.0f).roundTo(2);

    private final NumberSetting flyingMin = decimal("flyingMin", -10.0f, -200.0f, 200.0f).roundTo(2);

    private final NumberSetting flyingMax = decimal("flyingMax", 10.0f, -200.0f, 200.0f).roundTo(2);

    private final BoolSetting dynamicEffects = bool("dynamicEffects", true);

    private final NumberSetting slownessFOV = integer("slownessFOV", 70, 30, 110);

    private final NumberSetting speedFOV = integer("speedFOV", 70, 30, 110);

    private final NumberSetting speedTwoFOV = integer("speedTwoFOV", 70, 30, 110);

    private final NumberSetting movementModifier = decimal("movementModifier", 1.0f, 0.0f, 5.0f).roundTo(2);

    private final NumberSetting movementMin = decimal("movementMin", -10.0f, -200.0f, 200.0f).roundTo(2);

    private final NumberSetting movementMax = decimal("movementMax", 10.0f, -200.0f, 200.0f).roundTo(2);

    private final BoolSetting dynamicSprint = bool("dynamicSprint", true);

    private final NumberSetting sprintingFOV = integer("sprintingFOV", 70, 30, 110);

    private final NumberSetting sprintModifier = decimal("sprintModifier", 1.0f, 0.0f, 5.0f).roundTo(2);

    private final NumberSetting sprintMin = decimal("sprintMin", -10.0f, -200.0f, 200.0f).roundTo(2);

    private final NumberSetting sprintMax = decimal("sprintMax", 10.0f, -200.0f, 200.0f).roundTo(2);

    private final BoolSetting dynamicBow = bool("dynamicBow", true);

    private final NumberSetting aimingModifier = decimal("aimingModifier", 1.0f, 0.0f, 5.0f).roundTo(2);

    private final NumberSetting aimingMin = decimal("aimingMin", -10.0f, -200.0f, 200.0f).roundTo(2);

    private final NumberSetting aimingMax = decimal("aimingMax", 10.0f, -200.0f, 200.0f).roundTo(2);

    private long lastUpdate = -1L;

    private float field_6255 = 70.0f;

    private float field_6256;

    private boolean handPass;

    public ModuleFov() {
        super("FOV", false);
        instance=this;
    }

    @Override protected void layout(Page page) {
        page.section("generalOptions", s -> {
            s.add(staticFOV, defaultFov);
            s.add(smoothFov).hideIf(staticFOV::on);
        });

        page.group(dynamicBow, g -> g.add(aimingModifier, aimingMin, aimingMax)).hideIf(staticFOV::on);
        page.group(dynamicEffects, g -> g.add(speedFOV, speedTwoFOV, slownessFOV, movementModifier, movementMin, movementMax)).hideIf(staticFOV::on);
        page.group(dynamicSprint, g -> g.add(sprintingFOV, sprintModifier, sprintMin, sprintMax)).hideIf(staticFOV::on);
        page.group(dynamicFlying, g -> g.add(flyingFov, flyingModifier, flyingMin, flyingMax)).hideIf(staticFOV::on);
    }

    public static float playerModifier(float original) {
        ModuleFov m=instance;return m==null || !m.isEnabled()?original:m.field_6074()?1f:m.field_6075();
    }
    public static float fov(float original) {
        ModuleFov m=instance;if(m==null || !m.isEnabled())return original;
        float base=m.field_6255(),setting=mc().gameSettings.fovSetting;
        return setting<=0?base:original*(base/setting);
    }

    private boolean field_6074() {
        return staticFOV.on();
    }

    private float field_6255() {
        float f = mc().gameSettings.fovSetting;
        if (field_6074()) {
            f = defaultFov.intValue();
        } else {
            boolean matched = false;
            EntityPlayerSP player = mc().thePlayer;
            if (player != null) {
                if (player.isSprinting() && dynamicSprint.on()) {
                    f = sprintingFOV.intValue();
                    matched = true;
                }
                if (player.capabilities.isFlying && dynamicFlying.on()) {
                    f = flyingFov.intValue();
                    matched = true;
                }
                if (player.isPotionActive(Potion.moveSlowdown) && dynamicEffects.on()) {
                    f = slownessFOV.intValue();
                    matched = true;
                }
                if (player.isPotionActive(Potion.moveSpeed) && dynamicEffects.on()) {
                    int amplifier = player.getActivePotionEffect(Potion.moveSpeed).getAmplifier();
                    f = (amplifier == 0 ? speedFOV : speedTwoFOV).intValue();
                    matched = true;
                }
            }
            if (!matched) f = defaultFov.intValue();
        }
        if (field_6256 != f) {
            field_6256 = f;
            lastUpdate = -1L;
        }
        if (!staticFOV.on() && smoothFov.on() && field_6255 != field_6256) {
            long now = Minecraft.getSystemTime();
            long delta = lastUpdate == -1L ? 1L : Math.max(1L, now - lastUpdate);
            if (rewindReplayPaused()) delta = 0L;
            lastUpdate = now;
            field_6255 += (field_6256 - field_6255) * MathHelper.clamp_float(0.005f * (float)delta, 0.0f, 1.0f);
            if (Math.abs(field_6256 - field_6255) < 1.0E-4f) field_6255 = field_6256;
        } else {
            field_6255 = field_6256;
        }
        return field_6255;
    }

    private float field_6075() {
        EntityPlayerSP player = mc().thePlayer;
        if (player == null) return 1.0f;
        boolean flying = player.capabilities.isFlying;
        boolean sprinting = player.isSprinting();
        double speed = player.getEntityAttribute(SharedMonsterAttributes.movementSpeed).getAttributeValue();
        float walkSpeed = player.capabilities.getWalkSpeed();
        float f2 = 1.0f;
        if (flying && dynamicFlying.on()) {
            f2 *= 1.0f + MathHelper.clamp_float(0.1f * flyingModifier.value(), flyingMin.value(), flyingMax.value());
        }
        float f3 = (float)((speed / (double)walkSpeed + 1.0) / 2.0);
        float f4 = (float)speed;
        float f5 = Math.abs((f4 / (sprinting ? 1.3f : 1.0f) - walkSpeed) / (f4 - walkSpeed));
        double d = !dynamicEffects.on() ? 0.0
            : (double)MathHelper.clamp_float(f5 * (f3 - 1.0f) * movementModifier.value(), movementMin.value(), movementMax.value());
        if (sprinting && dynamicSprint.on()) {
            d += (double)MathHelper.clamp_float((1.0f - f5) * (f3 - 1.0f) * sprintModifier.value(), sprintMin.value(), sprintMax.value());
        }
        f2 = (float)((double)f2 * (1.0 + d));
        if (walkSpeed == 0.0f || Float.isNaN(f2) || Float.isInfinite(f2)) {
            f2 = 1.0f;
        }
        if (dynamicBow.on()) {
            ItemStack item = !player.isUsingItem() ? null : player.getItemInUse();
            if (item != null && item.getItem() == Items.bow) {
                float f6 = (float)player.getItemInUseDuration() / 20.0f;
                f6 = f6 > 1.0f ? 1.0f : (f6 *= f6);
                f2 *= 1.0f - MathHelper.clamp_float(f6 * 0.15f * aimingModifier.value(), aimingMin.value(), aimingMax.value());
            }
        }
        return f2;
    }

    private static boolean rewindReplayPaused() {
        Module rewind = ModuleManager.get("rewind");
        if (rewind == null) return false;
        return false;
    }
}
