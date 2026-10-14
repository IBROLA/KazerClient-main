package com.cheatbreaker.client.kazerui.module.modules.hud;
import net.minecraft.client.Minecraft;
import net.minecraft.client.network.NetworkPlayerInfo;
final class PlayerLatency {
    static int read() {
        Minecraft mc=Minecraft.getMinecraft();
        if(mc.thePlayer==null || mc.getNetHandler()==null)return -1;
        NetworkPlayerInfo info=mc.getNetHandler().getPlayerInfo(mc.thePlayer.getUniqueID());
        return info==null?-1:info.getResponseTime();
    }
    private PlayerLatency(){}
}
