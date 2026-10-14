package com.cheatbreaker.client.kazerui.module.modules.visual;
import net.minecraft.client.entity.AbstractClientPlayer;
import net.minecraft.client.model.ModelBiped;
import net.minecraft.client.model.ModelPlayer;
import net.minecraft.entity.player.EnumPlayerModelParts;
final class SkinParts {
    private static final EnumPlayerModelParts[] PARTS={EnumPlayerModelParts.HAT,EnumPlayerModelParts.LEFT_PANTS_LEG,EnumPlayerModelParts.RIGHT_PANTS_LEG,EnumPlayerModelParts.LEFT_SLEEVE,EnumPlayerModelParts.RIGHT_SLEEVE,EnumPlayerModelParts.JACKET};
    static boolean slim(AbstractClientPlayer player){return player.getSkinType().equals("slim");}
    static boolean shown(AbstractClientPlayer player,int part){return player.isWearing(PARTS[part]);}
    static void hide(ModelBiped model,int parts) {
        if((parts&1)!=0)model.bipedHeadwear.showModel=false;
        if(model instanceof ModelPlayer){ModelPlayer p=(ModelPlayer)model;
            if((parts&2)!=0)p.bipedLeftLegwear.showModel=false;if((parts&4)!=0)p.bipedRightLegwear.showModel=false;
            if((parts&8)!=0)p.bipedLeftArmwear.showModel=false;if((parts&16)!=0)p.bipedRightArmwear.showModel=false;
            if((parts&32)!=0)p.bipedBodyWear.showModel=false;
        }
    }
}
