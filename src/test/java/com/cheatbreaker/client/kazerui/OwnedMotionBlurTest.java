package com.cheatbreaker.client.kazerui;

import com.cheatbreaker.client.kazerui.module.modules.visual.ModuleMotionBlur;
import java.io.StringReader;
import org.junit.Test;
import static org.junit.Assert.*;

public class OwnedMotionBlurTest {
    @Test public void preservesDonorCoefficientEquations() {
        for(int value=1;value<=10;value++) {
            assertEquals(.7f+value/100f*3-.01f,ModuleMotionBlur.accumulation(ModuleMotionBlur.Type.V1,value),.000001f);
            assertEquals(1-(value==10 ? .99f : value/10f),ModuleMotionBlur.accumulation(ModuleMotionBlur.Type.V2,value),.000001f);
            assertEquals(value/10f,ModuleMotionBlur.accumulation(ModuleMotionBlur.Type.V3,value),.000001f);
        }
        assertEquals(.1f,ModuleMotionBlur.accumulation(ModuleMotionBlur.Type.V3,-100),0);
        assertEquals(1f,ModuleMotionBlur.accumulation(ModuleMotionBlur.Type.V3,100),0);
    }
    @Test public void importsOldStrengthAndModeWithoutOverwritingNewOptions() throws Exception {
        OwnedUiContractTest.Store store=new OwnedUiContractTest.Store();
        com.cheatbreaker.client.kazerui.config.LegacyProfileMigration.readProfile(new StringReader("[Motion Blur]\nMode=Phosphor\nAmount=60\n"),"Default",store);
        assertEquals("V2",store.get("profile.Default.motion_blur.type",null));assertEquals("6",store.get("profile.Default.motion_blur.value",null));
        store.put("profile.Default.motion_blur.value","8");
        com.cheatbreaker.client.kazerui.config.LegacyProfileMigration.readProfile(new StringReader("[Motion Blur]\nMode=Old\nAmount=90\n"),"Default",store);
        assertEquals("8",store.get("profile.Default.motion_blur.value",null));assertEquals("V2",store.get("profile.Default.motion_blur.type",null));
        assertEquals("Phosphor",store.get("legacy.profile.Default.Motion Blur.Mode",null));
    }
}
