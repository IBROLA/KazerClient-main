package com.cheatbreaker.client.kazerui.module.modules.hud;
import com.cheatbreaker.client.kazerui.feature.HudAnchor;
import com.cheatbreaker.client.kazerui.module.Module;
import com.cheatbreaker.client.kazerui.module.Page;
import com.cheatbreaker.client.kazerui.module.hud.*;
import com.cheatbreaker.client.kazerui.module.setting.*;
import com.cheatbreaker.client.kazerui.module.render.TitleAccess;
import net.minecraft.client.renderer.GlStateManager;
public final class ModuleTitles extends Module {
    private static ModuleTitles instance;
    private final BoolSetting preview=bool("showInHudEditor",false),showTitle=bool("showTitle",true),showSubtitle=bool("showSubtitle",true);
    private final NumberSetting titleScale=decimal("titleScale",4,1,6),subtitleScale=decimal("subtitleScale",2,1,4);
    private final TextSetting sampleTitle=add(new TextSetting("title","Title")),sampleSubtitle=add(new TextSetting("subtitle","Subtitle"));
    public ModuleTitles(){super("TITLES",true);instance=this;hud(new Hud());}
    protected void layout(Page page){page.group(preview,s->s.add(sampleTitle,sampleSubtitle));page.add(showTitle,showSubtitle,titleScale,subtitleScale);}
    public static int vanillaTimer(int value){return instance!=null && instance.isEnabled()?0:value;}
    public static boolean show(boolean title){return instance==null || !instance.isEnabled() || (title?instance.showTitle.on():instance.showSubtitle.on());}
    public static float multiplier(boolean title){return instance==null || !instance.isEnabled()?1:(title?instance.titleScale.value()/4f:instance.subtitleScale.value()/2f);}
    private final class Hud extends HudElement {
        private String title="",subtitle="";private int timer;private long timerAt;private float titleWidth,subWidth,titleHeight,subHeight;private int alpha;
        Hud(){super(ModuleTitles.this,0,0,HudAnchor.MIDDLE_CENTER,.5f,1.5f);}
        public boolean editable(){return preview.on();}
        public boolean visible(boolean editing){
            if(editing){if(!preview.on())return false;title=sampleTitle.get();subtitle=sampleSubtitle.get();alpha=255;}
            else {
                if(!(mc().ingameGUI instanceof TitleAccess))return false;TitleAccess access=(TitleAccess)mc().ingameGUI;int remaining=access.kazerTitleTime();if(remaining<=0)return false;
                if(timer!=remaining){timer=remaining;timerAt=System.nanoTime();}float age=remaining-Math.min(1f,(System.nanoTime()-timerAt)/50000000f);int fadeIn=access.kazerFadeIn(),stay=access.kazerStay(),out=access.kazerFadeOut();
                alpha=255;if(remaining>out+stay && fadeIn>0)alpha=(int)((fadeIn+stay+out-age)*255f/fadeIn);if(remaining<=out && out>0)alpha=(int)(age*255f/out);alpha=Math.max(0,Math.min(255,alpha));
                title=access.kazerTitle();subtitle=access.kazerSubtitle();
            }
            if(title==null)title="";if(subtitle==null)subtitle="";
            titleWidth=showTitle.on()?(MixedHudText.width(title)+1)*titleScale.value():0;subWidth=showSubtitle.on()?(MixedHudText.width(subtitle)+1)*subtitleScale.value():0;
            titleHeight=showTitle.on() && !title.isEmpty()?(MixedHudText.height()+1)*titleScale.value():0;subHeight=showSubtitle.on() && !subtitle.isEmpty()?(MixedHudText.height()+1)*subtitleScale.value():0;
            size(Math.max(titleWidth,subWidth),titleHeight+subHeight+(titleHeight>0 && subHeight>0?8:0));return width()>0 && height()>0 && alpha>3;
        }
        private void line(String text,float scale,float y){GlStateManager.pushMatrix();try{GlStateManager.translate(width()/2,y,0);GlStateManager.scale(scale,scale,1);MixedHudText.text(text,-(MixedHudText.width(text)+1)/2f,0,alpha<<24|0xFFFFFF,true);}finally{GlStateManager.popMatrix();}}
        public void render(boolean editing){if(titleHeight>0)line(title,titleScale.value(),0);if(subHeight>0)line(subtitle,subtitleScale.value(),titleHeight+(titleHeight>0?8:0));}
    }
}
