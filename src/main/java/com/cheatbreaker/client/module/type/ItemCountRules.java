package com.cheatbreaker.client.module.type;

import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemPotion;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionEffect;

import java.util.List;

public final class ItemCountRules {
    private ItemCountRules() {
    }

    public static int count(ItemCountType type, ItemStack[] stacks) {
        int total = 0;
        if (stacks == null) {
            return 0;
        }
        for (ItemStack stack : stacks) {
            if (matches(type, stack)) {
                total += stack.stackSize;
            }
        }
        return total;
    }

    public static boolean matches(ItemCountType type, ItemStack stack) {
        if (stack == null || stack.getItem() == null) {
            return false;
        }
        switch (type) {
            case HEALING_POT:
                return isSplashHealingPotion(stack);
            case MUSHROOM_SOUP:
                return stack.getItem() == Items.mushroom_stew;
            case ARROW:
                return stack.getItem() == Items.arrow;
            case GOLDEN_APPLE:
                return stack.getItem() == Items.golden_apple && stack.getItemDamage() == 0;
            case ENCHANTED_GOLDEN_APPLE:
                return stack.getItem() == Items.golden_apple && stack.getItemDamage() == 1;
            default:
                return false;
        }
    }

    private static boolean isSplashHealingPotion(ItemStack stack) {
        if (stack.getItem() != Items.potionitem
                || !ItemPotion.isSplash(stack.getItemDamage())) {
            return false;
        }
        List effects = Items.potionitem.getEffects(stack);
        if (effects == null || effects.isEmpty()) {
            return false;
        }
        for (Object value : effects) {
            PotionEffect effect = (PotionEffect) value;
            if (effect.getPotionID() != Potion.heal.id
                    || effect.getAmplifier() < 0 || effect.getAmplifier() > 1) {
                return false;
            }
        }
        return true;
    }
}
