package com.cheatbreaker.client.kazerui.module.modules.visual;

import com.cheatbreaker.client.kazerui.module.Module;
import com.cheatbreaker.client.kazerui.module.Page;
import com.cheatbreaker.client.kazerui.module.render.PostShader;
import com.cheatbreaker.client.kazerui.module.setting.ChoiceSetting;
import com.cheatbreaker.client.kazerui.module.setting.NumberSetting;
import com.cheatbreaker.client.kazerui.event.UiTick;

/** User-owned three-mode effect; transport adapted to the native hosts. */
public final class ModuleMotionBlur extends Module {
    public enum Type implements ChoiceSetting.Option {
        V1("v1",0),V2("v2",1),V3("v3",2);
        final String lang;final int id;
        Type(String lang,int id){this.lang=lang;this.id=id;}
        @Override public String langId(){return lang;}
    }
    private static ModuleMotionBlur instance;
    private final NumberSetting value=integer("value",5,1,10);
    private final ChoiceSetting<Type> type=choice("type",Type.V3);
    private final PostShader shader=new PostShader("motion_blur",new String[]{"vec3 Phosphor"},"PrevSampler");
    private Object world;
    private Type lastType;
    private int lastValue;
    public ModuleMotionBlur(){super("MOTION_BLUR",false);instance=this;}
    @Override protected void layout(Page page){page.add(type,value);}
    public static float accumulation(Type type,int value) {
        value=Math.max(1,Math.min(10,value));float v=value/10.0f;
        if(type==Type.V1)v=0.7f+value/100.0f*3.0f-0.01f;
        else if(type==Type.V2){if(v>=1)v=.99f;v=1-v;}return v;
    }
    public static void postProcess() {
        ModuleMotionBlur m=instance;if(m==null)return;
        if(!m.isEnabled() || mc().theWorld==null || mc().gameSettings.anaglyph){m.release();return;}
        Type type=m.type.get();int value=m.value.intValue();
        if(m.world!=mc().theWorld || type!=m.lastType || value!=m.lastValue){m.shader.reset();m.world=mc().theWorld;m.lastType=type;m.lastValue=value;}
        m.shader.render(mc(),accumulation(type,value),type.id);
    }
    @Override public void tick(UiTick.ClientTickEvent event) {
        if(event.phase==UiTick.Phase.END && (!isEnabled() || mc().theWorld==null))release();
    }
    // Also clear a failed compilation with no allocated GL objects, allowing
    // the next enable to retry. delete() is a no-op on the driver when empty.
    private void release(){shader.delete();world=null;lastType=null;}
    @Override protected void onDisable(){release();}
    public static void resourceReload(){if(instance!=null)instance.release();}
    public static boolean hasResources(){return instance!=null && instance.shader.hasResources();}
}
