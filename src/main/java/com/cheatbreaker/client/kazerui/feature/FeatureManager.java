package com.cheatbreaker.client.kazerui.feature;

import com.cheatbreaker.client.kazerui.gui.KazerGfx;
import com.cheatbreaker.client.kazerui.gui.ui.HudOptions;
import com.cheatbreaker.client.kazerui.gui.ui.OptionCatalog;
import com.cheatbreaker.client.kazerui.gui.ui.UiModel;
import com.cheatbreaker.client.kazerui.gui.ui.UiStore;
import com.cheatbreaker.client.kazerui.gui.KazerMovementScreen;
import com.cheatbreaker.client.kazerui.module.Module;
import com.cheatbreaker.client.kazerui.module.ModuleManager;
import com.cheatbreaker.client.kazerui.module.hud.HudElement;
import java.util.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;
import com.cheatbreaker.client.kazerui.config.Configuration;

public final class FeatureManager implements UiStore {
    private final List<ClientFeature> features = new ArrayList<ClientFeature>();
    private final Configuration config;
    private final UiModel model;
    private final Map<ClientFeature,RenderLayout> renderLayouts = new IdentityHashMap<ClientFeature,RenderLayout>();
    private static final class RenderLayout {
        long revision = Long.MIN_VALUE;
        float x, y;
        HudAnchor anchor;
    }
    public FeatureManager(java.io.File file) {
        config = new Configuration(file); config.load();
        com.cheatbreaker.client.kazerui.config.LegacyProfileMigration.readDirectory(file.getParentFile(), this);
        if (!config.hasKey("ui", "profiles")) {
            put("profiles", "Default"); put("activeProfile", "Default");
            for (String id : new String[]{"fps", "coordinates"})
                put("profile.Default." + id + ".enabled", "" + config.getBoolean(id, "features", true, "Show this HUD feature"));
        }
        model = new UiModel(this);
        ModuleManager.init(model);
        for (HudElement hud : ModuleManager.huds()) features.add(hud);
        ModuleManager.hudListener = new ModuleManager.HudListener() {
            @Override public void added(HudElement hud) { if (!features.contains(hud)) features.add(hud); }
            @Override public void removed(HudElement hud) { features.remove(hud); renderLayouts.remove(hud); }
        };
    }
    @Override public String get(String key, String fallback) {
        return config.hasKey("ui", key) ? config.getCategory("ui").get(key).getString() : fallback;
    }
    @Override public void put(String key, String value) { config.get("ui", key, value).set(value); }
    @Override public void remove(String key) { if (config.hasCategory("ui")) config.getCategory("ui").remove(key); }
    @Override public java.util.Collection<String> keys() {
        return config.hasCategory("ui") ? new ArrayList<String>(config.getCategory("ui").keySet()) : Collections.<String>emptyList();
    }
    @Override public void save() { if (config.hasChanged()) config.save(); }
    public UiModel ui() { return model; }
    /** Called only after Kazer's existing profile/auth/protocol services finish setup. */
    public void retainNativeSettings(com.cheatbreaker.client.CheatBreaker client) {
        String marker="migration.runtime."+com.cheatbreaker.client.kazerui.KazerUiBrand.MINECRAFT+".v1";
        if(!"true".equals(get(marker,null))) {
            StringBuilder snapshot=new StringBuilder();
            for(com.cheatbreaker.client.module.AbstractModule module:client.moduleManager.modules) {
                snapshot.append('[').append(module.getName()).append("]\n-State=").append(module.isEnabled());
                snapshot.append("\n-RenderHUD=").append(module.isRenderHud());
                snapshot.append("\n-xTranslation=").append(module.getXTranslation());
                snapshot.append("\n-yTranslation=").append(module.getYTranslation());
                if(module.getGuiAnchor()!=null) snapshot.append("\n-Anchor=").append(module.getGuiAnchor().getLabel());
                snapshot.append('\n');
                for(com.cheatbreaker.client.config.Setting setting:module.getSettingsList()) {
                    if(setting.getLabel().equalsIgnoreCase("label")||setting.getValue()==null) continue;
                    snapshot.append(setting.getLabel()).append('=').append(setting.getValue());
                    if(setting.rainbow) snapshot.append(";rainbow");
                    snapshot.append('\n');
                }
            }
            try {
                com.cheatbreaker.client.kazerui.config.LegacyProfileMigration.readProfile(new java.io.StringReader(snapshot.toString()),model.profile(),this);
                put(marker,"true"); model.revision++;
            } catch(java.io.IOException impossible) { throw new IllegalStateException(impossible); }
        }
        com.cheatbreaker.client.kazerui.config.LegacyProfileMigration.finishReplacements(this,model.profiles);
        model.revision++;
        for (com.cheatbreaker.client.module.AbstractModule module : client.moduleManager.modules)
            ModuleManager.retainNativeSettings(module);
    }
    public void add(ClientFeature feature) { features.add(feature); }
    public List<ClientFeature> getFeatures() { return Collections.unmodifiableList(features); }
    public int size() { return features.size(); }
    public boolean isHudVisible() { return true; }
    public boolean isEnabled(String id) {
        Module m = ModuleManager.get(id);
        if (m != null) return m.isEnabled();
        return model.flag(id, "enabled", com.cheatbreaker.client.kazerui.gui.ui.ModuleCatalog.defaultEnabled(id));
    }

    public boolean isShown(ClientFeature feature) {
        if (!isEnabled(feature.getId())) return false;
        if (!(feature instanceof HudElement)) return true;
        HudElement hud = (HudElement)feature;
        if (!hud.module.isHudEnabled()) return false;
        boolean preview = preview();
        if (preview && !hud.editable()) return false;
        return hud.visible(preview) && (hud.screenSpace() || hud.width() > 0 && hud.height() > 0);
    }

    private static boolean preview() { return Minecraft.getMinecraft().currentScreen instanceof KazerMovementScreen; }
    public void toggle(String id) { model.toggleModule(id); }
    public String label(ClientFeature feature) {
        String text = feature.getText();
        if (feature.getId().equals("coordinates") && !model.flag("coordinates", "labels", true)) text = text.replace("XYZ: ", "");
        return text;
    }

    public float scale(String id) {
        HudElement hud = ModuleManager.hud(id);
        if (hud != null) return hud.scale();
        return Math.max(.25f, Math.min(5, model.number(id, HudOptions.SCALE, HudOptions.defaultNumber(id, HudOptions.SCALE, 1)))); }

    private String option(String id, String key) { String d = HudOptions.defaultValue(id, key); return model.value(id, key, d == null ? "false" : d); }
    private boolean flag(String id, String key) { return Boolean.parseBoolean(option(id, key)); }

    private float[] size(ClientFeature feature) {
        if (feature instanceof HudElement) {
            HudElement hud = (HudElement)feature;
            hud.visible(preview());
            return new float[]{hud.width(), hud.height()};
        }
        String id = feature.getId();
        float textWidth = Minecraft.getMinecraft().fontRendererObj.getStringWidth(shown(feature));
        float w = flag(id, HudOptions.STATIC_WIDTH) ? Math.max(model.number(id, HudOptions.BACKGROUND_WIDTH, HudOptions.defaultNumber(id, HudOptions.BACKGROUND_WIDTH, 56)), textWidth + 4) : textWidth + 8;
        float h = flag(id, HudOptions.STATIC_HEIGHT) ? model.number(id, HudOptions.BACKGROUND_HEIGHT, HudOptions.defaultNumber(id, HudOptions.BACKGROUND_HEIGHT, 18)) : 13;
        return new float[]{w, h};
    }

    private String shown(ClientFeature feature) {
        return flag(feature.getId(), HudOptions.BRACKETS) ? "[" + label(feature) + "]" : label(feature);
    }

    public float[] bounds(ClientFeature feature, ScaledResolution resolution) {
        String id = feature.getId(); float scale = scale(id);
        float[] size = size(feature);
        float w = size[0] * scale, h = size[1] * scale;
        float sw = resolution.getScaledWidth(), sh = resolution.getScaledHeight();
        float x = model.number(id, "x", Float.parseFloat(UiModel.defaultOption(id, "x"))), y = model.number(id, "y", Float.parseFloat(UiModel.defaultOption(id, "y")));
        HudAnchor anchor = anchor(id);
        if (anchor != null) { x += anchor.originX(sw, w); y += anchor.originY(sh, h); }
        if (feature instanceof HudElement) y += ((HudElement)feature).visualOffsetY() * scale;
        x = Math.max(0, Math.min(sw - w, x));
        y = Math.max(0, Math.min(sh - h, y));
        return new float[]{x, y, w, h};
    }

    public HudAnchor anchor(String id) { return HudAnchor.fromId(model.value(id, "anchor", UiModel.defaultOption(id, "anchor"))); }

    public void setPosition(ClientFeature feature, float x, float y, ScaledResolution resolution) {
        String id = feature.getId(); float[] b = bounds(feature, resolution);
        HudAnchor anchor = anchor(id);
        if (anchor != null) { x -= anchor.originX(resolution.getScaledWidth(), b[2]); y -= anchor.originY(resolution.getScaledHeight(), b[3]); }
        if (feature instanceof HudElement) y -= ((HudElement)feature).visualOffsetY() * scale(feature.getId());
        model.set(id, "x", "" + x); model.set(id, "y", "" + y);
    }

    public void reanchor(ClientFeature feature, ScaledResolution resolution) {
        float[] b = bounds(feature, resolution);
        HudAnchor anchor = HudAnchor.at(b[0] + b[2] / 2, b[1] + b[3] / 2, resolution.getScaledWidth(), resolution.getScaledHeight());
        model.set(feature.getId(), "anchor", anchor.id);
        setPosition(feature, b[0], b[1], resolution);
    }
    public void renderHud(ScaledResolution resolution) {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.thePlayer == null || (mc.gameSettings.showDebugInfo && model.flag("global", "hideDebug", true))) return;
        boolean preview=preview();
        for (ClientFeature feature : features) {
            if (!preview && feature instanceof HudElement && ((HudElement)feature).nativeDraw()) continue;
            if (isShown(feature)) renderPrepared(feature, resolution);
        }
    }

    public void render(ClientFeature feature, ScaledResolution resolution) {
        if (feature instanceof HudElement) ((HudElement)feature).visible(preview());
        renderPrepared(feature,resolution);
    }

    private void renderPrepared(ClientFeature feature, ScaledResolution resolution) {
        com.cheatbreaker.client.kazerui.event.UiOverlayMetrics.dimensions(resolution);
        Minecraft mc = Minecraft.getMinecraft(); String id = feature.getId();
        if (feature instanceof HudElement) {
            HudElement hud=(HudElement)feature;
            RenderLayout layout=renderLayouts.get(feature);
            if (layout==null) {layout=new RenderLayout();renderLayouts.put(feature,layout);}
            if (layout.revision!=model.revision) {
                layout.x=model.number(id,"x",Float.parseFloat(UiModel.defaultOption(id,"x")));
                layout.y=model.number(id,"y",Float.parseFloat(UiModel.defaultOption(id,"y")));
                layout.anchor=anchor(id);layout.revision=model.revision;
            }
            float scale=hud.scale(),w=hud.width()*scale,h=hud.height()*scale;
            float x=layout.x,y=layout.y,sw=resolution.getScaledWidth(),sh=resolution.getScaledHeight();
            if (layout.anchor!=null) {x+=layout.anchor.originX(sw,w);y+=layout.anchor.originY(sh,h);}
            y+=hud.visualOffsetY()*scale;
            x=Math.max(0,Math.min(sw-w,x));y=Math.max(0,Math.min(sh-h,y));
            GlStateManager.pushMatrix();
            try {
                if (!hud.screenSpace()) { GlStateManager.translate(x,y,0); GlStateManager.scale(scale,scale,1); }
                GlStateManager.color(1,1,1,1);hud.render(preview());
            } finally {GlStateManager.color(1,1,1,1);GlStateManager.popMatrix();}
            return;
        }
        float[] b = bounds(feature, resolution);
        float[] size = size(feature);
        GlStateManager.pushMatrix(); GlStateManager.translate(b[0], b[1], 0); GlStateManager.scale(scale(id), scale(id), 1);
        if (flag(id, HudOptions.BACKGROUND)) KazerGfx.rect(0, 0, size[0], size[1], color(id, HudOptions.BACKGROUND_COLOR));
        if (flag(id, HudOptions.BORDER)) {
            float t = Math.max(.5f, Math.min(3, model.number(id, HudOptions.BORDER_THICKNESS, HudOptions.defaultNumber(id, HudOptions.BORDER_THICKNESS, .5f))));
            KazerGfx.outline(0, 0, size[0], size[1], t, color(id, HudOptions.BORDER_COLOR));
        }
        boolean shadow = flag(id, HudOptions.TEXT_SHADOW), brackets = flag(id, HudOptions.BRACKETS);
        String text = label(feature);
        float x = (size[0] - mc.fontRendererObj.getStringWidth(shown(feature))) / 2, y = (size[1] - 8) / 2;
        if (brackets) x = mc.fontRendererObj.drawString("[", x, y, color(id, HudOptions.BRACKET_COLOR), shadow);
        x = mc.fontRendererObj.drawString(text, x, y, color(id, HudOptions.TEXT_COLOR), shadow);
        if (brackets) mc.fontRendererObj.drawString("]", x, y, color(id, HudOptions.BRACKET_COLOR), shadow);
        GlStateManager.popMatrix();
    }

    private int color(String id, String option) {
        String fallback = HudOptions.defaultValue(id, option);
        int argb = color(id, option, fallback == null ? 0xFFFFFFFF : (int)Long.parseLong(fallback, 16));
        OptionCatalog.Node node = OptionCatalog.find(id, option);
        if (!model.flag(id, option + "Chroma", node != null && node.chromaOn)) return argb;
        float speed = Math.max(1, Math.min(100, model.number(id, option + "ChromaSpeed", 40)));
        float hue = (System.currentTimeMillis() % (long)(200000 / speed)) / (200000f / speed);
        return argb & 0xFF000000 | java.awt.Color.HSBtoRGB(hue, 1, 1) & 0xFFFFFF;
    }
    private int color(String id, String option, int fallback) {
        try { return (int)Long.parseLong(model.value(id, option, String.format("%08X", fallback)), 16); }
        catch (NumberFormatException e) { return fallback; }
    }
}
