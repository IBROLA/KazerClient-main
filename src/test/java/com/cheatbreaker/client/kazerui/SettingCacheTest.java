package com.cheatbreaker.client.kazerui;

import com.cheatbreaker.client.kazerui.gui.ui.UiModel;
import com.cheatbreaker.client.kazerui.module.Module;
import com.cheatbreaker.client.kazerui.module.ModuleManager;
import com.cheatbreaker.client.kazerui.module.setting.Setting;
import java.lang.reflect.Field;
import org.junit.Test;
import static org.junit.Assert.*;

public class SettingCacheTest {
    @Test public void colorCacheTracksProfilesOptionsAndInvalidSpeedWithoutFrameAllocations() throws Exception {
        Field field=ModuleManager.class.getDeclaredField("model");field.setAccessible(true);Object previous=field.get(null);
        try {
            OwnedUiContractTest.Store store=new OwnedUiContractTest.Store();store.put("profiles","Default|Other");
            UiModel model=new UiModel(store);field.set(null,model);
            Module module=new Module("cache_color",true) { };
            com.cheatbreaker.client.kazerui.module.setting.ColorSetting color=module.color("tint",0x7F55AA33);
            assertFalse(color.chroma());assertEquals(0x7F55AA33,color.color(0));
            model.set("cache_color","tintChroma","true");model.set("cache_color","tintChromaType","SHIFT");model.set("cache_color","tintChromaSpeed","NaN");
            assertTrue(color.chroma());for(int i=0;i<1000;i++)assertEquals(0x7F,color.color(i)>>>24);
            model.action("profile:Other");assertFalse(color.chroma());
            model.action("profile:Default");assertTrue(color.chroma());color.noChroma();assertFalse(color.chroma());
        } finally {field.set(null,previous);}
    }
    @Test public void moduleKeyAndEnabledCacheRemainCorrectAcrossProfileEditsAndResets() throws Exception {
        Field field=ModuleManager.class.getDeclaredField("model"); field.setAccessible(true);
        Object previous=field.get(null);
        try {
            OwnedUiContractTest.Store store=new OwnedUiContractTest.Store();
            store.put("profiles","Default|Other");
            UiModel model=new UiModel(store); field.set(null,model);
            Module module=new Module("CACHE_TEST",false) { };
            assertSame(module.key(),module.key());
            assertFalse(module.isEnabled());
            model.set("cache_test","enabled","true");
            for(int i=0;i<1000;i++) assertTrue(module.isEnabled());
            assertTrue(module.isHudEnabled());model.set("cache_test","renderHud","false");
            assertTrue(module.isEnabled());assertFalse(module.isHudEnabled());
            model.set("cache_test","renderHud","true");assertTrue(module.isHudEnabled());
            model.action("profile:Other"); assertFalse(module.isEnabled());
            model.action("profile:Default"); assertTrue(module.isEnabled());
            model.resetProfile("Default"); assertFalse(module.isEnabled());
        } finally { field.set(null,previous); }
    }
    @Test public void revisionCacheInvalidatesForEditsProfilesResetsAndBadValues() throws Exception {
        Field field=ModuleManager.class.getDeclaredField("model"); field.setAccessible(true);
        Object previous=field.get(null);
        try {
            OwnedUiContractTest.Store store=new OwnedUiContractTest.Store();
            store.put("profiles","Default|Other");
            UiModel model=new UiModel(store); field.set(null,model);
            final int[] parses={0};
            Module module=new Module("cache_test",false) { };
            Setting<Integer> setting=module.add(new Setting<Integer>("value",4) {
                protected Integer parse(String raw) { parses[0]++; return Integer.valueOf(raw); }
                protected String kind() { return "integer"; }
            });
            model.set("cache_test","value","8");
            for(int i=0;i<1000;i++)assertEquals(Integer.valueOf(8),setting.get());
            assertEquals(1,parses[0]);
            model.set("cache_test","value","9"); assertEquals(Integer.valueOf(9),setting.get());
            model.action("profile:Other"); assertEquals(Integer.valueOf(4),setting.get());
            model.set("cache_test","value","bad"); assertEquals(Integer.valueOf(4),setting.get());
            model.action("profile:Default"); assertEquals(Integer.valueOf(9),setting.get());
            model.resetProfile("Default"); assertEquals(Integer.valueOf(4),setting.get());
        } finally { field.set(null,previous); }
    }
}
