package com.cheatbreaker.client.kazerui.module.hud;
/** Gameplay HUDs use Minecraft's resource-pack-aware font and matching metrics. */
public final class MixedHudText {
    public static int width(String text){return (int)Draw.width(text);}
    public static int height(){return Draw.fontHeight();}
    public static float text(String text,float x,float y,int color,boolean shadow){
        return Draw.text(text,x,y,color,shadow);
    }
    private MixedHudText(){}
}
