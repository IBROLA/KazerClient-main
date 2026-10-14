package com.cheatbreaker.client.kazerui;
import com.cheatbreaker.client.kazerui.gui.ui.UiModel;
import com.cheatbreaker.client.kazerui.feature.HudAnchor;
import com.cheatbreaker.client.kazerui.module.Module;
import com.cheatbreaker.client.kazerui.module.ModuleManager;
import com.cheatbreaker.client.kazerui.module.hud.HudElement;
import com.cheatbreaker.client.kazerui.module.hud.TextHud;
import java.lang.reflect.*;
import java.util.*;
import org.junit.Test;
import static org.junit.Assert.*;
public class OwnedHudCacheTest {
    @Test public void anchorCacheFollowsProfileResetAndModelReplacement() throws Exception {
        Field field=ModuleManager.class.getDeclaredField("model");field.setAccessible(true);Object before=field.get(null);
        try {
            OwnedUiContractTest.Store store=new OwnedUiContractTest.Store();store.put("profiles","Default|Other");
            UiModel model=new UiModel(store);field.set(null,model);
            HudElement hud=new HudElement(new Module("fps",true){},0,0,HudAnchor.TOP_LEFT){public boolean visible(boolean p){return true;}public void render(boolean p){}};
            model.set("fps","anchor","bottomRight");for(int i=0;i<1000;i++)assertSame(HudAnchor.BOTTOM_RIGHT,hud.currentAnchor());
            model.action("profile:Other");assertNotSame(HudAnchor.BOTTOM_RIGHT,hud.currentAnchor());
            model.action("profile:Default");assertSame(HudAnchor.BOTTOM_RIGHT,hud.currentAnchor());
            model.resetProfile("Default");assertNotSame(HudAnchor.BOTTOM_RIGHT,hud.currentAnchor());
            field.set(null,new UiModel(store));assertNotNull(hud.currentAnchor());
        } finally {field.set(null,before);}
    }
    @Test public void singleLineMetricsCacheHandlesTextBracketsFontAndMultilineTransitions() throws Exception {
        Field field=ModuleManager.class.getDeclaredField("model");field.setAccessible(true);Object before=field.get(null);
        try {
            UiModel model=new UiModel(new OwnedUiContractTest.Store());field.set(null,model);
            class Hud extends TextHud {
                String value="A";boolean multiline;long metrics;int measurements;
                Hud(){super(new Module("cache_hud",true){},0,0,HudAnchor.TOP_LEFT,sizes(10,18,22,40,56,62));background.set(false);brackets.set(false);}
                protected String text(boolean preview){return value;}
                protected List<String> lines(boolean preview){return multiline?Arrays.asList("ABC","DE"):null;}
                protected float measureText(String text){measurements++;return text.length()*6;}
                protected float textHeight(){return 9;}
                protected long textMetricsVersion(){return metrics;}
            }
            Hud hud=new Hud();for(int i=0;i<1000;i++)assertTrue(hud.visible(false));assertEquals(1,hud.measurements);assertEquals(6,hud.width(),0);
            hud.value="AA";hud.visible(false);assertEquals(2,hud.measurements);assertEquals(12,hud.width(),0);
            hud.brackets.set(true);hud.visible(false);assertEquals(3,hud.measurements);assertEquals(24,hud.width(),0);
            hud.metrics++;hud.visible(false);assertEquals(4,hud.measurements);
            hud.multiline=true;hud.visible(false);assertEquals(30,hud.width(),0);
            hud.multiline=false;hud.visible(false);assertEquals(24,hud.width(),0);
            hud.value="";assertFalse(hud.visible(false));
        } finally {field.set(null,before);}
    }
    @Test public void labelCacheUsesStateIdentityFlagsConfigurationAndResourceGenerations() {
        com.cheatbreaker.client.kazerui.module.hud.HudTextCache cache=new com.cheatbreaker.client.kazerui.module.hud.HudTextCache();
        assertNull(cache.get(60,0,1,0,null));String minute=new String("1:00");cache.put(60,0,1,0,null,minute);
        for(int i=0;i<1000;i++)assertSame(minute,cache.get(60,0,1,0,null));
        assertNull(cache.get(59,0,1,0,null));assertNull(cache.get(60,1,1,0,null));assertNull(cache.get(60,0,2,0,null));assertNull(cache.get(60,0,1,1,null));
        cache.put(1,0,2,1,"potion.moveSpeed","Speed II");assertEquals("Speed II",cache.get(1,0,2,1,"potion.moveSpeed"));
        assertNull(cache.get(2,0,2,1,"potion.moveSpeed"));assertNull(cache.get(1,0,2,1,"potion.damageBoost"));
    }
}
