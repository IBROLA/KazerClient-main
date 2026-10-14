package com.cheatbreaker.client.kazerui.module.modules.hud;

import com.cheatbreaker.client.kazerui.feature.HudAnchor;
import com.cheatbreaker.client.kazerui.module.Module;
import com.cheatbreaker.client.kazerui.module.Page;
import com.cheatbreaker.client.kazerui.module.hud.TextHud;
import com.cheatbreaker.client.kazerui.module.setting.ChoiceSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.init.Items;
import net.minecraft.item.ItemPotion;
import net.minecraft.item.ItemStack;
import net.minecraft.potion.PotionEffect;

import com.cheatbreaker.client.kazerui.event.UiTick;

public final class ModulePotionCounter extends Module {
    public enum PotionCounterType implements ChoiceSetting.Option {
        POTION("potion"), SOUP("soup");
        private final String id;
        PotionCounterType(String id) { this.id = id; }
        @Override public String langId() { return id; }
    }

    private final ChoiceSetting<PotionCounterType> potionCounter = choice("potionCounter", PotionCounterType.POTION);

    private int count;

    public ModulePotionCounter() {
        super("POTION_COUNTER", false);
        hud(new Hud());
    }

    @Override protected void layout(Page page) {
        page.section("generalOptions", s -> s.add(potionCounter));
    }

    
    public void tick(UiTick.ClientTickEvent event) {
        if (event.phase != UiTick.Phase.END || !isEnabled()) return;
        count = count();
    }

    private int count() {
        EntityPlayerSP player = Minecraft.getMinecraft().thePlayer;
        if (player == null) return 0;
        InventoryPlayer inventory = player.inventory;
        int total = 0;
        for (ItemStack stack : inventory.mainInventory) total += stackSize(stack);

        total += stackSize(inventory.getItemStack());
        return total;
    }

    private int stackSize(ItemStack stack) {
        if (stack == null || stack.getItem() == null) return 0;
        if (potionCounter.is(PotionCounterType.SOUP)) {
            return stack.getItem() == Items.mushroom_stew ? stack.stackSize : 0;
        }
        if (!(stack.getItem() instanceof ItemPotion) || !ItemPotion.isSplash(stack.getMetadata())) return 0;
        java.util.List<PotionEffect> effects=(java.util.List<PotionEffect>)((ItemPotion)stack.getItem()).getEffects(stack);
        if(effects==null)return 0;
        for (PotionEffect effect : effects) {
            if (effect.getPotionID() == 6) return stack.stackSize;
        }
        return 0;
    }

    private final class Hud extends TextHud {
        private int shownCount=Integer.MIN_VALUE;
        private boolean shownSoup;
        private String shownText;
        Hud() { super(ModulePotionCounter.this, 0, 62, HudAnchor.TOP_CENTER, sizes(10, 18, 22, 40, 56, 62)); }

        @Override protected String text(boolean preview) {
            int n = Minecraft.getMinecraft().thePlayer == null ? 0 : ModulePotionCounter.this.count;
            boolean soup=potionCounter.is(PotionCounterType.SOUP);
            if(shownCount!=n || shownSoup!=soup){
                shownCount=n;shownSoup=soup;String type=soup?"soup":"pot";
                shownText=n+" "+(n==1?type:type+"s");
            }
            return shownText;
        }
    }
}
