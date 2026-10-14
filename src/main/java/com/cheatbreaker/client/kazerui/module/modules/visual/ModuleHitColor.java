package com.cheatbreaker.client.kazerui.module.modules.visual;
import com.cheatbreaker.client.kazerui.module.Module;
import com.cheatbreaker.client.kazerui.module.Page;
import com.cheatbreaker.client.kazerui.module.setting.*;
public final class ModuleHitColor extends Module {
    private static ModuleHitColor instance;
    private final ColorSetting color=color("hitArmorColor",0x66990000);
    private final BoolSetting armor=bool("shouldColorArmor",true);
    public ModuleHitColor(){super("HIT_COLOR",false);instance=this;}
    protected void layout(Page page){page.add(color,armor);}
    private static boolean active(){return instance!=null && instance.isEnabled();}
    private static float component(float original,int shift){return active()?((instance.color.color(0)>>>shift)&255)/255f:original;}
    public static float red(float value){return component(value,16);}public static float green(float value){return component(value,8);}
    public static float blue(float value){return component(value,0);}public static float alpha(float value){return component(value,24);}
    public static boolean armorEnabled(){return !active() || instance.armor.on();}
    public static boolean combineArmor(boolean original){return active()?instance.armor.on():original;}
}
