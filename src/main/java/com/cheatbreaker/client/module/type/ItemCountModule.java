package com.cheatbreaker.client.module.type;

import com.cheatbreaker.client.config.Setting;
import com.cheatbreaker.client.event.type.GuiDrawEvent;
import com.cheatbreaker.client.event.type.RenderPreviewEvent;
import com.cheatbreaker.client.module.AbstractModule;
import com.cheatbreaker.client.ui.module.CBGuiAnchor;
import com.cheatbreaker.client.ui.module.CBModulePlaceGui;
import com.cheatbreaker.client.ui.module.CBModulesGui;
import net.minecraft.client.renderer.RenderHelper;
import cn.kazer.client189.compat.RenderItem;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;

import java.util.List;

public final class ItemCountModule extends AbstractModule implements com.cheatbreaker.client.module.CompositeHudModule {
    private static final int POT_DAMAGE = 24581;
    private static final int ENCHANTED_APPLE_DAMAGE = 1;
    private final ItemCountState state = new ItemCountState();
    private ItemCountType activeType = ItemCountType.HEALING_POT;
    private boolean syncing;
    private final RenderItem renderItem = new RenderItem();
    private final java.util.List<AbstractModule> childUnits = new java.util.ArrayList<>();

    public ItemCountModule() {
        super("Items Counter");
        this.setDefaultAnchor(CBGuiAnchor.RIGHT_TOP);
        this.setDefaultTranslations(-1.0F, 11.0F);
        this.setDefaultState(true);

        for (ItemCountType type : ItemCountType.values()) {
            addChildGroup(type);
            this.childUnits.add(new ChildUnit(type));
        }
        // The module-level "Scale" setting (added by AbstractModule) is superseded by the
        // per-child Scale settings; remove it from the panel so only the per-child sliders
        // remain. The inherited scale field is still used as the active-child mirror during
        // rendering and editor interaction.
        this.getSettingsList().removeIf(setting -> "Scale".equals(setting.getLabel()));
        this.setPreviewLabel("0 Pot", 1.5F);
        this.scale.onChange(value -> {
            if (!this.syncing) {
                child(this.activeType).scale = ((Number) value).floatValue();
            }
        });
        this.addEvent(GuiDrawEvent.class, this::renderHud);
        this.addEvent(RenderPreviewEvent.class, this::renderPreview);
        this.syncParentFromActive();
    }

    private void addChildGroup(ItemCountType type) {
        Setting enable = new Setting(this, displayName(type)).setValue(type == ItemCountType.HEALING_POT)
                .setDisplayLabel(childSettingDisplayLabel(displayName(type), type))
                .onChange(value -> {
                    boolean enabled = (Boolean) value;
                    ItemCountChildState child = child(type);
                    boolean wasEnabled = child.enabled;
                    child.enabled = enabled;
                    if (enabled && !wasEnabled) {
                        child.renderHud = true;
                    }
                });
        Setting displayMode = new Setting(this, displayModeLabel(type))
                .setValue(ItemCountContent.defaultDisplayMode(type).name())
                .setDisplayLabel(childSettingDisplayLabel(displayModeLabel(type), type))
                .acceptedValues(ItemCountDisplayMode.ICON.name(), ItemCountDisplayMode.CLASSIC.name())
                .setParent(enable);
        new Setting(this, scaleLabel(type))
                .setValue(child(type).scale)
                .setDisplayLabel(childSettingDisplayLabel(scaleLabel(type), type))
                .setMinMax(1.0126582f * 0.49375f, 0.55f * 2.7272727f)
                .setParent(enable)
                .onChange(value -> child(type).scale = ((Number) value).floatValue());
        Setting showBackground = new Setting(this, showBackgroundLabel(type))
                .setValue(child(type).showBackground != null && child(type).showBackground)
                .setDisplayLabel(childSettingDisplayLabel(showBackgroundLabel(type), type))
                .setParent(enable)
                .onChange(value -> child(type).showBackground = (Boolean) value);
        new Setting(this, textColorLabel(type))
                .setValue(child(type).textColor == null ? 0xFFFFFFFF : child(type).textColor)
                .setDisplayLabel(childSettingDisplayLabel(textColorLabel(type), type))
                .setMinMax(Integer.MIN_VALUE, Integer.MAX_VALUE)
                .setParent(enable)
                .onChange(value -> child(type).textColor = (Integer) value);
        new Setting(this, backgroundColorLabel(type))
                .setValue(child(type).backgroundColor == null ? 0x6F000000 : child(type).backgroundColor)
                .setDisplayLabel(childSettingDisplayLabel(backgroundColorLabel(type), type))
                .setMinMax(Integer.MIN_VALUE, Integer.MAX_VALUE)
                .setParent(showBackground)
                .onChange(value -> child(type).backgroundColor = (Integer) value);
        // displayMode setting kept as a configured value holder; mode lookup is state-driven.
        displayMode.onChange(value -> child(type).displayMode = String.valueOf(value));
        child(type).displayMode = ItemCountContent.defaultDisplayMode(type).name();
    }

    public ItemCountState getChildState() {
        return this.state;
    }

    @Override
    public List<AbstractModule> getChildHudUnits() {
        return this.childUnits;
    }

    public ItemCountType getActiveType() {
        return this.activeType;
    }

    public ItemCountType getPlacementTarget() {
        return ItemCountType.HEALING_POT;
    }

    public void setActiveType(ItemCountType type) {
        if (type == null) {
            return;
        }
        this.activeType = type;
        this.syncParentFromActive();
    }

    public boolean isChildEnabled(ItemCountType type) {
        return this.child(type).enabled;
    }

    public void setChildEnabled(ItemCountType type, boolean enabled) {
        ItemCountChildState child = child(type);
        boolean wasEnabled = child.enabled;
        child.enabled = enabled;
        if (enabled && !wasEnabled) {
            child.renderHud = true;
        }
        for (Setting setting : this.getSettingsList()) {
            if (setting.getLabel().equalsIgnoreCase(displayName(type))) {
                setting.setValue(enabled, false);
            }
        }
    }

    public boolean isChildRenderHud(ItemCountType type) {
        return this.child(type).renderHud;
    }

    public void setChildRenderHud(ItemCountType type, boolean renderHud) {
        this.child(type).renderHud = renderHud;
    }

    public ItemCountDisplayMode getDisplayMode(ItemCountType type) {
        ItemCountChildState child = child(type);
        try {
            return ItemCountDisplayMode.valueOf(child.displayMode);
        } catch (Exception ignored) {
            return ItemCountContent.defaultDisplayMode(type);
        }
    }

    public void setDisplayMode(ItemCountType type, ItemCountDisplayMode mode) {
        child(type).displayMode = mode.name();
        for (Setting setting : this.getSettingsList()) {
            if (setting.getLabel().equalsIgnoreCase(displayModeLabel(type))) {
                setting.setValue(mode.name(), false);
            }
        }
    }

    public float getChildScale(ItemCountType type) {
        return this.child(type).scale;
    }

    public void setChildScale(ItemCountType type, float scale) {
        ItemCountChildState child = child(type);
        child.scale = scale;
        for (Setting setting : this.getSettingsList()) {
            if (setting.getLabel().equalsIgnoreCase(scaleLabel(type))) {
                setting.setValue(scale, false);
            }
        }
    }

    public String serializeState() {
        return ItemCountStateCodec.encode(this.state);
    }

    public void loadState(String encoded) {
        ItemCountState decoded = ItemCountStateCodec.decode(encoded);
        this.state.children = decoded.children;
        this.syncSettingsFromChildren();
        this.syncParentFromActive();
    }

    public void resetChildState() {
        this.state.children = ItemCountState.defaults();
        this.activeType = ItemCountType.HEALING_POT;
        this.syncSettingsFromChildren();
        this.syncParentFromActive();
    }

    private void syncSettingsFromChildren() {
        for (Setting setting : this.getSettingsList()) {
            for (ItemCountType type : ItemCountType.values()) {
                if (setting.getLabel().equalsIgnoreCase(displayName(type))) {
                    setting.setValue(this.child(type).enabled, false);
                } else if (setting.getLabel().equalsIgnoreCase(displayModeLabel(type))) {
                    setting.setValue(this.child(type).displayMode, false);
                } else if (setting.getLabel().equalsIgnoreCase(scaleLabel(type))) {
                    setting.setValue(this.child(type).scale, false);
                } else if (setting.getLabel().equalsIgnoreCase(showBackgroundLabel(type))) {
                    boolean bg = this.child(type).showBackground != null && this.child(type).showBackground;
                    setting.setValue(bg, false);
                } else if (setting.getLabel().equalsIgnoreCase(textColorLabel(type))) {
                    setting.setValue(this.child(type).textColor == null ? 0xFFFFFFFF : this.child(type).textColor, false);
                } else if (setting.getLabel().equalsIgnoreCase(backgroundColorLabel(type))) {
                    setting.setValue(this.child(type).backgroundColor == null ? 0x6F000000 : this.child(type).backgroundColor, false);
                }
            }
        }
    }

    private ItemCountChildState child(ItemCountType type) {
        for (ItemCountChildState child : this.state.children) {
            if (type.id().equalsIgnoreCase(child.id)) {
                return child;
            }
        }
        ItemCountChildState created = new ItemCountChildState(type);
        this.state.children.add(created);
        return created;
    }

    public List<Setting> settingsFor(ItemCountType type) {
        java.util.List<Setting> result = new java.util.ArrayList<>();
        for (Setting setting : this.getSettingsList()) {
            String label = setting.getLabel();
            if (label.equalsIgnoreCase(displayName(type))
                    || label.equalsIgnoreCase(displayModeLabel(type))
                    || label.equalsIgnoreCase(scaleLabel(type))
                    || label.equalsIgnoreCase(showBackgroundLabel(type))
                    || label.equalsIgnoreCase(textColorLabel(type))
                    || label.equalsIgnoreCase(backgroundColorLabel(type))) {
                result.add(setting);
            }
        }
        return result;
    }

    public static String displayName(ItemCountType type) {
        switch (type) {
            case HEALING_POT: return "Potion";
            case MUSHROOM_SOUP: return "Soup";
            case ARROW: return "Arrow";
            case GOLDEN_APPLE: return "Gapple";
            case ENCHANTED_GOLDEN_APPLE: return "Enchanted Gapple";
            default: return type.name();
        }
    }

    static String childSettingDisplayLabel(String fullLabel, ItemCountType type) {
        if (fullLabel.equalsIgnoreCase(displayName(type))) {
            return "Status";
        }

        String prefix = displayName(type) + " ";
        if (fullLabel.regionMatches(true, 0, prefix, 0, prefix.length())) {
            String stripped = fullLabel.substring(prefix.length()).trim();
            if (!stripped.isEmpty()) {
                return stripped;
            }
        }

        return fullLabel;
    }

    private static String showBackgroundLabel(ItemCountType type) {
        return displayName(type) + " Show Background";
    }

    private static String textColorLabel(ItemCountType type) {
        return displayName(type) + " Text Color";
    }

    private static String backgroundColorLabel(ItemCountType type) {
        return displayName(type) + " Background Color";
    }

    private static String displayModeLabel(ItemCountType type) {
        return displayName(type) + " Display Mode";
    }

    private static String scaleLabel(ItemCountType type) {
        return displayName(type) + " Scale";
    }

    private void syncParentFromActive() {
        if (this.syncing) {
            return;
        }
        ItemCountChildState active = child(this.activeType);
        this.syncing = true;
        try {
            try {
                super.setAnchor(CBGuiAnchor.valueOf(active.anchor));
            } catch (Exception ignored) {
                super.setAnchor(this.defaultGuiAnchor);
                active.anchor = this.defaultGuiAnchor.name();
            }
            super.setTranslations(active.xTranslation, active.yTranslation);
            this.scale.setValue(active.scale, false);
        } finally {
            this.syncing = false;
        }
    }

    @Override
    public void setRenderHud(boolean renderHud) {
        super.setRenderHud(renderHud);
        if (renderHud) {
            restoreChildrenVisibility();
        }
    }

    void restoreChildrenVisibility() {
        // Module Hide→Show only re-opens the master switch; each child keeps its own state.
        // Only enabled-but-hidden children (legacy from profiles predating the two-state
        // model, or tests) get their visibility restored; disabled children stay disabled.
        for (ItemCountChildState child : this.state.children) {
            if (child.enabled && !child.renderHud) {
                child.renderHud = true;
            }
        }
    }

    @Override
    public void setAnchor(CBGuiAnchor anchor) {
        super.setAnchor(anchor);
        if (!this.syncing && anchor != null) {
            child(this.activeType).anchor = anchor.name();
        }
    }

    @Override
    public void setTranslations(float x, float y) {
        super.setTranslations(x, y);
        if (!this.syncing) {
            ItemCountChildState active = child(this.activeType);
            active.xTranslation = x;
            active.yTranslation = y;
        }
    }

    @Override
    public void setXTranslation(float x) {
        super.setXTranslation(x);
        if (!this.syncing) child(this.activeType).xTranslation = x;
    }

    @Override
    public void setYTranslation(float y) {
        super.setYTranslation(y);
        if (!this.syncing) child(this.activeType).yTranslation = y;
    }

    private void renderHud(GuiDrawEvent event) {
        if (!this.isRenderHud() || this.minecraft.thePlayer == null
                || this.minecraft.currentScreen instanceof CBModulesGui
                || this.minecraft.currentScreen instanceof CBModulePlaceGui) {
            return;
        }
        this.renderChildren(event, this.minecraft.thePlayer.inventory.mainInventory, false);
    }

    private void renderPreview(RenderPreviewEvent event) {
        if (!this.isRenderHud()) {
            return;
        }
        boolean placement = this.minecraft.currentScreen instanceof CBModulePlaceGui;
        if (!placement && !(this.minecraft.currentScreen instanceof CBModulesGui)) {
            return;
        }
        ItemStack[] inventory = this.minecraft.thePlayer == null ? new ItemStack[0]
                : this.minecraft.thePlayer.inventory.mainInventory;
        if (placement) {
            // CBModulePlaceGui is a single-module placement screen; per the composite contract it
            // targets the placement target child (Healing Pot) so only that child is shown there.
            this.renderChild(event, child(this.getPlacementTarget()), inventory, true);
        } else {
            this.renderChildren(event, inventory, true);
        }
    }

    private void renderChildren(GuiDrawEvent event, ItemStack[] inventory, boolean preview) {
        for (ItemCountType type : ItemCountType.values()) {
            ItemCountChildState child = child(type);
            if (child.enabled && child.renderHud) {
                this.renderChild(event, child, inventory, preview);
            }
        }
        this.syncParentFromActive();
    }

    private void renderChild(GuiDrawEvent event, ItemCountChildState child, ItemStack[] inventory, boolean preview) {
        ItemCountType type = ItemCountType.fromId(child.id);
        if (type == null) return;
        int count = ItemCountRules.count(type, inventory);
        ItemCountDisplayMode mode = getDisplayMode(type);
        String text = mode == ItemCountDisplayMode.CLASSIC
                ? ItemCountContent.classicText(type, count) : null;
        String countOverlay = mode == ItemCountDisplayMode.ICON
                ? ItemCountContent.countText(count) : null;
        int textWidth = text == null ? 0 : this.minecraft.fontRendererObj.getStringWidth(text);
        boolean background = child.showBackground != null && child.showBackground;
        float[] dimensions = ItemCountContent.dimensions(mode, 16, textWidth,
                this.minecraft.fontRendererObj.FONT_HEIGHT, background);
        this.syncing = true;
        try {
            super.setAnchor(parseAnchor(child.anchor));
            super.setTranslations(child.xTranslation, child.yTranslation);
            this.scale.setValue(child.scale, false);
            this.setDimensions(dimensions[0], dimensions[1]);
            ChildUnit unit = childUnitFor(type);
            if (unit != null) {
                unit.syncScale();
                unit.width = dimensions[0];
                unit.height = dimensions[1];
            }
            boolean depthTestEnabled = GL11.glIsEnabled(GL11.GL_DEPTH_TEST);
            boolean blendEnabled = GL11.glIsEnabled(GL11.GL_BLEND);
            boolean lightingEnabled = GL11.glIsEnabled(GL11.GL_LIGHTING);
            boolean rescaleEnabled = GL11.glIsEnabled(GL12.GL_RESCALE_NORMAL);
            GL11.glPushMatrix();
            try {
                super.scaleAndTranslate(event.getResolution());
                if (mode == ItemCountDisplayMode.CLASSIC) {
                    int textColor = child.textColor == null ? 0xFFFFFFFF : child.textColor;
                    if (background) {
                        int bg = child.backgroundColor == null ? 0x6F000000 : child.backgroundColor;
                        // FPS-style padded background with a few px of spare area around the
                        // text; text offset into the padding, no shadow (background on = no shadow).
                        cn.kazer.client189.compat.LegacyGui.drawRect(0.0F, 0.0F, dimensions[0], dimensions[1], bg);
                        this.minecraft.fontRendererObj.drawString(text, 2, 3, textColor);
                    } else {
                        this.minecraft.fontRendererObj.drawStringWithShadow(text, 0, 0, textColor);
                    }
                } else {
                    // Layer order top to bottom: count > icon > background. The background is
                    // the bottom layer, so it is drawn first, before the item render. The 2D
                    // rect must not write depth (depthMask off) or it would corrupt the depth
                    // environment the item glint relies on.
                    if (background) {
                        int bg = child.backgroundColor == null ? 0x6F000000 : child.backgroundColor;
                        boolean depthMask = GL11.glGetBoolean(GL11.GL_DEPTH_WRITEMASK);
                        cn.kazer.client189.compat.LegacyGL.glDepthMask(false);
                        cn.kazer.client189.compat.LegacyGui.drawRect(0.0F, 0.0F, 16.0F, 18.0F, bg);
                        cn.kazer.client189.compat.LegacyGL.glDepthMask(depthMask);
                    }
                    // The enchantment glint in RenderItem relies on GL_EQUAL depth masking: item
                    // pixels write depth, the glint quad only passes where that depth was written.
                    // Keeping GL_DEPTH_TEST disabled also disables depth writes, so the glint would
                    // bleed onto the transparent background. Enable depth test + depth writes only
                    // for the item render, then restore the prior HUD state.
                    cn.kazer.client189.compat.LegacyGL.glEnable(GL11.GL_DEPTH_TEST);
                    cn.kazer.client189.compat.LegacyGL.glDepthMask(true);
                    cn.kazer.client189.compat.LegacyGL.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
                    RenderHelper.enableGUIStandardItemLighting();
                    this.renderItem.renderItemAndEffectIntoGUI(this.minecraft.fontRendererObj,
                            this.minecraft.getTextureManager(), displayStack(type), 0, 0);
                    RenderHelper.disableStandardItemLighting();
                    cn.kazer.client189.compat.LegacyGL.glDisable(GL11.GL_LIGHTING);
                    cn.kazer.client189.compat.LegacyGL.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
                    // The item render pushed the icon into the GUI z-layer and wrote depth;
                    // keep depth testing on for the glint above but turn it off again before the
                    // count text so the number draws on top, like vanilla's overlay ordering.
                    cn.kazer.client189.compat.LegacyGL.glDisable(GL11.GL_DEPTH_TEST);
                    int textColor = child.textColor == null ? 0xFFFFFFFF : child.textColor;
                    int sWidth = this.minecraft.fontRendererObj.getStringWidth(countOverlay);
                    // Vanilla stack-size overlay position: RenderItem.renderItemOverlayIntoGUI
                    // draws at (x + 19 - 2 - width, y + 6 + 3) relative to the icon origin.
                    this.minecraft.fontRendererObj.drawStringWithShadow(countOverlay,
                            16 + 1 - sWidth, 6 + 3, textColor);
                }
            } finally {
                GL11.glPopMatrix();
                if (depthTestEnabled) cn.kazer.client189.compat.LegacyGL.glEnable(GL11.GL_DEPTH_TEST);
                else cn.kazer.client189.compat.LegacyGL.glDisable(GL11.GL_DEPTH_TEST);
                if (blendEnabled) cn.kazer.client189.compat.LegacyGL.glEnable(GL11.GL_BLEND);
                else cn.kazer.client189.compat.LegacyGL.glDisable(GL11.GL_BLEND);
                if (lightingEnabled) cn.kazer.client189.compat.LegacyGL.glEnable(GL11.GL_LIGHTING);
                else cn.kazer.client189.compat.LegacyGL.glDisable(GL11.GL_LIGHTING);
                if (rescaleEnabled) cn.kazer.client189.compat.LegacyGL.glEnable(GL12.GL_RESCALE_NORMAL);
                else cn.kazer.client189.compat.LegacyGL.glDisable(GL12.GL_RESCALE_NORMAL);
            }
        } finally {
            this.syncing = false;
        }
    }

    public static ItemStack displayStack(ItemCountType type) {
        switch (type) {
            case HEALING_POT: return new ItemStack(Items.potionitem, 1, POT_DAMAGE);
            case MUSHROOM_SOUP: return new ItemStack(Items.mushroom_stew);
            case ARROW: return new ItemStack(Items.arrow);
            case GOLDEN_APPLE: return new ItemStack(Items.golden_apple, 1, 0);
            case ENCHANTED_GOLDEN_APPLE: return new ItemStack(Items.golden_apple, 1, ENCHANTED_APPLE_DAMAGE);
            default: return new ItemStack(Items.arrow);
        }
    }

    public AbstractModule childUnitForView(ItemCountType type) {
        return childUnitFor(type);
    }

    private ChildUnit childUnitFor(ItemCountType type) {
        for (AbstractModule unit : this.childUnits) {
            if (unit instanceof ChildUnit && ((ChildUnit) unit).type == type) {
                return (ChildUnit) unit;
            }
        }
        return null;
    }

    private static CBGuiAnchor parseAnchor(String value) {
        try {
            return CBGuiAnchor.valueOf(value);
        } catch (Exception ignored) {
            return CBGuiAnchor.LEFT_TOP;
        }
    }

    /**
     * Editor-facing drag unit for one Child HUD. It is a thin AbstractModule whose position state
     * is the parent module's {@link ItemCountChildState}; the editor (CBModulesGui) drags/scales/
     * closes it like any other HUD module. Closing it (the editor ✕) only hides that Child HUD
     * (renderHud=false), never disables the parent or touches other children.
     */
    public List<Setting> settingsForChild(AbstractModule childUnit) {
        if (childUnit instanceof ChildUnit) {
            return settingsFor(((ChildUnit) childUnit).type);
        }
        return null;
    }

    private final class ChildUnit extends AbstractModule implements com.cheatbreaker.client.module.CompositeChildUnit {
        private final ItemCountType type;

        ChildUnit(ItemCountType type) {
            super("Items Counter: " + displayName(type));
            this.type = type;
            this.isEditable = true;
            this.scale.setValue(child().scale, false);
            this.scale.onChange(value -> child().scale = ((Number) value).floatValue());
        }

        @Override
        public AbstractModule getCompositeParent() {
            return ItemCountModule.this;
        }

        @Override
        public List<Setting> getSettingsList() {
            // Guard: during super() construction this.type is still null; the inherited
            // "Scale" setting must append to the default list, not the parent's filtered one.
            return this.type == null ? super.getSettingsList() : settingsFor(this.type);
        }

        void syncScale() {
            this.scale.setValue(child().scale, false);
        }

        private ItemCountChildState child() {
            return ItemCountModule.this.child(this.type);
        }

        @Override
        public CBGuiAnchor getGuiAnchor() {
            try {
                return CBGuiAnchor.valueOf(child().anchor);
            } catch (Exception ignored) {
                return CBGuiAnchor.LEFT_TOP;
            }
        }

        @Override
        public void setAnchor(CBGuiAnchor anchor) {
            child().anchor = anchor == null ? CBGuiAnchor.LEFT_TOP.name() : anchor.name();
        }

        @Override
        public com.cheatbreaker.client.ui.module.CBPositionEnum getPosition() {
            return com.cheatbreaker.client.ui.module.CBAnchorHelper.getHorizontalPositionEnum(getGuiAnchor());
        }

        @Override
        public float getXTranslation() {
            return child().xTranslation;
        }

        @Override
        public float getYTranslation() {
            return child().yTranslation;
        }

        @Override
        public void setTranslations(float x, float y) {
            ItemCountChildState c = child();
            c.xTranslation = x;
            c.yTranslation = y;
        }

        @Override
        public void setXTranslation(float x) {
            child().xTranslation = x;
        }

        @Override
        public void setYTranslation(float y) {
            child().yTranslation = y;
        }

        @Override
        public boolean isEnabled() {
            // The card's Hide from HUD is the module master switch: with the master off, the
            // child unit must not be visible/draggable in the editor regardless of its own
            // enabled flag (the settings page keeps reading the raw child state, independent).
            return ItemCountModule.this.isRenderHud() && child().enabled;
        }

        @Override
        public boolean isRenderHud() {
            return ItemCountModule.this.isRenderHud() && child().renderHud;
        }

        @Override
        public void setRenderHud(boolean renderHud) {
            child().renderHud = renderHud;
        }

        @Override
        public void setState(boolean state) {
            // GUI semantics: showing means visible, hiding means disabled. The editor X
            // disables the child outright (two states only, no enabled-but-hidden middle
            // state); position stays in the child state and is restored on re-enable.
            child().enabled = state;
            child().renderHud = state;
        }

        @Override
        public float[] getScaledPoints(ScaledResolution resolution, boolean bl) {
            float scale = (Float) this.scale.getValue();
            float f3 = this.width * scale;
            float f4 = this.height * scale;
            CBGuiAnchor anchor = getGuiAnchor();
            float f, f2;
            switch (anchor) {
                case LEFT_TOP: default:
                    f = 2.0f; f2 = 2.0f; break;
                case LEFT_MIDDLE:
                    f = 2.0f; f2 = (resolution.getScaledHeight() / 2f) - f4 / 2f; break;
                case LEFT_BOTTOM:
                    f = 2.0f; f2 = resolution.getScaledHeight() - f4 - 2.0f; break;
                case MIDDLE_TOP:
                    f = (resolution.getScaledWidth() / 2f) - f3 / 2f; f2 = 2.0f; break;
                case MIDDLE_MIDDLE:
                    f = (resolution.getScaledWidth() / 2f) - f3 / 2f;
                    f2 = (resolution.getScaledHeight() / 2f) - f4 / 2f; break;
                case MIDDLE_BOTTOM_LEFT:
                    f = (resolution.getScaledWidth() / 2f) - f3;
                    f2 = resolution.getScaledHeight() - f4 - 2.0f; break;
                case MIDDLE_BOTTOM_RIGHT:
                    f = resolution.getScaledWidth() / 2f;
                    f2 = resolution.getScaledHeight() - f4 - 2.0f; break;
                case RIGHT_TOP:
                    f = resolution.getScaledWidth() - f3 - 2.0f; f2 = 2.0f; break;
                case RIGHT_MIDDLE:
                    f = resolution.getScaledWidth() - f3;
                    f2 = (resolution.getScaledHeight() / 2f) - f4 / 2f; break;
                case RIGHT_BOTTOM:
                    f = resolution.getScaledWidth() - f3 - 2.0f;
                    f2 = resolution.getScaledHeight() - f4 - 2.0f; break;
            }
            float[] result = new float[2];
            result[0] = (f + (bl ? getXTranslation() : 0.0f)) / scale;
            result[1] = (f2 + (bl ? getYTranslation() : 0.0f)) / scale;
            return result;
        }

        @Override
        public void scaleAndTranslate(ScaledResolution resolution) {
            scaleAndTranslate(resolution, this.width, this.height);
        }

        @Override
        public void scaleAndTranslate(ScaledResolution resolution, float f, float f2) {
            float scale = (Float) this.scale.getValue();
            GL11.glScalef(scale, scale, scale);
            f *= scale;
            f2 *= scale;
            CBGuiAnchor anchor = getGuiAnchor();
            float f3, f4;
            switch (anchor) {
                case LEFT_TOP: default:
                    f3 = 2.0f; f4 = 2.0f; break;
                case LEFT_MIDDLE:
                    f3 = 2.0f; f4 = (resolution.getScaledHeight() / 2f) - f2 / 2f; break;
                case LEFT_BOTTOM:
                    f3 = 2.0f; f4 = resolution.getScaledHeight() - f2 - 2.0f; break;
                case MIDDLE_TOP:
                    f3 = (resolution.getScaledWidth() / 2f) - f / 2f; f4 = 2.0f; break;
                case MIDDLE_MIDDLE:
                    f3 = (resolution.getScaledWidth() / 2f) - f / 2f;
                    f4 = (resolution.getScaledHeight() / 2f) - f2 / 2f; break;
                case MIDDLE_BOTTOM_LEFT:
                    f3 = (resolution.getScaledWidth() / 2f) - f;
                    f4 = resolution.getScaledHeight() - f2 - 2.0f; break;
                case MIDDLE_BOTTOM_RIGHT:
                    f3 = resolution.getScaledWidth() / 2f;
                    f4 = resolution.getScaledHeight() - f2 - 2.0f; break;
                case RIGHT_TOP:
                    f3 = resolution.getScaledWidth() - f - 2.0f; f4 = 2.0f; break;
                case RIGHT_MIDDLE:
                    f3 = resolution.getScaledWidth() - f;
                    f4 = (resolution.getScaledHeight() / 2f) - f2 / 2f; break;
                case RIGHT_BOTTOM:
                    f3 = resolution.getScaledWidth() - f - 2.0f;
                    f4 = resolution.getScaledHeight() - f2 - 2.0f; break;
            }
            GL11.glTranslatef(f3 / scale, f4 / scale, 0.0f);
            GL11.glTranslatef(getXTranslation() / scale, getYTranslation() / scale, 0.0f);
        }
    }
}
