package com.cheatbreaker.client.kazerui.module.modules.visual;
import com.cheatbreaker.client.kazerui.module.Module;
import com.cheatbreaker.client.kazerui.module.Page;
import com.cheatbreaker.client.kazerui.module.setting.*;
public final class ModuleColorSaturation extends Module {
    private static ModuleColorSaturation instance;
    private final NumberSetting hue=decimal("colorSaturationHue",0,0,10),saturation=decimal("colorSaturationSaturation",5,0,10),brightness=decimal("colorSaturationBrightness",5,0,10),contrast=decimal("colorSaturationContrast",5,0,10);
    private final BoolSetting grayscale=bool("grayscale",false);
    private final ColorGradePass pass=new ColorGradePass();private long epoch=-1;
    public ModuleColorSaturation(){super("COLOR_SATURATION",false);instance=this;}
    protected void layout(Page page){page.add(hue,saturation,brightness,contrast,grayscale);}
    protected void onDisable(){pass.delete();}
    public static void postProcess(){ModuleColorSaturation m=instance;if(m==null || !m.isEnabled() || mc().theWorld==null)return;
        long epoch=com.cheatbreaker.client.kazerui.KazerResourceEpoch.current();if(m.epoch!=epoch){m.pass.delete();m.epoch=epoch;}
        m.pass.render(mc(),m.hue.value()/10f,m.grayscale.on()?0:m.saturation.value()/10f,m.brightness.value()/10f,m.contrast.value()/10f);
    }
}
