package com.cheatbreaker.client.kazerui.module.modules.mechanic;
import com.cheatbreaker.client.kazerui.module.GlobalToggleModule;
import com.cheatbreaker.client.kazerui.module.Page;
import com.cheatbreaker.client.kazerui.module.setting.*;
/** Brightness is a render override; the saved Minecraft gamma value is untouched. */
public final class ModuleLighting extends GlobalToggleModule {
    private static ModuleLighting instance;
    private final BoolSetting full=bool("fullBright",true);
    private final NumberSetting boost=decimal("brightnessBoost",1,1,10);
    public ModuleLighting(){super("LIGHTING","legacy_fullbright",true);instance=this;}
    protected void layout(Page page){page.add(full,boost);}
    public static float gamma(float original){ModuleLighting m=instance;return m==null || !m.isEnabled()?original:m.full.on()?100f:Math.min(100f,original+m.boost.value()-1f);}
    public static boolean fullBright(){ModuleLighting m=instance;return m!=null && m.isEnabled() && m.full.on();}
}
