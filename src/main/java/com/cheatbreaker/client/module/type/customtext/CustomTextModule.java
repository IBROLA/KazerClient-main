package com.cheatbreaker.client.module.type.customtext;

import com.cheatbreaker.client.event.type.GuiDrawEvent;
import com.cheatbreaker.client.event.type.RenderPreviewEvent;
import com.cheatbreaker.client.module.AbstractModule;
import com.cheatbreaker.client.module.CompositeChildUnit;
import com.cheatbreaker.client.module.CompositeHudModule;
import com.cheatbreaker.client.ui.module.CBAnchorHelper;
import com.cheatbreaker.client.ui.module.CBGuiAnchor;
import com.cheatbreaker.client.ui.module.CBModulePlaceGui;
import com.cheatbreaker.client.ui.module.CBModulesGui;
import com.cheatbreaker.client.ui.module.CBPositionEnum;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.texture.TextureUtil;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.opengl.GL11;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Multi-instance single-line text / local-image HUD module. Instances live in
 * {@link CustomTextState} (management-list order; the top row renders last so it sits on top),
 * persist through {@link CustomTextStateCodec}, and are dragged in the editor as dynamic
 * {@link CompositeChildUnit}s. Image channels are implicit: content that is an absolute local
 * image path renders the file; every failure renders the 16x16 missing-image instead.
 */
public final class CustomTextModule extends AbstractModule implements CompositeHudModule {
    private static final ResourceLocation MISSING_IMAGE = new ResourceLocation("textures/misc/unknown_pack.png");

    private final CustomTextState state = new CustomTextState();
    private final CustomTextImageCache imageCache = new CustomTextImageCache();
    private final Map<String, CustomTextChildUnit> childUnits = new LinkedHashMap<>();
    private boolean syncing;

    public CustomTextModule() {
        super("Custom Text");
        this.setDefaultAnchor(CBGuiAnchor.LEFT_TOP);
        this.setDefaultTranslations(0.0F, 24.0F);
        this.setDefaultState(true);
        // Per-instance scales supersede the module-level slider (same contract as Item Count).
        this.getSettingsList().removeIf(setting -> "Scale".equals(setting.getLabel()));
        CustomTextItem seed = this.state.add();
        seed.setContent("Kazer Client");
        seed.anchor = "LEFT_TOP";
        seed.yTranslation = 24.0F;
        this.childUnits.put(seed.id, new CustomTextChildUnit(seed));
        this.setPreviewLabel("Text", 1.5F);
        this.addEvent(GuiDrawEvent.class, this::renderHud);
        this.addEvent(RenderPreviewEvent.class, this::renderPreview);
    }

    public CustomTextState getState() {
        return this.state;
    }

    /** 序号 = 管理页当前行位置（越界返回 -1），codec 不存；命名/画布/子页标题 3 处共用。 */
    public int indexOf(CustomTextItem item) {
        return this.state.items.indexOf(item);
    }

    /** 序号按当前列表位置换算（1-based），codec 不存。 */
    public int positionOf(CustomTextItem item) {
        return this.state.items.indexOf(item);
    }

    public String displayName(CustomTextItem item) {
        return "Custom Text " + (positionOf(item) + 1) + ": " + item.content;
    }

    private String nextPositionLabel(CustomTextItem item) {
        int idx = this.state.items.indexOf(item);
        return idx >= 0 ? String.valueOf(idx + 1) : "?";
    }

    public CustomTextImageCache getImageCache() {
        return this.imageCache;
    }

    // --- instance management (management-page API) ---

    public CustomTextItem addInstance() {
        CustomTextItem item = this.state.add();
        if (item == null) {
            return null;
        }
        this.childUnits.put(item.id, new CustomTextChildUnit(item));
        attachImage(item.content);
        return item;
    }

    public boolean removeInstance(String id) {
        CustomTextItem item = this.state.byId(id);
        if (item == null) {
            return false;
        }
        detachImage(item.content);
        this.state.remove(id);
        this.childUnits.remove(id);
        return true;
    }

    public void setInstanceContent(String id, String content) {
        CustomTextItem item = this.state.byId(id);
        if (item == null) {
            return;
        }
        String previous = item.content;
        item.setContent(content);
        if (!previous.equals(item.content)) {
            detachImage(previous);
            attachImage(item.content);
        }
    }

    /** 子设置页设置定义，六行（CONTENT/STATUS/SCALE/SHOW BACKGROUND/TEXT COLOR/BACKGROUND COLOR）。 */
    public java.util.List<com.cheatbreaker.client.config.Setting> settingsForChild(com.cheatbreaker.client.module.AbstractModule childUnit) {
        java.util.List<com.cheatbreaker.client.config.Setting> settings = new java.util.ArrayList<>();
        if (!(childUnit instanceof CustomTextChildUnit)) return settings;
        CustomTextItem item = ((CustomTextChildUnit) childUnit).getItem();
        settings.add(new com.cheatbreaker.client.config.Setting("Content")
                .setValue(item.content)
                .onChange(v -> this.setInstanceContent(item.id, (String) v)));
        settings.add(new com.cheatbreaker.client.config.Setting("Status")
                .setValue(item.enabled ? "ON" : "OFF")
                .acceptedValues("ON", "OFF")
                .onChange(v -> this.setInstanceEnabled(item.id, "ON".equals(v))));
        settings.add(new com.cheatbreaker.client.config.Setting("Scale")
                .setValue(item.scale)
                .setMinMax(0.5F, 1.5F)
                .onChange(v -> this.runOnItem(item.id, t -> t.scale = ((Number) v).floatValue())));
        settings.add(new com.cheatbreaker.client.config.Setting("Show Background")
                .setValue(item.showBackground ? "ON" : "OFF")
                .acceptedValues("ON", "OFF")
                .onChange(v -> this.runOnItem(item.id, t -> t.showBackground = "ON".equals(v))));
        settings.add(new com.cheatbreaker.client.config.Setting("Text Color")
                .setValue(item.color)
                .setMinMax(Integer.MIN_VALUE, Integer.MAX_VALUE)
                .onChange(v -> this.runOnItem(item.id, t -> t.color = (Integer) v)));
        settings.add(new com.cheatbreaker.client.config.Setting("Background Color")
                .setValue(item.backgroundColor)
                .setMinMax(Integer.MIN_VALUE, Integer.MAX_VALUE)
                .onChange(v -> this.runOnItem(item.id, t -> t.backgroundColor = (Integer) v)));
        return settings;
    }

    private void runOnItem(String id, java.util.function.Consumer<CustomTextItem> writer) {
        CustomTextItem item = this.state.byId(id);
        if (item != null) {
            writer.accept(item);
        }
    }

    public void setInstanceEnabled(String id, boolean enabled) {
        CustomTextItem item = this.state.byId(id);
        if (item != null) {
            // Two-state editor contract: show enables, hide disables.
            item.enabled = enabled;
            item.renderHud = enabled;
        }
    }

    public void moveInstance(String id, int targetIndex) {
        this.state.move(id, targetIndex);
    }

    private void attachImage(String content) {
        if (CustomTextImageSupport.isImageReference(content)) {
            this.imageCache.acquire(content, System.currentTimeMillis());
        }
    }

    private void detachImage(String content) {
        if (!CustomTextImageSupport.isImageReference(content)) {
            return;
        }
        CustomTextImageCache.Entry dropped = this.imageCache.release(content);
        if (dropped != null && dropped.textureId >= 0) {
            TextureUtil.deleteTexture(dropped.textureId);
        }
    }

    // --- persistence ---

    public String serializeState() {
        return CustomTextStateCodec.encode(this.state);
    }

    public void loadState(String encoded) {
        for (CustomTextItem item : this.state.items) {
            detachImage(item.content);
        }
        CustomTextState decoded = CustomTextStateCodec.decode(encoded);
        this.state.items.clear();
        this.state.items.addAll(decoded.items);
        this.state.setNextId(decoded.getNextId());
        this.childUnits.clear();
        for (CustomTextItem item : this.state.items) {
            this.childUnits.put(item.id, new CustomTextChildUnit(item));
            attachImage(item.content);
        }
    }

    public void resetState() {
        loadState("");
    }

    @Override
    public List<AbstractModule> getChildHudUnits() {
        return new ArrayList<>(this.childUnits.values());
    }

    public CustomTextChildUnit childUnitFor(String id) {
        return this.childUnits.get(id);
    }

    // --- rendering ---

    private void renderHud(GuiDrawEvent event) {
        if (!this.isRenderHud() || this.minecraft.thePlayer == null
                || this.minecraft.currentScreen instanceof CBModulesGui) {
            return;
        }
        if (this.minecraft.currentScreen instanceof CBModulePlaceGui) {
            // 放置预览：只画正在放置的实例。PlaceGui 每帧经 child unit 把鼠标位置写回
            // item.anchor/x/y，renderInstance 走同一条渲染路径 → 预览位置 = 落定后实际位置。
            CustomTextItem placing = this.placementPreviewItem(this.minecraft.currentScreen);
            if (placing != null) {
                this.renderInstance(event, placing);
            }
            return;
        }
        this.renderInstances(event);
    }

    /** 放置预览目标：仅当屏幕是本模块某个实例的 PlaceGui 且实例启用时返回该 item。 */
    CustomTextItem placementPreviewItem(net.minecraft.client.gui.GuiScreen screen) {
        if (!(screen instanceof CBModulePlaceGui)) {
            return null;
        }
        AbstractModule placing = ((CBModulePlaceGui) screen).getModule();
        if (!(placing instanceof CustomTextChildUnit)) {
            return null;
        }
        CustomTextChildUnit unit = (CustomTextChildUnit) placing;
        if (unit.getCompositeParent() != this) {
            return null;
        }
        CustomTextItem item = unit.getItem();
        return item.enabled && item.renderHud ? item : null;
    }

    private void renderPreview(RenderPreviewEvent event) {
        if (!this.isRenderHud() || !(this.minecraft.currentScreen instanceof CBModulesGui)) {
            return;
        }
        this.renderInstances(event);
    }

    private void renderInstances(GuiDrawEvent event) {
        // Management list top = drawn last = visually on top.
        for (int i = this.state.items.size() - 1; i >= 0; i--) {
            CustomTextItem item = this.state.items.get(i);
            if (item.enabled && item.renderHud) {
                this.renderInstance(event, item);
            }
        }
    }

    private void renderInstance(GuiDrawEvent event, CustomTextItem item) {
        boolean image = CustomTextImageSupport.isImageReference(item.content);
        float width;
        float height;
        CustomTextImageCache.Entry imageEntry = null;
        if (image) {
            imageEntry = this.imageCache.entryFor(item.content);
            if (imageEntry == null) {
                imageEntry = lazyAttach(item);
            }
            java.awt.image.BufferedImage decoded = this.imageCache.lookup(item.content, System.currentTimeMillis());
            if (decoded != null) {
                width = decoded.getWidth();
                height = decoded.getHeight();
            } else {
                width = 16;
                height = 16;
            }
        } else {
            width = this.minecraft.fontRendererObj.getStringWidth(item.content);
            height = this.minecraft.fontRendererObj.FONT_HEIGHT;
        }
        CustomTextChildUnit unit = this.childUnits.get(item.id);
        this.setDimensions(width, height);
        if (unit != null) {
            unit.width = width;
            unit.height = height;
            unit.syncScale();
        }

        this.syncing = true;
        try {
            super.setAnchor(parseAnchor(item.anchor));
            super.setTranslations(item.xTranslation, item.yTranslation);
            this.scale.setValue(item.scale, false);
        } finally {
            this.syncing = false;
        }

        GL11.glPushMatrix();
        try {
            super.scaleAndTranslate(event.getResolution());
            if (!image) {
                if (item.showBackground) {
                    cn.kazer.client189.compat.LegacyGui.drawRect(-2, -2, width + 2, height + 2, item.backgroundColor);
                    this.minecraft.fontRendererObj.drawString(item.content, 0, 0, item.color);
                } else {
                    // Shadow/Background 二选一：没有背景时用阴影。
                    this.minecraft.fontRendererObj.drawStringWithShadow(item.content, 0, 0, item.color);
                }
            } else if (imageEntry != null && imageEntry.image != null) {
                if (imageEntry.textureId < 0) {
                    imageEntry.textureId = TextureUtil.uploadTextureImageAllocate(
                            TextureUtil.glGenTextures(), imageEntry.image, false, false);
                }
                cn.kazer.client189.compat.LegacyGL.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
                cn.kazer.client189.compat.LegacyGL.glEnable(GL11.GL_BLEND);
                cn.kazer.client189.compat.LegacyGL.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
                cn.kazer.client189.compat.LegacyGL.glBindTexture(GL11.GL_TEXTURE_2D, imageEntry.textureId);
                Gui.drawModalRectWithCustomSizedTexture(0, 0, 0.0F, 0.0F, (int) width, (int) height, width, height);
            } else {
                cn.kazer.client189.compat.LegacyGL.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
                cn.kazer.client189.compat.LegacyGL.glEnable(GL11.GL_BLEND);
                cn.kazer.client189.compat.LegacyGL.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
                this.minecraft.getTextureManager().bindTexture(MISSING_IMAGE);
                Gui.drawModalRectWithCustomSizedTexture(0, 0, 0.0F, 0.0F, 16, 16, 16.0F, 16.0F);
            }
        } finally {
            GL11.glPopMatrix();
        }
    }

    private CustomTextImageCache.Entry lazyAttach(CustomTextItem item) {
        this.imageCache.acquire(item.content, System.currentTimeMillis());
        return this.imageCache.entryFor(item.content);
    }

    private static CBGuiAnchor parseAnchor(String anchor) {
        try {
            return CBGuiAnchor.valueOf(anchor);
        } catch (Exception ignored) {
            return CBGuiAnchor.MIDDLE_MIDDLE;
        }
    }

    private static String previewText(String content) {
        return content.length() <= 12 ? content : content.substring(0, 12) + "...";
    }

    /**
     * Editor drag unit for one text/image instance. Position state lives on the
     * {@link CustomTextItem}; the editor X disables the instance (two-state contract), deletion
     * only exists on the module management page.
     */
    public final class CustomTextChildUnit extends AbstractModule implements CompositeChildUnit {
        private final CustomTextItem item;
        private PlacementSettledHandler onPlacementSettled;

        CustomTextChildUnit(CustomTextItem item) {
            super("Custom Text " + CustomTextModule.this.nextPositionLabel(item) + ": " + previewText(item.content));
            this.item = item;
            this.isEditable = true;
            this.scale.setValue(item.scale, false);
            this.scale.onChange(value -> {
                if (!CustomTextModule.this.syncing) {
                    this.item.scale = (Float) value;
                }
            });
        }

        @Override
        public String getName() {
            return CustomTextModule.this.displayName(this.item);
        }

        public CustomTextItem getItem() {
            return this.item;
        }

        void syncScale() {
            this.scale.setValue(this.item.scale, false);
        }

        @Override
        public AbstractModule getCompositeParent() {
            return CustomTextModule.this;
        }

        @Override
        public CBGuiAnchor getGuiAnchor() {
            return parseAnchor(this.item.anchor);
        }

        @Override
        public void setAnchor(CBGuiAnchor anchor) {
            this.item.anchor = anchor == null ? CBGuiAnchor.MIDDLE_MIDDLE.name() : anchor.name();
        }

        @Override
        public CBPositionEnum getPosition() {
            return CBAnchorHelper.getHorizontalPositionEnum(getGuiAnchor());
        }

        @Override
        public float getXTranslation() {
            return this.item.xTranslation;
        }

        @Override
        public float getYTranslation() {
            return this.item.yTranslation;
        }

        @Override
        public void setTranslations(float x, float y) {
            this.item.xTranslation = x;
            this.item.yTranslation = y;
        }

        @Override
        public void setXTranslation(float x) {
            this.item.xTranslation = x;
        }

        @Override
        public void setYTranslation(float y) {
            this.item.yTranslation = y;
        }

        @Override
        public boolean isEnabled() {
            return CustomTextModule.this.isRenderHud() && this.item.enabled;
        }

        @Override
        public boolean isRenderHud() {
            return CustomTextModule.this.isRenderHud() && this.item.renderHud;
        }

        @Override
        public void setRenderHud(boolean renderHud) {
            this.item.renderHud = renderHud;
        }

        @Override
        public void setState(boolean state) {
            this.item.enabled = state;
            this.item.renderHud = state;
        }

        @Override
        public float[] getScaledPoints(ScaledResolution resolution, boolean relative) {
            float scale = (Float) this.scale.getValue();
            float scaledWidth = this.width * scale;
            float scaledHeight = this.height * scale;
            CBGuiAnchor anchor = getGuiAnchor();
            float x;
            float y;
            switch (anchor) {
                case LEFT_TOP: default:
                    x = 2.0F; y = 2.0F; break;
                case LEFT_MIDDLE:
                    x = 2.0F; y = (resolution.getScaledHeight() / 2.0F) - scaledHeight / 2.0F; break;
                case LEFT_BOTTOM:
                    x = 2.0F; y = resolution.getScaledHeight() - scaledHeight - 2.0F; break;
                case MIDDLE_TOP:
                    x = (resolution.getScaledWidth() / 2.0F) - scaledWidth / 2.0F; y = 2.0F; break;
                case MIDDLE_MIDDLE:
                    x = (resolution.getScaledWidth() / 2.0F) - scaledWidth / 2.0F;
                    y = (resolution.getScaledHeight() / 2.0F) - scaledHeight / 2.0F; break;
                case MIDDLE_BOTTOM_LEFT:
                    x = (resolution.getScaledWidth() / 2.0F) - scaledWidth;
                    y = resolution.getScaledHeight() - scaledHeight - 2.0F; break;
                case MIDDLE_BOTTOM_RIGHT:
                    x = resolution.getScaledWidth() / 2.0F;
                    y = resolution.getScaledHeight() - scaledHeight - 2.0F; break;
                case RIGHT_TOP:
                    x = resolution.getScaledWidth() - scaledWidth - 2.0F; y = 2.0F; break;
                case RIGHT_MIDDLE:
                    x = resolution.getScaledWidth() - scaledWidth;
                    y = (resolution.getScaledHeight() / 2.0F) - scaledHeight / 2.0F; break;
                case RIGHT_BOTTOM:
                    x = resolution.getScaledWidth() - scaledWidth;
                    y = resolution.getScaledHeight() - scaledHeight - 2.0F; break;
            }
            return new float[]{(x + (relative ? getXTranslation() : 0.0F)) / scale,
                    (y + (relative ? getYTranslation() : 0.0F)) / scale};
        }

        @Override
        public void scaleAndTranslate(ScaledResolution resolution) {
            scaleAndTranslate(resolution, this.width, this.height);
        }

        @Override
        public void scaleAndTranslate(ScaledResolution resolution, float width, float height) {
            float scale = (Float) this.scale.getValue();
            GL11.glScalef(scale, scale, scale);
            float scaledWidth = width * scale;
            float scaledHeight = height * scale;
            CBGuiAnchor anchor = getGuiAnchor();
            float x;
            float y;
            switch (anchor) {
                case LEFT_TOP: default:
                    x = 2.0F; y = 2.0F; break;
                case LEFT_MIDDLE:
                    x = 2.0F; y = (resolution.getScaledHeight() / 2.0F) - scaledHeight / 2.0F; break;
                case LEFT_BOTTOM:
                    x = 2.0F; y = resolution.getScaledHeight() - scaledHeight - 2.0F; break;
                case MIDDLE_TOP:
                    x = (resolution.getScaledWidth() / 2.0F) - scaledWidth / 2.0F; y = 2.0F; break;
                case MIDDLE_MIDDLE:
                    x = (resolution.getScaledWidth() / 2.0F) - scaledWidth / 2.0F;
                    y = (resolution.getScaledHeight() / 2.0F) - scaledHeight / 2.0F; break;
                case MIDDLE_BOTTOM_LEFT:
                    x = (resolution.getScaledWidth() / 2.0F) - scaledWidth;
                    y = resolution.getScaledHeight() - scaledHeight - 2.0F; break;
                case MIDDLE_BOTTOM_RIGHT:
                    x = resolution.getScaledWidth() / 2.0F;
                    y = resolution.getScaledHeight() - scaledHeight - 2.0F; break;
                case RIGHT_TOP:
                    x = resolution.getScaledWidth() - scaledWidth - 2.0F; y = 2.0F; break;
                case RIGHT_MIDDLE:
                    x = resolution.getScaledWidth() - scaledWidth;
                    y = (resolution.getScaledHeight() / 2.0F) - scaledHeight / 2.0F; break;
                case RIGHT_BOTTOM:
                    x = resolution.getScaledWidth() - scaledWidth;
                    y = resolution.getScaledHeight() - scaledHeight - 2.0F; break;
            }
            GL11.glTranslatef(x / scale, y / scale, 0.0F);
            GL11.glTranslatef(getXTranslation() / scale, getYTranslation() / scale, 0.0F);
        }
        void clearPlacementTracking() {
            this.onPlacementSettled = null;
        }

        void onPlacementSettled() {
            if (this.onPlacementSettled != null) {
                this.onPlacementSettled.run();
                this.onPlacementSettled = null;
            }
        }

        public void setOnPlacementSettled(PlacementSettledHandler handler) {
            this.onPlacementSettled = handler;
        }
    }

    /** Placement 落定监听入口；模块级非静态接口以避免 Inner Class 规则限制。 */
    public interface PlacementSettledHandler {
        void run();
    }
}
