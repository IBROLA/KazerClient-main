package com.cheatbreaker.client.kazerui.cosmetics.render;

import com.cheatbreaker.client.CheatBreaker;
import com.cheatbreaker.client.kazerui.cosmetics.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.ModelBiped;
import net.minecraft.client.entity.AbstractClientPlayer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.util.MathHelper;
import net.minecraft.util.ResourceLocation;
import java.util.List;

/** Shared owned cloak physics and model adapters. Caller retains native layer GL state. */
public final class CosmeticRender {
    private static final WingModel WINGS=new WingModel();
    public static List<Cosmetic> worn(AbstractClientPlayer player) {
        Minecraft mc=Minecraft.getMinecraft();
        if(player instanceof Mannequin)return ((Mannequin)player).cosmetics();
        if(player==mc.thePlayer)return Loadout.equipped();
        // Apollo targets synthetic NPC UUIDs only. Ordinary player rendering
        // skips this lookup and keeps the existing presence cache path.
        if(player.getUniqueID().getLeastSignificantBits()==0) {
            List<Cosmetic> npc=CheatBreaker.getInstance().getNetHandler().getApollo().cosmetics(player.getUniqueID());
            if(npc!=null)return npc;
        }
        return CheatBreaker.getInstance().getPresenceClient().cosmetics(player.getUniqueID());
    }
    public static ResourceLocation cape(AbstractClientPlayer player) {
        if(player.isInvisible())return null;
        if(!(player instanceof Mannequin)&&!visible(CosmeticCategory.CLOAKS))return null;
        for(Cosmetic item:worn(player))if(item.type==CosmeticType.CLOAK)return CosmeticTextures.get(item);
        return null;
    }
    public static void accessories(AbstractClientPlayer player,ModelBiped model) {
        accessories(player,model,0);
    }
    public static void accessories(AbstractClientPlayer player,ModelBiped model,float partialTicks) {
        if(player.isInvisible())return;
        com.cheatbreaker.client.kazerui.module.modules.visual.Module3dSkins.render(player,model);
        List<Cosmetic> items=worn(player);if(items.isEmpty())return;
        for(Cosmetic item:items) {
            if(item.type==CosmeticType.CLOAK)continue;
            if(!(player instanceof Mannequin)&&!visible(CosmeticCategory.of(item.type)))continue;
            if(item.geckolib) {
                GeckoCosmetics.render(player,model,item,partialTicks);
            }else if(item.type==CosmeticType.WINGS) {
                ResourceLocation texture=CosmeticTextures.get(item);if(texture==null)continue;
                GlStateManager.pushMatrix();
                try { model.bipedBody.postRender(.0625F);if(player.isSneaking())GlStateManager.translate(0,.2F,0);WINGS.render(.13F,.0625F,texture); }
                finally { GlStateManager.popMatrix(); }
            }else ObjCosmetics.render(player,model,item);
        }
    }
    private static boolean visible(CosmeticCategory category) {
        return com.cheatbreaker.client.kazerui.module.NativeGlobalSettings.cosmeticVisible(category);
    }
    /** Copied from the owned donor's cloak layer; movement interpolation remains native. */
    public static void cloak(AbstractClientPlayer p,float partialTicks,Runnable draw) {
        GlStateManager.color(1,1,1,1);GlStateManager.pushMatrix();
        try {
            GlStateManager.translate(0,0,.125F);
            double dx=p.prevChasingPosX+(p.chasingPosX-p.prevChasingPosX)*partialTicks-(p.prevPosX+(p.posX-p.prevPosX)*partialTicks);
            double dy=p.prevChasingPosY+(p.chasingPosY-p.prevChasingPosY)*partialTicks-(p.prevPosY+(p.posY-p.prevPosY)*partialTicks);
            double dz=p.prevChasingPosZ+(p.chasingPosZ-p.prevChasingPosZ)*partialTicks-(p.prevPosZ+(p.posZ-p.prevPosZ)*partialTicks);
            float yaw=p.prevRenderYawOffset+(p.renderYawOffset-p.prevRenderYawOffset)*partialTicks;
            double sin=MathHelper.sin(yaw*(float)Math.PI/180F),cos=-MathHelper.cos(yaw*(float)Math.PI/180F);
            float lift=MathHelper.clamp_float((float)dy*10F,-6F,32F),forward=Math.max(0,(float)(dx*sin+dz*cos)*100F),side=(float)(dx*cos-dz*sin)*100F;
            float bob=p.prevCameraYaw+(p.cameraYaw-p.prevCameraYaw)*partialTicks;
            lift+=MathHelper.sin((p.prevDistanceWalkedModified+(p.distanceWalkedModified-p.prevDistanceWalkedModified)*partialTicks)*6F)*32F*bob;
            if(p.isSneaking())lift+=25F;
            GlStateManager.rotate(6F+forward/2F+lift,1,0,0);GlStateManager.rotate(side/2F,0,0,1);
            GlStateManager.rotate(-side/2F,0,1,0);GlStateManager.rotate(180,0,1,0);draw.run();
        }finally { GlStateManager.popMatrix(); }
    }
    private CosmeticRender() {}
}
