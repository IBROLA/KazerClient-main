package com.cheatbreaker.client.kazerui;

import com.cheatbreaker.client.config.Setting;
import com.cheatbreaker.client.kazerui.gui.ui.UiModel;
import com.cheatbreaker.client.kazerui.module.NativeSettingsAdapter;
import com.cheatbreaker.client.kazerui.module.ModuleManager;
import com.cheatbreaker.client.module.AbstractModule;
import java.lang.reflect.Field;
import java.util.Map;
import org.junit.Test;
import static org.junit.Assert.*;

public class NativeSettingsAdapterTest {
    private static final class NativeModule extends AbstractModule {
        final Setting opacity=new Setting(this,"Nametag Opacity").setValue(1f).setMinMax(.1f,1f);
        final Setting color=new Setting(this,"Color").setValue(-1).setMinMax(Integer.MIN_VALUE,Integer.MAX_VALUE);
        final Setting type=new Setting(this,"Type").setValue("A").acceptedValues("A","B");
        NativeModule() { super("Nametags"); setDefaultState(true); }
    }
    @Test @SuppressWarnings("unchecked") public void appliesTypedOptionsAndStateWithoutChangingDefaultsOrDuplicatingProfiles() throws Exception {
        Field modelField=ModuleManager.class.getDeclaredField("model"); modelField.setAccessible(true);
        Field registryField=ModuleManager.class.getDeclaredField("BY_KEY"); registryField.setAccessible(true);
        Object previousModel=modelField.get(null);
        Map<String,Object> registry=(Map<String,Object>)registryField.get(null);
        Object previousModule=registry.get("nametags");
        try {
            OwnedUiContractTest.Store store=new OwnedUiContractTest.Store();
            store.put("legacy.profile.Default.Nametags.Nametag Opacity","0.75");
            UiModel model=new UiModel(store); modelField.set(null,model);
            NativeModule nativeModule=new NativeModule();
            NativeSettingsAdapter adapter=new NativeSettingsAdapter(nativeModule); registry.put("nametags",adapter);
            model.set("nametags","legacy_nametag_opacity","0.5");
            assertEquals(.5f,(Float)nativeModule.opacity.getValue(),0);
            model.set("nametags","legacy_nametag_opacity","9");
            assertEquals(1f,(Float)nativeModule.opacity.getValue(),0);
            model.set("nametags","legacy_color","FF55FF55");
            assertEquals(0xFF55FF55,nativeModule.color.getValue());
            model.set("nametags","legacy_type","B"); assertEquals("B",nativeModule.type.getValue());
            assertEquals("enum",adapter.page().options.get(adapter.page().options.size()-1).kind);
            adapter.setEnabled(false); assertFalse(nativeModule.isEnabled()); assertTrue(nativeModule.defaultState);
            adapter.setEnabled(true); assertTrue(nativeModule.isEnabled());
            model.resetProfile("Default");
            new NativeSettingsAdapter(nativeModule);
            assertNull("Reset values must not be reseeded from stale old config",store.get("profile.Default.nametags.legacy_nametag_opacity",null));
        } finally {
            modelField.set(null,previousModel);
            if(previousModule==null) registry.remove("nametags"); else registry.put("nametags",previousModule);
        }
    }
}
