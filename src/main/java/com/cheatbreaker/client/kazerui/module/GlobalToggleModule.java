package com.cheatbreaker.client.kazerui.module;
/** Module cards and General settings address the same existing global control. */
public class GlobalToggleModule extends Module {
    private final String option;private final boolean fallback;
    private com.cheatbreaker.client.kazerui.gui.ui.UiModel cachedModel;private long revision=Long.MIN_VALUE;private boolean enabled;
    public GlobalToggleModule(String id,String option,boolean fallback){super(id,fallback);this.option=option;this.fallback=fallback;}
    public boolean isEnabled(){com.cheatbreaker.client.kazerui.gui.ui.UiModel model=ModuleManager.model();if(model==null)return fallback;
        if(cachedModel!=model || revision!=model.revision){enabled=model.flag("global",option,fallback);cachedModel=model;revision=model.revision;}return enabled;
    }
    public void setEnabled(boolean value){if(ModuleManager.model()!=null)ModuleManager.model().set("global",option,Boolean.toString(value));}
}
