package com.cheatbreaker.client.kazerui.module.hud;

import com.cheatbreaker.client.kazerui.feature.ClientFeature;
import com.cheatbreaker.client.kazerui.feature.HudAnchor;
import com.cheatbreaker.client.kazerui.gui.ui.UiModel;
import com.cheatbreaker.client.kazerui.module.Module;
import com.cheatbreaker.client.kazerui.module.ModuleManager;
import com.cheatbreaker.client.kazerui.module.Page;
import com.cheatbreaker.client.kazerui.module.setting.NumberSetting;

public abstract class HudElement implements ClientFeature {
    public final Module module;
    public final HudAnchor anchor;
    private UiModel anchorModel;
    private long anchorRevision=Long.MIN_VALUE;
    private HudAnchor cachedAnchor;
    private final float defaultX, defaultY;

    public final NumberSetting scale;
    private float width, height;

    protected HudElement(Module module, float x, float y, HudAnchor anchor) {
        this(module, x, y, anchor, 0.25f, 5.0f);
    }

    protected HudElement(Module module, float x, float y, HudAnchor anchor, float minScale, float maxScale) {
        this.module = module;
        this.defaultX = x; this.defaultY = y; this.anchor = anchor;
        this.scale = module.add(NumberSetting.decimal("scale", 1.0f, minScale, maxScale));
    }

    @Override public String getId() { return module.key(); }

    public HudAnchor currentAnchor() {
        UiModel model = ModuleManager.model();
        if (model != null) {
            if(model!=anchorModel || model.revision!=anchorRevision) {
                HudAnchor current = HudAnchor.fromId(model.value(getId(), "anchor", UiModel.defaultOption(getId(), "anchor")));
                cachedAnchor=current==null?anchor:current;anchorModel=model;anchorRevision=model.revision;
            }
            return cachedAnchor;
        }
        return anchor;
    }

    public float defaultX() { return defaultX; }
    public float defaultY() { return defaultY; }
    @Override public String getText() { return ""; }

    public abstract boolean visible(boolean preview);

    public abstract void render(boolean preview);

    protected void size(float w, float h) { width = w; height = h; }
    public float width() { return width; }
    public float height() { return height; }

    public float scale() { return scale.value(); }

    public boolean screenSpace() { return false; }
    /** Native chat draws outside the module overlay; only its editor preview belongs here. */
    public boolean nativeDraw() { return false; }
    /** Local visual translation shared by editor bounds and the native draw/input plane. */
    public float visualOffsetY() { return 0; }

    public boolean editable() { return true; }

    public void layout(Page page) { page.addFirst(scale); }
}
