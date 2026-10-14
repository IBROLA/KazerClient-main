package com.cheatbreaker.client.kazerui.module.modules.mechanic;
import com.cheatbreaker.client.kazerui.module.Module;
import com.cheatbreaker.client.kazerui.module.Page;
import com.cheatbreaker.client.kazerui.module.ModuleManager;
import com.cheatbreaker.client.kazerui.module.setting.*;
import com.cheatbreaker.client.kazerui.event.UiTick;
import net.minecraft.client.settings.KeyBinding;
public final class ModuleZoom extends Module {
    private static ModuleZoom instance;
    private final KeySetting key=keybind("zoomKeybind","C");
    private final BoolSetting toggle=bool("toggleKeyZoom",false),smoothCamera=bool("smoothCamera",true),smoothZoom=bool("smoothZoom",true),variable=bool("variableZoom",false);
    private final NumberSetting scrollSpeed=decimal("zoomScrollSpeed",1,.25f,5),divisor=integer("zoomDivisor",4,2,10),sensitivity=decimal("cameraSensitivity",1,.1f,2);
    private boolean active,down,savedSmooth;private float variableFactor=1,previous=1,current=1;
    public ModuleZoom(){super("ZOOM",true);instance=this;
        String saved=ModuleManager.model().store.get("zoomKeyName",null);
        if(saved!=null && ModuleManager.model().store.get(ModuleManager.model().key("zoom","zoomKeybind"),null)==null)ModuleManager.model().store.put(ModuleManager.model().key("zoom","zoomKeybind"),saved);
        key.onChange(this::syncBinding);
    }
    private void syncBinding(){
        com.cheatbreaker.client.CheatBreaker core=com.cheatbreaker.client.CheatBreaker.getInstance();if(core==null)return;
        KeyBinding binding=core.globalSettings.keyBindings.binding("Zoom");if(binding!=null){binding.setKeyCode(key.code());KeyBinding.resetKeyBindingArrayAndHash();}
        ModuleManager.model().store.put("zoomKeyName",key.get());ModuleManager.model().store.put("zoomKey",Integer.toString(key.code()));
    }
    protected void layout(Page page){page.add(key,toggle,smoothCamera,smoothZoom,variable,scrollSpeed,divisor,sensitivity);}
    protected void onDisable(){stop();}
    private void stop(){if(active)mc().gameSettings.smoothCamera=savedSmooth;active=false;down=false;variableFactor=previous=current=1;}
    public void tick(UiTick.ClientTickEvent event) {
        if(event.phase!=UiTick.Phase.START)return;
        boolean available=isEnabled() && mc().theWorld!=null && mc().currentScreen==null && mc().inGameHasFocus;
        boolean pressed=available && key.isDown(),next=toggle.on()?(pressed && !down?!active:active):pressed;
        if(!available)next=false;down=pressed;
        if(next!=active){if(next)savedSmooth=mc().gameSettings.smoothCamera;else{mc().gameSettings.smoothCamera=savedSmooth;variableFactor=1;}active=next;}
        if(active)mc().gameSettings.smoothCamera=smoothCamera.on() || savedSmooth;
        previous=current;float target=active?1/(divisor.value()*(variable.on()?variableFactor:1)):1;
        current=smoothZoom.on()?current+(target-current)*.75f:target;
    }
    public static boolean ownsNative(){return instance!=null;}
    public static boolean scroll(int delta){ModuleZoom m=instance;if(m==null || !m.isEnabled() || !m.active || !m.variable.on() || delta==0)return false;
        m.variableFactor=Math.max(1.4f/m.divisor.value(),Math.min(10,m.variableFactor+Math.signum(delta)*m.scrollSpeed.value()*.5f));return true;}
    public static float fov(float original,float partial){ModuleZoom m=instance;return m==null || !m.isEnabled()?original:original*(m.previous+(m.current-m.previous)*partial);}
    public static float sensitivity(float original){ModuleZoom m=instance;return m==null || !m.isEnabled() || !m.active?original:original*m.sensitivity.value();}
}
