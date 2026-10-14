package com.cheatbreaker.client.ui.element.module;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.cheatbreaker.client.CheatBreaker;
import com.cheatbreaker.client.config.Setting;
import com.cheatbreaker.client.module.AbstractModule;
import com.cheatbreaker.client.module.staff.StaffModule;
import com.cheatbreaker.client.ui.element.*;
import com.cheatbreaker.client.ui.element.type.*;
import com.cheatbreaker.client.ui.element.type.custom.*;
import com.cheatbreaker.client.ui.module.CBModulesGui;
import com.cheatbreaker.client.ui.util.RenderUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.PositionedSoundRecord;
import net.minecraft.client.gui.Gui;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.input.Keyboard;

public class ModuleListElement extends AbstractScrollableElement {
    /** Container panel corner radius (GUI px). The hover fill of a flush button is the
     * same rect as the panel's own top-left corner, so reusing this radius keeps the
     * highlight's corner arc identical to the panel edge. */
    static final int PANEL_CORNER_RADIUS = 8;
    private static final int MODULE_ROW_HEIGHT = 18;
    private final int colour;
    protected final List<ModuleSettingsElement> moduleSettingsElementList;
    private final boolean lIIIIIllllIIIIlIlIIIIlIlI;
    private final GlobalSettingsElement globalSettingsElement;
    public AbstractScrollableElement scrollable;
    public boolean llIlIIIlIIIIlIlllIlIIIIll = false;
    public AbstractModule module;
    private final ModulesGuiButtonElement backButton;
    private final ModulesGuiButtonElement applyToAllTextButton;
    private final Map<AbstractModule, List<AbstractModulesGuiElement>> moduleElementListMap;
    private final List<AbstractModulesGuiElement> elementsList;

    public ModuleListElement(List<AbstractModule> list, float f, int n, int n2, int n3, int n4) {
        super(f, n, n2, n3, n4);
        this.lIIIIIllllIIIIlIlIIIIlIlI = list == CheatBreaker.getInstance().moduleManager.staffModules;
        this.colour = -12418828;
        this.globalSettingsElement = new GlobalSettingsElement(this, this.colour, f);
        this.moduleSettingsElementList = new ArrayList<>();
        for (Object object : list) {
            if (((AbstractModule)object).isStaffModule() && !((AbstractModule)object).isStaffEnabledModule()) continue;
            this.moduleSettingsElementList.add(new ModuleSettingsElement(this, this.colour, (AbstractModule)object, f));
        }
        this.backButton = new ModulesGuiButtonElement(null, "arrow-64.png", this.x + 2, this.y + 4, 28, 28, -12418828, f)
                .setHoverCornerRadius(PANEL_CORNER_RADIUS);
        this.module = null;
        this.moduleElementListMap = new HashMap<>();
        for (AbstractModule object : list) {
            if (object.isStaffModule() && !object.isStaffEnabledModule()) continue;
            ArrayList<AbstractModulesGuiElement> object2 = new ArrayList<>();
            if (object instanceof com.cheatbreaker.client.module.type.ItemCountModule) {
                com.cheatbreaker.client.module.type.ItemCountModule itemCountModule = (com.cheatbreaker.client.module.type.ItemCountModule) object;
                object2.add(new ItemCountSettingsView(itemCountModule, this, f));
                for (AbstractModule childUnit : itemCountModule.getChildHudUnits()) {
                    ArrayList<AbstractModulesGuiElement> childElements = new ArrayList<>();
                    for (Setting cBSetting : itemCountModule.settingsForChild(childUnit)) {
                        if (cBSetting.getType().equals(Setting.Type.INTEGER) && cBSetting.getLabel().toLowerCase().contains("color")) {
                            childElements.add(new ColorPickerElement(cBSetting, f));
                        } else if (cBSetting.getType().equals(Setting.Type.STRING_ARRAY)) {
                            childElements.add(new ChoiceElement(cBSetting, f));
                        } else if (cBSetting.getType().equals(Setting.Type.BOOLEAN)) {
                            childElements.add(new ToggleElement(cBSetting, f));
                        } else if (cBSetting.getType().equals(Setting.Type.INTEGER) || cBSetting.getType().equals(Setting.Type.DOUBLE) || cBSetting.getType().equals(Setting.Type.FLOAT)) {
                            childElements.add(new SliderElement(cBSetting, f));
                        }
                    }
                    this.moduleElementListMap.put(childUnit, childElements);
                }
                this.moduleElementListMap.put(object, object2);
                continue;
            }
            if (object instanceof com.cheatbreaker.client.module.type.autotexthotkey.AutoTextHotkeyModule) {
                com.cheatbreaker.client.module.type.autotexthotkey.AutoTextHotkeyModule autoTextHotkeyModule =
                        (com.cheatbreaker.client.module.type.autotexthotkey.AutoTextHotkeyModule) object;
                object2.add(new AutoTextHotkeySettingsView(autoTextHotkeyModule, f));
                this.moduleElementListMap.put(object, object2);
                continue;
            }
            if (object instanceof com.cheatbreaker.client.module.type.customtext.CustomTextModule) {
                com.cheatbreaker.client.module.type.customtext.CustomTextModule customTextModule = (com.cheatbreaker.client.module.type.customtext.CustomTextModule) object;
                object2.add(new CustomTextSettingsView(customTextModule, f));
                this.moduleElementListMap.put(object, object2);
                for (com.cheatbreaker.client.module.AbstractModule childUnit : customTextModule.getChildHudUnits()) {
                    java.util.List<AbstractModulesGuiElement> childElements = new ArrayList<>();
                    childElements.add(new CustomTextChildContentElement(customTextModule,
                            (com.cheatbreaker.client.module.type.customtext.CustomTextModule.CustomTextChildUnit) childUnit, f));
                    for (Setting cBSetting : customTextModule.settingsForChild(childUnit)) {
                        if (!cBSetting.isVisible()) continue;
                        // CONTENT 由上面的 ContentElement 处理，这里跳过避免重复。
                        if (cBSetting.getType().equals(Setting.Type.STRING)) continue;
                        switch (cBSetting.getType()) {
                            case BOOLEAN:
                                childElements.add(new ToggleElement(cBSetting, f));
                                break;
                            case INTEGER:
                            case FLOAT:
                            case DOUBLE:
                                if (cBSetting.getLabel().toLowerCase().contains("color")) {
                                    childElements.add(new ColorPickerElement(cBSetting, f));
                                } else {
                                    childElements.add(new SliderElement(cBSetting, f));
                                }
                                break;
                            case STRING_ARRAY:
                                childElements.add(new ChoiceElement(cBSetting, f));
                                break;
                            default:
                                break;
                        }
                    }
                    this.moduleElementListMap.put(childUnit, childElements);
                }
                continue;
            }
            for (Setting cBSetting : object.getSettingsList()) {
                if (!cBSetting.isVisible()) continue;
                switch (cBSetting.getType()) {
                    case BOOLEAN: {
                        object2.add(new ToggleElement(cBSetting, f));
                        break;
                    }
                    case DOUBLE:
                    case INTEGER:
                    case FLOAT: {
                        if (object.isStaffModule() && cBSetting == ((StaffModule)object).getKeybindSetting() || object.isStaffModule() && cBSetting == object.scale) break;
                        if (!object.defaultRenderHud && cBSetting == object.scale) break;
                        if (cBSetting.getType().equals(Setting.Type.INTEGER) && cBSetting.getLabel().toLowerCase().contains("color")) {
                            object2.add(new ColorPickerElement(cBSetting, f));
                            break;
                        }
                        object2.add(new SliderElement(cBSetting, f));
                        break;
                    }
                    case STRING_ARRAY: {
                        object2.add(new ChoiceElement(cBSetting, f));
                        break;
                    }
                    case STRING: {
                        if (!cBSetting.getLabel().equalsIgnoreCase("label")) break;
                        object2.add(new LabelElement(cBSetting, f));
                    }
                }
            }
            if (object.isStaffModule()) {
                object2.add(new KeybindElement(((StaffModule)object).getKeybindSetting(), f));
                if (object == CheatBreaker.getInstance().moduleManager.xray) {
                    object2.add(new XRayOptionsElement(CheatBreaker.getInstance().moduleManager.xray.lIllIllIlIIllIllIlIlIIlIl(), "Blocks", f));
                }
            }
            this.moduleElementListMap.put(object, object2);
        }
        this.elementsList = new ArrayList<>();
        for (Setting setting : CheatBreaker.getInstance().globalSettings.settingsList) {
            this.elementsList.addAll(createGlobalSettingElements(setting, f));
        }
        int n5 = 25;
        for (Object object2 : this.elementsList) {
            n5 += ((AbstractModulesGuiElement)object2).getHeight();
        }
        this.applyToAllTextButton = new ModulesGuiButtonElement(CheatBreaker.getInstance().playBold18px, null, "Apply to all text", this.x + n3 - 120, this.y + n5 + 4, 110, 28, -12418828, f);
    }

    /** 由外部在拖拽/增删 Custom Text 实例后调用，动态重建其子设置条目。 */
    public void cbzRebuildCustomTextChildEntries(com.cheatbreaker.client.module.type.customtext.CustomTextModule module) {
        for (com.cheatbreaker.client.module.AbstractModule childUnit : module.getChildHudUnits()) {
            java.util.List<AbstractModulesGuiElement> elements = this.moduleElementListMap.computeIfAbsent(childUnit, k -> new ArrayList<>());
            elements.clear();
            for (com.cheatbreaker.client.config.Setting setting : module.settingsForChild(childUnit)) {
                if (!setting.isVisible()) continue;
                switch (setting.getType()) {
                    case BOOLEAN:
                        elements.add(new ToggleElement(setting, this.scale));
                        break;
                    case INTEGER:
                        if (setting.getLabel().toLowerCase().contains("color")) {
                            elements.add(new ColorPickerElement(setting, this.scale));
                        } else {
                            elements.add(new SliderElement(setting, this.scale));
                        }
                        break;
                    case DOUBLE:
                    case FLOAT:
                        elements.add(new SliderElement(setting, this.scale));
                        break;
                    case STRING_ARRAY:
                        elements.add(new ChoiceElement(setting, this.scale));
                        break;
                    default:
                        break;
                }
            }
        }
    }

    static String settingsPageTitle(String moduleName, boolean compositeChild) {
        return compositeChild ? moduleName : moduleName + " Settings";
    }

    /**
     * Row cursor advance for ALL settings pages (module settings pages, composite child
     * pages, and the global CheatBreaker Settings page). Choice rows are 2px shorter than
     * slider rows and their label baseline sits 2px lower, so without the correction the
     * visible gap after a choice row is 12px instead of the 16px of its neighbours.
     * Contract: every settings-page row cursor must advance through this method.
     */
    static int settingsRowAdvance(AbstractModulesGuiElement element) {
        return 2 + element.getHeight() + (element instanceof ChoiceElement ? 4 : 0);
    }

    /** 模块列表行高 18 + 行距 2；内容高度 = rowsTop + n × 行距 + 底部留白（2），必须盖住最末行的实际绘制底部。 */
    static int listContentHeight(int rowsTop, int rowCount, int rowHeight) {
        return rowsTop + rowCount * (rowHeight + 2) + 2;
    }

    static List<AbstractModulesGuiElement> createGlobalSettingElements(Setting setting, float scale) {
        List<AbstractModulesGuiElement> elements = new ArrayList<>();
        switch (setting.getType()) {
            case BOOLEAN: {
                elements.add(new ToggleElement(setting, scale));
                break;
            }
            case DOUBLE:
            case INTEGER:
            case FLOAT: {
                if (setting.getType().equals(Setting.Type.INTEGER) && setting.getLabel().toLowerCase().contains("color")) {
                    elements.add(new ColorPickerElement(setting, scale));
                    break;
                }
                if (setting.getLabel().equals("World Time")) {
                    elements.add(new WorldTimeElement(setting, scale));
                    break;
                }
                elements.add(new SliderElement(setting, scale));
                break;
            }
            case STRING_ARRAY: {
                elements.add(new ChoiceElement(setting, scale));
                break;
            }
            case STRING: {
                if (!setting.getLabel().equalsIgnoreCase("label")) break;
                elements.add(new LabelElement(setting, scale));
                if (!CheatBreaker.getInstance().globalSettings.getCrosshairSettingsLabel().getValue().equals(setting.getValue())) break;
                elements.add(new CrosshairElement(scale));
            }
        }
        return elements;
    }

    @Override
    public void handleDrawElement(int mouseX, int mouseY, float partialTicks) {
        RenderUtil.lIIIIlIIllIIlIIlIIIlIIllI(this.x, this.y, this.x + this.width, this.y + this.height + 2, (double) PANEL_CORNER_RADIUS, -657931);
        this.preDraw(mouseX, mouseY);
        if (this.module == null && !this.llIlIIIlIIIIlIlllIlIIIIll) {
            int rowsTop = this.lIIIIIllllIIIIlIlIIIIlIlI ? 4 : 24;
            this.IlllIllIlIIIIlIIlIIllIIIl = listContentHeight(rowsTop, this.moduleSettingsElementList.size(), MODULE_ROW_HEIGHT);
            if (!this.lIIIIIllllIIIIlIlIIIIlIlI) {
                this.globalSettingsElement.setDimensions(this.x + 4, this.y + 4, this.width - 12, 18);
                this.globalSettingsElement.yOffset = this.lIIIIllIIlIlIllIIIlIllIlI;
                this.globalSettingsElement.handleDrawElement(mouseX, mouseY, partialTicks);
            }
            for (int i = 0; i < this.moduleSettingsElementList.size(); ++i) {
                ModuleSettingsElement moduleSettingsElement = this.moduleSettingsElementList.get(i);
                moduleSettingsElement.setDimensions(this.x + 4, this.y + rowsTop + i * (MODULE_ROW_HEIGHT + 2), this.width - 12, MODULE_ROW_HEIGHT);
                moduleSettingsElement.yOffset = this.lIIIIllIIlIlIllIIIlIllIlI;
                moduleSettingsElement.handleDrawElement(mouseX, mouseY, partialTicks);
            }
        } else if (this.llIlIIIlIIIIlIlllIlIIIIll && !this.lIIIIIllllIIIIlIlIIIIlIlI) {
            cn.kazer.client189.compat.LegacyGui.drawRect(this.x + 32, this.y + 4, this.x + 33, this.y + this.height - 4, 0x2F2F2F2F);
            this.IlllIllIlIIIIlIIlIIllIIIl = 25;
            this.backButton.setDimensions(this.x + 2, this.y + 2, 28, 28);
            this.backButton.yOffset = this.lIIIIllIIlIlIllIIIlIllIlI;
            this.backButton.handleDrawElement(mouseX, mouseY, partialTicks);
            CheatBreaker.getInstance().ubuntuMedium16px.drawString("Kazer Client Settings".toUpperCase(), this.x + 38, (float)(this.y + 9), -1358954496);
            cn.kazer.client189.compat.LegacyGui.drawRect(this.x + 38, this.y + 17, this.x + this.width - 6, this.y + 18, 0x2F2F2F2F);
            int n3 = 0;
            for (AbstractModulesGuiElement abstractElement : this.elementsList) {
                if (!isSettingVisible(abstractElement)) continue;
                abstractElement.setDimensions(this.x + 38, this.y + 22 + n3, this.width - 40, abstractElement.getHeight());
                abstractElement.yOffset = this.lIIIIllIIlIlIllIIIlIllIlI;
                abstractElement.handleDrawElement(mouseX, mouseY, partialTicks);
                n3 += settingsRowAdvance(abstractElement);
                this.IlllIllIlIIIIlIIlIIllIIIl += settingsRowAdvance(abstractElement);
            }
            this.applyToAllTextButton.yOffset = this.lIIIIllIIlIlIllIIIlIllIlI;
            this.applyToAllTextButton.setDimensions(this.x + this.width - 118, this.y + this.IlllIllIlIIIIlIIlIIllIIIl, 100, 20);
            this.applyToAllTextButton.handleDrawElement(mouseX, mouseY, partialTicks);
            this.IlllIllIlIIIIlIIlIIllIIIl += 24;
        } else {
            cn.kazer.client189.compat.LegacyGui.drawRect(this.x + 32, this.y + 4, this.x + 33, this.y + (Math.max(this.height, this.IlllIllIlIIIIlIIlIIllIIIl)) - 4, 0x2F2F2F2F);
            this.IlllIllIlIIIIlIIlIIllIIIl = 37;
            this.backButton.setDimensions(this.x + 2, this.y + 2, 28, 28);
            this.backButton.yOffset = this.lIIIIllIIlIlIllIIIlIllIlI;
            this.backButton.handleDrawElement(mouseX, mouseY, partialTicks);
            CheatBreaker.getInstance().ubuntuMedium16px.drawMixedString(settingsPageTitle(this.module.getName(), this.module instanceof com.cheatbreaker.client.module.CompositeChildUnit).toUpperCase(), this.x + 38, (float)(this.y + 6), -1358954496);
            cn.kazer.client189.compat.LegacyGui.drawRect(this.x + 38, this.y + 17, this.x + this.width - 12, this.y + 18, 0x2F2F2F2F);
            int n4 = 0;
            if (this.module != null && moduleElementListMap.containsKey(this.module)) {
                for (AbstractModulesGuiElement abstractElement : this.moduleElementListMap.get(this.module)) {
                    if (!isSettingVisible(abstractElement)) continue;
                    abstractElement.setDimensions(this.x + 38, this.y + 22 + n4, this.width - 40, abstractElement.getHeight());
                    abstractElement.yOffset = this.lIIIIllIIlIlIllIIIlIllIlI;
                    abstractElement.handleDrawElement(mouseX, mouseY, partialTicks);
                    int advance = settingsRowAdvance(abstractElement);
                    n4 += advance;
                    this.IlllIllIlIIIIlIIlIIllIIIl += advance;
                }
            }
            if (this.module == CheatBreaker.getInstance().moduleManager.freelook) {
                CheatBreaker.getInstance().playRegular14px.drawString("bind freelook hotkey in Controls", this.x + 38, (float)(this.y + this.height - 18), -1895825408);
            }
        }
        this.postDraw(mouseX, mouseY);
    }

    @Override
    public void handleMouseClick(int mouseX, int mouseY, int button) {
        block13: {
            block14: {
                block12: {
                    if (this.module != null || this.llIlIIIlIIIIlIlllIlIIIIll) break block12;
                    if (this.globalSettingsElement.isMouseInside(mouseX, mouseY) && !this.lIIIIIllllIIIIlIlIIIIlIlI) {
                        Minecraft.getMinecraft().getSoundHandler().playSound(PositionedSoundRecord.create(new ResourceLocation("gui.button.press"), 1.0f));
                        this.llIlIIIlIIIIlIlllIlIIIIll = true;
                        this.lIIIIllIIlIlIllIIIlIllIlI = 0;
                        this.IllIIIIIIIlIlIllllIIllIII = 0.0;
                        this.yOffset = 0;
                    } else {
                        for (ModuleSettingsElement moduleSettingsElement : this.moduleSettingsElementList) {
                            if (!moduleSettingsElement.isMouseInside(mouseX, mouseY) || !this.lIIIIlIIllIIlIIlIIIlIIllI(moduleSettingsElement.module)) continue;
                            moduleSettingsElement.handleMouseClick(mouseX, mouseY, button);
                        }
                    }
                    break block13;
                }
                if (!this.backButton.isMouseInside(mouseX, mouseY)) break block14;
                Minecraft.getMinecraft().getSoundHandler().playSound(PositionedSoundRecord.create(new ResourceLocation("gui.button.press"), 1.0f));
                // Composite child settings step back to the composite chooser page first;
                // the chooser page itself steps back to the module list as usual. When stepping
                // back from a child, stay on this settings element (do not restore scrollable,
                // which would jump the view to the card grid).
                boolean childBack = this.module instanceof com.cheatbreaker.client.module.CompositeChildUnit;
                this.module = childBack ? ((com.cheatbreaker.client.module.CompositeChildUnit) this.module).getCompositeParent() : null;
                this.llIlIIIlIIIIlIlllIlIIIIll = false;
                if (childBack) {
                    break block13;
                }
                if (this.scrollable == null) break block13;
                CBModulesGui.instance.currentScrollableElement = this.scrollable;
                break block13;
            }
            if (this.module != null && moduleElementListMap.containsKey(this.module)) {
                for (AbstractModulesGuiElement abstractElement : this.moduleElementListMap.get(this.module)) {
                    if (!isSettingVisible(abstractElement)) continue;
                    if (!abstractElement.isMouseInside(mouseX, mouseY)) continue;
                    abstractElement.handleMouseClick(mouseX, mouseY, button);
                }
            } else if (this.llIlIIIlIIIIlIlllIlIIIIll) {
                if (this.applyToAllTextButton.isMouseInside(mouseX, mouseY)) {
                    for (AbstractModule cBModule : CheatBreaker.getInstance().moduleManager.modules) {
                        for (Setting cBSetting : cBModule.getSettingsList()) {
                            if (cBSetting.getType() != Setting.Type.INTEGER || !cBSetting.getLabel().toLowerCase().contains("color") || cBSetting.getLabel().toLowerCase().contains("background")) continue;
                            Minecraft.getMinecraft().getSoundHandler().playSound(PositionedSoundRecord.create(new ResourceLocation("gui.button.press"), 1.0f));
                            cBSetting.setValue(CheatBreaker.getInstance().globalSettings.defaultColor.getValue());
                        }
                    }
                } else {
                    for (AbstractModulesGuiElement abstractElement : this.elementsList) {
                        if (!isSettingVisible(abstractElement)) continue;
                        if (!abstractElement.isMouseInside(mouseX, mouseY)) continue;
                        abstractElement.handleMouseClick(mouseX, mouseY, button);
                    }
                }
            }
        }
    }

    @Override
    public boolean handleKeyTyped(char character, int keyCode) {
        for (AbstractModulesGuiElement element : this.getVisibleSettingElements()) {
            if (element.handleKeyTyped(character, keyCode)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void handleGlobalMouseClick(int mouseX, int mouseY, int button) {
        for (AbstractModulesGuiElement element : this.getVisibleSettingElements()) {
            element.handleGlobalMouseClick(mouseX, mouseY, button);
        }
    }

    @Override
    public void onFocusLost() {
        for (AbstractModulesGuiElement element : this.getVisibleSettingElements()) {
            element.onFocusLost();
        }
    }

    /**
     * A parent-toggle-gated setting disappears while its parent is off. The element list is
     * built once per page, so the check runs on every draw/click rather than at build time.
     */
    private static boolean isSettingVisible(AbstractModulesGuiElement element) {
        Setting setting = element.getSetting();
        return setting == null || setting.isVisible();
    }

    private List<AbstractModulesGuiElement> getVisibleSettingElements() {
        if (this.module != null && this.moduleElementListMap.containsKey(this.module)) {
            java.util.List<AbstractModulesGuiElement> visible = new ArrayList<>();
            for (AbstractModulesGuiElement element : this.moduleElementListMap.get(this.module)) {
                if (isSettingVisible(element)) {
                    visible.add(element);
                }
            }
            return visible;
        }
        if (this.llIlIIIlIIIIlIlllIlIIIIll) {
            java.util.List<AbstractModulesGuiElement> visible = new ArrayList<>();
            for (AbstractModulesGuiElement element : this.elementsList) {
                if (isSettingVisible(element)) {
                    visible.add(element);
                }
            }
            return visible;
        }
        return java.util.Collections.emptyList();
    }

    @Override
    public boolean lIIIIlIIllIIlIIlIIIlIIllI(AbstractModule cBModule) {
        return !cBModule.getSettingsList().isEmpty() || cBModule.getName().contains("Zans")
                || cBModule instanceof com.cheatbreaker.client.module.type.customtext.CustomTextModule;
    }

    @Override
    public void lIIIIIIIIIlIllIIllIlIIlIl(AbstractModule cBModule) {
        Minecraft.getMinecraft().getSoundHandler().playSound(PositionedSoundRecord.create(new ResourceLocation("gui.button.press"), 1.0f));
        this.lIIIIllIIlIlIllIIIlIllIlI = 0;
        this.IllIIIIIIIlIlIllllIIllIII = 0.0;
        this.yOffset = 0;
        this.module = cBModule;
        this.scrollable = null;
    }
}
