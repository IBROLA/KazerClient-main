package com.cheatbreaker.client.kazerui.module.modules.hud;
import com.cheatbreaker.client.kazerui.feature.HudAnchor;
import com.cheatbreaker.client.kazerui.module.Module;
import com.cheatbreaker.client.kazerui.module.Page;
import com.cheatbreaker.client.kazerui.module.hud.TextHud;
import com.cheatbreaker.client.kazerui.module.hud.MixedTextHud;
import com.cheatbreaker.client.kazerui.module.setting.BoolSetting;
import com.cheatbreaker.client.kazerui.event.UiTick;
import net.minecraft.entity.Entity;
import java.util.*;
/** Passive client observations; attack dispatch and server state are untouched. */
public final class ModulePvpInfo extends Module {
    private static ModulePvpInfo instance;
    private static final List<String> PREVIEW=Arrays.asList("Attacks: 12","Observed Hits: 9","Hits Taken: 3","Longest Combo: 4","Health Lost: 8.0","Health Recovered: 4.0");
    private final BoolSetting resetWorld=bool("resetOnWorldChange",false),melee=bool("meleeStatistics",true),health=bool("healthStatistics",true);
    private int attacks,hitsTaken,observedHits,combo,longest,target=-1;private long attackAt;private float lost,recovered,lastHealth=Float.NaN;private Object world;
    private List<String> lines=Collections.emptyList();private boolean dirty=true;
    public ModulePvpInfo(){super("PVP_INFO",false);instance=this;melee.onChange(()->dirty=true);health.onChange(()->dirty=true);hud(new MixedTextHud(this,0,70,HudAnchor.TOP_LEFT,TextHud.sizes(10,18,32,40,140,250)) {
        protected String text(boolean preview){return "";}
        protected List<String> lines(boolean preview){return preview?PREVIEW:ModulePvpInfo.this.lines;}
    });}
    protected void layout(Page page){page.add(resetWorld,melee,health);}
    public static void attack(Entity entity){ModulePvpInfo m=instance;if(m==null || !m.isEnabled() || entity==null)return;m.attacks++;m.target=entity.getEntityId();m.attackAt=System.currentTimeMillis();m.dirty=true;}
    public static void status(Entity entity,byte status){ModulePvpInfo m=instance;if(m==null || !m.isEnabled() || status!=2 || entity==null)return;
        if(entity==mc().thePlayer){m.hitsTaken++;m.combo=0;m.dirty=true;}
        else if(entity.getEntityId()==m.target && System.currentTimeMillis()-m.attackAt<500){m.target=-1;m.observedHits++;m.combo++;m.longest=Math.max(m.longest,m.combo);m.dirty=true;}
    }
    public void tick(UiTick.ClientTickEvent event){if(event.phase!=UiTick.Phase.END || !isEnabled())return;
        if(world!=mc().theWorld){world=mc().theWorld;lastHealth=Float.NaN;target=-1;combo=0;if(resetWorld.on()){attacks=hitsTaken=observedHits=longest=0;lost=recovered=0;}dirty=true;}
        if(mc().thePlayer==null){lines=Collections.emptyList();return;}
        float now=mc().thePlayer.getHealth();if(!Float.isNaN(lastHealth) && now!=lastHealth){if(now<lastHealth)lost+=lastHealth-now;else recovered+=now-lastHealth;dirty=true;}lastHealth=now;
        if(System.currentTimeMillis()-attackAt>1500)combo=0;
        if(dirty){List<String> next=new ArrayList<String>(6);if(melee.on()){next.add("Attacks: "+attacks);next.add("Observed Hits: "+observedHits);next.add("Hits Taken: "+hitsTaken);next.add("Longest Combo: "+longest);}if(health.on()){next.add(String.format(Locale.ROOT,"Health Lost: %.1f",lost));next.add(String.format(Locale.ROOT,"Health Recovered: %.1f",recovered));}lines=Collections.unmodifiableList(next);dirty=false;}
    }
}
