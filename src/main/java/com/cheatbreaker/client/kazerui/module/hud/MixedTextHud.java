package com.cheatbreaker.client.kazerui.module.hud;
import com.cheatbreaker.client.kazerui.feature.HudAnchor;
import com.cheatbreaker.client.kazerui.module.Module;
import com.cheatbreaker.client.kazerui.module.Page;
import com.cheatbreaker.client.kazerui.module.setting.*;
import java.util.List;
/** Independent renderer for new HUDs; existing HUD fonts/layouts remain unchanged. */
public abstract class MixedTextHud extends HudElement {
    public final BoolSetting background,brackets,staticBackgroundWidth,staticBackgroundHeight,border,textShadow;
    public final NumberSetting backgroundWidth,backgroundHeight,borderThickness;
    public final ColorSetting textColor,bracketColor,backgroundColor,borderColor;
    private String line,measuredLine;private List<String> block,measuredBlock;
    private long epoch=Long.MIN_VALUE;private boolean measuredBrackets;private float contentWidth,contentHeight,step;
    private boolean bg,withBrackets,shadow;private float padding,edge;
    protected MixedTextHud(Module module,float x,float y,HudAnchor anchor,int[] sizes){
        super(module,x,y,anchor);background=module.bool("background",true);brackets=module.bool("brackets",true);
        staticBackgroundWidth=module.bool("staticBackgroundWidth",false);staticBackgroundHeight=module.bool("staticBackgroundHeight",false);
        border=module.bool("border",false);textShadow=module.bool("textShadow",true);
        backgroundWidth=module.integer("backgroundWidth",sizes[4],sizes[3],sizes[5]);backgroundHeight=module.integer("backgroundHeight",sizes[1],sizes[0],sizes[2]);
        borderThickness=module.decimal("borderThickness",.5f,.5f,3);
        textColor=module.color("textColor",0xFFFFFFFF);bracketColor=module.color("bracketColor",0xFFFFFFFF);
        backgroundColor=module.color("backgroundColor",0x6F000000);borderColor=module.color("borderColor",0x9F000000);
    }
    protected abstract String text(boolean preview);
    protected List<String> lines(boolean preview){return null;}
    public boolean visible(boolean preview){
        block=lines(preview);line=block==null?text(preview):null;
        if(block==null?(line==null || line.isEmpty()):block.isEmpty())return false;
        bg=background.on();withBrackets=!bg && brackets.on();padding=bg?4:0;edge=bg && border.on()?borderThickness.value():0;
        int c=backgroundColor.color(0);boolean pale=bg && ((c>>16&255)*.299f+(c>>8&255)*.587f+(c&255)*.114f)>160;
        shadow=textShadow.on() && !pale;
        long next=com.cheatbreaker.client.kazerui.KazerResourceEpoch.current()*2+(Draw.font().getUnicodeFlag()?1:0);
        if(next!=epoch || block!=measuredBlock || (line!=null && !line.equals(measuredLine)) || withBrackets!=measuredBrackets){
            contentWidth=0;int count=block==null?1:block.size();step=MixedHudText.height()+2;
            if(block==null)contentWidth=MixedHudText.width(line);else for(String row:block)contentWidth=Math.max(contentWidth,MixedHudText.width(row));
            if(withBrackets)contentWidth+=MixedHudText.width("[")+MixedHudText.width("]");
            contentHeight=count*step-2;epoch=next;measuredBlock=block;measuredLine=line;measuredBrackets=withBrackets;
        }
        float w=contentWidth+(shadow?1:0)+padding*2,h=contentHeight+(shadow?1:0)+padding*2;
        if(bg && staticBackgroundWidth.on())w=Math.max(w,backgroundWidth.value());
        if(bg && staticBackgroundHeight.on())h=Math.max(h,backgroundHeight.value());
        size(w+edge*2,h+edge*2);return true;
    }
    protected ColorSetting lineColor(){return textColor;}
    protected void drawLine(String row,float x,float y){
        if(withBrackets)x=MixedHudText.text("[",x,y,bracketColor.color(x+y),shadow);
        x=MixedHudText.text(row,x,y,lineColor().color(x+y),shadow);
        if(withBrackets)MixedHudText.text("]",x,y,bracketColor.color(x+y),shadow);
    }
    public void render(boolean preview){
        if(bg){Draw.fill(backgroundColor,edge,edge,width()-edge*2,height()-edge*2);if(edge>0)Draw.border(borderColor,edge,edge,width()-edge*2,height()-edge*2,edge);}
        float y=edge+padding;
        if(block==null)drawLine(line,edge+padding,y);else for(String row:block){drawLine(row,edge+padding,y);y+=step;}
    }
    public void layout(Page page){super.layout(page);page.add(textShadow,brackets);
        page.group(background,s->{s.add(staticBackgroundWidth,staticBackgroundHeight,backgroundWidth,backgroundHeight);s.group(border,g->g.add(borderThickness,borderColor));s.add(backgroundColor);});
        page.section("colorOptions",s->s.add(textColor,bracketColor));
    }
}
