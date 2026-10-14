package com.cheatbreaker.client.kazerui.module;
import com.cheatbreaker.client.kazerui.module.setting.*;
import com.cheatbreaker.client.kazerui.gui.ui.*;
import java.util.*;

/** Explicit bulk edits only. Changing a default color never silently rewrites profiles. */
final class GlobalHudStyle {
    private final Module owner;
    private final Map<String,Setting<?>> styles=new LinkedHashMap<String,Setting<?>>();
    private final Map<String,ButtonSetting> buttons=new LinkedHashMap<String,ButtonSetting>();
    GlobalHudStyle(Module owner) {
        this.owner=owner;
        color("textColor","Text Color",0xFFFFFFFF);color("borderColor","Border Color",0xFFFFFFFF);
        color("bracketColor","Bracket Color",0xFFFFFFFF);color("backgroundColor","Background Color",0x6F000000);
        number("backgroundHeight","Background Height",18,9,100,true);number("backgroundWidth","Background Width",56,9,400,true);
        number("borderThickness","Border Thickness",.5f,.5f,3,false);
        bool("background","Show Background",false);bool("brackets","Show Brackets",false);bool("border","Border",false);bool("textShadow","Text Shadow",true);
    }
    private void add(String key,String label,Setting<?> value) {
        value.label(()->label);styles.put(key,value);
        ButtonSetting button=owner.add(new ButtonSetting("apply_"+key,()->apply(key)));
        button.label(()->"Apply to All: "+label);buttons.put(key,button);
    }
    private void color(String key,String label,int def){add(key,label,owner.color("style_"+key,def).noChroma());}
    private void number(String key,String label,float def,float min,float max,boolean integer) {
        add(key,label,integer?owner.integer("style_"+key,(int)def,(int)min,(int)max):owner.decimal("style_"+key,def,min,max));
    }
    private void bool(String key,String label,boolean def){add(key,label,owner.bool("style_"+key,def));}
    void layout(Page page) {
        page.section("Global Color",s->rows(s,"textColor","borderColor","bracketColor","backgroundColor"));
        page.section("Global Background Options",s->rows(s,"backgroundHeight","backgroundWidth","borderThickness"));
        page.section("Global Toggle",s->rows(s,"background","brackets","border","textShadow"));
    }
    private void rows(Page page,String...keys){for(String key:keys)page.add(styles.get(key),buttons.get(key));}
    private void apply(String key) {
        Setting<?> setting=styles.get(key);String fallback=setting.node(Collections.<OptionCatalog.Node>emptyList(),null).value;
        String value=ModuleManager.store("global",setting.key,fallback);
        int changed=0;
        for(Module module:ModuleManager.modules()) {
            if(module==owner)continue;
            changed+=applyModule(module,key,value);
            for(Module child:module.children())changed+=applyModule(child,key,value);
        }
        ModuleManager.model().store.save();
        ModuleManager.model().notify("Updated "+changed+" compatible HUD settings.");
    }
    private static int applyModule(Module module,String key,String value) {
        if(module.huds().isEmpty()&&!(module instanceof NativeSettingsAdapter))return 0;
        for(OptionCatalog.Node node:OptionCatalog.flatten(module.page().options)) {
            String normalized=node.key.replace("legacy_","").replace("_","").toLowerCase(Locale.ROOT);
            String wanted=key.toLowerCase(Locale.ROOT);
            if(normalized.equals(wanted)||key.equals("background")&&normalized.equals("showbackground")) {
                ModuleManager.put(module.key(),node.key,value);return 1;
            }
        }
        return 0;
    }
}
