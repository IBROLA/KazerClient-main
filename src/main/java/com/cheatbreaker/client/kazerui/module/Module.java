package com.cheatbreaker.client.kazerui.module;

import com.cheatbreaker.client.kazerui.gui.ui.KazerLang;
import com.cheatbreaker.client.kazerui.gui.ui.OptionCatalog;
import com.cheatbreaker.client.kazerui.module.hud.HudElement;
import com.cheatbreaker.client.kazerui.module.setting.BoolSetting;
import com.cheatbreaker.client.kazerui.module.setting.ChoiceSetting;
import com.cheatbreaker.client.kazerui.module.setting.ColorSetting;
import com.cheatbreaker.client.kazerui.module.setting.KeySetting;
import com.cheatbreaker.client.kazerui.module.setting.NumberSetting;
import com.cheatbreaker.client.kazerui.module.setting.Setting;
import com.cheatbreaker.client.kazerui.module.setting.TextSetting;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import net.minecraft.client.Minecraft;

public abstract class Module {
    public final String id;
    private final String storageKey;
    private final boolean defaultEnabled;
    private final List<Setting<?>> settings = new ArrayList<Setting<?>>();
    private final List<HudElement> huds = new ArrayList<HudElement>();
    private final List<Module> children = new ArrayList<Module>();
    private Module parent;

    private String label;

    protected boolean childRowsLast;

    private java.util.function.BooleanSupplier rowShown;

    protected boolean keybindAllowed = true;

    protected Module(String id, boolean defaultEnabled) {
        this.id = id;
        this.storageKey = id.toLowerCase(Locale.ROOT);
        this.defaultEnabled = defaultEnabled;
    }

    public String key() { return storageKey; }

    private com.cheatbreaker.client.kazerui.gui.ui.UiModel enabledModel;
    private long enabledRevision = Long.MIN_VALUE;
    private boolean ownEnabled;
    private boolean ownRenderHud=true;

    public boolean isEnabled() {
        if (parent != null && !parent.isEnabled()) return false;
        com.cheatbreaker.client.kazerui.gui.ui.UiModel model = ModuleManager.model();
        if (model == null) return defaultEnabled;
        if (enabledModel != model || enabledRevision != model.revision) {
            ownEnabled = Boolean.parseBoolean(ModuleManager.store(storageKey, "enabled", defaultEnabled ? "true" : "false"));
            ownRenderHud=Boolean.parseBoolean(ModuleManager.store(storageKey,"renderHud","true"));
            enabledModel = model; enabledRevision = model.revision;
        }
        return ownEnabled;
    }
    public boolean isHudEnabled() { return isEnabled() && ownRenderHud && (parent==null || parent.isHudEnabled()); }

    public void setEnabled(boolean on) {
        boolean was = isEnabled();
        ModuleManager.put(key(), "enabled", "" + on);
        if (was != isEnabled()) { if (on) onEnable(); else onDisable(); }
    }

    public boolean defaultEnabled() { return defaultEnabled; }

    protected void onEnable() {}
    protected void onDisable() {}

    /** Called explicitly by the native host, not by reflection or Forge injection. */
    public void tick(com.cheatbreaker.client.kazerui.event.UiTick.ClientTickEvent event) { }

    protected void layout(Page page) {}

    public List<Setting<?>> settings() { return Collections.unmodifiableList(settings); }
    public List<HudElement> huds() { return Collections.unmodifiableList(huds); }
    public List<Module> children() { return Collections.unmodifiableList(children); }
    public Module parent() { return parent; }

    public <S extends Setting<?>> S add(S setting) {
        setting.attach(this);
        settings.add(setting);
        return setting;
    }

    public void removeSetting(Setting<?> setting) { settings.remove(setting); }

    public BoolSetting bool(String key, boolean def) { return add(new BoolSetting(key, def)); }
    public NumberSetting integer(String key, int def, int min, int max) { return add(NumberSetting.integer(key, def, min, max)); }
    public NumberSetting decimal(String key, float def, float min, float max) { return add(NumberSetting.decimal(key, def, min, max)); }
    public ColorSetting color(String key, int argb) { return add(new ColorSetting(key, argb)); }
    public <E extends Enum<E> & ChoiceSetting.Option> ChoiceSetting<E> choice(String key, E def) { return add(new ChoiceSetting<E>(key, def)); }
    public KeySetting keybind(String key) { return add(new KeySetting(key)); }
    public KeySetting keybind(String key, String def) { return add(new KeySetting(key, def, false)); }
    public KeySetting keyCombo(String key) { return add(new KeySetting(key, "NONE", true)); }
    public TextSetting text(String key) { return add(new TextSetting(key)); }

    protected <H extends HudElement> H hud(H element) {
        huds.add(element);
        return element;
    }

    protected <M extends Module> M child(M module, String label) {
        Module m = module;
        m.parent = this;
        m.label = label;
        children.add(module);
        return module;
    }

    protected <M extends Module> M addChild(M module, String label) {
        child(module, label);
        ModuleManager.attached(module);
        return module;
    }

    protected void removeChild(Module module) {
        if (!children.remove(module)) return;
        ModuleManager.detached(module);
        module.parent = null;
    }

    public OptionCatalog.Page page() {
        Page page = new Page();
        layout(page);
        for (HudElement hud : huds) hud.layout(page);
        for (Module child : children) {
            if (child.label != null) page.sectionPage(child.label).rows.add(child);
            else if (childRowsLast) page.lastRows.add(child);
            else page.rows.add(child);
        }
        boolean hud = !huds.isEmpty();
        for (Module child : children) hud |= !child.huds.isEmpty();
        return new OptionCatalog.Page(hud, keybindAllowed && parent == null, page.nodes());
    }

    OptionCatalog.Node rowNode() {
        Map<String, Object> fields = new HashMap<String, Object>();
        fields.put("type", "child");
        fields.put("key", id);
        fields.put("hud", !huds.isEmpty());
        fields.put("default", "" + defaultEnabled);
        final java.util.function.BooleanSupplier shown = rowShown;
        return OptionCatalog.Node.of(fields, page().options, shown == null ? null : new java.util.function.BooleanSupplier() {
            @Override public boolean getAsBoolean() { return !shown.getAsBoolean(); }
        });
    }

    protected void rowShownWhen(java.util.function.BooleanSupplier rule) { rowShown = rule; }

    protected String lang(String key, Object... args) { return KazerLang.get("features." + id + ".info", key, args); }

    protected static Minecraft mc() { return Minecraft.getMinecraft(); }
}
