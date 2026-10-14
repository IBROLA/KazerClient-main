package com.cheatbreaker.client.kazerui.module.modules.hud;
import net.minecraft.client.Minecraft;
/** Native renderer counters sampled on ticks, without reflective access in HUD draws. */
final class NativeHudData {
    private static int visible,total;
    static int visibleChunks(){return visible;}
    static int totalChunks(){return total;}
    static void updateChunks() {
        Minecraft mc=Minecraft.getMinecraft();
        if(mc.theWorld==null||mc.thePlayer==null||mc.renderGlobal==null){visible=total=0;return;}
        String text=mc.renderGlobal.getDebugInfoRenders();
        int slash=text.indexOf('/'),start=text.indexOf(':')+1;
        if(slash<start){visible=total=0;return;}
        visible=number(text,start,slash);total=number(text,slash+1,text.length());
    }
    private static int number(String text,int start,int end) {
        int result=0;boolean begun=false;
        for(int i=start;i<end;i++) {char c=text.charAt(i);if(c>='0'&&c<='9'){begun=true;result=result*10+c-'0';}else if(begun)break;}
        return result;
    }
}
