package com.cheatbreaker.client.kazerui.module;

import com.cheatbreaker.client.config.Setting;
import com.cheatbreaker.client.module.AbstractModule;
import com.cheatbreaker.client.kazerui.config.LegacyProfileMigration;
import com.cheatbreaker.client.kazerui.event.UiTick;
import com.cheatbreaker.client.kazerui.gui.ui.UiModel;
import java.util.*;

/** Settings-only bridge for retained Kazer-specific protocol and animation features.
 * Matching imported modules never register through this adapter. */
public final class NativeSettingsAdapter extends Module {
    private final AbstractModule nativeModule;
    private final List<Value> values = new ArrayList<Value>();
    private long appliedRevision = Long.MIN_VALUE;
    public NativeSettingsAdapter(AbstractModule module) {
        super(Objects.requireNonNull(LegacyProfileMigration.moduleId(module.getName())), module.defaultState);
        nativeModule = module;
        for (int i=0; i<module.getSettingsList().size(); i++) {
            Setting setting = module.getSettingsList().get(i);
            if (setting.getLabel().equalsIgnoreCase("label")) continue;
            Object fallback = i < module.getDefaultSettingsValues().size() ? module.getDefaultSettingsValues().get(i) : setting.getValue();
            if (fallback == null || fallback instanceof String[]) continue;
            Value value = add(new Value(setting, fallback));
            value.label(setting::getDisplayLabel);
            value.onChange(value::apply);
            if (setting.getParent() != null) value.hideIf(() -> !setting.isVisible());
            values.add(value);
        }
        seed();
    }
    private void seed() {
        UiModel model = ModuleManager.model();
        String marker="migration.nativeSettings."+key()+".v1";
        if("true".equals(model.store.get(marker,null))) return;
        for (String profile : model.profiles) {
            String prefix = "profile."+profile+"."+key()+".";
            if (model.store.get(prefix+"enabled",null)==null)
                model.store.put(prefix+"enabled",String.valueOf(profile.equals(model.profile()) ? nativeModule.isEnabled() : nativeModule.defaultState));
            for (Value value : values) if (model.store.get(prefix+value.key,null)==null) {
                String raw = model.store.get("legacy.profile."+profile+"."+nativeModule.getName()+"."+value.nativeSetting.getLabel(),null);
                if (raw != null) {
                    try { model.store.put(prefix+value.key,value.format(value.fromLegacy(raw))); }
                    catch (RuntimeException ignored) { /* Preserved raw legacy value remains available. */ }
                }
            }
        }
        model.store.put(marker,"true");
    }
    @Override protected void layout(Page page) { for (Value value : values)
        if(!key().equals("nametags") || (!value.key.equals("legacy_show_kazer_icon_in_tab")&&!value.key.equals("legacy_show_nametag_icon")))page.add(value); }
    @Override protected void onEnable() { apply(); }
    @Override protected void onDisable() { apply(); }
    private void apply() {
        nativeModule.applySharedUiState(isEnabled());
        for (Value value : values) value.apply();
        appliedRevision = ModuleManager.model().revision;
    }
    @Override public void tick(UiTick.ClientTickEvent event) {
        if (event.phase == UiTick.Phase.START && appliedRevision != ModuleManager.model().revision) apply();
    }
    static final class Value extends com.cheatbreaker.client.kazerui.module.setting.Setting<Object> {
        final Setting nativeSetting;
        final boolean color;
        Value(Setting setting,Object fallback) {
            super("legacy_"+setting.getLabel().toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+","_"),fallback);
            nativeSetting = setting;
            color = fallback instanceof Integer && setting.getMinimumValue() != null && setting.getMaximumValue() != null
                && setting.getMinimumValue().intValue()==Integer.MIN_VALUE && setting.getMaximumValue().intValue()==Integer.MAX_VALUE;
        }
        @Override protected Object parse(String raw) {
            if (defaultValue instanceof Boolean) return Boolean.parseBoolean(raw);
            if (color) return (int)Long.parseLong(raw,16);
            if (defaultValue instanceof Number) {
                double number = Double.parseDouble(raw);
                if (Double.isNaN(number)||Double.isInfinite(number)) throw new IllegalArgumentException("Non-finite option");
                if (nativeSetting.getMinimumValue()!=null) number=Math.max(number,nativeSetting.getMinimumValue().doubleValue());
                if (nativeSetting.getMaximumValue()!=null) number=Math.min(number,nativeSetting.getMaximumValue().doubleValue());
                if (defaultValue instanceof Integer) return (int)Math.round(number);
                if (defaultValue instanceof Float) return (float)number;
                return number;
            }
            if (nativeSetting.getAcceptedValues()!=null && !Arrays.asList(nativeSetting.getAcceptedValues()).contains(raw)) return defaultValue;
            return raw;
        }
        Object fromLegacy(String raw) { return color ? Integer.parseInt(raw.split(";",2)[0]) : parse(raw); }
        @Override protected String format(Object value) { return color ? String.format(Locale.ROOT,"%08X",(Integer)value) : String.valueOf(value); }
        @Override protected String kind() {
            if (color) return "color";
            if (defaultValue instanceof Boolean) return "bool";
            if (defaultValue instanceof Number) return "number";
            return nativeSetting.getAcceptedValues()==null ? "string" : "enum";
        }
        @Override protected void describe(Map<String,Object> fields) {
            if (defaultValue instanceof Number && !color) {
                fields.put("min", nativeSetting.getMinimumValue()==null ? -100000 : nativeSetting.getMinimumValue());
                fields.put("max", nativeSetting.getMaximumValue()==null ? 100000 : nativeSetting.getMaximumValue());
                fields.put("integer",defaultValue instanceof Integer);
            }
            if (nativeSetting.getAcceptedValues()!=null) {
                List<List<String>> options = new ArrayList<List<String>>();
                for (String value : nativeSetting.getAcceptedValues()) options.add(Arrays.asList(value,value));
                fields.put("values",options);
            }
            if (color) fields.put("chroma",false);
        }
        void apply() { Object value=get(); if (!Objects.equals(value,nativeSetting.getValue())) nativeSetting.setValue(value,false); }
    }
}
