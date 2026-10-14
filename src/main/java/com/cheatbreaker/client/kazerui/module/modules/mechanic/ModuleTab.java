package com.cheatbreaker.client.kazerui.module.modules.mechanic;
import com.cheatbreaker.client.kazerui.module.Module;
import com.cheatbreaker.client.kazerui.module.Page;
import com.cheatbreaker.client.kazerui.module.setting.*;
import com.cheatbreaker.client.kazerui.event.UiTick;
import com.cheatbreaker.client.kazerui.module.hud.MixedHudText;
import net.minecraft.util.IChatComponent;
import net.minecraft.client.renderer.GlStateManager;
public final class ModuleTab extends Module {
    private static ModuleTab instance;
    private final BoolSetting logo=bool("tabLogo",true);
    private final BoolSetting toggle=bool("toggleKeyTab",false),highlight=bool("highlightOwnName",false),shadow=bool("nameShadow",true),hidePing=bool("hidePing",false),numbers=bool("displayPingAsNumber",false),dynamic=bool("dynamicPingColor",false),noHeader=bool("disableHeader",false),noFooter=bool("disableFooter",false);
    private final KeySetting key=keybind("tabKeybind",nativeKeyName());
    private final ColorSetting background=color("backgroundColor",0x80000000),rows=color("rowsColor",0x20FFFFFF),own=color("nameColor",0xFFFF5555),pingColor=color("pingNumberColor",0xFFFFFF55);
    private static final String[] PINGS=new String[10000];
    private boolean held,active,customKey;private long keyRevision=Long.MIN_VALUE;
    private static String nativeKeyName(){net.minecraft.client.Minecraft client=mc();if(client==null || client.gameSettings==null)return "TAB";int code=client.gameSettings.keyBindPlayerList.getKeyCode();String name=code<0?"MOUSE"+(code+101):org.lwjgl.input.Keyboard.getKeyName(code);return name==null?"NONE":name;}
    public ModuleTab(){super("TAB",false);instance=this;}
    protected void layout(Page page){page.add(key,toggle,noHeader,noFooter);page.section("colorOptions",s->s.add(background,rows));page.group(highlight,s->s.add(own));page.add(shadow,hidePing);page.group(numbers,s->s.add(dynamic,pingColor));}
    public static boolean logoEnabled(boolean legacy){ModuleTab m=instance;return m==null || !m.isEnabled()?legacy:m.logo.on();}
    public void tick(UiTick.ClientTickEvent event){if(event.phase!=UiTick.Phase.START)return;
        com.cheatbreaker.client.kazerui.gui.ui.UiModel model=com.cheatbreaker.client.kazerui.module.ModuleManager.model();
        if(keyRevision!=model.revision){customKey=model.value(key(),key.key,null)!=null;keyRevision=model.revision;}
        boolean down=isEnabled() && mc().currentScreen==null && (customKey?key.isDown():mc().gameSettings.keyBindPlayerList.isKeyDown());
        if(down && !held)active=!active;held=down;if(mc().currentScreen!=null || !isEnabled())active=false;
    }
    public static boolean listKey(boolean original){ModuleTab m=instance;return m==null || !m.isEnabled()?original:m.toggle.on()?m.active:m.customKey?m.held:original;}
    public static int boxColor(int original){ModuleTab m=instance;return m==null || !m.isEnabled()?original:original==0x80000000?m.background.color(0):original==0x20FFFFFF?m.rows.color(0):original;}
    public static boolean highlighting(){return instance!=null && instance.isEnabled() && instance.highlight.on();}
    public static int nameColor(int original,boolean local){ModuleTab m=instance;return m!=null && m.isEnabled() && local && m.highlight.on()?m.own.color(0):original;}
    public static boolean nameShadow(boolean original){ModuleTab m=instance;return m==null || !m.isEnabled()?original:m.shadow.on();}
    public static IChatComponent header(IChatComponent original){ModuleTab m=instance;return m!=null && m.isEnabled() && m.noHeader.on()?null:original;}
    public static IChatComponent footer(IChatComponent original){ModuleTab m=instance;return m!=null && m.isEnabled() && m.noFooter.on()?null:original;}
    public static boolean drawPing(int x,int y,int width,int ping){ModuleTab m=instance;if(m==null || !m.isEnabled())return false;if(m.hidePing.on() || ping<0)return true;if(!m.numbers.on())return false;
        String text=ping>=10000?">9999":PINGS[ping];if(text==null)PINGS[ping]=text=Integer.toString(ping);
        int color=m.dynamic.on()?ping<65?0xFF55FF55:ping<120?0xFFFFFF55:ping<250?0xFFFF5555:0xFFAA0000:m.pingColor.color(0);
        GlStateManager.pushMatrix();try{GlStateManager.translate(x+width-12,y+2,0);GlStateManager.scale(.5,.5,1);MixedHudText.text(text,24-MixedHudText.width(text),0,color,false);}finally{GlStateManager.popMatrix();}return true;
    }
}
