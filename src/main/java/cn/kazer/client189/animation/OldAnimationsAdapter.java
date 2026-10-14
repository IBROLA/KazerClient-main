/* SPDX-License-Identifier: LGPL-3.0-only
 * Adapted from Sk1erLLC/OldAnimations (AnimationHandler, SneakHandler and renderer mixins),
 * commit ab10fa4879bf54a899bba777d7a0bb70c946e4fb.
 * Modifications: Kazer settings/lifecycle and ASM bridge instead of Essential/Mixin.
 * See META-INF/licenses/OldAnimations-NOTICE.md for license and corresponding source.
 */
package cn.kazer.client189.animation;

import static cn.kazer.client189.animation.AnimationPreferences.*;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.ItemRenderer;
import net.minecraft.client.renderer.block.model.ItemCameraTransforms;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.*;
import net.minecraft.potion.Potion;
import net.minecraft.util.MathHelper;
import net.minecraft.util.MovingObjectPosition;

public final class OldAnimationsAdapter {
    private static final OldSwingState SWING=new OldSwingState();
    private static EntityPlayer lastPlayer;
    private static float eye=1.62F, lastEye=1.62F, cameraPartial;
    private OldAnimationsAdapter() { }
    public static void reset() { SWING.reset(); lastPlayer=null; eye=lastEye=1.62F; }
    public static void tick() {
        Minecraft mc=Minecraft.getMinecraft();
        if(mc.thePlayer!=lastPlayer) { reset(); lastPlayer=mc.thePlayer; }
        if(mc.thePlayer==null || !active(ENABLED)) { reset(); return; }
        boolean punch=mc.currentScreen==null && active(PUNCH) && mc.gameSettings.keyBindAttack.isKeyDown()
                && mc.objectMouseOver!=null && mc.objectMouseOver.typeOfHit==MovingObjectPosition.MovingObjectType.BLOCK;
        int duration=mc.thePlayer.isPotionActive(Potion.digSpeed)
                ?5-mc.thePlayer.getActivePotionEffect(Potion.digSpeed).getAmplifier()
                :(mc.thePlayer.isPotionActive(Potion.digSlowdown)
                ?8+mc.thePlayer.getActivePotionEffect(Potion.digSlowdown).getAmplifier()*2:6);
        SWING.tick(punch,duration);
        lastEye=eye;
        if(mc.thePlayer.isSneaking()) eye=1.54F;
        else if(!active(LONG)) eye=1.62F;
        else if(eye<1.62F) { float delta=1.62F-eye; delta*=.4; eye=1.62F-delta; }
    }
    public static float useSwing(float nativeSwing,float partial) {
        Minecraft mc=Minecraft.getMinecraft();
        return active(BLOCK_HIT) && mc.thePlayer!=null
                ?SWING.interpolate(partial,mc.thePlayer.getSwingProgress(partial)):nativeSwing;
    }
    public static boolean skipNoneTransform() {
        return active(MODEL);
    }
    /** Only the first-person branch predicate, never the real drinking/bow timer. */
    public static int renderedUseCount(Object player,ItemStack held) {
        EntityPlayer p=(EntityPlayer)player; int count=p.getItemInUseCount();

        if(!active(ITEMS)) return count;
        boolean override=false;
        if(held!=null && active(PUNCH) && count<=0 && Minecraft.getMinecraft().gameSettings.keyBindUseItem.isKeyDown()) {
            EnumAction action=held.getItemUseAction(); Item item=held.getItem();
            boolean consume=false;
            if(item instanceof ItemFood && p.canEat(alwaysEdible((ItemFood)item)))
                consume=action==EnumAction.EAT || action==EnumAction.DRINK;
            override=action==EnumAction.BLOCK || consume;
        }
        return ((count>0||override)&&p.isUsingItem())?1:0;
    }
    private static boolean alwaysEdible(ItemFood item) {
        try { return FoodField.VALUE.getBoolean(item); }
        catch(IllegalAccessException e) { throw new IllegalStateException(e); }
    }
    private static final class FoodField {
        static final java.lang.reflect.Field VALUE=net.minecraftforge.fml.relauncher.ReflectionHelper.findField(
                ItemFood.class,"alwaysEdible","field_77852_bZ");
    }
    public static void beforeRightClick() {
        Minecraft mc=Minecraft.getMinecraft();
        if(!active(PUNCH)||mc.thePlayer==null||mc.playerController==null) return;
        ItemStack held=mc.thePlayer.getHeldItem();
        if(mc.playerController.getIsHittingBlock() && held!=null
                && (held.getItemUseAction()!=EnumAction.NONE || held.getItem() instanceof ItemBlock))
            mc.playerController.resetBlockRemoving();
    }
    /** Native item use forbids block removal; never manufacture use from a mouse button. */
    public static boolean cancelBlockClick() {
        Minecraft mc=Minecraft.getMinecraft();
        return active(PUNCH) && mc.thePlayer!=null && mc.thePlayer.isUsingItem()
                && mc.currentScreen==null && mc.objectMouseOver!=null
                && mc.objectMouseOver.typeOfHit==MovingObjectPosition.MovingObjectType.BLOCK;
    }
    public static float equipClamp(float original,float min,float max,ItemStack stored,int slot,float equipped) {
        Minecraft mc=Minecraft.getMinecraft();
        if(active(SWITCH) && mc.thePlayer!=null && slot==mc.thePlayer.inventory.currentItem
                && ItemStack.areItemsEqual(stored,mc.thePlayer.inventory.getCurrentItem())) original=1F-equipped;
        return MathHelper.clamp_float(original,min,max);
    }
    public static void bowScale(float x,float y,float z) {
        boolean old=active(BOW);
        if(old) { GlStateManager.rotate(-335,0,0,1); GlStateManager.rotate(-50,0,1,0); GlStateManager.translate(0,.5F,0); }
        GlStateManager.scale(x,y,z);
        if(old) { GlStateManager.translate(0,-.5F,0); GlStateManager.rotate(50,0,1,0); GlStateManager.rotate(335,0,0,1); }
    }
    public static void renderFirstPerson(Object renderer,EntityLivingBase entity,ItemStack stack,ItemCameraTransforms.TransformType transform) {

        if(stack!=null && active(ITEMS)) {
            if(stack.getItem().shouldRotateAroundWhenRendering() && (active(MODEL)||active(ROD)))
                GlStateManager.rotate(180,0,1,0);
            transform=firstPersonTransform(stack)?ItemCameraTransforms.TransformType.FIRST_PERSON:ItemCameraTransforms.TransformType.NONE;
        }
        ((ItemRenderer)renderer).renderItem(entity,stack,transform);
    }
    private static boolean firstPersonTransform(ItemStack stack) {

        switch(stack.getItemUseAction()) {
            case BOW: if(!active(BOW)) return true; break;
            case EAT: case DRINK: if(!active(CONSUME)) return true; break;
            case BLOCK: if(!active(SWORD)) return true; break;
            case NONE:
                if(stack.getItem() instanceof ItemFishingRod && active(ROD)) { /* original transform */ }
                else if(!active(MODEL)) return true;
        }
        GlStateManager.translate(.58800083F,.36999986F,-.77000016F);
        GlStateManager.translate(0,-.3F,0);
        GlStateManager.scale(1.5F,1.5F,1.5F);
        GlStateManager.rotate(50,0,1,0);
        GlStateManager.rotate(335,0,0,1);
        GlStateManager.translate(-.9375F,-.0625F,0);
        GlStateManager.scale(-2,2,-2);
        if(Minecraft.getMinecraft().getRenderItem().shouldRenderItemIn3D(stack)) {
            GlStateManager.scale(.58823526F,.58823526F,.58823526F);
            GlStateManager.rotate(-25,0,0,1);
            GlStateManager.rotate(0,1,0,0);
            GlStateManager.rotate(135,0,1,0);
            GlStateManager.translate(0,-.25F,-.125F);
            GlStateManager.scale(.5F,.5F,.5F); return true;
        }
        GlStateManager.scale(.5F,.5F,.5F); return false;
    }
    public static float blockingArmYaw(float nativeYaw) {
        return active(THIRD)?0:nativeYaw;
    }
    public static void thirdPersonPose(Object entity,ItemCameraTransforms.TransformType transform) {

        if(!active(THIRD) || transform!=ItemCameraTransforms.TransformType.THIRD_PERSON || !(entity instanceof EntityPlayer)) return;
        EntityPlayer p=(EntityPlayer)entity; ItemStack held=p.getHeldItem();
        if(held!=null && p.getItemInUseCount()>0 && held.getItemUseAction()==EnumAction.BLOCK) {
            GlStateManager.translate(-.15F,-.2F,0); GlStateManager.rotate(70,1,0,0); GlStateManager.translate(.119F,.2F,-.024F);
        }
    }
    public static boolean redArmor() { return active(RED); }
    public static boolean healthFlash(boolean nativeFlash) { return nativeFlash && !(active(HEALTH)); }
    public static void cameraPartial(float partial) { cameraPartial=partial; }
    public static float cameraEye(Object entity,float partial) {
        Entity e=(Entity)entity;
        if(e!=Minecraft.getMinecraft().thePlayer || !active(ANIMATIONS)) return e.getEyeHeight();
        return active(SMOOTH)?lastEye+(eye-lastEye)*partial:eye;
    }
    public static float directionEye(Object entity) { return cameraEye(entity,cameraPartial); }
}
