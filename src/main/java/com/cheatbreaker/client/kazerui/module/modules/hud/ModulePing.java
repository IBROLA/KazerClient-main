package com.cheatbreaker.client.kazerui.module.modules.hud;
import com.cheatbreaker.client.kazerui.feature.HudAnchor;
import com.cheatbreaker.client.kazerui.event.UiTick;
import com.cheatbreaker.client.kazerui.module.Module;
import com.cheatbreaker.client.kazerui.module.Page;
import com.cheatbreaker.client.kazerui.module.hud.TextHud;
import com.cheatbreaker.client.kazerui.module.hud.MixedTextHud;
import com.cheatbreaker.client.kazerui.module.setting.*;
/** Samples the server's vanilla player-list latency outside HUD draws. */
public final class ModulePing extends Module {
    public enum Mode implements ChoiceSetting.Option { LATEST, AVERAGE;public String langId(){return name().toLowerCase(java.util.Locale.ROOT);} }
    private final NumberSetting interval=integer("updateIntervalSec",1,1,120),averageSamples=integer("averageSamples",3,2,20);
    private final ChoiceSetting<Mode> mode=choice("pingMode",Mode.LATEST);
    private final BoolSetting showMs=bool("pingShowMs",true),prefix=bool("showPingPrefix",true),dynamic=bool("dynamicPingColor",true);
    private final ColorSetting low=color("lowPingNumberColor",0xFF55FF55),medium=color("mediumPingNumberColor",0xFFFFFF55),high=color("highPingNumberColor",0xFFFF5555);
    private final int[] samples=new int[20];private int cursor,count,ping=-1;private long nextSample;private String line="Ping: ...";
    public ModulePing(){super("PING",false);hud(new Hud());}
    protected void layout(Page page){page.add(interval,mode,averageSamples,prefix,showMs,dynamic);page.section("colorOptions",s->s.add(low,medium,high).hideIf(()->!dynamic.on()));}
    public void tick(UiTick.ClientTickEvent event) {
        if(event.phase!=UiTick.Phase.END || !isEnabled())return;
        long now=System.currentTimeMillis();if(now<nextSample)return;nextSample=now+interval.intValue()*1000L;
        int measured=PlayerLatency.read();
        if(measured<0){count=cursor=0;ping=-1;}else {
            samples[cursor++%samples.length]=measured;count=Math.min(count+1,samples.length);ping=measured;
            if(mode.get()==Mode.AVERAGE){int n=Math.min(count,averageSamples.intValue()),sum=0;for(int i=0;i<n;i++)sum+=samples[(cursor-1-i+samples.length)%samples.length];ping=sum/n;}
        }
        line=(prefix.on()?"Ping: ":"")+(ping<0?"...":Integer.toString(ping))+(showMs.on()?" ms":"");
    }
    private final class Hud extends MixedTextHud {
        Hud(){super(ModulePing.this,0,40,HudAnchor.TOP_RIGHT,TextHud.sizes(10,18,32,40,72,160));}
        protected String text(boolean preview){return preview && ping<0?"Ping: 42 ms":mc().theWorld==null?"":line;}
        protected ColorSetting lineColor(){return !dynamic.on()?textColor:ping<65?low:ping<120?medium:high;}
    }
}
