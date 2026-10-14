package com.cheatbreaker.client.kazerui.module.setting;

import com.cheatbreaker.client.kazerui.module.ModuleManager;
import java.awt.Color;
import java.util.Locale;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ScaledResolution;

public final class ColorSetting extends Setting<Integer> {
    private boolean alpha = true, chroma = true, chromaOn;
    private final float[] hsb = new float[3];
    private boolean haveBase;
    private int previousBase;
    private Object optionModel;
    private long optionRevision=Long.MIN_VALUE;
    private boolean cachedChroma, shift;
    private float cachedSpeed=40;

    public ColorSetting(String key, int argb) { super(key, argb); }

    public ColorSetting noAlpha() { alpha = false; return this; }

    public ColorSetting noChroma() { chroma = false; optionRevision=Long.MIN_VALUE; return this; }

    public ColorSetting chromaOn() { chromaOn = true; optionRevision=Long.MIN_VALUE; return this; }

    public int argb() {
        int c = get();
        return alpha ? c : c | 0xFF000000;
    }

    public boolean chroma() {
        refreshOptions();
        return cachedChroma;
    }

    private void refreshOptions() {
        com.cheatbreaker.client.kazerui.gui.ui.UiModel model=ModuleManager.model();
        if (model!=null && optionModel==model && optionRevision==model.revision) return;
        String raw = ModuleManager.store(owner.key(), key + "Chroma", null);
        cachedChroma=chroma && (raw == null ? chromaOn : Boolean.parseBoolean(raw));
        try { cachedSpeed=Float.parseFloat(ModuleManager.store(owner.key(),key+"ChromaSpeed","40")); }
        catch(NumberFormatException invalid) { cachedSpeed=40; }
        if(Float.isNaN(cachedSpeed)||Float.isInfinite(cachedSpeed)) cachedSpeed=40;
        cachedSpeed=Math.max(1,Math.min(100,cachedSpeed));
        shift="SHIFT".equals(ModuleManager.store(owner.key(),key+"ChromaType","WAVE"));
        optionModel=model;optionRevision=model==null ? Long.MIN_VALUE : model.revision;
    }

    public int color(float position) {
        int base = argb();
        if (!chroma()) return base;
        if (!haveBase||previousBase!=base) {
            Color.RGBtoHSB(base >> 16 & 255,base >> 8 & 255,base & 255,hsb);previousBase=base;haveBase=true;
        }
        double d = (100.1f - cachedSpeed) / 100.0f;
        long now = System.nanoTime();
        float hue;
        if (shift) {
            hue = (float)(now / (1.0E10 * d) % 1.0);
        } else {
            int extent=com.cheatbreaker.client.kazerui.event.UiOverlayMetrics.extent;
            if(extent==0) {
                ScaledResolution res=new ScaledResolution(Minecraft.getMinecraft());
                extent=res.getScaledWidth()+res.getScaledHeight();
            }
            float offset = extent - position;
            hue = (float)((now + (double)offset * (2.0E7 * d)) / (1.0E10 * d) % 1.0);
        }
        return base & 0xFF000000 | Color.HSBtoRGB(hue, hsb[1], hsb[2]) & 0xFFFFFF;
    }

    public int color() { return color(0); }

    @Override protected Integer parse(String raw) {
        String v = raw.trim();
        return v.matches("[0-9A-Fa-f]{8}") ? (int)Long.parseLong(v, 16) : null;
    }

    @Override protected String format(Integer value) { return String.format(Locale.ROOT, "%08X", value); }

    @Override protected String kind() { return "color"; }

    @Override protected void describe(Map<String, Object> fields) {
        fields.put("alpha", alpha);
        fields.put("chroma", chroma);
        fields.put("chromaOn", chromaOn);
    }
}
