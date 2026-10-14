package com.cheatbreaker.client.ui.module;

import com.cheatbreaker.client.CheatBreaker;
import com.cheatbreaker.client.module.AbstractModule;
import com.cheatbreaker.client.module.CompositeHudModule;
import com.cheatbreaker.client.ui.element.*;
import com.cheatbreaker.client.ui.element.module.ModulesGuiButtonElement;
import com.cheatbreaker.client.ui.element.module.ModuleListElement;
import com.cheatbreaker.client.ui.element.module.ModulePreviewContainer;
import com.cheatbreaker.client.ui.element.profile.ProfilesListElement;
import com.cheatbreaker.client.ui.util.RenderUtil;
import com.cheatbreaker.client.ui.util.font.CBFontRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.PositionedSoundRecord;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.GL11;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class CBModulesGui extends GuiScreen {

    private static final float MIN_MODULE_SIZE = 18.0f;
    private static final int SNAP_THRESHOLD = 2;
    static final float SNAP_GAP = 1.0f;
    private static final int GUIDE_COLOR = -3596854;
    private static final float GUIDE_LINE_WIDTH = 0.5f;

    public static CBModulesGui instance;
    private final ResourceLocation cogIcon = new ResourceLocation("client/icons/cog-64.png");
    private final ResourceLocation deleteIcon = new ResourceLocation("client/icons/delete-64.png");
    private final ResourceLocation landingLogo = new ResourceLocation("client/kazer_logo.png");
    private final List<CBModulePosition> positions = new ArrayList<>();
    private final List<AbstractScrollableElement> IllIIlIIlllllIllIIIlllIII = new ArrayList();
    private final List<ModulesGuiButtonElement> buttons = new ArrayList<>();
    private List<AbstractModule> modules;
    private ModulesGuiButtonElement showGuidesButton;
    public ModulesGuiButtonElement helpButton;
    private ModulesGuiButtonElement openModsButton;
    public AbstractScrollableElement IIIIllIIllIIIIllIllIIIlIl;
    public AbstractScrollableElement profilesElement;
    protected AbstractScrollableElement modulesElement;
    protected AbstractScrollableElement staffModulesElement;
    protected AbstractScrollableElement lIIIIllIIlIlIllIIIlIllIlI = null;
    public AbstractScrollableElement currentScrollableElement = null;
    private static AbstractModule draggingModule;
    private boolean IlIlIIIlllllIIIlIlIlIllII = false;
    private float IIlIIllIIIllllIIlllIllIIl;
    private float lllIlIIllllIIIIlIllIlIIII;
    private List<ModuleActionData> undoList;
    private List<ModuleActionData> redo;
    private int someMouseX;
    private int someMouseY;
    private boolean IlIIIIllIIIIIlllIIlIIlllI = false;
    private ModuleDataHolder dataHolder;
    public static boolean IlIlllIIIIllIllllIllIIlIl;
    private int arrowKeyMoves;
    private int IIllIlIllIlIllIIlIllIlIII = 0;
    private AbstractModule pendingIconModule;
    private CornerIconGuard.Icon pendingIcon;
    private int pendingPressX;
    private int pendingPressY;
    private long pendingPressTimeMs;
    private int dragButton = -1;
    private long landingOpenedAt;

    @Override
    public void onGuiClosed() {
        this.blurSettingEditors();
        Keyboard.enableRepeatEvents(false);
        cn.kazer.client189.compat.KazerCompat.vignette().cancelPreview();
        //this.mc.entityRenderer.deactivateShader();
    }

    /**
     * 从外部页面（主区域元素）打开该模块在 settings 面板上的子设置页。
     * 宿主是右侧面板 IIIIllIIllIIIIllIllIIIlIl（ModuleListElement），与画布齿轮开子页同一通路；
     * modulesElement 是网格预览容器（其 lIIIIIIIIIlIllIIllIlIIlIl 为 no-op），不能用。
     */
    public void cbzOpenModuleSubPage(AbstractModule module) {
        if (module == null || !(this.IIIIllIIllIIIIllIllIIIlIl instanceof ModuleListElement)) return;
        ModuleListElement panel = (ModuleListElement) this.IIIIllIIllIIIIllIllIIIlIl;
        panel.llIlIIIlIIIIlIlllIlIIIIll = false;
        panel.lIIIIIIIIIlIllIIllIlIIlIl(module);   // 重置滚动并设 module（内部自带点击音）
        this.currentScrollableElement = panel;
    }

    /**
     * Rebuilds the editor canvas module list. Composite modules emit dynamic child units, so
     * management pages (e.g. Custom Text add/remove) call this to make instance changes visible
     * without reopening the editor.
     */
    public void cbzRefreshModuleList() {
        this.modules = new ArrayList<>();
        for (AbstractModule module : CheatBreaker.getInstance().moduleManager.modules) {
            if (module instanceof CompositeHudModule) {
            	this.modules.addAll(((CompositeHudModule) module).getChildHudUnits());
            } else {
            	this.modules.add(module);
            }
        }
        if (this.draggingModule != null && !this.modules.contains(this.draggingModule)) {
            this.draggingModule = null;
        }
    }

    @Override
    public void initGui() {
        //this.IllIllIIIlIIlllIIIllIllII(); shader shid
        Keyboard.enableRepeatEvents(true);
        this.modules = new ArrayList<>();
        for (AbstractModule module : CheatBreaker.getInstance().moduleManager.modules) {
            if (module instanceof CompositeHudModule) {
                this.modules.addAll(((CompositeHudModule) module).getChildHudUnits());
            } else {
                this.modules.add(module);
            }
        }
        this.undoList = new ArrayList<>();
        this.redo = new ArrayList<>();
        this.someMouseX = -1;
        this.someMouseY = -1;
        this.arrowKeyMoves = 0;
        instance = this;
        draggingModule = null;
        this.dragButton = -1;
        this.cancelPendingIcon();
        this.landingOpenedAt = Minecraft.getSystemTime();
        IlIlllIIIIllIllllIllIIlIl = false;
        this.lIIIIllIIlIlIllIIIlIllIlI = null;
        this.currentScrollableElement = null;
        this.dataHolder = null;
        IlIlllIIIIllIllllIllIIlIl = false;
        float f = 1.0f / CheatBreaker.getInstance().getScaleFactor();
        int n = (int)((float)this.width / f);
        int n2 = (int)((float)this.height / f);
        this.IllIIlIIlllllIllIIIlllIII.clear();
        this.buttons.clear();
        List<AbstractModule> modules = CheatBreaker.getInstance().moduleManager.modules;
        List<AbstractModule> staffModules = CheatBreaker.getInstance().moduleManager.staffModules;
        this.modulesElement = new ModulePreviewContainer(f, n / 2 - 565, n2 / 2 + 14, 370, n2 / 2 - 35);
        this.IllIIlIIlllllIllIIIlllIII.add(this.modulesElement);
        this.staffModulesElement = new ModuleListElement(staffModules, f, n / 2 + 195, n2 / 2 + 14, 370, n2 / 2 - 35);
        this.IllIIlIIlllllIllIIIlllIII.add(this.staffModulesElement);
        this.IIIIllIIllIIIIllIllIIIlIl = new ModuleListElement(modules, f, n / 2 + 195, n2 / 2 + 14, 370, n2 / 2 - 35);
        this.IllIIlIIlllllIllIIIlllIII.add(this.IIIIllIIllIIIIllIllIIIlIl);
        this.profilesElement = new ProfilesListElement(f, n / 2 - 565, n2 / 2 + 14, 370, n2 / 2 - 35);
        this.IllIIlIIlllllIllIIIlllIII.add(this.profilesElement);
        this.showGuidesButton = new ModulesGuiButtonElement(null, "eye-64.png", 4, n2 - 32, 28, 28, -12418828, f);
        this.helpButton = new ModulesGuiButtonElement(null, "?", 36, n2 - 32, 28, 28, -12418828, f);
        this.openModsButton = new ModulesGuiButtonElement(CheatBreaker.getInstance().robotoBold14px,
                null, "Mods", n / 2 - 48, n2 / 2 - 2, 96, 28, -13916106, f)
                .useKazerLandingStyle();
        this.buttons.add(this.openModsButton);
        IlIlllIIIIllIllllIllIIlIl = false;
        this.lIIIIllIIlIlIllIIIlIllIlI = null;
        this.IIllIlIllIlIllIIlIllIlIII = 5;
    }

    @Override
    public void updateScreen() {
        this.pruneHiddenEditorState();
        if (this.pendingIconModule != null) {
            if (!isHudModuleVisible(this.pendingIconModule) || !Mouse.isButtonDown(0)) {
                this.cancelPendingIcon();
            } else {
                int pendingMouseX = Mouse.getX() * this.width / this.mc.displayWidth;
                int pendingMouseY = this.height - Mouse.getY() * this.height / this.mc.displayHeight - 1;
                if (CornerIconGuard.exceedsDragThreshold(this.pendingPressX, this.pendingPressY, pendingMouseX, pendingMouseY)
                        || CornerIconGuard.exceedsHold(this.pendingPressTimeMs, Minecraft.getSystemTime())) {
                    AbstractModule pendingModule = this.pendingIconModule;
                    int pressX = this.pendingPressX;
                    int pressY = this.pendingPressY;
                    this.cancelPendingIcon();
                    this.beginModuleDrag(pendingModule, new ScaledResolution(this.mc), pressX, pressY, 0);
                }
            }
        }
        float f = 1.0f / CheatBreaker.getInstance().getScaleFactor();
        int n = (int)((float)this.width / f);
        int n2 = (int)((float)this.height / f);
        this.lIIIIlIIllIIlIIlIIIlIIllI(n);
        if (!this.positions.isEmpty()) {
            boolean leftKey = Keyboard.isKeyDown(Keyboard.KEY_LEFT);
            boolean rightDown = Keyboard.isKeyDown(Keyboard.KEY_RIGHT);
            boolean upDown = Keyboard.isKeyDown(Keyboard.KEY_UP);
            boolean downDown = Keyboard.isKeyDown(Keyboard.KEY_DOWN);
            if (leftKey || rightDown || upDown || downDown) {
                ++this.arrowKeyMoves;
                if (this.arrowKeyMoves > 10) {
                    for (CBModulePosition position : this.positions) {
                        AbstractModule module = position.module;
                        if (module == null) continue;
                        if (leftKey) {
                            module.setTranslations((int)module.getXTranslation() - 1, (int)module.getYTranslation());
                            continue;
                        }
                        if (rightDown) {
                            module.setTranslations((int)module.getXTranslation() + 1, (int)module.getYTranslation());
                            continue;
                        }
                        if (upDown) {
                            module.setTranslations((int)module.getXTranslation(), (int)module.getYTranslation() - 1);
                            continue;
                        }
                        module.setTranslations((int)module.getXTranslation(), (int)module.getYTranslation() + 1);
                    }
                }
            }
        }
        float f2 = IIllIlIllIlIllIIlIllIlIII + getSmoothFloat((float) IIllIlIllIlIllIIlIllIlIII);
        this.IIllIlIllIlIllIIlIllIlIII = (float)this.IIllIlIllIlIllIIlIllIlIII + f2 >= (float)255 ? 255 : (int)((float)this.IIllIlIllIlIllIIlIllIlIII + f2);
    }

    private float getIntersectionFloat(Rectangle rectangle, Rectangle rectangle2) {
        float f = Math.max(Math.abs(rectangle.x - rectangle2.x) - rectangle2.width / 2, 0);
        float f2 = Math.max(Math.abs(rectangle.y - rectangle2.y) - rectangle2.height / 2, 0);
        return f * f + f2 * f2;
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float f) {
        float f2;
        float f3;
        Rectangle object;
        long now = Minecraft.getSystemTime();
        if (cn.kazer.client189.compat.KazerCompat.vignette().isPreviewVisible(now)) {
            cn.kazer.client189.compat.KazerCompat.drawVignette(width, height, cn.kazer.client189.compat.KazerCompat.vignette().previewProgress(now));
        }
        super.drawScreen(mouseX, mouseY, f);
        ///this.lIIlllIIlIlllllllllIIIIIl(); blur shader
        ScaledResolution scaledResolution = new ScaledResolution(mc);
        float scale = 1.0f / CheatBreaker.getInstance().getScaleFactor();
        if (draggingModule != null) {
            if (!Mouse.isButtonDown(1)) {
                RenderUtil.lIIIIlIIllIIlIIlIIIlIIllI(2, 0.0, 2.916666637692187 * 0.8571428656578064, this.height, 0.0, -15599126);
                RenderUtil.lIIIIlIIllIIlIIlIIIlIIllI((float)this.width - 5.0f * 0.5f, 0.0, this.width - 2, this.height, 0.0, -15599126);
                RenderUtil.lIIIIlIIllIIlIIlIIIlIIllI(0.0, 2, this.width, 1.1547619104385376 * 2.164948442965692, 0.0, -15599126);
                RenderUtil.lIIIIlIIllIIlIIlIIIlIIllI(0.0, (float)this.height - 1.3529412f * 2.5869565f, this.width, this.height - 3, 0.0, -15599126);
            }
            this.modules.sort((cBModule, cBModule2) -> {
                if (cBModule == draggingModule || cBModule2 == draggingModule || cBModule.getGuiAnchor() == null || cBModule2.getGuiAnchor() == null) {
                    return 0;
                }
                float[] modulePoints = cBModule.getScaledPoints(scaledResolution, true);
                float[] modulePoints2 = cBModule2.getScaledPoints(scaledResolution, true);
                float[] selectedPoints = draggingModule.getScaledPoints(scaledResolution, true);
                Rectangle rectangle = new Rectangle((int)(modulePoints[0] * (Float) cBModule.scale.getValue()), (int)(modulePoints[1] * ((Float)cBModule.scale.getValue()).floatValue()), (int)(cBModule.width * ((Float)cBModule.scale.getValue()).floatValue()), (int)(cBModule.height * ((Float)cBModule.scale.getValue()).floatValue()));
                Rectangle rectangle2 = new Rectangle((int)(modulePoints2[0] * (Float) cBModule2.scale.getValue()), (int)(modulePoints2[1] * ((Float)cBModule2.scale.getValue()).floatValue()), (int)(cBModule2.width * ((Float)cBModule2.scale.getValue()).floatValue()), (int)(cBModule2.height * ((Float)cBModule2.scale.getValue()).floatValue()));
                Rectangle rectangle3 = new Rectangle((int)(selectedPoints[0] * (Float) draggingModule.scale.getValue()), (int)(selectedPoints[1] * (Float) CBModulesGui.draggingModule.scale.getValue()), (int)(CBModulesGui.draggingModule.width * (Float) CBModulesGui.draggingModule.scale.getValue()), (int)(CBModulesGui.draggingModule.height * (Float) CBModulesGui.draggingModule.scale.getValue()));
                try {
                    if (this.getIntersectionFloat(rectangle, rectangle3) > this.getIntersectionFloat(rectangle2, rectangle3)) {
                        return -1;
                    }
                    return 1;
                }
                catch (Exception exception) {
                    return 0;
                }
            });
            CBModulePosition CBModulePosition = this.getModulePosition(draggingModule);
            if (CBModulePosition != null) {
                this.positions.remove(CBModulePosition);
                this.positions.add(CBModulePosition);
            }
            for (CBModulePosition position : this.positions) {
                this.lIIIIlIIllIIlIIlIIIlIIllI(position, mouseX, mouseY, scaledResolution);
                if (!(Boolean) CheatBreaker.getInstance().globalSettings.snapModules.getValue() || !this.IlIlIIIlllllIIIlIlIlIllII || Mouse.isButtonDown(1) || position.module != draggingModule) continue;
                boolean snappedHorizontallyToModule = false;
                boolean snappedVerticallyToModule = false;
                for (AbstractModule cBModule3 : this.modules) {
                    if (this.getModulePosition(cBModule3) != null || !isHudModuleVisible(cBModule3)) continue;
                    boolean bl = true;
                    boolean bl2 = true;
                    float[] arrf = cBModule3.getScaledPoints(scaledResolution, true);
                    float[] scaledPoints = position.module.getScaledPoints(scaledResolution, true);
                    float f6 = arrf[0] * (Float) cBModule3.scale.getValue() - scaledPoints[0] * (Float) position.module.scale.getValue();
                    float f7 = (arrf[0] + cBModule3.width) * (Float) cBModule3.scale.getValue() - (scaledPoints[0] + position.module.width) * (Float) position.module.scale.getValue();
                    float f8 = (arrf[0] + cBModule3.width) * (Float) cBModule3.scale.getValue() - scaledPoints[0] * (Float) position.module.scale.getValue();
                    float f9 = arrf[0] * (Float) cBModule3.scale.getValue() - (scaledPoints[0] + position.module.width) * (Float) position.module.scale.getValue();
                    float f10 = arrf[1] * (Float) cBModule3.scale.getValue() - scaledPoints[1] * (Float) position.module.scale.getValue();
                    f3 = (arrf[1] + cBModule3.height) * (Float) cBModule3.scale.getValue() - (scaledPoints[1] + position.module.height) * (Float) position.module.scale.getValue();
                    f2 = (arrf[1] + cBModule3.height) * (Float) cBModule3.scale.getValue() - scaledPoints[1] * (Float) position.module.scale.getValue();
                    float f11 = arrf[1] * (Float) cBModule3.scale.getValue() - (scaledPoints[1] + position.module.height) * (Float) position.module.scale.getValue();
                    if (this.isWithinSnapThreshold(f6)) {
                        bl = false;
                        snappedHorizontallyToModule = true;
                        this.snapHorizontally(f6);
                    }
                    if (this.isWithinSnapThreshold(f7) && bl) {
                        bl = false;
                        snappedHorizontallyToModule = true;
                        this.snapHorizontally(f7);
                    }
                    if (this.isWithinSnapThreshold(f9) && bl) {
                        bl = false;
                        snappedHorizontallyToModule = true;
                        this.snapHorizontally(snapDelta(f9, -1));
                    }
                    if (this.isWithinSnapThreshold(f8) && bl) {
                        snappedHorizontallyToModule = true;
                        this.snapHorizontally(snapDelta(f8, 1));
                    }
                    if (this.isWithinSnapThreshold(f10)) {
                        bl2 = false;
                        snappedVerticallyToModule = true;
                        this.snapVertically(f10);
                    }
                    if (this.isWithinSnapThreshold(f3) && bl2) {
                        bl2 = false;
                        snappedVerticallyToModule = true;
                        this.snapVertically(f3);
                    }
                    if (this.isWithinSnapThreshold(f11) && bl2) {
                        bl2 = false;
                        snappedVerticallyToModule = true;
                        this.snapVertically(snapDelta(f11, -1));
                    }
                    if (!this.isWithinSnapThreshold(f2) || !bl2) continue;
                    snappedVerticallyToModule = true;
                    this.snapVertically(snapDelta(f2, 1));
                }
                this.snapToScreenCenter(position.module, scaledResolution, !snappedHorizontallyToModule, !snappedVerticallyToModule);
            }
        } else if (this.dataHolder != null) {
            float f12 = 1.0f;
            switch (this.dataHolder.unknown) {
                case RIGHT_BOTTOM: {
                    int n4 = mouseY - this.dataHolder.mouseY + (mouseX - this.dataHolder.mouseX);
                    f12 = this.dataHolder.scale - (float)n4 / (float)115;
                    break;
                }
                case LEFT_TOP: {
                    int n4 = mouseY - this.dataHolder.mouseY + (mouseX - this.dataHolder.mouseX);
                    f12 = this.dataHolder.scale + (float)n4 / (float)115;
                    break;
                }
                case RIGHT_TOP: {
                    int n4 = mouseX - this.dataHolder.mouseX - (mouseY - this.dataHolder.mouseY);
                    f12 = this.dataHolder.scale - (float)n4 / (float)115;
                    break;
                }
                case LEFT_BOTTOM: {
                    int n4 = mouseX - this.dataHolder.mouseX - (mouseY - this.dataHolder.mouseY);
                    f12 = this.dataHolder.scale + (float)n4 / (float)115;
                }
            }
            if (f12 >= 1.0421053f * 0.47979796f && f12 <= 1.8962264f * 0.7910448f) {
                this.dataHolder.module.scale.setValue((float) ((double) Math.round((double) f12 * (double) 100) / (double) 100));
            }
        }
        this.lIIIIlIIllIIlIIlIIIlIIllI(scaledResolution);
        boolean bl = true;
        for (Object object2 : this.modules) {
            boolean bl3 = this.lIIIIlIIllIIlIIlIIIlIIllI(scale, (AbstractModule)object2, scaledResolution, mouseX, mouseY, bl);
            if (bl3) continue;
            bl = false;
        }
        GL11.glPushMatrix();
        GL11.glScalef(scale, scale, scale);
        int n5 = (int)((float)this.width / scale);
        int n6 = (int)((float)this.height / scale);
        if (this.currentScrollableElement == null && this.lIIIIllIIlIlIllIIIlIllIlI == null) {
            float landingProgress = Math.min(1.0F,
                    (Minecraft.getSystemTime() - this.landingOpenedAt) / 420.0F);
            landingProgress = 1.0F - (float)Math.pow(1.0F - landingProgress, 3.0D);
            int landingAlpha = Math.max(4, Math.min(255, Math.round(255.0F * landingProgress)));
            cn.kazer.client189.compat.LegacyGL.glColor4f(1F, 1F, 1F, landingProgress);
            RenderUtil.lIIIIlIIllIIlIIlIIIlIIllI(this.landingLogo,
                    n5 / 2F - 32F, n6 / 2F - 80F, 64F, 56F);
            CheatBreaker.getInstance().playRegular18px.drawCenteredString("KAZER CLIENT",
                    n5 / 2F, n6 / 2F - 19F, landingAlpha << 24 | 0xFFFFFF);
            this.openModsButton.setOpacity(landingProgress);
            cn.kazer.client189.compat.LegacyGL.glColor4f(1F, 1F, 1F, 1F);
        }
        for (ModulesGuiButtonElement llllIIIIIlIlIlIlIllIIIIII2 : this.buttons) {
            llllIIIIIlIlIlIlIllIIIIII2.handleDrawElement(mouseX, mouseY, f);
        }
        if (draggingModule == null) {
            GL11.glPushMatrix();
            cn.kazer.client189.compat.LegacyGL.glEnable(3089);
            RenderUtil.lIIIIlIIllIIlIIlIIIlIIllI(n5 / 2 - 185, n6 / 2 + 15, n5 / 2 + 185, n6 - 20, (float)scaledResolution.getScaleFactor() * scale, n6);
            for (AbstractScrollableElement lllIllIllIlIllIlIIllllIIl2 : this.IllIIlIIlllllIllIIIlllIII) {
                if (lllIllIllIlIllIlIIllllIIl2 != this.lIIIIllIIlIlIllIIIlIllIlI && lllIllIllIlIllIlIIllllIIl2 != this.currentScrollableElement) continue;
                lllIllIllIlIllIlIIllllIIl2.handleDrawElement(mouseX, mouseY, f);
            }
            cn.kazer.client189.compat.LegacyGL.glDisable(3089);
            GL11.glPopMatrix();
        }
        GL11.glPopMatrix();
        if (this.someMouseX != -1) {
            if (Mouse.isButtonDown(0)) {
                if (this.someMouseX != mouseX && this.someMouseY != mouseY) {
                    cn.kazer.client189.compat.LegacyGui.drawRect(mouseX, this.someMouseY, (float)mouseX + 1.1538461f * 0.43333334f, mouseY, -1358888961);
                    cn.kazer.client189.compat.LegacyGui.drawRect((float)this.someMouseX - 0.4329897f * 1.1547619f, mouseY, (float)mouseX + 18.2f * 0.027472526f, (float)mouseY + 0.121212125f * 4.125f, -1358888961);
                    cn.kazer.client189.compat.LegacyGui.drawRect((float)this.someMouseX - 0.8666667f * 0.5769231f, this.someMouseY, this.someMouseX, mouseY, -1358888961);
                    cn.kazer.client189.compat.LegacyGui.drawRect((float)this.someMouseX - 0.557971f * 0.8961039f, (float)this.someMouseY - 0.3611111f * 1.3846154f, (float)mouseX + 1.2692307f * 0.3939394f, this.someMouseY, -1358888961);
                    cn.kazer.client189.compat.LegacyGui.drawRect(this.someMouseX, this.someMouseY, mouseX, mouseY, 0x1F00FFFF);
                }
            } else {
                this.positions.clear();
                for (AbstractModule cBModule4 : this.modules) {
                    int n10;
                    int n11;
                    if (!isHudModuleVisible(cBModule4)) continue;
                    float[] arrf = cBModule4.getScaledPoints(scaledResolution, true);
                    float f14 = scale / (Float) cBModule4.scale.getValue();
                    object = new Rectangle((int)(arrf[0] * (Float) cBModule4.scale.getValue() - 2.0f), (int)(arrf[1] * (Float) cBModule4.scale.getValue() - 2.0f), (int)(cBModule4.width * ((Float)cBModule4.scale.getValue()).floatValue() + (float)4), (int)(cBModule4.height * ((Float)cBModule4.scale.getValue()).floatValue() + (float)4));
                    if (!object.intersects(new Rectangle(n11 = Math.min(this.someMouseX, mouseX), n10 = Math.min(this.someMouseY, mouseY), Math.max(this.someMouseX, mouseX) - n11, Math.max(this.someMouseY, mouseY) - n10))) continue;
                    f3 = (float)mouseX - cBModule4.getXTranslation();
                    f2 = (float)mouseY - cBModule4.getYTranslation();
                    this.positions.add(new CBModulePosition(cBModule4, f3, f2));
                }
                this.someMouseX = -1;
                this.someMouseY = -1;
            }
        }
    }

    private void drawHelpMenu(float f) {
        GL11.glPushMatrix();
        GL11.glTranslatef(4, (float)this.height - (float)173 * f, 0.0f);
        GL11.glScalef(f, f, f);
        cn.kazer.client189.compat.LegacyGui.drawRect(0.0f, 0.0f, 240, 128, -1895825408);
        CheatBreaker.getInstance().ubuntuMedium16px.drawString("Shortcuts & Movement", 4, 2.0f, -1);
        cn.kazer.client189.compat.LegacyGui.drawRect(4, 12, 234, 2.5815217f * 4.8421054f, 0x4FFFFFFF);
        int n = 16;
        this.renderRoundButton("Mouse1", 6, n);
        CheatBreaker.getInstance().playRegular14px.drawString("| " + EnumChatFormatting.LIGHT_PURPLE + "HOLD" + EnumChatFormatting.LIGHT_PURPLE + " Add mods to selected region", 80, (float)n+3, -1);
        this.renderRoundButton("Mouse1", 6, n += 12);
        CheatBreaker.getInstance().playRegular14px.drawString("| " + EnumChatFormatting.LIGHT_PURPLE + "HOLD" + EnumChatFormatting.LIGHT_PURPLE + " Select & drag mods", 80, (float)n+3, -1);
        this.renderRoundButton("Mouse2", 6, n += 12);
        CheatBreaker.getInstance().playRegular14px.drawString("| " + EnumChatFormatting.LIGHT_PURPLE + "HOLD" + EnumChatFormatting.LIGHT_PURPLE + " Don't lock mods while dragging", 80, (float)n+3, -1);
        this.renderRoundButton("CTRL", 6, n += 12);
        CheatBreaker.getInstance().playRegular14px.drawString("+", 30, (float)n+3, -1);
        this.renderRoundButton("Mouse1", 36, n);
        CheatBreaker.getInstance().playRegular14px.drawString("| Toggle (multiple) mod selection", 80, (float)n+3, -1);
        this.renderRoundButton("CTRL", 6, n += 12);
        CheatBreaker.getInstance().playRegular14px.drawString("+", 30, (float)n+3, -1);
        this.renderRoundButton("Z", 36, n);
        CheatBreaker.getInstance().playRegular14px.drawString("| Undo mod movements", 80, (float)n+3, -1);
        this.renderRoundButton("CTRL", 6, n += 12);
        CheatBreaker.getInstance().playRegular14px.drawString("+", 30, (float)n+3, -1);
        this.renderRoundButton("Y", 36, n);
        CheatBreaker.getInstance().playRegular14px.drawString("| Redo mod movements", 80, (float)n+3, -1);
        n = 100;
        this.renderRoundButton("Up", 31, n);
        this.renderRoundButton("Left", 6, n += 12);
        this.renderRoundButton("Down", 26, n);
        this.renderRoundButton("Right", 51, n);
        CheatBreaker.getInstance().playRegular14px.drawString("| Move selected mod with precision", 80, (float)n, -1);
        GL11.glPopMatrix();
    }

    private void renderRoundButton(String string, int n, int n2) {
        CBFontRenderer lIlIllIlIlIIIllllIlIllIll2 = CheatBreaker.getInstance().playRegular14px;
        float f = lIlIllIlIlIIIllllIlIllIll2.getStringWidth(string);
        RenderUtil.lIIIIlIIllIIlIIlIIIlIIllI(n, n2, (float)n + f + (float)4, n2 + 10, (double)2, -1073741825);
        lIlIllIlIlIIIllllIlIllIll2.drawString(string, n + 2, (float)n2+ 3, -16777216);
    }

    @Override
    public void mouseClicked(int mouseX, int mouseY, int n3) throws java.io.IOException {
        if (n3 == 0 && this.openModsButton != null && this.openModsButton.isMouseInside(mouseX, mouseY)) {
            Minecraft.getMinecraft().getSoundHandler().playSound(PositionedSoundRecord.create(
                    new ResourceLocation("gui.button.press"), 1.0f));
            Minecraft.getMinecraft().displayGuiScreen(new KazerModulesGui(this));
            return;
        }
        this.routeGlobalMouseClick(this.lIIIIllIIlIlIllIIIlIllIlI, mouseX, mouseY, n3);
        if (this.currentScrollableElement != this.lIIIIllIIlIlIllIIIlIllIlI) {
            this.routeGlobalMouseClick(this.currentScrollableElement, mouseX, mouseY, n3);
        }
        ScaledResolution scaledResolution = new ScaledResolution(mc);
        AbstractScrollableElement inputTarget = resolveInputTarget(
                this.lIIIIllIIlIlIllIIIlIllIlI, this.currentScrollableElement);
        if (inputTarget != null && inputTarget.isMouseInside(mouseX, mouseY)) {
            inputTarget.handleMouseClick(mouseX, mouseY, n3);
        } else {
            if (!(draggingModule != null && this.IlIlIIIlllllIIIlIlIlIllII)) {
                IconHit iconHit = this.findIconHit(scaledResolution, mouseX, mouseY);
                if (iconHit != null && n3 == 0) {
                    this.armPendingIcon(iconHit.module, iconHit.icon, mouseX, mouseY);
                    return;
                }
            }
            for (AbstractModule object : this.modules) {
                CBGuiAnchor cBGuiAnchor;
                SomeRandomAssEnum dELETE_ME_D;
                if (!isHudModuleVisible(object)) continue;
                float[] scaledPoints = object.getScaledPoints(scaledResolution, true);
                boolean bl4 = (float)mouseX > scaledPoints[0] * (Float) object.scale.getValue() && (float)mouseX < (scaledPoints[0] + object.width) * (Float) object.scale.getValue() && (float)mouseY > scaledPoints[1] * ((Float) object.scale.getValue()).floatValue() && (float)mouseY < (scaledPoints[1] + object.height) * ((Float) object.scale.getValue()).floatValue();
                boolean bl5 = this.dataHolder != null && this.dataHolder.module == object && this.dataHolder.unknown == SomeRandomAssEnum.LEFT_BOTTOM || !bl4 && (float)mouseX >= (scaledPoints[0] + object.width - (float)5) * ((Float) object.scale.getValue()).floatValue() && (float)mouseX <= (scaledPoints[0] + object.width + (float)5) * ((Float) object.scale.getValue()).floatValue() && (float)mouseY >= (scaledPoints[1] - (float)5) * ((Float) object.scale.getValue()).floatValue() && (float)mouseY <= (scaledPoints[1] + (float)5) * (Float) object.scale.getValue();
                boolean bl6 = this.dataHolder != null && this.dataHolder.module == object && this.dataHolder.unknown == SomeRandomAssEnum.RIGHT_TOP || !bl4 && (float)mouseX >= (scaledPoints[0] - (float)5) * (Float) object.scale.getValue() && (float)mouseX <= (scaledPoints[0] + (float)5) * ((Float) object.scale.getValue()).floatValue() && (float)mouseY >= (scaledPoints[1] + object.height - (float)5) * ((Float) object.scale.getValue()).floatValue() && (float)mouseY <= (scaledPoints[1] + object.height + (float)5) * (Float) object.scale.getValue();
                boolean bl7 = this.dataHolder != null && this.dataHolder.module == object && this.dataHolder.unknown == SomeRandomAssEnum.RIGHT_BOTTOM || !bl4 && (float)mouseX >= (scaledPoints[0] - (float)5) * (Float) object.scale.getValue() && (float)mouseX <= (scaledPoints[0] + (float)5) * ((Float) object.scale.getValue()).floatValue() && (float)mouseY >= (scaledPoints[1] - (float)5) * ((Float) object.scale.getValue()).floatValue() && (float)mouseY <= (scaledPoints[1] + (float)5) * ((Float) object.scale.getValue()).floatValue();
                boolean bl = this.dataHolder != null && this.dataHolder.module == object && this.dataHolder.unknown == SomeRandomAssEnum.LEFT_TOP || !bl4 && (float)mouseX >= (scaledPoints[0] + object.width - (float)5) * (Float) object.scale.getValue() && (float)mouseX <= (scaledPoints[0] + object.width + (float)5) * ((Float) object.scale.getValue()).floatValue() && (float)mouseY >= (scaledPoints[1] + object.height - (float)5) * ((Float) object.scale.getValue()).floatValue() && (float)mouseY <= (scaledPoints[1] + object.height + (float)5) * (Float) object.scale.getValue();
                if (this.someMouseX != -1 || !bl5 && !bl6 && !bl7 && !bl) continue;
                if (bl5) {
                    dELETE_ME_D = SomeRandomAssEnum.LEFT_BOTTOM;
                    cBGuiAnchor = CBGuiAnchor.LEFT_BOTTOM;
                } else if (bl6) {
                    dELETE_ME_D = SomeRandomAssEnum.RIGHT_TOP;
                    cBGuiAnchor = CBGuiAnchor.RIGHT_TOP;
                } else if (bl7) {
                    dELETE_ME_D = SomeRandomAssEnum.RIGHT_BOTTOM;
                    cBGuiAnchor = CBGuiAnchor.RIGHT_BOTTOM;
                } else {
                    dELETE_ME_D = SomeRandomAssEnum.LEFT_TOP;
                    cBGuiAnchor = CBGuiAnchor.LEFT_TOP;
                }
                if (this.lIIIIIIIIIlIllIIllIlIIlIl(scaledResolution, mouseX, mouseY)) continue;
                if (n3 == 0) {
                    this.undoList.add(new ModuleActionData(this, this.positions));
                    this.dataHolder = new ModuleDataHolder(this, object, dELETE_ME_D, mouseX, mouseY);
                    this.lIIIIlIIllIIlIIlIIIlIIllI(object, cBGuiAnchor, scaledResolution);
                } else if (n3 == 1) {
                    CBGuiAnchor cBGuiAnchor2 = object.getGuiAnchor();
                    this.lIIIIlIIllIIlIIlIIIlIIllI(object, cBGuiAnchor, scaledResolution);
                    object.scale.setValue(1.0f);
                    this.lIIIIlIIllIIlIIlIIIlIIllI(object, cBGuiAnchor2, scaledResolution);
                }
                return;
            }
            if (draggingModule == null) {
                this.IlllIIIlIlllIllIlIIlllIlI(mouseX, mouseY, n3);
                this.lIIIIlIIllIIlIIlIIIlIIllI(scaledResolution, mouseX, mouseY, n3);
            }
            for (Object object : this.buttons) {
                if (!((AbstractModulesGuiElement)object).isMouseInside(mouseX, mouseY)) continue;
                return;
            }
            boolean bl = this.lIIIIIIIIIlIllIIllIlIIlIl(scaledResolution, mouseX, mouseY);
            if (bl) {
                return;
            }
            if (!this.positions.isEmpty()) {
                this.positions.clear();
                Minecraft.getMinecraft().getSoundHandler().playSound(PositionedSoundRecord.create(new ResourceLocation("gui.button.press"), 1.0f));
            }
            this.someMouseX = mouseX;
            this.someMouseY = mouseY;
        }
        if (!this.positions.isEmpty()) {
            this.arrowKeyMoves = 0;
        }
    }

    @Override
    public void handleMouseInput() throws java.io.IOException {
        super.handleMouseInput();
        int n = Mouse.getEventDWheel();
        if (this.lIIIIllIIlIlIllIIIlIllIlI != null) {
            this.lIIIIllIIlIlIllIIIlIllIlI.onScroll(n);
        }
    }

    private void lIIIIlIIllIIlIIlIIIlIIllI(AbstractModule cBModule, CBGuiAnchor cBGuiAnchor, ScaledResolution scaledResolution) {
        if (cBGuiAnchor != cBModule.getGuiAnchor()) {
            float[] scaledPointsWithTranslations = cBModule.getScaledPoints(scaledResolution, true);
            cBModule.setAnchor(cBGuiAnchor);
            float[] scaledPointsWithoutTranslations = cBModule.getScaledPoints(scaledResolution, false);
            cBModule.setTranslations(
                    scaledPointsWithTranslations[0] * (Float) cBModule.scale.getValue() - scaledPointsWithoutTranslations[0] * (Float) cBModule.scale.getValue(),
                    scaledPointsWithTranslations[1] * (Float) cBModule.scale.getValue() - scaledPointsWithoutTranslations[1] * (Float) cBModule.scale.getValue()
            );
            this.quantizeTopLeft(cBModule, scaledResolution);
        }
    }

    @Override
    public void mouseReleased(int mouseX, int mouseY, int button) {
        if (button == 0) {
            cn.kazer.client189.compat.KazerCompat.vignette().previewReleased(Minecraft.getSystemTime());
        }
        ScaledResolution scaledResolution = new ScaledResolution(mc);
        if (this.dataHolder != null && button == 0) {
            this.lIIIIlIIllIIlIIlIIIlIIllI(this.dataHolder.module, this.dataHolder.anchor, scaledResolution);
            this.quantizeTopLeft(this.dataHolder.module, scaledResolution);
            this.dataHolder = null;
        }
        if (button == 0 && this.pendingIconModule != null) {
            AbstractModule pendingModule = this.pendingIconModule;
            CornerIconGuard.Icon icon = this.pendingIcon;
            long pressTime = this.pendingPressTimeMs;
            int pressX = this.pendingPressX;
            int pressY = this.pendingPressY;
            this.cancelPendingIcon();
            boolean inside = this.isOnIcon(pendingModule, icon, scaledResolution, mouseX, mouseY);
            if (CornerIconGuard.resolveRelease(pressTime, Minecraft.getSystemTime(), pressX, pressY, mouseX, mouseY, inside) == CornerIconGuard.ReleaseOutcome.EXECUTE) {
                this.executePendingIcon(pendingModule, icon);
            }
        }
        if (draggingModule != null && button == this.dragButton) {
            if (this.IlIlIIIlllllIIIlIlIlIllII) {
                for (CBModulePosition CBModulePosition : this.positions) {
                    CBGuiAnchor cBGuiAnchor = CBAnchorHelper.getAnchor(mouseX, mouseY, scaledResolution);
                    if (cBGuiAnchor == CBGuiAnchor.MIDDLE_MIDDLE || cBGuiAnchor == CBModulePosition.module.getGuiAnchor() || !this.IlIlIIIlllllIIIlIlIlIllII) continue;
                    this.lIIIIlIIllIIlIIlIIIlIIllI(CBModulePosition.module, cBGuiAnchor, scaledResolution);
                    CBModulePosition.x = (float)mouseX - CBModulePosition.module.getXTranslation();
                    CBModulePosition.y = (float)mouseY - CBModulePosition.module.getYTranslation();
                }
                if (this.getModulePosition(draggingModule) == null) {
                    float x = (float)mouseX - draggingModule.getXTranslation();
                    float y = (float)mouseY - draggingModule.getYTranslation();
                    this.positions.add(new CBModulePosition(draggingModule, x, y));
                }
                for (CBModulePosition position : this.positions) {
                    this.quantizeTopLeft(position.module, scaledResolution);
                }
                this.quantizeTopLeft(draggingModule, scaledResolution);
                Minecraft.getMinecraft().getSoundHandler().playSound(PositionedSoundRecord.create(new ResourceLocation("gui.button.press"), 1.0f));
            }
            System.out.println(draggingModule.getName());
            draggingModule = null;
            this.dragButton = -1;
        }
    }

    @Override
    public void keyTyped(char c, int n) throws java.io.IOException {
        if (this.routeKeyTyped(this.lIIIIllIIlIlIllIIIlIllIlI, c, n)
                || this.currentScrollableElement != this.lIIIIllIIlIlIllIIIlIllIlI
                && this.routeKeyTyped(this.currentScrollableElement, c, n)) {
            return;
        }
        if (n == 1) {
            CheatBreaker.getInstance().configManager.write();
        }
        super.keyTyped(c, n);
        if (n == Keyboard.KEY_Z && isCtrlKeyDown()) {
            if (!this.undoList.isEmpty()) {
                int n2 = this.undoList.size() - 1;
                ModuleActionData moduleActionData = this.undoList.get(this.undoList.size() - 1);
                for (int i = 0; i < moduleActionData.modules.size(); ++i) {
                    AbstractModule cBModule = moduleActionData.modules.get(i);
                    float f = moduleActionData.xTranslations.get(i);
                    float f2 = moduleActionData.yTranslations.get(i);
                    CBGuiAnchor cBGuiAnchor = moduleActionData.anchors.get(i);
                    Float f3 = moduleActionData.scales.get(i);
                    cBModule.setAnchor(cBGuiAnchor);
                    cBModule.setTranslations(f, f2);
                    cBModule.scale.setValue(f3);
                }
                if (this.redo.size() > 50) {
                    this.redo.remove(0);
                }
                this.redo.add(moduleActionData);
                this.undoList.remove(n2);
            }
        } else if (n == Keyboard.KEY_Y && isCtrlKeyDown()) {
            if (!this.redo.isEmpty()) {
                int n3 = this.redo.size() - 1;
                ModuleActionData moduleActionData = this.redo.get(this.redo.size() - 1);
                for (int i = 0; i < moduleActionData.modules.size(); ++i) {
                    AbstractModule module = moduleActionData.modules.get(i);
                    float xTranslation = moduleActionData.xTranslations.get(i);
                    float yTranslation = moduleActionData.yTranslations.get(i);
                    CBGuiAnchor anchor = moduleActionData.anchors.get(i);
                    float scale = moduleActionData.scales.get(i);
                    module.setAnchor(anchor);
                    module.setTranslations(xTranslation, yTranslation);
                    module.scale.setValue(scale);
                }
                if (this.redo.size() > 50) {
                    this.redo.remove(0);
                }
                this.undoList.add(moduleActionData);
                this.redo.remove(n3);
            }
        } else {
            this.arrowKeyMoves = 0;
            for (CBModulePosition CBModulePosition : this.positions) {
                AbstractModule cBModule = CBModulePosition.module;
                if (cBModule == null) continue;
                switch (n) {
                    case 203: {
                        cBModule.setTranslations(cBModule.getXTranslation() - 1.0f, cBModule.getYTranslation());
                        break;
                    }
                    case 205: {
                        cBModule.setTranslations(cBModule.getXTranslation() + 1.0f, cBModule.getYTranslation());
                        break;
                    }
                    case 200: {
                        cBModule.setTranslations(cBModule.getXTranslation(), cBModule.getYTranslation() - 1.0f);
                        break;
                    }
                    case 208: {
                        cBModule.setTranslations(cBModule.getXTranslation(), cBModule.getYTranslation() + 1.0f);
                    }
                }
            }
        }
    }

    private boolean routeKeyTyped(AbstractScrollableElement element, char character, int keyCode) {
        return element != null && element.handleKeyTyped(character, keyCode);
    }

    static AbstractScrollableElement resolveInputTarget(AbstractScrollableElement active,
                                                        AbstractScrollableElement transitioning) {
        return transitioning != null ? transitioning : active;
    }

    private void routeGlobalMouseClick(AbstractScrollableElement element, int mouseX, int mouseY, int button) {
        if (element != null) {
            element.handleGlobalMouseClick(mouseX, mouseY, button);
        }
    }

    private void blurSettingEditors() {
        if (this.lIIIIllIIlIlIllIIIlIllIlI != null) {
            this.lIIIIllIIlIlIllIIIlIllIlI.onFocusLost();
        }
        if (this.currentScrollableElement != null
                && this.currentScrollableElement != this.lIIIIllIIlIlIllIIIlIllIlI) {
            this.currentScrollableElement.onFocusLost();
        }
    }

    /** 休整态位置网格：模块最终绘制左上角（基址 + translation，GUI px）必须是整数；只改 translation，不动基址。 */
    static float quantizedTranslation(float basePx, float translationPx) {
        return Math.round(basePx + translationPx) - basePx;
    }

    private void quantizeTopLeft(AbstractModule module, ScaledResolution resolution) {
        float scale = (Float) module.scale.getValue();
        float[] base = module.getScaledPoints(resolution, false);
        module.setTranslations(
                quantizedTranslation(base[0] * scale, module.getXTranslation()),
                quantizedTranslation(base[1] * scale, module.getYTranslation()));
    }

    private void snapHorizontally(float f) {
        for (CBModulePosition CBModulePosition : this.positions) {
            CBModulePosition.module.setTranslations(CBModulePosition.module.getXTranslation() + f, CBModulePosition.module.getYTranslation());
        }
    }

    private void snapVertically(float f) {
        for (CBModulePosition CBModulePosition : this.positions) {
            CBModulePosition.module.setTranslations(CBModulePosition.module.getXTranslation(), CBModulePosition.module.getYTranslation() + f);
        }
    }

    private void snapToScreenCenter(AbstractModule module, ScaledResolution scaledResolution, boolean snapX, boolean snapY) {
        float[] offsets = this.getScreenCenterOffsets(module, scaledResolution);
        if (snapX && this.isWithinSnapThreshold(offsets[0])) {
            this.snapHorizontally(offsets[0]);
        }
        if (snapY && this.isWithinSnapThreshold(offsets[1])) {
            this.snapVertically(offsets[1]);
        }
    }

    private void renderScreenCenterGuides(AbstractModule module, ScaledResolution scaledResolution) {
        float[] offsets = this.getScreenCenterOffsets(module, scaledResolution);
        float centerX = (float) scaledResolution.getScaledWidth() / 2.0f;
        float centerY = (float) scaledResolution.getScaledHeight() / 2.0f;
        if (this.isWithinSnapThreshold(offsets[0])) {
            RenderUtil.lIIIIlIIllIIlIIlIIIlIIllI(centerX - GUIDE_LINE_WIDTH, 0.0, centerX, this.height, 0.0, GUIDE_COLOR);
        }
        if (this.isWithinSnapThreshold(offsets[1])) {
            RenderUtil.lIIIIlIIllIIlIIlIIIlIIllI(0.0, centerY, this.width, centerY + GUIDE_LINE_WIDTH, 0.0, GUIDE_COLOR);
        }
    }

    private float[] getScreenCenterOffsets(AbstractModule module, ScaledResolution scaledResolution) {
        float scale = (Float) module.scale.getValue();
        float[] points = module.getScaledPoints(scaledResolution, true);
        float moduleCenterX = points[0] * scale + module.width * scale / 2.0f;
        float moduleCenterY = points[1] * scale + module.height * scale / 2.0f;
        return new float[]{
                (float) scaledResolution.getScaledWidth() / 2.0f - moduleCenterX,
                (float) scaledResolution.getScaledHeight() / 2.0f - moduleCenterY
        };
    }

    private boolean isWithinSnapThreshold(float f) {
        return Math.abs(f) <= (float) SNAP_THRESHOLD;
    }

    /** 相邻贴边吸附在邻居边缘外侧保留 1px 缝；同边对齐与屏幕中心传 gapSign=0 保持 0 偏移。 */
    static float snapDelta(float edgeDelta, int gapSign) {
        return edgeDelta + gapSign * SNAP_GAP;
    }

    static float[] interactionBounds(float width, float height) {
        float interactionWidth = Math.max(width, MIN_MODULE_SIZE);
        float interactionHeight = Math.max(height, MIN_MODULE_SIZE);
        float left = (width - interactionWidth) / 2.0F;
        float top = (height - interactionHeight) / 2.0F;
        return new float[]{left, top, left + interactionWidth, top + interactionHeight};
    }

    static boolean isHudModuleVisible(boolean hasAnchor, boolean enabled, boolean renderHud) {
        return hasAnchor && enabled && renderHud;
    }

    private static boolean isHudModuleVisible(AbstractModule module) {
        return module != null && isHudModuleVisible(module.getGuiAnchor() != null,
                module.isEnabled(), module.isRenderHud());
    }

    public void onHudModuleHidden(AbstractModule module) {
        this.positions.removeIf(position -> position.module == module);
        if (draggingModule == module) {
            draggingModule = null;
            this.IlIlIIIlllllIIIlIlIlIllII = false;
        }
        if (this.dataHolder != null && this.dataHolder.module == module) {
            this.dataHolder = null;
        }
    }

    private void pruneHiddenEditorState() {
        this.positions.removeIf(position -> !isHudModuleVisible(position.module));
        if (draggingModule != null && !isHudModuleVisible(draggingModule)) {
            draggingModule = null;
            this.IlIlIIIlllllIIIlIlIlIllII = false;
        }
        if (this.dataHolder != null && !isHudModuleVisible(this.dataHolder.module)) {
            this.dataHolder = null;
        }
    }

    private boolean isWithinInteractionBounds(AbstractModule module, ScaledResolution resolution,
                                              int mouseX, int mouseY) {
        float[] points = module.getScaledPoints(resolution, true);
        float[] bounds = interactionBounds(module.width, module.height);
        float scale = (Float)module.scale.getValue();
        return mouseX > (points[0] + bounds[0]) * scale
                && mouseX < (points[0] + bounds[2]) * scale
                && mouseY > (points[1] + bounds[1]) * scale
                && mouseY < (points[1] + bounds[3]) * scale;
    }

    private void lIIIIlIIllIIlIIlIIIlIIllI(ScaledResolution scaledResolution, int n, int n2, int n3) {
        for (AbstractModule cBModule : this.modules) {
            if (!isHudModuleVisible(cBModule))
                continue;
            if (!this.isWithinInteractionBounds(cBModule, scaledResolution, n, n2))
                continue;
            if (n3 == 0 || n3 == 1) {
                this.beginModuleDrag(cBModule, scaledResolution, n, n2, n3);
            }
            break;
        }
    }

    private void IlllIIIlIlllIllIlIIlllIlI(int n, int n2, int n3) {
        for (ModulesGuiButtonElement llllIIIIIlIlIlIlIllIIIIII2 : this.buttons) {
            if (n3 != 0 || !llllIIIIIlIlIlIlIllIIIIII2.isMouseInside(n, n2) || IlIlllIIIIllIllllIllIIlIl) continue;
            if (llllIIIIIlIlIlIlIllIIIIII2.lIIIIllIIlIlIllIIIlIllIlI != null && this.lIIIIllIIlIlIllIIIlIllIlI != llllIIIIIlIlIlIlIllIIIIII2.lIIIIllIIlIlIllIIIlIllIlI && this.currentScrollableElement == null) {
                Minecraft.getMinecraft().getSoundHandler().playSound(PositionedSoundRecord.create(new ResourceLocation("gui.button.press"), 1.0f));
                this.currentScrollableElement = llllIIIIIlIlIlIlIllIIIIII2.lIIIIllIIlIlIllIIIlIllIlI;
                continue;
            }
            if (llllIIIIIlIlIlIlIllIIIIII2.lIIIIllIIlIlIllIIIlIllIlI == null || this.currentScrollableElement != null) continue;
            Minecraft.getMinecraft().getSoundHandler().playSound(PositionedSoundRecord.create(new ResourceLocation("gui.button.press"), 1.0f));
            IlIlllIIIIllIllllIllIIlIl = true;
        }
    }

    private IconHit findIconHit(ScaledResolution scaledResolution, int mouseX, int mouseY) {
        for (AbstractModule module : this.modules) {
            if (!isHudModuleVisible(module)) continue;
            float[] points = module.getScaledPoints(scaledResolution, true);
            float scale = (Float) module.scale.getValue();
            if (!module.getSettingsList().isEmpty()
                    && CornerIconGuard.hitsGear(points[0], points[1], scale, module.width, module.height, mouseX, mouseY)) {
                return new IconHit(module, CornerIconGuard.Icon.GEAR);
            }
            if (CornerIconGuard.hitsClose(points[0], points[1], scale, module.width, module.height, mouseX, mouseY)) {
                return new IconHit(module, CornerIconGuard.Icon.CLOSE);
            }
        }
        return null;
    }

    private boolean isOnIcon(AbstractModule module, CornerIconGuard.Icon icon, ScaledResolution scaledResolution, int mouseX, int mouseY) {
        float[] points = module.getScaledPoints(scaledResolution, true);
        float scale = (Float) module.scale.getValue();
        if (icon == CornerIconGuard.Icon.GEAR) {
            return CornerIconGuard.hitsGear(points[0], points[1], scale, module.width, module.height, mouseX, mouseY);
        }
        return CornerIconGuard.hitsClose(points[0], points[1], scale, module.width, module.height, mouseX, mouseY);
    }

    private void armPendingIcon(AbstractModule module, CornerIconGuard.Icon icon, int mouseX, int mouseY) {
        this.pendingIconModule = module;
        this.pendingIcon = icon;
        this.pendingPressX = mouseX;
        this.pendingPressY = mouseY;
        this.pendingPressTimeMs = Minecraft.getSystemTime();
    }

    private void cancelPendingIcon() {
        this.pendingIconModule = null;
        this.pendingIcon = null;
    }

    private void executePendingIcon(AbstractModule module, CornerIconGuard.Icon icon) {
        Minecraft.getMinecraft().getSoundHandler().playSound(PositionedSoundRecord.create(new ResourceLocation("gui.button.press"), 1.0f));
        if (icon == CornerIconGuard.Icon.GEAR) {
            Minecraft.getMinecraft().displayGuiScreen(new KazerModulesGui(this, module));
        } else {
            module.setState(false);
        }
    }

    private void beginModuleDrag(AbstractModule module, ScaledResolution scaledResolution, int mouseX, int mouseY, int button) {
        boolean bl5 = true;
        if (this.getModulePosition(module) != null) {
            this.removePositionForModule(module);
            bl5 = false;
        }
        float f4 = (float) mouseX - module.getXTranslation() * (Float) module.scale.getValue();
        float f5 = (float) mouseY - module.getYTranslation() * (Float) module.scale.getValue();
        this.IIlIIllIIIllllIIlllIllIIl = mouseX;
        this.lllIlIIllllIIIIlIllIlIIII = mouseY;
        this.IlIlIIIlllllIIIlIlIlIllII = false;
        draggingModule = module;
        this.dragButton = button;
        if (this.getModulePosition(module) == null) {
            if (!isCtrlKeyDown() && bl5) {
                this.positions.clear();
            }
            if (bl5 || !isCtrlKeyDown()) {
                this.positions.add(new CBModulePosition(module, f4, f5));
            }
        }
        this.IlllIIIlIlllIllIlIIlllIlI(scaledResolution, mouseX, mouseY);
    }

    private static final class IconHit {
        final AbstractModule module;
        final CornerIconGuard.Icon icon;

        IconHit(AbstractModule module, CornerIconGuard.Icon icon) {
            this.module = module;
            this.icon = icon;
        }
    }

    private boolean lIIIIIIIIIlIllIIllIlIIlIl(ScaledResolution scaledResolution, int mouseX, int mouseY) {
        boolean bl = false;
        for (AbstractModule cBModule : this.modules) {
            if (!isHudModuleVisible(cBModule)) continue;
            boolean bl2 = this.isWithinInteractionBounds(cBModule, scaledResolution, mouseX, mouseY);
            bl = bl || bl2;
        }
        return bl;
    }

    private boolean lIIIIlIIllIIlIIlIIIlIIllI(float f, AbstractModule cBModule, ScaledResolution scaledResolution, int n, int n2, boolean bl) {
        int n3;
        int n4;
        int n5;
        int n6;
        float[] object;
        boolean bl2;
        if (!isHudModuleVisible(cBModule)) {
            return true;
        }
        boolean bl3 = false;
        GL11.glPushMatrix();
        float[] arrf = cBModule.getScaledPoints(scaledResolution, true);
        cBModule.scaleAndTranslate(scaledResolution);
        bl2 = this.someMouseX != -1;
        if (bl2) {
            Rectangle rectangle1 = new Rectangle((int)(arrf[0] * (Float) cBModule.scale.getValue() - 2.0f), (int)(arrf[1] * ((Float)cBModule.scale.getValue()).floatValue() - 2.0f), (int)(cBModule.width * ((Float)cBModule.scale.getValue()).floatValue() + (float)4), (int)(cBModule.height * ((Float)cBModule.scale.getValue()).floatValue() + (float)4));
            n6 = Math.min(this.someMouseX, n);
            n5 = Math.min(this.someMouseY, n2);
            n4 = Math.max(this.someMouseX, n) - n6;
            n3 = Math.max(this.someMouseY, n2) - n5;
            Rectangle rectangle = new Rectangle(n6, n5, n4, n3);
            bl2 = rectangle1.intersects(rectangle);
        }
        object = cBModule.getScaledPoints(scaledResolution, true);
        n6 = this.isWithinInteractionBounds(cBModule, scaledResolution, n, n2) ? 1 : 0;
        if (!this.IlIIIIllIIIIIlllIIlIIlllI) {
            if (this.getModulePosition(cBModule) != null || bl2) {
                cn.kazer.client189.compat.LegacyGui.drawRectWithOutline(0.0f, 0.0f, cBModule.width, cBModule.height, 2.064516f * 0.2421875f, -1627324417, 0x1AFFFFFF);
            } else {
                cn.kazer.client189.compat.LegacyGui.drawRectWithOutline(0.0f, 0.0f, cBModule.width, cBModule.height, 1.2179487f * 0.41052634f, 0x6FFFFFFF, 0x1AFFFFFF);
            }
        }
        if (!this.IlIIIIllIIIIIlllIIlIIlllI && n6 != 0) {
            float moduleScale = (Float) cBModule.scale.getValue();
            n5 = !cBModule.getSettingsList().isEmpty() && CornerIconGuard.hitsGear(object[0], object[1], moduleScale, cBModule.width, cBModule.height, n, n2) ? 1 : 0;
            int n8 = n4 = CornerIconGuard.hitsClose(object[0], object[1], moduleScale, cBModule.width, cBModule.height, n, n2) ? 1 : 0;
            if (!cBModule.getSettingsList().isEmpty()) {
                cn.kazer.client189.compat.LegacyGL.glColor4f(1.0f, 1.0f, 1.0f, n5 != 0 ? 1.0f : 0.20895523f * 2.8714287f);
                RenderUtil.drawIcon(this.cogIcon, CornerIconGuard.ICON_SIZE / 2.0f, CornerIconGuard.GEAR_LOCAL_X, cBModule.height - CornerIconGuard.ICON_TOP_OFFSET);
            }
            cn.kazer.client189.compat.LegacyGL.glColor4f(1.2952381f * 0.61764705f, 0.4181818f * 0.47826087f, 0.09268292f * 2.1578948f, n4 != 0 ? 1.0f : 2.025f * 0.2962963f);
            RenderUtil.drawIcon(this.deleteIcon, CornerIconGuard.ICON_SIZE / 2.0f, cBModule.width - CornerIconGuard.CLOSE_RIGHT_INSET, cBModule.height - CornerIconGuard.ICON_TOP_OFFSET);
        }
        GL11.glPushMatrix();
        float f3 = f / (Float) cBModule.scale.getValue();
        GL11.glScalef(f3, f3, f3);
        if (bl) {
            n4 = this.dataHolder != null && this.dataHolder.module == cBModule && this.dataHolder.unknown == SomeRandomAssEnum.LEFT_BOTTOM || n6 == 0 && (float)n >= (object[0] + cBModule.width - (float)5) * ((Float)cBModule.scale.getValue()).floatValue() && (float)n <= (object[0] + cBModule.width + (float)5) * ((Float)cBModule.scale.getValue()).floatValue() && (float)n2 >= (object[1] - (float)5) * ((Float)cBModule.scale.getValue()).floatValue() && (float)n2 <= (object[1] + (float)5) * ((Float)cBModule.scale.getValue()).floatValue() ? 1 : 0;
            n3 = this.dataHolder != null && this.dataHolder.module == cBModule && this.dataHolder.unknown == SomeRandomAssEnum.RIGHT_TOP || n6 == 0 && (float)n >= (object[0] - (float)5) * ((Float)cBModule.scale.getValue()).floatValue() && (float)n <= (object[0] + (float)5) * ((Float)cBModule.scale.getValue()).floatValue() && (float)n2 >= (object[1] + cBModule.height - (float)5) * ((Float)cBModule.scale.getValue()).floatValue() && (float)n2 <= (object[1] + cBModule.height + (float)5) * ((Float)cBModule.scale.getValue()).floatValue() ? 1 : 0;
            boolean bl5 = this.dataHolder != null && this.dataHolder.module == cBModule && this.dataHolder.unknown == SomeRandomAssEnum.RIGHT_BOTTOM || n6 == 0 && (float)n >= (object[0] - (float)5) * ((Float)cBModule.scale.getValue()).floatValue() && (float)n <= (object[0] + (float)5) * ((Float)cBModule.scale.getValue()).floatValue() && (float)n2 >= (object[1] - (float)5) * ((Float)cBModule.scale.getValue()).floatValue() && (float)n2 <= (object[1] + (float)5) * ((Float)cBModule.scale.getValue()).floatValue();
            boolean bl6 = this.dataHolder != null && this.dataHolder.module == cBModule && this.dataHolder.unknown == SomeRandomAssEnum.LEFT_TOP || n6 == 0 && (float)n >= (object[0] + cBModule.width - (float)5) * ((Float)cBModule.scale.getValue()).floatValue() && (float)n <= (object[0] + cBModule.width + (float)5) * ((Float)cBModule.scale.getValue()).floatValue() && (float)n2 >= (object[1] + cBModule.height - (float)5) * ((Float)cBModule.scale.getValue()).floatValue() && (float)n2 <= (object[1] + cBModule.height + (float)5) * ((Float)cBModule.scale.getValue()).floatValue();
            GL11.glPushMatrix();
            float f4 = 4;
            if (this.someMouseX == -1 && bl5) {
                GL11.glTranslatef(0.0f, 0.0f, 0.0f);
                cn.kazer.client189.compat.LegacyGui.drawRect(-f4 / 2.0f, -f4 / 2.0f, f4 / 2.0f, f4 / 2.0f, -16711936);
            }
            if (this.someMouseX == -1 && n4 != 0) {
                GL11.glTranslatef(cBModule.width / f3, 0.0f, 0.0f);
                cn.kazer.client189.compat.LegacyGui.drawRect(-f4 / 2.0f, -f4 / 2.0f, f4 / 2.0f, f4 / 2.0f, -16711936);
            }
            if (this.someMouseX == -1 && bl6) {
                GL11.glTranslatef(cBModule.width / f3, cBModule.height / f3, 0.0f);
                cn.kazer.client189.compat.LegacyGui.drawRect(-f4 / 2.0f, -f4 / 2.0f, f4 / 2.0f, f4 / 2.0f, -16711936);
            }
            if (this.someMouseX == -1 && n3 != 0) {
                GL11.glTranslatef(0.0f, cBModule.height / f3, 0.0f);
                cn.kazer.client189.compat.LegacyGui.drawRect(-f4 / 2.0f, -f4 / 2.0f, f4 / 2.0f, f4 / 2.0f, -16711936);
            }
            GL11.glPopMatrix();
            bl3 = this.someMouseX == -1 && (bl5 || n4 != 0 || n3 != 0 || bl6);
        }
        n4 = arrf[1] - CheatBreaker.getInstance().ubuntuMedium16px.getHeight() - (float)6 < 0.0f ? 1 : 0;
        float f5 = n4 != 0 ? cBModule.height * (Float) cBModule.scale.getValue() / f : (float)(-CheatBreaker.getInstance().ubuntuMedium16px.getHeight() - 4);
        switch (cBModule.getPosition()) {
            case LEFT: {
                float f6 = 0.0f;
                CheatBreaker.getInstance().ubuntuMedium16px.drawMixedString(cBModule.getName(), f6, f5, -1);
                break;
            }
            case CENTER: {
                float f7 = cBModule.width * (Float) cBModule.scale.getValue() / f / 2.0f;
                CheatBreaker.getInstance().ubuntuMedium16px.drawMixedString(cBModule.getName(), f7, f5, -1);
                break;
            }
            case RIGHT: {
                float f8 = cBModule.width * (Float) cBModule.scale.getValue() / f - (float) CheatBreaker.getInstance().ubuntuMedium16px.getMixedStringWidth(cBModule.getName());
                CheatBreaker.getInstance().ubuntuMedium16px.drawMixedString(cBModule.getName(), f8, f5, -1);
            }
        }
        GL11.glPopMatrix();
        GL11.glPopMatrix();
        return !bl3;
    }

    private void lIIIIlIIllIIlIIlIIIlIIllI(ScaledResolution scaledResolution) {
        if (!Mouse.isButtonDown(1) && draggingModule != null) {
            for (CBModulePosition CBModulePosition : this.positions) {
                if (CBModulePosition.module != draggingModule || !(Boolean) CheatBreaker.getInstance().globalSettings.snapModules.getValue()) continue;
                Object var5_5 = null;
                for (AbstractModule cBModule : this.modules) {
                    if (this.getModulePosition(cBModule) != null || !isHudModuleVisible(cBModule)) continue;
                    float[] arrf = cBModule.getScaledPoints(scaledResolution, true);
                    float[] arrf2 = CBModulePosition.module.getScaledPoints(scaledResolution, true);
                    boolean bl = false;
                    float f2 = arrf[0] * (Float) cBModule.scale.getValue() - arrf2[0] * (Float) CBModulePosition.module.scale.getValue();
                    float f3 = (arrf[0] + cBModule.width) * (Float) cBModule.scale.getValue() - (arrf2[0] + CBModulePosition.module.width) * (Float) CBModulePosition.module.scale.getValue();
                    float f4 = (arrf[0] + cBModule.width) * (Float) cBModule.scale.getValue() - arrf2[0] * (Float) CBModulePosition.module.scale.getValue();
                    float f5 = arrf[0] * (Float) cBModule.scale.getValue() - (arrf2[0] + CBModulePosition.module.width) * (Float) CBModulePosition.module.scale.getValue();
                    float f6 = arrf[1] * (Float) cBModule.scale.getValue() - arrf2[1] * (Float) CBModulePosition.module.scale.getValue();
                    float f7 = (arrf[1] + cBModule.height) * (Float) cBModule.scale.getValue() - (arrf2[1] + CBModulePosition.module.height) * (Float) CBModulePosition.module.scale.getValue();
                    float f8 = (arrf[1] + cBModule.height) * (Float) cBModule.scale.getValue() - arrf2[1] * (Float) CBModulePosition.module.scale.getValue();
                    float f9 = arrf[1] * (Float) cBModule.scale.getValue() - (arrf2[1] + CBModulePosition.module.height) * (Float) CBModulePosition.module.scale.getValue();
                    if (this.isWithinSnapThreshold(f2)) {
                        bl = true;
                        RenderUtil.lIIIIlIIllIIlIIlIIIlIIllI(arrf[0] * (Float) cBModule.scale.getValue() - 0.6666667f * 0.75f, 0.0, arrf[0] * (Float) cBModule.scale.getValue(), this.height, 0.0, GUIDE_COLOR);
                    }
                    if (this.isWithinSnapThreshold(f3)) {
                        bl = true;
                        RenderUtil.lIIIIlIIllIIlIIlIIIlIIllI((arrf[0] + cBModule.width) * (Float) cBModule.scale.getValue(), 0.0, (arrf[0] + cBModule.width) * (Float) cBModule.scale.getValue() + 1.7272727f * 0.28947368f, this.height, 0.0, GUIDE_COLOR);
                    }
                    if (this.isWithinSnapThreshold(f5)) {
                        bl = true;
                        RenderUtil.lIIIIlIIllIIlIIlIIIlIIllI(arrf[0] * (Float) cBModule.scale.getValue(), 0.0, arrf[0] * (Float) cBModule.scale.getValue() + 0.29775283f * 1.6792452f, this.height, 0.0, GUIDE_COLOR);
                    }
                    if (this.isWithinSnapThreshold(f4)) {
                        bl = true;
                        RenderUtil.lIIIIlIIllIIlIIlIIIlIIllI((arrf[0] + cBModule.width) * (Float) cBModule.scale.getValue(), 0.0, (arrf[0] + cBModule.width) * (Float) cBModule.scale.getValue() + 1.5238096f * 0.328125f, this.height, 0.0, GUIDE_COLOR);
                    }
                    if (this.isWithinSnapThreshold(f6)) {
                        bl = true;
                        RenderUtil.lIIIIlIIllIIlIIlIIIlIIllI(0.0, arrf[1] * (Float) cBModule.scale.getValue(), this.width, arrf[1] * (Float) cBModule.scale.getValue() + 0.3888889f * 1.2857143f, 0.0, GUIDE_COLOR);
                    }
                    if (this.isWithinSnapThreshold(f7)) {
                        bl = true;
                        RenderUtil.lIIIIlIIllIIlIIlIIIlIIllI(0.0, (arrf[1] + cBModule.height) * (Float) cBModule.scale.getValue(), this.width, (arrf[1] + cBModule.height) * (Float) cBModule.scale.getValue() + 0.51724136f * 0.9666667f, 0.0, GUIDE_COLOR);
                    }
                    if (this.isWithinSnapThreshold(f9)) {
                        bl = true;
                        RenderUtil.lIIIIlIIllIIlIIlIIIlIIllI(0.0, arrf[1] * (Float) cBModule.scale.getValue(), this.width, arrf[1] * (Float) cBModule.scale.getValue() + 0.16666667f * 3.0f, 0.0, GUIDE_COLOR);
                    }
                    if (this.isWithinSnapThreshold(f8)) {
                        bl = true;
                        RenderUtil.lIIIIlIIllIIlIIlIIIlIIllI(0.0, (arrf[1] + cBModule.height) * ((Float)cBModule.scale.getValue()).floatValue() - 0.5810811f * 0.8604651f, this.width, (arrf[1] + cBModule.height) * ((Float)cBModule.scale.getValue()).floatValue(), 0.0, GUIDE_COLOR);
                    }
                    if (!bl) continue;
                    GL11.glPushMatrix();
                    cBModule.scaleAndTranslate(scaledResolution);
                    cn.kazer.client189.compat.LegacyGui.drawRectWithOutline(0.0f, 0.0f, cBModule.width, cBModule.height, 0.01923077f * 26.0f, 0, 449387978);
                    GL11.glPopMatrix();
                }
                this.renderScreenCenterGuides(CBModulePosition.module, scaledResolution);
            }
        }
    }

    private float lIIIIlIIllIIlIIlIIIlIIllI(AbstractModule cBModule, float f, float[] arrf, int n) {
        float f2 = f;
        float padding = 2.0f;
        if (f2 + arrf[0] * (Float) cBModule.scale.getValue() < padding) {
            f2 = -arrf[0] * (Float) cBModule.scale.getValue() + padding;
        } else if (f2 + arrf[0] * (Float) cBModule.scale.getValue() + (float)n > (float)this.width - padding) {
            f2 = (float)this.width - arrf[0] * (Float) cBModule.scale.getValue() - (float)n - padding;
        }
        return f2;
    }

    private float lIIIIIIIIIlIllIIllIlIIlIl(AbstractModule cBModule, float f, float[] arrf, int n) {
        float f2 = f;
        float padding = 2.0f;
        if (f2 + arrf[1] * (Float) cBModule.scale.getValue() < padding) {
            f2 = -arrf[1] * (Float) cBModule.scale.getValue() + padding;
        } else if (f2 + arrf[1] * (Float) cBModule.scale.getValue() + (float)n > (float)this.height - padding) {
            f2 = (float)this.height - arrf[1] * (Float) cBModule.scale.getValue() - (float)n - padding;
        }
        return f2;
    }

    private void lIIIIlIIllIIlIIlIIIlIIllI(CBModulePosition CBModulePosition, int n, int n2, ScaledResolution scaledResolution) {
        if (!isHudModuleVisible(CBModulePosition.module)) {
            return;
        }
        float f = (float)n - CBModulePosition.x;
        float f2 = (float)n2 - CBModulePosition.y;
        if (!(this.IlIlIIIlllllIIIlIlIlIllII || CBModulePosition.module != draggingModule || (float)n == this.IIlIIllIIIllllIIlllIllIIl && (float)n2 == this.lllIlIIllllIIIIlIllIlIIII)) {
            if (this.undoList.size() > 50) {
                this.undoList.remove(0);
            }
            this.undoList.add(new ModuleActionData(this, this.positions));
            CheatBreaker.getInstance().createNewProfile();
            this.IlIlIIIlllllIIIlIlIlIllII = true;
        }
        float[] arrf = CBModulePosition.module.getScaledPoints(scaledResolution, false);
        if (!Mouse.isButtonDown(1) && this.IlIlIIIlllllIIIlIlIlIllII && CBModulePosition.module == draggingModule) {
            float f3 = f;
            float f4 = f2;
            f = this.lIIIIlIIllIIlIIlIIIlIIllI(CBModulePosition.module, f, arrf, (int)(CBModulePosition.module.width * (Float) CBModulePosition.module.scale.getValue()));
            f2 = this.lIIIIIIIIIlIllIIllIlIIlIl(CBModulePosition.module, f2, arrf, (int)(CBModulePosition.module.height * (Float) CBModulePosition.module.scale.getValue()));
            float f5 = f3 - f;
            float f6 = f4 - f2;
            for (CBModulePosition dragCache2 : this.positions) {
                if (dragCache2 == CBModulePosition) continue;
                arrf = dragCache2.module.getScaledPoints(scaledResolution, false);
                float f7 = this.lIIIIlIIllIIlIIlIIIlIIllI(dragCache2.module, dragCache2.module.getXTranslation() - f5, arrf, (int)(dragCache2.module.width * (Float) dragCache2.module.scale.getValue()));
                float f8 = this.lIIIIIIIIIlIllIIllIlIIlIl(dragCache2.module, dragCache2.module.getYTranslation() - f6, arrf, (int)(dragCache2.module.height * (Float) dragCache2.module.scale.getValue()));
                dragCache2.module.setTranslations(f7, f8);
            }
        }
        if (this.IlIlIIIlllllIIIlIlIlIllII) {
            CBModulePosition.module.setTranslations(f, f2);
        }
    }

    private void lIIIIlIIllIIlIIlIIIlIIllI(int n) {
        if (IlIlllIIIIllIllllIllIIlIl) {
            if (this.lIIIIllIIlIlIllIIIlIllIlI != null) {
                this.lIIIIlIIllIIlIIlIIIlIIllI(this.lIIIIllIIlIlIllIIIlIllIlI, true, n);
            }
        } else if (this.currentScrollableElement != null) {
            if (this.lIIIIllIIlIlIllIIIlIllIlI != null) {
                this.lIIIIlIIllIIlIIlIIIlIIllI(this.lIIIIllIIlIlIllIIIlIllIlI, true, n);
            }
            this.lIIIIlIIllIIlIIlIIIlIIllI(this.currentScrollableElement, false, n);
        }
    }

    private void lIIIIlIIllIIlIIlIIIlIIllI(AbstractScrollableElement lllIllIllIlIllIlIIllllIIl2, boolean bl, int n) {
        if (bl) {
            lllIllIllIlIllIlIIllllIIl2.x = lllIllIllIlIllIlIIllllIIl2.IlIlllIIIIllIllllIllIIlIl;
            IlIlllIIIIllIllllIllIIlIl = false;
            this.lIIIIllIIlIlIllIIIlIllIlI = null;
        } else {
            lllIllIllIlIllIlIIllllIIl2.x = n / 2 - 185;
            this.currentScrollableElement = null;
            this.lIIIIllIIlIlIllIIIlIllIlI = lllIllIllIlIllIlIIllllIIl2;
        }
    }

    public static float getSmoothFloat(float f) {
        float f2 = f / (float)(Minecraft.getDebugFPS() + 1);
        return Math.max(f2, 1.0f);
    }

    private CBModulePosition getModulePosition(AbstractModule cBModule) {
        for (CBModulePosition CBModulePosition : this.positions) {
            if (cBModule != CBModulePosition.module) continue;
            return CBModulePosition;
        }
        return null;
    }

    private void IlllIIIlIlllIllIlIIlllIlI(ScaledResolution scaledResolution, int n, int n2) {
        for (CBModulePosition CBModulePosition : this.positions) {
            if (CBModulePosition.module == null || CBModulePosition.module.getGuiAnchor() == null) continue;
            CBModulePosition.x = (float)n - CBModulePosition.module.getXTranslation();
            CBModulePosition.y = (float)n2 - CBModulePosition.module.getYTranslation();
        }
    }

    private void removePositionForModule(AbstractModule cBModule) {
        this.positions.removeIf(CBModulePosition -> CBModulePosition.module == cBModule);
    }

    static {
        IlIlllIIIIllIllllIllIIlIl = false;
    }

}
