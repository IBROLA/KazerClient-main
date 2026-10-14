package com.cheatbreaker.client.kazerui.module.modules.mechanic;
import net.minecraft.client.Minecraft;
import net.minecraft.client.network.NetworkPlayerInfo;
import net.minecraft.util.ResourceLocation;
/** Version-native lookup. Skin requests remain asynchronous in Minecraft's skin manager. */
final class ChatPlayers {
    static boolean known(String name){return Minecraft.getMinecraft().getNetHandler()!=null&&Minecraft.getMinecraft().getNetHandler().getPlayerInfo(name)!=null;}
    static ResourceLocation skin(String name){
        if(Minecraft.getMinecraft().getNetHandler()==null)return null;
        NetworkPlayerInfo info=Minecraft.getMinecraft().getNetHandler().getPlayerInfo(name);
        return info==null?null:info.getLocationSkin();
    }
}
