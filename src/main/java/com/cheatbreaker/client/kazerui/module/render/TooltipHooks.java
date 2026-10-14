package com.cheatbreaker.client.kazerui.module.render;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.item.ItemStack;
import com.cheatbreaker.client.kazerui.module.ModuleManager;
import com.cheatbreaker.client.kazerui.module.modules.hud.ModuleSaturation;

/** Donor tooltip components, with native hosts and bounded per-tooltip storage. */
public final class TooltipHooks {
    public interface Component { void draw(int x,int y); }
    private static final List<Component> COMPONENTS=new ArrayList<Component>();
    private static final char BASE='\uE000';
    private TooltipHooks() { }
    public static void food(ItemStack stack,List<String> lines) {
        COMPONENTS.clear();
        com.cheatbreaker.client.kazerui.module.Module module=ModuleManager.get("saturation");
        if(module instanceof ModuleSaturation)((ModuleSaturation)module).tooltip(stack,lines);
    }
    public static void insert(List<String> lines,int index,int width,int height,Component component) {
        int id=COMPONENTS.size();if(id>=16)return;COMPONENTS.add(component);
        StringBuilder blank=new StringBuilder();for(int i=0;i<(width+3)/4;i++)blank.append(' ');
        int rows=Math.max(1,(height+9)/10);index=Math.min(index,lines.size());
        lines.add(index,blank+"\u00a7"+(char)(BASE+id));
        for(int i=1;i<rows;i++)lines.add(index+i,blank.toString());
    }
    public static int drawLine(Object renderer,String text,float x,float y,int color) {
        int at=text.lastIndexOf('\u00a7');
        if(at>=0 && at+1<text.length()) {
            int id=text.charAt(at+1)-BASE;
            if(id>=0 && id<COMPONENTS.size()) {COMPONENTS.get(id).draw((int)x,(int)y);return (int)x;}
        }
        return ((FontRenderer)renderer).drawStringWithShadow(text,x,y,color);
    }
}
