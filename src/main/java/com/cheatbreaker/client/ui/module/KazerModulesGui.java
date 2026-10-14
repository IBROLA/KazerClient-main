package com.cheatbreaker.client.ui.module;

import com.cheatbreaker.client.CheatBreaker;
import com.cheatbreaker.client.config.Setting;
import com.cheatbreaker.client.config.GlobalSettings;
import com.cheatbreaker.client.config.Profile;
import com.cheatbreaker.client.config.ProfileNameValidator;
import com.cheatbreaker.client.module.AbstractModule;
import com.cheatbreaker.client.module.type.armourstatus.ArmourStatusModule;
import com.cheatbreaker.client.module.type.bossbar.BossBarModule;
import com.cheatbreaker.client.module.type.ChatModule;
import com.cheatbreaker.client.module.type.keystrokes.KeystrokesModule;
import com.cheatbreaker.client.module.type.motionblur.MotionBlurModule;
import com.cheatbreaker.client.module.type.nametag.NametagModule;
import com.cheatbreaker.client.module.type.togglesprint.ToggleSprintModule;
import com.cheatbreaker.client.ui.util.RenderUtil;
import com.cheatbreaker.client.ui.util.KazerUiIcons;
import com.cheatbreaker.client.ui.util.font.CBFontRenderer;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.client.settings.GameSettings;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.GL11;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Compact translucent control centre based on the NEW UI references.  It intentionally
 * owns presentation only; module logic continues to live in the original CheatBreakerZ
 * modules so HUD behaviour is not duplicated or desynchronised.
 */
public final class KazerModulesGui extends GuiScreen {
    private static final int PANEL_W = 500;
    private static final int PANEL_H = 310;
    private static final int SIDEBAR_W = 108;
    private static final int HEADER_H = 40;
    private static final int CARD_W = 118;
    private static final int CARD_H = 108;
    private static final int GAP = 7;
    private static final int PANEL = 0xA8121512;
    private static final int CARD = 0x5E111311;
    private static final int CARD_HOVER = 0x76232623;
    private static final int CARD_BAR = 0x80505250;
    private static final int LINE = 0x70676A67;
    private static final int TEXT = 0xFFF2F2F2;
    private static final int MUTED = 0xFF9A9A9A;
    private static final int ON = 0xFF25A965;
    private static final int OFF = 0xFFB71F4B;
    private static final int TRACK = 0xFF575B58;

    private final ResourceLocation logo = new ResourceLocation("client/kazer_logo.png");
    private GuiTextField search;
    private int searchX;
    private int searchY;
    private int searchW;
    private int searchH;
    private AbstractModule selectedModule;
    private Page page = Page.MODS;
    private SettingsCategory settingsCategory = SettingsCategory.GENERAL;
    private int left;
    private int top;
    private int moduleScroll;
    private int settingsScroll;
    private int globalScroll;
    private boolean draggingGlobalScrollbar;
    private final Map<Setting, KeyBinding> referenceBindings = new LinkedHashMap<>();
    private Setting recordingBinding;
    private ModCategory modCategory = ModCategory.ALL;
    private boolean creatingProfile;
    private GuiTextField profileName;
    private String profileError;
    private Setting draggingSetting;
    private int draggingSettingX;
    private int draggingSettingW;
    private Setting colorEditor;
    private int draggingColorChannel = -1;
    private boolean draggingModuleScrollbar;
    private boolean draggingSettingsScrollbar;
    private int scrollbarGrabOffset;
    private Setting editingNumericSetting;
    private String editingNumericText = "";
    private boolean replaceNumericText;
    private Setting editingStringSetting;
    private String editingStringText = "";
    private boolean replaceStringText;
    private final GuiScreen parent;

    private enum Page { MODS, SETTINGS }
    private enum SettingsCategory { GENERAL, PERFORMANCE, CONTROLS }
    private enum ModCategory { ALL, NEW, HUD, SERVER, MECHANIC }

    public KazerModulesGui() {
        this(new CBModulesGui());
    }

    public KazerModulesGui(GuiScreen parent) {
        this.parent = parent;
    }

    public KazerModulesGui(GuiScreen parent, AbstractModule module) {
        this.parent = parent;
        this.selectedModule = module;
        this.page = Page.MODS;
    }

    @Override
    public void initGui() {
        Keyboard.enableRepeatEvents(true);
        left = (width - PANEL_W) / 2;
        top = (height - PANEL_H) / 2;
        searchX = left + PANEL_W - 150;
        searchY = top + HEADER_H + 7;
        searchW = 140;
        searchH = 14;
        search = new GuiTextField(0, fontRendererObj, searchX, searchY, searchW, searchH);
        search.setMaxStringLength(48);
        search.setEnableBackgroundDrawing(false);
        search.setTextColor(0xFFF1F3F1);
        profileName = new GuiTextField(0, fontRendererObj, left + PANEL_W / 2 - 80,
                top + PANEL_H / 2 - 3, 160, 18);
        profileName.setMaxStringLength(24);
        profileName.setEnableBackgroundDrawing(false);
        profileName.setTextColor(TEXT);
    }

    @Override
    public void onGuiClosed() {
        commitNumericEdit();
        commitStringEdit();
        Keyboard.enableRepeatEvents(false);
        if (CheatBreaker.getInstance().configManager != null) {
            CheatBreaker.getInstance().configManager.write();
        }
    }

    @Override
    public boolean doesGuiPauseGame() {
        return false;
    }

    @Override
    public void updateScreen() {
        search.updateCursorCounter();
        if (profileName != null && creatingProfile) profileName.updateCursorCounter();
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        if (draggingGlobalScrollbar && Mouse.isButtonDown(0)) updateGlobalScrollbar(mouseY);
        if (draggingModuleScrollbar && Mouse.isButtonDown(0)) {
            updateModuleScrollbar(mouseY);
        }
        if (draggingSettingsScrollbar && Mouse.isButtonDown(0)) {
            updateSettingsScrollbar(mouseY);
        }
        if (draggingSetting != null && Mouse.isButtonDown(0)) {
            setNumericFromMouse(draggingSetting, mouseX, draggingSettingX, draggingSettingW);
        }
        if (colorEditor != null && draggingColorChannel >= 0 && Mouse.isButtonDown(0)) {
            setColorChannelFromMouse(draggingColorChannel, mouseX);
        }
        cn.kazer.client189.compat.LegacyGL.glColor4f(1F, 1F, 1F, 1F);
        drawPanel();
        drawHeader(mouseX, mouseY);
        drawSidebar(mouseX, mouseY);
        if (selectedModule != null) {
            drawModuleSettings(mouseX, mouseY);
        } else if (page == Page.MODS) {
            drawModules(mouseX, mouseY);
        } else {
            drawClientSettings(mouseX, mouseY);
        }
        if (creatingProfile) drawProfileDialog(mouseX, mouseY);
        if (colorEditor != null) drawColorDialog(mouseX, mouseY);
        super.drawScreen(mouseX, mouseY, partialTicks);
    }

    private void drawProfileDialog(int mouseX, int mouseY) {
        int x = left + PANEL_W / 2 - 100;
        int y = top + PANEL_H / 2 - 38;
        cn.kazer.client189.compat.LegacyGui.drawRect(left, top, left + PANEL_W, top + PANEL_H, 0x88000000);
        cn.kazer.client189.compat.LegacyGui.drawRect(x, y, x + 200, y + 82, 0xF01A211D);
        cn.kazer.client189.compat.LegacyGui.drawRect(x, y, x + 200, y + 1, 0xFF59D7C9);
        fontRendererObj.drawString("NEW PROFILE", x + 10, y + 10, TEXT);
        profileName.drawTextBox();
        button("SAVE", x + 10, y + 55, 84, 18, mouseX, mouseY);
        button("CANCEL", x + 106, y + 55, 84, 18, mouseX, mouseY);
        if (profileError != null) fontRendererObj.drawString(profileError, x + 10, y + 38, 0xFFFF6B6B);
    }

    private void drawColorDialog(int mouseX, int mouseY) {
        int x = left + PANEL_W / 2 - 105;
        int y = top + PANEL_H / 2 - 62;
        cn.kazer.client189.compat.LegacyGui.drawRect(left, top, left + PANEL_W, top + PANEL_H, 0x99000000);
        RenderUtil.lIIIIlIIllIIlIIlIIIlIIllI(x, y, x + 210, y + 124, 4.0D, 0xF0181C19);
        cn.kazer.client189.compat.LegacyGui.drawRect(x, y, x + 210, y + 1, 0xFF59D7C9);
        smallBoldFont().drawString(colorEditor.getDisplayLabel().toUpperCase(Locale.ROOT), x + 10, y + 10, TEXT);
        int argb = ((Number)colorEditor.getValue()).intValue();
        String[] labels = {"A", "R", "G", "B"};
        for (int channel = 0; channel < 4; channel++) {
            int value = channelValue(argb, channel);
            int rowY = y + 28 + channel * 18;
            smallBoldFont().drawString(labels[channel], x + 10, rowY + 5, MUTED);
            cn.kazer.client189.compat.LegacyGui.drawRect(x + 28, rowY + 5, x + 174, rowY + 8, 0xFF555A56);
            int thumb = x + 28 + Math.round(value / 255F * 146F);
            cn.kazer.client189.compat.LegacyGui.drawRect(thumb - 1, rowY + 2, thumb + 2, rowY + 11, 0xFF59D7C9);
            smallBoldFont().drawString(String.valueOf(value), x + 181, rowY + 5, TEXT);
        }
        button("DONE", x + 116, y + 101, 84, 16, mouseX, mouseY);
    }

    private static int channelValue(int argb, int channel) {
        return argb >> ((3 - channel) * 8) & 255;
    }

    private static int withChannel(int argb, int channel, int value) {
        int shift = (3 - channel) * 8;
        return argb & ~(255 << shift) | (value & 255) << shift;
    }

    private void setColorChannelFromMouse(int channel, int mouseX) {
        int dialogX = left + PANEL_W / 2 - 105;
        int value = Math.max(0, Math.min(255, Math.round((mouseX - (dialogX + 28)) / 146F * 255F)));
        colorEditor.setValue(withChannel(((Number)colorEditor.getValue()).intValue(), channel, value));
        afterSettingChanged(colorEditor);
    }

    private void drawPanel() {
        cn.kazer.client189.compat.LegacyGui.drawRect(left, top, left + PANEL_W, top + PANEL_H, PANEL);
        cn.kazer.client189.compat.LegacyGui.drawRect(left, top + HEADER_H - 1, left + PANEL_W, top + HEADER_H, LINE);
    }

    private void drawHeader(int mouseX, int mouseY) {
        cn.kazer.client189.compat.LegacyGL.glColor4f(1F, 1F, 1F, 1F);
        RenderUtil.lIIIIlIIllIIlIIlIIIlIIllI(logo, left + 9, top + 7, 25, 25);
        CheatBreaker.getInstance().playRegular18px.drawString("KAZER CLIENT", left + 37, top + 12, TEXT);
        tab("MODS", left + 126, top + 9, 54, 22, page == Page.MODS, mouseX, mouseY);
        tab("SETTINGS", left + 188, top + 9, 68, 22, page == Page.SETTINGS, mouseX, mouseY);
        boolean closeHover = inside(mouseX, mouseY, left + PANEL_W - 29, top + 8, 20, 20);
        RenderUtil.lIIIIlIIllIIlIIlIIIlIIllI(left + PANEL_W - 29, top + 8,
                left + PANEL_W - 9, top + 28, 3.0D, closeHover ? 0xD76B6E6B : 0xB4555955);
        headerFont().drawCenteredString("X", left + PANEL_W - 19, top + 13, TEXT);
    }

    private void tab(String label, int x, int y, int w, int h, boolean active, int mouseX, int mouseY) {
        boolean hover = inside(mouseX, mouseY, x, y, w, h);
        RenderUtil.lIIIIlIIllIIlIIlIIIlIIllI(x, y, x + w, y + h, 3.0D,
                active ? 0xA9616461 : hover ? 0x8A505450 : 0x64404440);
        drawCentered(smallBoldFont(), label, x, y, w, h, active ? TEXT : MUTED);
    }

    private void drawSidebar(int mouseX, int mouseY) {
        smallBoldFont().drawString("PROFILES", left + 10, top + 49, 0xFFDADADA);
        int profileY = top + 62;
        List<Profile> profiles = CheatBreaker.getInstance().profiles == null
                ? Collections.<Profile>emptyList() : CheatBreaker.getInstance().profiles;
        for (Profile profile : profiles) {
            if (profileY + 20 >= top + PANEL_H - 54) break;
            boolean active = profile == CheatBreaker.getInstance().activeProfile;
            boolean hover = inside(mouseX, mouseY, left + 7, profileY, SIDEBAR_W - 14, 20);
            cn.kazer.client189.compat.LegacyGui.drawRect(left + 7, profileY, left + SIDEBAR_W - 7, profileY + 20,
                    active ? 0x8A505350 : hover ? 0x55454A46 : 0x22151715);
            String name = profile.getName();
            drawCentered(smallBoldFont(), name, left + 7, profileY, SIDEBAR_W - 14, 20, TEXT);
            if (profile.isEditable()) {
                smallBoldFont().drawCenteredString("X", left + SIDEBAR_W - 14, profileY + 7,
                        inside(mouseX, mouseY, left + SIDEBAR_W - 20, profileY + 4, 12, 12)
                                ? 0xFFFF7373 : MUTED);
            }
            profileY += 22;
        }
        button("SAVE NEW PROFILE", left + 7, top + PANEL_H - 47, SIDEBAR_W - 14, 16, mouseX, mouseY);
        button("EDIT HUD LAYOUT", left + 7, top + PANEL_H - 25, SIDEBAR_W - 14, 17, mouseX, mouseY);
    }

    private void button(String text, int x, int y, int w, int h, int mouseX, int mouseY) {
        boolean hover = inside(mouseX, mouseY, x, y, w, h);
        RenderUtil.lIIIIlIIllIIlIIlIIIlIIllI(x, y, x + w, y + h, 2.0D,
                hover ? 0xC25D625E : 0x74424743);
        CBFontRenderer font = smallBoldFont().getStringWidth(text) <= w - 6 ? smallBoldFont() : regularFont();
        drawCentered(font, text, x, y, w, h, TEXT);
    }

    private CBFontRenderer regularFont() {
        return CheatBreaker.getInstance().playRegular14px;
    }

    private CBFontRenderer headerFont() {
        return CheatBreaker.getInstance().playBold18px;
    }

    private CBFontRenderer smallBoldFont() {
        return CheatBreaker.getInstance().robotoBold14px;
    }

    private void drawModules(int mouseX, int mouseY) {
        int categoryX = left + SIDEBAR_W + 9;
        categoryPill("ALL", categoryX, top + HEADER_H + 7, 30, modCategory == ModCategory.ALL);
        categoryPill("NEW", categoryX + 34, top + HEADER_H + 7, 36, modCategory == ModCategory.NEW);
        categoryPill("HUD", categoryX + 74, top + HEADER_H + 7, 38, modCategory == ModCategory.HUD);
        categoryPill("SERVER", categoryX + 116, top + HEADER_H + 7, 48, modCategory == ModCategory.SERVER);
        categoryPill("MECHANIC", categoryX + 168, top + HEADER_H + 7, 60, modCategory == ModCategory.MECHANIC);
        drawSearch();
        List<AbstractModule> modules = visibleModules();
        moduleScroll = Math.min(moduleScroll, maxModuleScroll(modules.size()));
        int contentX = left + SIDEBAR_W + 9;
        int clipTop = top + HEADER_H + 28;
        int clipBottom = top + PANEL_H - 10;
        int startY = top + HEADER_H + 31 - moduleScroll;
        ScaledResolution scaled = new ScaledResolution(mc);
        int scale = scaled.getScaleFactor();
        cn.kazer.client189.compat.LegacyGL.glEnable(GL11.GL_SCISSOR_TEST);
        GL11.glScissor(contentX * scale, mc.displayHeight - clipBottom * scale,
                (left + PANEL_W - 8 - contentX) * scale, (clipBottom - clipTop) * scale);
        for (int i = 0; i < modules.size(); i++) {
            int col = i % 3;
            int row = i / 3;
            int x = contentX + col * (CARD_W + GAP);
            int y = startY + row * (CARD_H + GAP);
            drawModuleCard(modules.get(i), x, y, mouseX, mouseY);
        }
        cn.kazer.client189.compat.LegacyGL.glDisable(GL11.GL_SCISSOR_TEST);
        int max = maxModuleScroll(modules.size());
        if (max > 0) {
            int trackY = clipTop + 2;
            int trackH = clipBottom - clipTop - 4;
            int thumbH = Math.max(18, trackH * trackH / (trackH + max));
            int thumbY = trackY + (trackH - thumbH) * moduleScroll / max;
            cn.kazer.client189.compat.LegacyGui.drawRect(left + PANEL_W - 6, trackY, left + PANEL_W - 3, trackY + trackH, 0x55343A36);
            cn.kazer.client189.compat.LegacyGui.drawRect(left + PANEL_W - 6, thumbY, left + PANEL_W - 3, thumbY + thumbH, 0xCC58D6C8);
        }
    }

    private void categoryPill(String label, int x, int y, int w, boolean active) {
        RenderUtil.lIIIIlIIllIIlIIlIIIlIIllI(x, y, x + w, y + 14, 2.0D,
                active ? 0xFF4D8DE8 : 0xC64A668A);
        drawCentered(smallBoldFont(), label, x, y, w, 14, TEXT);
    }

    private int maxModuleScroll(int moduleCount) {
        int rows = (moduleCount + 2) / 3;
        int contentHeight = rows == 0 ? 0 : rows * CARD_H + (rows - 1) * GAP;
        int viewportHeight = PANEL_H - HEADER_H - 42;
        return Math.max(0, contentHeight - viewportHeight);
    }

    private int maxSettingsScroll(int settingCount) {
        if (selectedModule instanceof cn.kazer.client189.animation.BlockHitModule)
            return Math.max(0,visualLayout().height-(PANEL_H-HEADER_H-74));
        int rows = (settingCount + 1) / 2;
        return Math.max(0, rows * 21 - (PANEL_H - HEADER_H - 74));
    }

    private void drawSettingsScrollbar(int settingCount, int headerY) {
        int max = maxSettingsScroll(settingCount);
        if (max <= 0) return;
        int trackY = headerY + 56;
        int trackH = top + PANEL_H - 8 - trackY;
        int thumbH = Math.max(18, trackH * trackH / (trackH + max));
        int thumbY = trackY + (trackH - thumbH) * settingsScroll / max;
        cn.kazer.client189.compat.LegacyGui.drawRect(left + PANEL_W - 6, trackY, left + PANEL_W - 3, trackY + trackH, 0x55343A36);
        cn.kazer.client189.compat.LegacyGui.drawRect(left + PANEL_W - 6, thumbY, left + PANEL_W - 3, thumbY + thumbH, 0xCC58D6C8);
    }

    private boolean beginScrollbarDrag(int mouseX, int mouseY) {
        if (mouseX < left + PANEL_W - 9 || mouseX > left + PANEL_W - 1) return false;
        if (page == Page.SETTINGS && selectedModule == null) {
            int max = maxGlobalScroll();
            int trackY = globalRowsTop(), trackH = globalViewportHeight();
            if (max <= 0 || mouseY < trackY || mouseY > trackY + trackH) return false;
            int thumbH = Math.max(18, trackH * trackH / (trackH + max));
            int thumbY = trackY + (trackH - thumbH) * globalScroll / max;
            scrollbarGrabOffset = mouseY >= thumbY && mouseY <= thumbY + thumbH ? mouseY - thumbY : thumbH / 2;
            draggingGlobalScrollbar = true;
            updateGlobalScrollbar(mouseY);
            return true;
        }
        if (page == Page.MODS && selectedModule == null) {
            int max = maxModuleScroll(visibleModules().size());
            if (max <= 0) return false;
            int trackY = top + HEADER_H + 30;
            int trackH = top + PANEL_H - 12 - trackY;
            if (mouseY < trackY || mouseY > trackY + trackH) return false;
            int thumbH = Math.max(18, trackH * trackH / (trackH + max));
            int thumbY = trackY + (trackH - thumbH) * moduleScroll / max;
            scrollbarGrabOffset = mouseY >= thumbY && mouseY <= thumbY + thumbH
                    ? mouseY - thumbY : thumbH / 2;
            draggingModuleScrollbar = true;
            updateModuleScrollbar(mouseY);
            return true;
        }
        if (selectedModule != null) {
            int max = maxSettingsScroll(visibleSettings(selectedModule).size());
            if (max <= 0) return false;
            int headerY = top + HEADER_H + 8;
            int trackY = headerY + 56;
            int trackH = top + PANEL_H - 8 - trackY;
            if (mouseY < trackY || mouseY > trackY + trackH) return false;
            int thumbH = Math.max(18, trackH * trackH / (trackH + max));
            int thumbY = trackY + (trackH - thumbH) * settingsScroll / max;
            scrollbarGrabOffset = mouseY >= thumbY && mouseY <= thumbY + thumbH
                    ? mouseY - thumbY : thumbH / 2;
            draggingSettingsScrollbar = true;
            updateSettingsScrollbar(mouseY);
            return true;
        }
        return false;
    }

    private void updateModuleScrollbar(int mouseY) {
        int max = maxModuleScroll(visibleModules().size());
        if (max <= 0) return;
        int trackY = top + HEADER_H + 30;
        int trackH = top + PANEL_H - 12 - trackY;
        int thumbH = Math.max(18, trackH * trackH / (trackH + max));
        int range = trackH - thumbH;
        int thumbY = Math.max(trackY, Math.min(trackY + range, mouseY - scrollbarGrabOffset));
        moduleScroll = range <= 0 ? 0 : Math.round((thumbY - trackY) * max / (float)range);
    }

    private void updateSettingsScrollbar(int mouseY) {
        if (selectedModule == null) return;
        int max = maxSettingsScroll(visibleSettings(selectedModule).size());
        if (max <= 0) return;
        int trackY = top + HEADER_H + 64;
        int trackH = top + PANEL_H - 8 - trackY;
        int thumbH = Math.max(18, trackH * trackH / (trackH + max));
        int range = trackH - thumbH;
        int thumbY = Math.max(trackY, Math.min(trackY + range, mouseY - scrollbarGrabOffset));
        settingsScroll = range <= 0 ? 0 : Math.round((thumbY - trackY) * max / (float)range);
    }

    @Override
    public void handleMouseInput() throws java.io.IOException {
        super.handleMouseInput();
        int wheel = Mouse.getEventDWheel();
        if (wheel == 0) return;
        int mouseX = Mouse.getEventX() * width / mc.displayWidth;
        int mouseY = height - Mouse.getEventY() * height / mc.displayHeight - 1;
        int scrollStep = Math.max(1, Math.round(24 * ((Number)CheatBreaker.getInstance().globalSettings.scrollingSpeed.getValue()).floatValue() / 100F));
        if (page == Page.SETTINGS && selectedModule == null) {
            if (!inside(mouseX, mouseY, left + SIDEBAR_W, globalRowsTop(), PANEL_W - SIDEBAR_W, globalViewportHeight())) return;
            commitNumericEdit(); commitStringEdit();
            globalScroll = Math.max(0, Math.min(maxGlobalScroll(), globalScroll + (wheel > 0 ? -scrollStep : scrollStep)));
            return;
        }
        if (selectedModule != null) {
            if (!inside(mouseX, mouseY, left + SIDEBAR_W, top + HEADER_H,
                    PANEL_W - SIDEBAR_W, PANEL_H - HEADER_H)) return;
            int max = maxSettingsScroll(visibleSettings(selectedModule).size());
            settingsScroll = Math.max(0, Math.min(max, settingsScroll + (wheel > 0 ? -scrollStep : scrollStep)));
            return;
        }
        if (page != Page.MODS) return;
        if (!inside(mouseX, mouseY, left + SIDEBAR_W, top + HEADER_H, PANEL_W - SIDEBAR_W, PANEL_H - HEADER_H)) return;
        int direction = wheel > 0 ? -1 : 1;
        moduleScroll = Math.max(0, Math.min(maxModuleScroll(visibleModules().size()), moduleScroll + direction * scrollStep));
    }

    private void drawSearch() {
        int x = searchX;
        int y = searchY;
        RenderUtil.lIIIIlIIllIIlIIlIIIlIIllI(x - 1, y - 1, x + searchW + 1, y + searchH + 1,
                3.0D, 0xB36B6F6C);
        RenderUtil.lIIIIlIIllIIlIIlIIIlIIllI(x, y, x + searchW, y + searchH,
                3.0D, 0xB9343734);
        if (search.getText().isEmpty()) {
            cn.kazer.client189.compat.LegacyGL.glColor4f(1F, 1F, 1F, search.isFocused() ? 1F : 0.72F);
            KazerUiIcons.draw(KazerUiIcons.Icon.SEARCH, x + 5, y + 1, 12, TEXT);
            cn.kazer.client189.compat.LegacyGL.glColor4f(1F, 1F, 1F, 1F);
        } else {
            String value = search.getText() + (search.isFocused() && (System.currentTimeMillis() / 500L & 1L) == 0L ? "_" : "");
            CBFontRenderer font = regularFont();
            while (font.getMixedStringWidth(value) > searchW - 14 && value.length() > 1)
                value = value.substring(1);
            font.drawCenteredInRect(value, x + 7, y, searchW - 14, searchH, TEXT);
        }
        if (search.isFocused()) cn.kazer.client189.input.WindowsImeSupport.updateCaretPosition(
                x+searchW/2+(int)(regularFont().getMixedStringWidth(search.getText())/2),y+2,11);
    }

    private void drawModuleCard(AbstractModule module, int x, int y, int mouseX, int mouseY) {
        boolean active = module.isEnabled() && (module.getGuiAnchor() == null || module.isRenderHud());
        boolean hover = inside(mouseX, mouseY, x, y, CARD_W, CARD_H);
        RenderUtil.lIIIIlIIllIIlIIlIIIlIIllI(x, y, x + CARD_W, y + CARD_H, 5.0D, 0xB0787B78);
        RenderUtil.lIIIIlIIllIIlIIlIIIlIIllI(x + 1, y + 1, x + CARD_W - 1, y + CARD_H - 1, 4.0D,
                hover ? CARD_HOVER : CARD);
        cn.kazer.client189.compat.LegacyGui.drawRect(x + 1, y + CARD_H - 39, x + CARD_W - 1, y + CARD_H - 38, 0xB07B7E7B);
        cn.kazer.client189.compat.LegacyGui.drawRect(x + 1, y + CARD_H - 38, x + CARD_W - 1, y + CARD_H - 19, CARD_BAR);
        cn.kazer.client189.compat.LegacyGui.drawRect(x + CARD_W - 21, y + CARD_H - 38, x + CARD_W - 20, y + CARD_H - 19, 0xAA888B88);
        RenderUtil.lIIIIlIIllIIlIIlIIIlIIllI(x + 1, y + CARD_H - 19, x + CARD_W - 1,
                y + CARD_H - 1, 4.0D, active ? ON : OFF);
        cn.kazer.client189.compat.LegacyGui.drawRect(x + 1, y + CARD_H - 19, x + CARD_W - 1, y + CARD_H - 5, active ? ON : OFF);
        drawModuleIcon(module, x + CARD_W / 2, y + 31);
        boolean favorite = CheatBreaker.getInstance().globalSettings.favoriteModules.contains(module.getName());
        if (favorite || hover) KazerUiIcons.draw(KazerUiIcons.Icon.STAR, x + 5, y + 5, 12,
                favorite ? 0xFFFFD36B : inside(mouseX, mouseY, x + 3, y + 3, 16, 16) ? TEXT : 0xFF959A95);
        regularFont().drawCenteredString(module.getName(), x + CARD_W / 2F, y + 55, MUTED);
        drawCentered(smallBoldFont(), "OPTIONS", x + 1, y + CARD_H - 38, CARD_W - 22, 19, TEXT);
        cn.kazer.client189.compat.LegacyGL.glColor4f(1F, 1F, 1F, 1F);
        RenderUtil.lIIIIlIIllIIlIIlIIIlIIllI(new ResourceLocation("client/icons/cog-64.png"),
                x + CARD_W - 17, y + CARD_H - 35, 12, 12);
        drawCentered(smallBoldFont(), active ? "ENABLED" : "DISABLED",
                x + 1, y + CARD_H - 19, CARD_W - 2, 18, TEXT);
    }

    private void drawModuleIcon(AbstractModule module, int centerX, int centerY) {
        if (module instanceof ChatModule) {
            KazerUiIcons.draw(KazerUiIcons.Icon.CHAT, centerX - 12, centerY - 12, 24, TEXT);
            return;
        }
        if (module instanceof cn.kazer.client189.animation.BlockHitModule) {
            KazerUiIcons.draw(KazerUiIcons.Icon.VISUALS,centerX-16,centerY-16,32,TEXT);
            return;
        }
        cn.kazer.client189.compat.LegacyGL.glColor4f(1F, 1F, 1F, 1F);
        ResourceLocation icon = moduleIcon(module);
        if (icon != null) {
            RenderUtil.lIIIIlIIllIIlIIlIIIlIIllI(icon, centerX - 16, centerY - 16, 32, 32);
        }
    }

    private ResourceLocation moduleIcon(AbstractModule module) {
        String name;
        if (module instanceof ArmourStatusModule) name = "armor_status";
        else if (module instanceof ToggleSprintModule) name = "toggle_sprint";
        else if (module == CheatBreaker.getInstance().moduleManager.potionStatus) name = "potion_effects";
        else if (module instanceof KeystrokesModule) name = "key_strokes";
        else if (module == CheatBreaker.getInstance().moduleManager.fpsModule) name = "fps";
        else if (module == CheatBreaker.getInstance().moduleManager.cpsModule) name = "cps";
        else if (module instanceof NametagModule) name = "nametags";
        else if (module instanceof BossBarModule) name = "boss_bar";
        else if (module instanceof MotionBlurModule) name = "motion_blur";
        else if (module == CheatBreaker.getInstance().moduleManager.scoreboard) name = "scoreboard";
        else if (module instanceof ChatModule) name = "chat";
        else return null;
        return new ResourceLocation("client/icons/newui/" + name + ".png");
    }

    private void drawModuleSettings(int mouseX, int mouseY) {
        int x = left + SIDEBAR_W + 10;
        int y = top + HEADER_H + 8;
        RenderUtil.lIIIIlIIllIIlIIlIIIlIIllI(x, y, x + 18, y + 18, 9.0D,
                inside(mouseX, mouseY, x, y, 18, 18) ? 0xB0646864 : 0x80444844);
        headerFont().drawCenteredString("<", x + 9, y + 6, TEXT);
        headerFont().drawString(selectedModule.getName().toUpperCase(Locale.ROOT), x + 26, y + 7, TEXT);
        button("RESET", left + PANEL_W - 75, y, 63, 18, mouseX, mouseY);
        smallBoldFont().drawString(description(selectedModule), x, y + 27, MUTED);
        cn.kazer.client189.compat.LegacyGui.drawRect(x, y + 37, left + PANEL_W - 10, y + 38, LINE);
        boolean visuals=selectedModule instanceof cn.kazer.client189.animation.BlockHitModule;
        if(!visuals)smallBoldFont().drawString("GENERAL", x + 2, y + 45, 0xFFD7D7D7);
        List<Setting> settings = visibleSettings(selectedModule);
        int settingsViewport = top + PANEL_H - 8 - (y + 43);
        int maxSettingsScroll = maxSettingsScroll(settings.size());
        settingsScroll = Math.min(settingsScroll, maxSettingsScroll);
        int startY = y + 58 - settingsScroll;
        int colW = 181;
        ScaledResolution scaled = new ScaledResolution(mc);
        int scale = scaled.getScaleFactor();
        cn.kazer.client189.compat.LegacyGL.glEnable(GL11.GL_SCISSOR_TEST);
        GL11.glScissor(x * scale, mc.displayHeight - (top + PANEL_H - 8) * scale,
                (left + PANEL_W - 10 - x) * scale, settingsViewport * scale);
        if(visuals)drawVisualSettings(x,startY,mouseX,mouseY);
        else for (int i = 0; i < settings.size(); i++) {
            int col = i % 2;
            int row = i / 2;
            int sx = x + col * (colW + 8);
            int sy = startY + row * 21;
            drawSetting(settings.get(i), sx, sy, colW, mouseX, mouseY);
        }
        cn.kazer.client189.compat.LegacyGL.glDisable(GL11.GL_SCISSOR_TEST);
        drawSettingsScrollbar(settings.size(), y);
    }

    private cn.kazer.client189.animation.VisualSettingsLayout visualLayout() {
        cn.kazer.client189.animation.BlockHitModule m=(cn.kazer.client189.animation.BlockHitModule)selectedModule;
        return new cn.kazer.client189.animation.VisualSettingsLayout(Boolean.TRUE.equals(m.animations.getValue()),Boolean.TRUE.equals(m.items.getValue()));
    }

    private Setting visualSetting(String label) {
        for(Setting setting:selectedModule.getSettingsList())if(label.equals(setting.getLabel()))return setting;
        throw new IllegalStateException("Unknown visual preference: "+label);
    }

    private void drawVisualSettings(int x,int startY,int mouseX,int mouseY) {
        for(cn.kazer.client189.animation.VisualSettingsLayout.Row row:visualLayout().rows){
            int sx=x+row.column*189,sy=startY+row.y,w=row.wide?370:181;
            if(row.heading){smallBoldFont().drawString(row.label,sx+4,sy+4,0xFFD7D7D7);continue;}
            Setting setting=visualSetting(row.label);
            if(setting.getType()!=Setting.Type.STRING_ARRAY){drawSetting(setting,sx,sy,w,mouseX,mouseY);continue;}
            cn.kazer.client189.compat.LegacyGui.drawRect(sx,sy,sx+w,sy+20,0x163F423F);
            boolean old="1.7".equals(setting.getValue());int tx=sx+4,ty=sy+5,thumb=tx+(old?10:1);
            RenderUtil.lIIIIlIIllIIlIIlIIIlIIllI(tx,ty,tx+30,ty+11,2D,0xFF787D79);
            RenderUtil.lIIIIlIIllIIlIIlIIIlIIllI(tx+1,ty+1,tx+29,ty+10,2D,0xFF454C47);
            RenderUtil.lIIIIlIIllIIlIIlIIIlIIllI(thumb,ty+1,thumb+19,ty+10,2D,old?ON:0xFF526879);
            drawCentered(smallBoldFont(),String.valueOf(setting.getValue()),thumb,ty+1,19,9,TEXT);
            regularFont().drawString(fitText(setting.getDisplayLabel(),w-42),sx+38,sy+6,MUTED);
        }
    }

    private void drawClientSettings(int mouseX, int mouseY) {
        int x = left + SIDEBAR_W + 12;
        int y = top + HEADER_H + 10;
        headerFont().drawString("KAZER CLIENT SETTINGS", x, y + 3, TEXT);
        smallBoldFont().drawMixedString("NEW", x + 276, y + 3, 0xFFE9C668);
        smallBoldFont().drawMixedString("EXISTING", x + 307, y + 3, 0xFF58CFC2);
        cn.kazer.client189.compat.LegacyGui.drawRect(x, y + 14, left + PANEL_W - 12, y + 15, LINE);
        int tabsX = x + 4;
        int tabWidth = 116;
        int tabGap = 6;
        settingsTab("GENERAL", SettingsCategory.GENERAL, tabsX, y + 21, tabWidth, mouseX, mouseY);
        settingsTab("PERFORMANCE", SettingsCategory.PERFORMANCE, tabsX + tabWidth + tabGap, y + 21, tabWidth, mouseX, mouseY);
        settingsTab("CONTROLS", SettingsCategory.CONTROLS, tabsX + (tabWidth + tabGap) * 2, y + 21, tabWidth, mouseX, mouseY);
        globalScroll = Math.min(globalScroll, maxGlobalScroll());
        int row = 0;
        ScaledResolution scaled = new ScaledResolution(mc);
        int scale = scaled.getScaleFactor();
        cn.kazer.client189.compat.LegacyGL.glEnable(GL11.GL_SCISSOR_TEST);
        GL11.glScissor((x - 7) * scale, mc.displayHeight - (top + PANEL_H - 8) * scale,
                (left + PANEL_W - x + 2) * scale, globalViewportHeight() * scale);
        for (Setting setting : globalSettingsForCategory()) {
            int col = row % 2;
            int sy = globalRowsTop() + (row / 2) * 24 - globalScroll;
            drawSetting(setting, x + col * 188, sy, 180, mouseX, mouseY);
            cn.kazer.client189.compat.LegacyGui.drawRect(x + col * 188 - 5, sy + 8, x + col * 188 - 2, sy + 11,
                    isReferenceAddition(setting) ? 0xFFE9C668 : 0xFF58CFC2);
            row++;
        }
        cn.kazer.client189.compat.LegacyGL.glDisable(GL11.GL_SCISSOR_TEST);
        int max = maxGlobalScroll();
        if (max > 0) {
            int h = globalViewportHeight(), thumbH = Math.max(18, h * h / (h + max));
            int thumbY = globalRowsTop() + (h - thumbH) * globalScroll / max;
            cn.kazer.client189.compat.LegacyGui.drawRect(left + PANEL_W - 6, globalRowsTop(), left + PANEL_W - 3, globalRowsTop() + h, 0x55343A36);
            cn.kazer.client189.compat.LegacyGui.drawRect(left + PANEL_W - 6, thumbY, left + PANEL_W - 3, thumbY + thumbH, 0xCC58D6C8);
        }
    }

    private int globalRowsTop() { return top + HEADER_H + 60; }
    private int globalViewportHeight() { return top + PANEL_H - 8 - globalRowsTop(); }
    private int maxGlobalScroll() { return Math.max(0, ((globalSettingsForCategory().size() + 1) / 2) * 24 - globalViewportHeight()); }
    private void updateGlobalScrollbar(int mouseY) {
        int max = maxGlobalScroll(), h = globalViewportHeight();
        int thumbH = Math.max(18, h * h / (h + max)), range = h - thumbH;
        globalScroll = range <= 0 ? 0 : Math.round(Math.max(0, Math.min(range, mouseY - scrollbarGrabOffset - globalRowsTop())) * max / (float)range);
    }

    private boolean isReferenceAddition(Setting setting) {
        GlobalSettings g = CheatBreaker.getInstance().globalSettings;
        return setting == g.particleAmount || setting == g.limitUnfocusedFps || setting == g.maxUnfocusedFps
                || setting == g.maxMainMenuFps || setting == g.entityDistance || setting == g.tileEntityDistance
                || setting == g.keepInventoryCentered || setting == g.disableHotbarScrolling || setting == g.showAchievements
                || setting == g.showCrosshairThirdPerson || setting == g.scrollingSpeed
                || setting == CheatBreaker.getInstance().moduleManager.nametagModule.showNametagIcon;
    }

    private void settingsTab(String label, SettingsCategory category, int x, int y, int w,
                             int mouseX, int mouseY) {
        boolean active = settingsCategory == category;
        boolean hover = inside(mouseX, mouseY, x, y, w, 20);
        RenderUtil.lIIIIlIIllIIlIIlIIIlIIllI(x, y, x + w, y + 20, 3.0D,
                active ? 0xDD4D8DE8 : hover ? 0x88434C47 : 0x55343B37);
        drawCentered(smallBoldFont(), label, x, y, w, 20, active ? TEXT : MUTED);
    }

    private List<Setting> globalSettingsForCategory() {
        List<Setting> result = new ArrayList<>();
        GlobalSettings g = CheatBreaker.getInstance().globalSettings;
        for (Setting setting : CheatBreaker.getInstance().globalSettings.settingsList) {
            if ("label".equalsIgnoreCase(setting.getLabel()) || !setting.isVisible()) continue;
            if (setting == CheatBreaker.getInstance().globalSettings.showChatBackground) continue; // Now owned by Chat.
            boolean performance = setting == CheatBreaker.getInstance().globalSettings.enableFpsBoost
                    || setting == CheatBreaker.getInstance().globalSettings.slowChunkLoading
                    || setting == CheatBreaker.getInstance().globalSettings.fullBright
                    || setting == CheatBreaker.getInstance().globalSettings.enchantmentGlint
                    || setting == CheatBreaker.getInstance().globalSettings.clearGlass
                    || setting == CheatBreaker.getInstance().globalSettings.redString
                    || setting == g.particleAmount || setting == g.limitUnfocusedFps || setting == g.maxUnfocusedFps
                    || setting == g.maxMainMenuFps || setting == g.entityDistance || setting == g.tileEntityDistance;
            boolean controls = setting == CheatBreaker.getInstance().globalSettings.snapModules
                    || setting == CheatBreaker.getInstance().globalSettings.showChatBackground
                    || setting == CheatBreaker.getInstance().globalSettings.showPotionInfo
                    || setting == CheatBreaker.getInstance().globalSettings.customCrosshair
                    || setting == CheatBreaker.getInstance().globalSettings.crosshairOutline
                    || setting == CheatBreaker.getInstance().globalSettings.crosshairColor
                    || setting == CheatBreaker.getInstance().globalSettings.crosshairThickness
                    || setting == CheatBreaker.getInstance().globalSettings.crosshairSize
                    || setting == CheatBreaker.getInstance().globalSettings.crosshairGap
                    || setting == g.disableHotbarScrolling;
            if (settingsCategory == SettingsCategory.PERFORMANCE && performance
                    || settingsCategory == SettingsCategory.CONTROLS && controls
                    || settingsCategory == SettingsCategory.GENERAL && !performance && !controls) {
                result.add(setting);
            }
        }
        if (settingsCategory == SettingsCategory.GENERAL) {
            result.add(CheatBreaker.getInstance().moduleManager.nametagModule.showBadgeInTab);
            result.add(CheatBreaker.getInstance().moduleManager.nametagModule.showNametagIcon);
        }
        if (settingsCategory == SettingsCategory.CONTROLS) {
            if (referenceBindings.isEmpty()) {
                referenceBindings.put(new Setting("Mod Menu").setValue(0, false), g.openMenu);
                referenceBindings.put(new Setting("Freelook").setValue(0, false), g.dragLook);
                referenceBindings.put(new Setting("Zoom").setValue(0, false), cn.kazer.client189.compat.KazerCompat.zoomKey());
            }
            result.addAll(referenceBindings.keySet());
        }
        return result;
    }

    private void drawSetting(Setting setting, int x, int y, int w, int mouseX, int mouseY) {
        cn.kazer.client189.compat.LegacyGui.drawRect(x, y, x + w, y + 20, 0x163F423F);
        String label = setting.getDisplayLabel();
        if (referenceBindings.containsKey(setting)) {
            regularFont().drawMixedString(label, x + 4, y + 6, MUTED);
            button(recordingBinding == setting ? "PRESS KEY" : GameSettings.getKeyDisplayString(referenceBindings.get(setting).getKeyCode()),
                    x + w / 2, y + 2, w / 2 - 3, 16, mouseX, mouseY);
            return;
        }
        if ("label".equalsIgnoreCase(setting.getLabel())) {
            smallBoldFont().drawString(String.valueOf(setting.getValue()).toUpperCase(Locale.ROOT), x + 3, y + 7, 0xFFD0D0D0);
            return;
        }
        if (setting.getType() == Setting.Type.BOOLEAN) {
            drawSwitch((Boolean) setting.getValue(), x + 4, y + 5);
            regularFont().drawString(fitText(label, w - 42), x + 38, y + 6, MUTED);
        } else if (setting.getType() == Setting.Type.STRING_ARRAY) {
            int valueAreaX = x + w / 2;
            regularFont().drawString(fitText(label, valueAreaX - x - 8), x + 4, y + 6, MUTED);
            drawChevron(valueAreaX + 5, y + 10, false);
            drawChevron(x + w - 6, y + 10, true);
            drawCentered(regularFont(), fitText(String.valueOf(setting.getValue()), w / 2 - 26),
                    valueAreaX + 14, y, w / 2 - 28, 20, MUTED);
        } else if (setting.getType() == Setting.Type.STRING) {
            int valueAreaX = x + w / 2;
            regularFont().drawString(fitText(label, valueAreaX - x - 8), x + 4, y + 6, MUTED);
            boolean editing = setting == editingStringSetting;
            String value = editing ? editingStringText : String.valueOf(setting.getValue());
            drawValueField(valueAreaX, y + 2, w / 2 - 3, 16, editing,
                    inside(mouseX, mouseY, valueAreaX, y, w / 2, 20));
            if (editing) {
                if ((System.currentTimeMillis() / 500L & 1L) == 0L) value += "|";
            }
            value = fitText(value, w / 2 - 10);
            drawCentered(regularFont(), value, valueAreaX + 3, y + 2, w / 2 - 9, 16, TEXT);
        } else if (setting.getType() == Setting.Type.FLOAT || setting.getType() == Setting.Type.DOUBLE || setting.getType() == Setting.Type.INTEGER) {
            if (setting.getLabel().toLowerCase(Locale.ROOT).contains("color")) {
                regularFont().drawString(fitText(label, w - 32), x + 4, y + 6, MUTED);
                RenderUtil.lIIIIlIIllIIlIIlIIIlIIllI(x + w - 19, y + 4, x + w - 5, y + 16,
                        2.0D, 0xFF777B78);
                RenderUtil.lIIIIlIIllIIlIIlIIIlIIllI(x + w - 18, y + 5, x + w - 6, y + 15,
                        1.0D, setting.getColorValue());
            } else {
                int trackX = numericTrackX(setting, x, w);
                int trackW = numericTrackW(setting, x, w);
                regularFont().drawString(fitText(label, trackX - x - 8), x + 4, y + 6, MUTED);
                Number min = setting.getMinimumValue();
                Number max = setting.getMaximumValue();
                if (min != null && max != null && max.doubleValue() > min.doubleValue()) {
                    double percent = (((Number)setting.getValue()).doubleValue() - min.doubleValue())
                            / (max.doubleValue() - min.doubleValue());
                    percent = Math.max(0D, Math.min(1D, percent));
                    RenderUtil.lIIIIlIIllIIlIIlIIIlIIllI(trackX, y + 9, trackX + trackW, y + 12, 1.5D, 0xFF434B47);
                    int thumb = trackX + (int)Math.round(trackW * percent);
                    if (thumb > trackX) RenderUtil.lIIIIlIIllIIlIIlIIIlIIllI(trackX, y + 9, thumb, y + 12, 1.5D, 0xFF368E85);
                    RenderUtil.lIIIIlIIllIIlIIlIIIlIIllI(thumb - 3, y + 6, thumb + 3, y + 15, 2D, 0xFF58CFC2);
                }
                boolean editing = setting == editingNumericSetting;
                String value = editing ? editingNumericText : formatNumber(setting);
                drawValueField(x + w - 39, y + 2, 36, 16, editing,
                        inside(mouseX, mouseY, x + w - 42, y, 42, 20));
                if (editing) {
                    if ((System.currentTimeMillis() / 500L & 1L) == 0L) value += "|";
                }
                value = fitText(value, 32);
                drawCentered(regularFont(), value, x + w - 39, y + 2, 36, 16, TEXT);
            }
        }
    }

    private String fitText(String value, int maxWidth) {
        if (value == null) return "";
        if (maxWidth <= 0) return "";
        if (regularFont().getStringWidth(value) <= maxWidth) return value;
        String suffix = "...";
        int suffixWidth = regularFont().getStringWidth(suffix);
        if (suffixWidth >= maxWidth) return "";
        String shortened = value;
        while (shortened.length() > 0
                && regularFont().getStringWidth(shortened) + suffixWidth > maxWidth) {
            shortened = shortened.substring(0, shortened.length() - 1);
        }
        return shortened + suffix;
    }

    private int numericTrackX(Setting setting, int x, int w) {
        return x + Math.max(72, w / 2);
    }

    private int numericTrackW(Setting setting, int x, int w) {
        int trackX = numericTrackX(setting, x, w);
        return Math.max(24, x + w - 43 - trackX);
    }

    private String formatNumber(Setting setting) {
        if (setting.getType() == Setting.Type.INTEGER) return String.valueOf(setting.getValue());
        return String.format(Locale.ROOT, "%.2f", ((Number)setting.getValue()).doubleValue()).replaceAll("0+$", "").replaceAll("\\.$", "");
    }

    private void drawSwitch(boolean value, int x, int y) {
        RenderUtil.lIIIIlIIllIIlIIlIIIlIIllI(x, y, x + 30, y + 11, 2D, 0xFF787D79);
        RenderUtil.lIIIIlIIllIIlIIlIIIlIIllI(x + 1, y + 1, x + 29, y + 10, 2D, 0xFF454C47);
        int thumbX = x + (value ? 10 : 1);
        RenderUtil.lIIIIlIIllIIlIIlIIIlIIllI(thumbX, y + 1, thumbX + 19, y + 10, 2D, value ? ON : OFF);
        drawCentered(smallBoldFont(), value ? "ON" : "OFF", thumbX, y + 1, 19, 9, TEXT);
    }

    private void drawValueField(int x, int y, int w, int h, boolean editing, boolean hover) {
        RenderUtil.lIIIIlIIllIIlIIlIIIlIIllI(x, y, x + w, y + h, 2D,
                editing ? 0xFF538F88 : hover ? 0x99657068 : 0x6649524D);
        RenderUtil.lIIIIlIIllIIlIIlIIIlIIllI(x + 1, y + 1, x + w - 1, y + h - 1, 1D,
                editing ? 0xEF242F2B : 0xA51A211E);
    }

    private void drawChevron(int x, int y, boolean right) {
        for (int i = 0; i < 4; i++) {
            int dx = right ? i : -i;
            cn.kazer.client189.compat.LegacyGui.drawRect(x + dx, y - 4 + i, x + dx + 2, y - 2 + i, 0xFF38A7B5);
            cn.kazer.client189.compat.LegacyGui.drawRect(x + dx, y + 2 - i, x + dx + 2, y + 4 - i, 0xFF38A7B5);
        }
    }

    private void drawCentered(CBFontRenderer font, String text, int x, int y, int w, int h, int color) {
        font.drawCenteredInRect(text, x, y, w, h, color);
    }

    private List<AbstractModule> visibleModules() {
        String query = search == null ? "" : search.getText().trim().toLowerCase(Locale.ROOT);
        List<AbstractModule> result = new ArrayList<>();
        for (AbstractModule module : CheatBreaker.getInstance().moduleManager.modules) {
            if (!matchesCategory(module)) continue;
            if (query.isEmpty() || module.getName().toLowerCase(Locale.ROOT).contains(query)) result.add(module);
        }
        result.sort((a, b) -> Boolean.compare(
                CheatBreaker.getInstance().globalSettings.favoriteModules.contains(b.getName()),
                CheatBreaker.getInstance().globalSettings.favoriteModules.contains(a.getName())));
        return result;
    }

    private boolean matchesCategory(AbstractModule module) {
        switch (modCategory) {
            case NEW:
                return module instanceof NametagModule || module instanceof MotionBlurModule || module instanceof ChatModule;
            case SERVER:
                return module instanceof BossBarModule || module == CheatBreaker.getInstance().moduleManager.scoreboard;
            case MECHANIC:
                return module instanceof ToggleSprintModule || module instanceof MotionBlurModule || module instanceof ChatModule
                        || module instanceof cn.kazer.client189.animation.BlockHitModule;
            case HUD:
                return !(module instanceof ToggleSprintModule) && !(module instanceof MotionBlurModule)
                        && !(module instanceof cn.kazer.client189.animation.BlockHitModule);
            default:
                return true;
        }
    }

    private List<Setting> visibleSettings(AbstractModule module) {
        List<Setting> result = new ArrayList<>();
        for (Setting setting : module.getSettingsList()) {
            if (setting.isVisible() && isImplementedSetting(module, setting)) result.add(setting);
        }
        return result;
    }

    /** Do not expose legacy placeholders whose old renderer never consumed their values. */
    static boolean isImplementedSetting(AbstractModule module, Setting setting) {
        String label = setting.getLabel();
        if(module instanceof cn.kazer.client189.animation.BlockHitModule&&"Scale".equals(label))return false;
        if ("label".equalsIgnoreCase(label) || "Scale".equalsIgnoreCase(label)) return true;
        if (module instanceof ArmourStatusModule) {
            return !equalsAny(label, "Protection", "Armor Bars", "Show Helmet", "Show Chestplate",
                    "Show Leggings", "Show Boots", "Move Armor Pieces Individually", "Vanilla Mode",
                    "Text Shadow", "Show Background");
        }
        if (module == CheatBreaker.getInstance().moduleManager.potionStatus) {
            return equalsAny(label, "Show While Typing", "Show Effect Name", "Blink", "Blink Duration",
                    "Name Color", "Duration Color");
        }
        if (module instanceof KeystrokesModule) {
            return !equalsAny(label, "Mode", "Text Shadow", "Border", "Animate", "Border Thickness");
        }
        if (module instanceof NametagModule) {
            return !equalsAny(label, "Display Toggle Nametags Message", "Replace Own Nametag Color");
        }
        if (module == CheatBreaker.getInstance().moduleManager.scoreboard) {
            return !equalsAny(label, "Border Thickness", "Border", "Display Toggle Message");
        }
        if (module instanceof BossBarModule) {
            return !equalsAny(label, "Use Custom Boss Bar", "Use Minecraft GUI Scale");
        }
        return true;
    }

    private static boolean equalsAny(String value, String... candidates) {
        for (String candidate : candidates) {
            if (candidate.equalsIgnoreCase(value)) return true;
        }
        return false;
    }

    private String description(AbstractModule module) {
        if (module instanceof ArmourStatusModule) return "Displays equipped armor and the held item on the HUD.";
        if (module instanceof BossBarModule) return "Hide, move or customize the Minecraft boss bar.";
        if (module instanceof NametagModule) return "Customize player nametags.";
        if (module instanceof MotionBlurModule) return "Blur your surroundings while moving.";
        if (module instanceof cn.kazer.client189.animation.BlockHitModule) return "Sk1er OldAnimations: 1.7 item positions, blocking and consuming animations.";
        if (module instanceof ToggleSprintModule) return "Make sprinting and sneaking toggleable.";
        if (module instanceof ChatModule) return "Customize chat visibility and copy notifications.";
        if (module == CheatBreaker.getInstance().moduleManager.potionStatus) return "Displays active potion effects on the HUD.";
        if (module == CheatBreaker.getInstance().moduleManager.scoreboard) return "Hide, move or customize the scoreboard.";
        return "Configure " + module.getName() + " and its HUD appearance.";
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int button) throws java.io.IOException {
        super.mouseClicked(mouseX, mouseY, button);
        if (colorEditor != null) {
            int x = left + PANEL_W / 2 - 105;
            int y = top + PANEL_H / 2 - 62;
            for (int channel = 0; channel < 4; channel++) {
                int rowY = y + 28 + channel * 18;
                if (inside(mouseX, mouseY, x + 24, rowY, 154, 14)) {
                    draggingColorChannel = channel;
                    setColorChannelFromMouse(channel, mouseX);
                    return;
                }
            }
            if (inside(mouseX, mouseY, x + 116, y + 101, 84, 16)) {
                colorEditor = null;
                draggingColorChannel = -1;
            }
            return;
        }
        if (creatingProfile) {
            profileName.mouseClicked(mouseX, mouseY, button);
            int x = left + PANEL_W / 2 - 100;
            int y = top + PANEL_H / 2 - 38;
            if (inside(mouseX, mouseY, x + 10, y + 55, 84, 18)) saveNewProfile();
            else if (inside(mouseX, mouseY, x + 106, y + 55, 84, 18)) closeProfileDialog();
            return;
        }
        if (editingNumericSetting != null) commitNumericEdit();
        if (editingStringSetting != null) commitStringEdit();
        if (button == 0 && beginScrollbarDrag(mouseX, mouseY)) return;
        if (page == Page.MODS && selectedModule == null) search.mouseClicked(mouseX, mouseY, button);
        else search.setFocused(false);
        if (inside(mouseX, mouseY, left + PANEL_W - 29, top + 8, 20, 20)) {
            mc.displayGuiScreen(parent);
            return;
        }
        if (inside(mouseX, mouseY, left + 126, top + 9, 54, 22)) {
            page = Page.MODS; selectedModule = null; return;
        }
        if (inside(mouseX, mouseY, left + 188, top + 9, 68, 22)) {
            page = Page.SETTINGS; selectedModule = null; return;
        }
        if (inside(mouseX, mouseY, left + 7, top + PANEL_H - 25, SIDEBAR_W - 14, 17)) {
            mc.displayGuiScreen(parent);
            return;
        }
        if (inside(mouseX, mouseY, left + 7, top + PANEL_H - 47, SIDEBAR_W - 14, 16)) {
            creatingProfile = true;
            profileError = null;
            profileName.setText("");
            profileName.setFocused(true);
            search.setFocused(false);
            return;
        }
        if (handleProfileClick(mouseX, mouseY)) return;
        if (page == Page.MODS && selectedModule == null && handleCategoryClick(mouseX, mouseY)) return;
        if (selectedModule != null) {
            handleSettingsClick(mouseX, mouseY);
        } else if (page == Page.MODS) {
            handleModuleClick(mouseX, mouseY);
        } else {
            handleGlobalClick(mouseX, mouseY);
        }
    }

    private boolean handleCategoryClick(int mouseX, int mouseY) {
        int x = left + SIDEBAR_W + 9;
        int y = top + HEADER_H + 7;
        ModCategory category = null;
        if (inside(mouseX, mouseY, x, y, 30, 14)) category = ModCategory.ALL;
        else if (inside(mouseX, mouseY, x + 34, y, 36, 14)) category = ModCategory.NEW;
        else if (inside(mouseX, mouseY, x + 74, y, 38, 14)) category = ModCategory.HUD;
        else if (inside(mouseX, mouseY, x + 116, y, 48, 14)) category = ModCategory.SERVER;
        else if (inside(mouseX, mouseY, x + 168, y, 60, 14)) category = ModCategory.MECHANIC;
        if (category == null) return false;
        modCategory = category;
        moduleScroll = 0;
        return true;
    }

    private boolean handleProfileClick(int mouseX, int mouseY) {
        int profileY = top + 62;
        List<Profile> profiles = new ArrayList<>(CheatBreaker.getInstance().profiles);
        for (Profile profile : profiles) {
            if (profileY + 20 >= top + PANEL_H - 54) break;
            if (profile.isEditable() && inside(mouseX, mouseY, left + SIDEBAR_W - 20, profileY + 4, 12, 12)) {
                deleteProfile(profile);
                return true;
            }
            if (inside(mouseX, mouseY, left + 7, profileY, SIDEBAR_W - 14, 20)) {
                switchProfile(profile);
                return true;
            }
            profileY += 22;
        }
        return false;
    }

    private void switchProfile(Profile profile) {
        if (profile == null || profile == CheatBreaker.getInstance().activeProfile) return;
        Profile old = CheatBreaker.getInstance().activeProfile;
        if (old != null && old.isEditable()) CheatBreaker.getInstance().configManager.writeProfile(old.getName());
        CheatBreaker.getInstance().activeProfile = profile;
        CheatBreaker.getInstance().configManager.readProfile(profile.getName());
        CheatBreaker.getInstance().configManager.writeGlobalConfig();
    }

    private void deleteProfile(Profile profile) {
        if (profile == null || !profile.isEditable()) return;
        boolean wasActive = profile == CheatBreaker.getInstance().activeProfile;
        if (!CheatBreaker.getInstance().configManager.deleteProfile(profile)) return;
        CheatBreaker.getInstance().profiles.remove(profile);
        for (int i = 0; i < CheatBreaker.getInstance().profiles.size(); i++) {
            CheatBreaker.getInstance().profiles.get(i).setIndex(i);
        }
        if (wasActive) {
            Profile fallback = CheatBreaker.getInstance().profiles.get(0);
            CheatBreaker.getInstance().activeProfile = fallback;
            CheatBreaker.getInstance().configManager.readProfile(fallback.getName());
        }
        CheatBreaker.getInstance().configManager.writeGlobalConfig();
    }

    private void handleModuleClick(int mouseX, int mouseY) {
        if (mouseY < top + HEADER_H + 28 || mouseY >= top + PANEL_H - 7) return;
        List<AbstractModule> modules = visibleModules();
        int contentX = left + SIDEBAR_W + 9;
        int startY = top + HEADER_H + 31 - moduleScroll;
        for (int i = 0; i < modules.size(); i++) {
            int x = contentX + (i % 3) * (CARD_W + GAP);
            int y = startY + (i / 3) * (CARD_H + GAP);
            if (!inside(mouseX, mouseY, x, y, CARD_W, CARD_H)) continue;
            AbstractModule module = modules.get(i);
            if (inside(mouseX, mouseY, x + 3, y + 3, 16, 16)) {
                CheatBreaker.getInstance().globalSettings.favoriteModules.toggle(module.getName());
                CheatBreaker.getInstance().configManager.writeGlobalConfig();
            } else if (inside(mouseX, mouseY, x, y + CARD_H - 19, CARD_W, 19)) {
                boolean enable = com.cheatbreaker.client.module.ModuleToggleState.next(module.isEnabled(), module.getGuiAnchor() != null, module.isRenderHud());
                module.setState(enable);
                if (module.getGuiAnchor() != null) module.setRenderHud(enable);
            } else {
                selectedModule = module;
                settingsScroll = 0;
                search.setFocused(false);
            }
            return;
        }
    }

    private void handleSettingsClick(int mouseX, int mouseY) {
        int x = left + SIDEBAR_W + 10;
        int y = top + HEADER_H + 8;
        if (inside(mouseX, mouseY, x, y, 18, 18)) {
            selectedModule = null;
            return;
        }
        if (inside(mouseX, mouseY, left + PANEL_W - 75, y, 63, 18)) {
            selectedModule.resetToDefaults();
            settingsScroll = 0;
            CheatBreaker.getInstance().configManager.write();
            return;
        }
        List<Setting> settings = visibleSettings(selectedModule);
        if (mouseY < y + 54 || mouseY >= top + PANEL_H - 8) return;
        int startY = y + 58 - settingsScroll;
        int colW = 181;
        if(selectedModule instanceof cn.kazer.client189.animation.BlockHitModule){
            for(cn.kazer.client189.animation.VisualSettingsLayout.Row row:visualLayout().rows){
                int sx=x+row.column*189,sy=startY+row.y,w=row.wide?370:181;
                if(row.heading||!inside(mouseX,mouseY,sx,sy,w,20))continue;
                Setting setting=visualSetting(row.label);
                if(setting.getType()==Setting.Type.STRING_ARRAY){
                    setting.setValue("1.7".equals(setting.getValue())?"1.8":"1.7");afterSettingChanged(setting);
                }else activateSetting(setting,mouseX,sx,w);
                return;
            }
            return;
        }
        for (int i = 0; i < settings.size(); i++) {
            int sx = x + (i % 2) * (colW + 8);
            int sy = startY + (i / 2) * 21;
            if (inside(mouseX, mouseY, sx, sy, colW, 20)) {
                activateSetting(settings.get(i), mouseX, sx, colW);
                return;
            }
        }
    }

    private void handleGlobalClick(int mouseX, int mouseY) {
        int x = left + SIDEBAR_W + 12;
        int headerY = top + HEADER_H + 10;
        int tabsX = x + 4;
        int tabWidth = 116;
        int tabGap = 6;
        if (inside(mouseX, mouseY, tabsX, headerY + 21, tabWidth, 20)) {
            settingsCategory = SettingsCategory.GENERAL;
            globalScroll = 0; recordingBinding = null;
            return;
        }
        if (inside(mouseX, mouseY, tabsX + tabWidth + tabGap, headerY + 21, tabWidth, 20)) {
            settingsCategory = SettingsCategory.PERFORMANCE;
            globalScroll = 0; recordingBinding = null;
            return;
        }
        if (inside(mouseX, mouseY, tabsX + (tabWidth + tabGap) * 2, headerY + 21, tabWidth, 20)) {
            settingsCategory = SettingsCategory.CONTROLS;
            globalScroll = 0; recordingBinding = null;
            return;
        }
        if (mouseY < globalRowsTop() || mouseY >= top + PANEL_H - 8) return;
        int y = globalRowsTop() - globalScroll;
        int row = 0;
        for (Setting setting : globalSettingsForCategory()) {
            int sx = x + (row % 2) * 188;
            int sy = y + (row / 2) * 24;
            if (inside(mouseX, mouseY, sx, sy, 180, 20)) {
                activateSetting(setting, mouseX, sx, 180);
                return;
            }
            row++;
        }
    }

    private void activateSetting(Setting setting, int mouseX, int x, int w) {
        if (referenceBindings.containsKey(setting)) { recordingBinding = setting; return; }
        if (setting.getType() == Setting.Type.BOOLEAN) {
            setting.setValue(!((Boolean) setting.getValue()));
            afterSettingChanged(setting);
        } else if (setting.getType() == Setting.Type.STRING_ARRAY) {
            String[] values = setting.getAcceptedValues();
            if (values == null || values.length == 0) return;
            int current = 0;
            for (int i = 0; i < values.length; i++) if (values[i].equals(setting.getValue())) current = i;
            int direction;
            if (mouseX >= x + w / 2 && mouseX < x + w / 2 + 18) direction = -1;
            else if (mouseX >= x + w - 18) direction = 1;
            else return;
            setting.setValue(values[(current + direction + values.length) % values.length]);
            afterSettingChanged(setting);
        } else if (setting.getType() == Setting.Type.STRING) {
            beginStringEdit(setting);
        } else if ((setting.getType() == Setting.Type.FLOAT || setting.getType() == Setting.Type.DOUBLE || setting.getType() == Setting.Type.INTEGER)
                && setting.getMinimumValue() != null && setting.getMaximumValue() != null
                && setting.getLabel().toLowerCase(Locale.ROOT).contains("color")) {
            colorEditor = setting;
        } else if ((setting.getType() == Setting.Type.FLOAT || setting.getType() == Setting.Type.DOUBLE || setting.getType() == Setting.Type.INTEGER)
                && setting.getMinimumValue() != null && setting.getMaximumValue() != null) {
            int trackX = numericTrackX(setting, x, w);
            int trackW = numericTrackW(setting, x, w);
            if (mouseX >= x + w - 42) {
                beginNumericEdit(setting);
                return;
            }
            if (mouseX < trackX - 3 || mouseX > trackX + trackW + 3) return;
            draggingSetting = setting;
            draggingSettingX = trackX;
            draggingSettingW = trackW;
            setNumericFromMouse(setting, mouseX, trackX, trackW);
        }
    }

    private void setNumericFromMouse(Setting setting, int mouseX, int x, int w) {
        double percent = Math.max(0D, Math.min(1D, (mouseX - x) / (double) w));
        double min = setting.getMinimumValue().doubleValue();
        double max = setting.getMaximumValue().doubleValue();
        double value = min + (max - min) * percent;
        if (setting.getType() == Setting.Type.INTEGER) setting.setValue((int)Math.round(value));
        else if (setting.getType() == Setting.Type.FLOAT) setting.setValue((float)(Math.round(value * 100D) / 100D));
        else setting.setValue(Math.round(value * 100D) / 100D);
        afterSettingChanged(setting);
    }

    private void beginNumericEdit(Setting setting) {
        if (editingStringSetting != null) commitStringEdit();
        editingNumericSetting = setting;
        editingNumericText = formatNumber(setting);
        replaceNumericText = true;
        draggingSetting = null;
    }

    private void commitNumericEdit() {
        if (editingNumericSetting == null) return;
        Number parsed = parseNumericValue(editingNumericSetting, editingNumericText);
        if (parsed != null) {
            editingNumericSetting.setValue(parsed);
            afterSettingChanged(editingNumericSetting);
        }
        editingNumericSetting = null;
        editingNumericText = "";
        replaceNumericText = false;
    }

    private void cancelNumericEdit() {
        editingNumericSetting = null;
        editingNumericText = "";
        replaceNumericText = false;
    }

    private void beginStringEdit(Setting setting) {
        if (editingNumericSetting != null) commitNumericEdit();
        editingStringSetting = setting;
        editingStringText = String.valueOf(setting.getValue());
        replaceStringText = true;
        draggingSetting = null;
        search.setFocused(false);
    }

    private void commitStringEdit() {
        if (editingStringSetting == null) return;
        editingStringSetting.setValue(editingStringText);
        afterSettingChanged(editingStringSetting);
        editingStringSetting = null;
        editingStringText = "";
        replaceStringText = false;
    }

    private void cancelStringEdit() {
        editingStringSetting = null;
        editingStringText = "";
        replaceStringText = false;
    }

    static Number parseNumericValue(Setting setting, String value) {
        if (setting == null || value == null || value.trim().isEmpty()) return null;
        try {
            double parsed = Double.parseDouble(value.trim());
            if (!Double.isFinite(parsed)) return null;
            double minimum = setting.getMinimumValue() == null ? -Double.MAX_VALUE
                    : setting.getMinimumValue().doubleValue();
            double maximum = setting.getMaximumValue() == null ? Double.MAX_VALUE
                    : setting.getMaximumValue().doubleValue();
            parsed = Math.max(minimum, Math.min(maximum, parsed));
            if (setting.getType() == Setting.Type.INTEGER) return (int)Math.round(parsed);
            if (setting.getType() == Setting.Type.FLOAT) return (float)parsed;
            return parsed;
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private void afterSettingChanged(Setting setting) {
        if (selectedModule instanceof KeystrokesModule
                && CheatBreaker.getInstance().moduleManager.keyStrokes != null) {
            CheatBreaker.getInstance().moduleManager.keyStrokes.initialize();
        }
        if ((setting == CheatBreaker.getInstance().globalSettings.clearGlass
                || setting == CheatBreaker.getInstance().globalSettings.redString)
                && mc.renderGlobal != null) {
            mc.renderGlobal.loadRenderers();
        }
        if (setting == CheatBreaker.getInstance().globalSettings.worldTime
                && mc.theWorld != null) {
            mc.theWorld.setWorldTime(((Number)setting.getValue()).longValue());
        }
        if (setting == CheatBreaker.getInstance().globalSettings.guiBlur) {
            if ((Boolean)setting.getValue()) cn.kazer.client189.compat.KazerCompat.loadGuiBlur();
            else mc.entityRenderer.stopUseShader();
        }
        if (setting == CheatBreaker.getInstance().globalSettings.defaultColor) {
            int color = ((Number)setting.getValue()).intValue();
            for (AbstractModule module : CheatBreaker.getInstance().moduleManager.modules) {
                for (Setting moduleSetting : module.getSettingsList()) {
                    String label = moduleSetting.getLabel().toLowerCase(Locale.ROOT);
                    if (moduleSetting.getType() == Setting.Type.INTEGER
                            && (label.equals("text color") || label.endsWith(" text color"))) {
                        moduleSetting.setValue(color);
                    }
                }
            }
        }
    }

    @Override
    protected void mouseReleased(int mouseX, int mouseY, int button) {
        super.mouseReleased(mouseX, mouseY, button);
        if (button == 0) {
            draggingSetting = null;
            draggingColorChannel = -1;
            draggingModuleScrollbar = false;
            draggingSettingsScrollbar = false;
            draggingGlobalScrollbar = false;
        }
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) throws java.io.IOException {
        if (recordingBinding != null) {
            KeyBinding binding = referenceBindings.get(recordingBinding);
            binding.setKeyCode(keyCode == Keyboard.KEY_ESCAPE ? 0 : keyCode);
            KeyBinding.resetKeyBindingArrayAndHash();
            CheatBreaker.getInstance().configManager.writeGlobalConfig();
            recordingBinding = null;
            return;
        }
        if (creatingProfile) {
            if (keyCode == Keyboard.KEY_ESCAPE) closeProfileDialog();
            else if (keyCode == Keyboard.KEY_RETURN) saveNewProfile();
            else profileName.textboxKeyTyped(typedChar, keyCode);
            return;
        }
        if (editingNumericSetting != null) {
            if (keyCode == Keyboard.KEY_ESCAPE) {
                cancelNumericEdit();
            } else if (keyCode == Keyboard.KEY_RETURN || keyCode == Keyboard.KEY_NUMPADENTER || keyCode == Keyboard.KEY_TAB) {
                commitNumericEdit();
            } else if (keyCode == Keyboard.KEY_A && (Keyboard.isKeyDown(Keyboard.KEY_LCONTROL)
                    || Keyboard.isKeyDown(Keyboard.KEY_RCONTROL))) {
                replaceNumericText = true;
            } else if (keyCode == Keyboard.KEY_BACK || keyCode == Keyboard.KEY_DELETE) {
                if (replaceNumericText) editingNumericText = "";
                else if (!editingNumericText.isEmpty()) {
                    editingNumericText = editingNumericText.substring(0, editingNumericText.length() - 1);
                }
                replaceNumericText = false;
            } else if ((typedChar >= '0' && typedChar <= '9') || typedChar == '-' || typedChar == '.') {
                if (replaceNumericText) editingNumericText = "";
                if (editingNumericText.length() < 16) editingNumericText += typedChar;
                replaceNumericText = false;
            }
            return;
        }
        if (editingStringSetting != null) {
            boolean control = Keyboard.isKeyDown(Keyboard.KEY_LCONTROL) || Keyboard.isKeyDown(Keyboard.KEY_RCONTROL);
            if (keyCode == Keyboard.KEY_ESCAPE) {
                cancelStringEdit();
            } else if (keyCode == Keyboard.KEY_RETURN || keyCode == Keyboard.KEY_NUMPADENTER || keyCode == Keyboard.KEY_TAB) {
                commitStringEdit();
            } else if (keyCode == Keyboard.KEY_A && control) {
                replaceStringText = true;
            } else if (keyCode == Keyboard.KEY_V && control) {
                String pasted = getClipboardString();
                if (pasted != null) {
                    if (replaceStringText) editingStringText = "";
                    editingStringText = (editingStringText + pasted.replaceAll("[\\r\\n]", " "));
                    if (editingStringText.length() > 128) editingStringText = editingStringText.substring(0, 128);
                    replaceStringText = false;
                }
            } else if (keyCode == Keyboard.KEY_BACK || keyCode == Keyboard.KEY_DELETE) {
                if (replaceStringText) editingStringText = "";
                else if (!editingStringText.isEmpty()) {
                    editingStringText = editingStringText.substring(0, editingStringText.length() - 1);
                }
                replaceStringText = false;
            } else if (typedChar >= 32 && typedChar != 127 && editingStringText.length() < 128) {
                if (replaceStringText) editingStringText = "";
                editingStringText += typedChar;
                replaceStringText = false;
            }
            return;
        }
        if (keyCode == Keyboard.KEY_ESCAPE) {
            if (colorEditor != null) colorEditor = null;
            else if (selectedModule != null) selectedModule = null;
            else mc.displayGuiScreen(parent);
            return;
        }
        if (page == Page.MODS && selectedModule == null) search.textboxKeyTyped(typedChar, keyCode);
    }

    private void closeProfileDialog() {
        creatingProfile = false;
        profileName.setFocused(false);
        profileError = null;
    }

    private void saveNewProfile() {
        String name = profileName.getText().trim();
        if (name.isEmpty()) {
            profileError = "Enter at least 1 character";
            return;
        }
        if (name.equalsIgnoreCase("default") || !ProfileNameValidator.isWindowsSafeFileName(name)) {
            profileError = "Choose another profile name";
            return;
        }
        for (Profile profile : CheatBreaker.getInstance().profiles) {
            if (profile.getName().equalsIgnoreCase(name)) {
                profileError = "That profile already exists";
                return;
            }
        }
        Profile profile = new Profile(name, true);
        profile.setIndex(CheatBreaker.getInstance().profiles.size());
        CheatBreaker.getInstance().profiles.add(profile);
        CheatBreaker.getInstance().activeProfile = profile;
        CheatBreaker.getInstance().configManager.writeProfile(name);
        CheatBreaker.getInstance().configManager.writeGlobalConfig();
        closeProfileDialog();
    }

    private static boolean inside(int mouseX, int mouseY, int x, int y, int w, int h) {
        return mouseX >= x && mouseX < x + w && mouseY >= y && mouseY < y + h;
    }
}
