package com.cheatbreaker.client.ui.element.module;

import com.cheatbreaker.client.CheatBreaker;
import com.cheatbreaker.client.module.type.customtext.CustomTextImageSupport;
import com.cheatbreaker.client.module.type.customtext.CustomTextItem;
import com.cheatbreaker.client.module.type.customtext.CustomTextModule;
import com.cheatbreaker.client.module.type.customtext.CustomTextState;
import com.cheatbreaker.client.ui.element.AbstractModulesGuiElement;
import com.cheatbreaker.client.ui.module.CBModulePlaceGui;
import com.cheatbreaker.client.ui.module.CBModulesGui;
import com.cheatbreaker.client.ui.module.CBTextInputOverlay;
import com.cheatbreaker.client.ui.util.RenderUtil;
import com.cheatbreaker.client.ui.util.font.CBFontRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.PositionedSoundRecord;
import net.minecraft.client.gui.Gui;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.GL11;

import java.util.HashMap;
import java.util.Map;

/**
 * Custom Text 管理页（PROFILE 风页的治理版）。
 * 布局：左上 ADD TEXT、右上 N / 32。每行整行可点进入子设置页；左侧方形启停；
 * 右部三个图标 Pbencil（就地编辑）、齿轮（进子设置页）、删除（二次确认）。
 * 行本体长按立即跟随鼠标，原位变暗、移动中显示插入线，松开后落定。
 */
public class CustomTextSettingsView extends AbstractModulesGuiElement {
    private static final int ROW_HEIGHT = 18;
    private static final int ROW_STEP = ROW_HEIGHT + 2;
    private static final long DELETE_CONFIRM_MS = 3000L;
    /** 拖拽从位移超过 6px 起算（非时间阈值）。 */
    private static final int DRAG_THRESHOLD_PX = 6;
    /** 行内容（图标组、计数器）的统一右缘留白；三枚图标各占一格 16px。 */
    private static final int RIGHT_INSET = 6;
    private static final int ICON_CELL = 16;
    /** 图标贴图渲染参数（drawIcon 第二参，实际边长 = 2 倍 = 8px）。 */
    private static final float ICON_SCALE = 4.0F;

    private static final int COLOUR_ROW_BASE = -657931;
    private static final int COLOUR_HOVER_FILL = -12418828;
    private static final int COLOUR_TEXT = -1895825408;
    private static final int COLOUR_TEXT_HOVERED_OR_SHADOW = -1;
    private static final int COLOUR_DOT_ON = -13916106;
    private static final int COLOUR_DOT_OFF = -723724;
    private static final int COLOUR_COUNTER = -6250336;
    private static final int COLOUR_DELETE_CONFIRM = -2129930;

    private static final ResourceLocation PENCIL_ICON = new ResourceLocation("client/icons/pencil-64.png");
    private static final ResourceLocation COG_ICON = new ResourceLocation("client/icons/cog-64.png");
    private static final ResourceLocation DELETE_ICON = new ResourceLocation("client/icons/delete-64.png");
    private static final ResourceLocation PLUS_ICON = new ResourceLocation("client/icons/plus-64.png");

    private final CustomTextModule module;

    // Rows interaction state (pointer in screen coords)
    private final Map<String, Integer> hoverFill = new HashMap<>();
    private String pressedRowId;
    /** 按下时所在的行区域；CONTENT 以外的区域只参与松开结算，不触发拖拽。 */
    private RowRegion pressedRegion = RowRegion.NONE;
    private int pressStartX;
    private int pressStartY;
    private boolean dragging;
    private String draggingId;
    private int dragTargetIndex = -1;
    private String confirmDeleteId;
    private long confirmDeleteUntilMs;

    public CustomTextSettingsView(CustomTextModule module, float scale) {
        super(scale);
        this.module = module;
    }

    @Override
    public int getHeight() {
        return 20 + this.module.getState().items.size() * (ROW_HEIGHT + 2) + 4;
    }

    private int rowsTop() {
        return this.y + 20;
    }

    private boolean insideRow(int mouseX, int mouseY, int rowY) {
        return (float) mouseX > (float) this.x * this.scale
                && (float) mouseX < (float) (this.x + this.width) * this.scale
                && (float) mouseY > (float) (rowY + this.yOffset) * this.scale
                && (float) mouseY < (float) (rowY + ROW_HEIGHT + this.yOffset) * this.scale;
    }

    private static int addWidth(Minecraft mc) {
        return stringWidth(mc, "ADD TEXT") + 20;
    }

    /** 管理页统一经 ubuntuMedium16 渲染（CJK 由 CBFontRenderer 自动回退 vanilla）。 */
    private static CBFontRenderer cbFont() {
        return CheatBreaker.getInstance() != null ? CheatBreaker.getInstance().ubuntuMedium16px : null;
    }

    private static int stringWidth(Minecraft mc, String text) {
        CBFontRenderer font = cbFont();
        return font != null ? font.getStringWidth(text) : mc.fontRendererObj.getStringWidth(text);
    }

    /** 无阴影混排绘制（CB 段 ubuntu，东亚段 vanilla 自动回退）。 */
    private static void drawText(Minecraft mc, String text, float x, float y, int color) {
        CBFontRenderer font = cbFont();
        if (font != null) {
            font.drawMixedString(text, x, y, color);
        } else {
            mc.fontRendererObj.drawString(text, (int) x, (int) y, color, false);
        }
    }

    private static String trimToWidth(Minecraft mc, String text, int width) {
        CBFontRenderer font = cbFont();
        return font != null ? font.lIIIIlIIllIIlIIlIIIlIIllI(text, width)
                : mc.fontRendererObj.trimStringToWidth(text, width);
    }

    private static int stepFill(int fill, boolean up) {
        int step = (int) CBModulesGui.getSmoothFloat(790);
        return up ? Math.min(75, fill + step) : Math.max(0, fill - step);
    }

    /** 行本体 hit region：pencil|cog|delete 三档格子像素独立判定；始终是左占左边框到内容宽度。 */
    enum RowRegion { NONE, CONTENT, TOGGLE, PENCIL, COG, DELETE }

    RowRegion hitRegion(int mouseX, int mouseY, int rowY) {
        if (!this.insideRow(mouseX, mouseY, rowY)) return RowRegion.NONE;
        if (mouseX <= (float) (this.x + 14) * this.scale) return RowRegion.TOGGLE;
        int rowRight = this.x + this.width - RIGHT_INSET;
        int deleteLeft = rowRight - ICON_CELL;
        int cogLeft = deleteLeft - ICON_CELL;
        int pencilLeft = cogLeft - ICON_CELL;
        if (mouseX > (float) deleteLeft * this.scale && mouseX <= (float) rowRight * this.scale) return RowRegion.DELETE;
        if (mouseX > (float) cogLeft * this.scale && mouseX <= (float) deleteLeft * this.scale) return RowRegion.COG;
        if (mouseX > (float) pencilLeft * this.scale && mouseX <= (float) cogLeft * this.scale) return RowRegion.PENCIL;
        return RowRegion.CONTENT;
    }

    private int iconCellLeft(RowRegion region) {
        int rowRight = this.x + this.width - RIGHT_INSET;
        switch (region) {
            case DELETE: return rowRight - ICON_CELL;
            case COG:    return rowRight - ICON_CELL * 2;
            case PENCIL:
            default:     return rowRight - ICON_CELL * 3;
        }
    }

    @Override
    public void handleDrawElement(int mouseX, int mouseY, float partialTicks) {
        Minecraft mc = Minecraft.getMinecraft();
        int right = this.x + this.width;

        // 右上：N / 32（右缘与图标组同线）；左上 ADD TEXT 在下方悬停感知段单次绘制
        String counter = this.module.getState().items.size() + " / " + CustomTextState.MAX_ITEMS;
        drawText(mc, counter, right - RIGHT_INSET - stringWidth(mc, counter), this.y + 5, COLOUR_COUNTER);

        for (int i = 0; i < this.module.getState().items.size(); i++) {
            CustomTextItem item = this.module.getState().items.get(i);
            int rowY = this.rowsTop() + i * ROW_STEP;
            RowRegion hoverRegion = this.draggingId == null ? this.hitRegion(mouseX, mouseY, rowY) : RowRegion.NONE;
            boolean rowHovered = hoverRegion == RowRegion.CONTENT || hoverRegion == RowRegion.TOGGLE;

            int baseBase = item.id.equals(this.draggingId) ? -1610612736 : COLOUR_ROW_BASE;
            cn.kazer.client189.compat.LegacyGui.drawRect(this.x, rowY, right, rowY + ROW_HEIGHT, baseBase);

            int fill = stepFill(this.hoverFill.getOrDefault(item.id, 0), rowHovered);
            this.hoverFill.put(item.id, fill);
            if (fill > 0) {
                int cover = fill * ROW_HEIGHT / 75;
                cn.kazer.client189.compat.LegacyGui.drawRect(this.x, rowY + ROW_HEIGHT - cover, right, rowY + ROW_HEIGHT, COLOUR_HOVER_FILL);
            }

            // 图标格独立的蓝色浮起覆盖（悬停单格，不扩散整行）
            int pencilFill = this.stepIconFill(item.id, RowRegion.PENCIL, hoverRegion);
            int cogFill = this.stepIconFill(item.id, RowRegion.COG, hoverRegion);
            int deleteFill = this.stepIconFill(item.id, RowRegion.DELETE, hoverRegion);
            if (pencilFill > 0) this.drawIconCellCover(RowRegion.PENCIL, rowY, pencilFill);
            if (cogFill > 0) this.drawIconCellCover(RowRegion.COG, rowY, cogFill);
            if (deleteFill > 0) this.drawIconCellCover(RowRegion.DELETE, rowY, deleteFill);

            // 启停方框：先描与分割线同色的边框，再填绿/灰白，禁用态才可见
            cn.kazer.client189.compat.LegacyGui.drawRect(this.x + 2, rowY + 4, this.x + 12, rowY + 14, 0x2F2F2F2F);
            cn.kazer.client189.compat.LegacyGui.drawRect(this.x + 3, rowY + 5, this.x + 11, rowY + 13,
                    item.enabled ? COLOUR_DOT_ON : COLOUR_DOT_OFF);

            String label = item.content;
            int labelWidth = right - RIGHT_INSET - ICON_CELL * 3 - 4 - (this.x + 16);
            if (stringWidth(mc, label) > labelWidth) {
                label = trimToWidth(mc, label, Math.max(0, labelWidth - stringWidth(mc, "..."))) + "...";
            }
            drawText(mc, label, this.x + 16, rowY + 5,
                    fill > 0 ? COLOUR_TEXT_HOVERED_OR_SHADOW : COLOUR_TEXT);

            this.drawRowIcons(item, rowY, pencilFill, cogFill, deleteFill);
        }

        // 拖拽时的"跟随鼠标"行条 + 目标插入线
        if (this.draggingId != null) {
            int dragY = (int) (((float) mouseY / this.scale) - ROW_HEIGHT / 2f - this.yOffset);
            cn.kazer.client189.compat.LegacyGui.drawRect(this.x, dragY, right, dragY + ROW_HEIGHT, -1610612736);
            drawText(mc, this.module.getState().byId(this.draggingId).content,
                    this.x + 16, dragY + 5, -1);
            if (this.dragTargetIndex >= 0) {
                int lineY = this.rowsTop() + this.dragTargetIndex * ROW_STEP;
                cn.kazer.client189.compat.LegacyGui.drawRect(this.x, lineY - 1, right, lineY + 1, -2039808);
            }
        }

        // Add 按钮：左上
        CustomTextState state = this.module.getState();
        boolean canAdd = state.items.size() < CustomTextState.MAX_ITEMS;
        boolean addHovered = canAdd && mouseX > (float) this.x * this.scale
                && mouseX < (float) (this.x + addWidth(mc)) * this.scale
                && mouseY > (float) (this.y + this.yOffset) * this.scale
                && mouseY < (float) (this.y + 14 + this.yOffset) * this.scale;
        drawText(mc, canAdd ? "+ ADD TEXT" : "ADD TEXT",
                this.x, this.y + 5,
                canAdd ? (addHovered ? COLOUR_HOVER_FILL : COLOUR_COUNTER) : -2130706433);

        // 按下后位移/释放驱动：每帧处理。拖拽只从行本体发起；图标按下不触发拖拽。
        if (this.pressedRowId != null) {
            boolean down = Mouse.isButtonDown(0);
            if (down && !this.dragging && this.pressedRegion == RowRegion.CONTENT) {
                int dist = Math.abs(mouseX - this.pressStartX) + Math.abs(mouseY - this.pressStartY);
                if (dist >= DRAG_THRESHOLD_PX) {
                    this.dragging = true;
                    this.draggingId = this.pressedRowId;
                }
            }
            if (!down) {
                if (this.dragging) {
                    if (this.dragTargetIndex >= 0) {
                        this.module.moveInstance(this.pressedRowId, this.dragTargetIndex);
                    }
                    this.resetRowPress();
                } else {
                    // 松手结算：按下区域与松开时指针仍在同一区域才触发，挪走即作废。
                    CustomTextItem item = this.module.getState().byId(this.pressedRowId);
                    RowRegion region = this.pressedRegion;
                    int index = item != null ? this.module.getState().items.indexOf(item) : -1;
                    RowRegion releasedOn = index >= 0
                            ? this.hitRegion(mouseX, mouseY, this.rowsTop() + index * ROW_STEP)
                            : RowRegion.NONE;
                    this.resetRowPress();
                    if (item != null && releasedOn == region) {
                        if (region == RowRegion.PENCIL) {
                            playClick();   // overlay 自身无音效，这里补
                            this.openContextEdit(item);
                        } else {
                            // panel.lIIIIIIIIIlIllIIllIlIIlIl 内部已有点击音，不重复播
                            this.openSubSettings(item); // CONTENT 与 COG 都进子设置页
                        }
                    }
                }
            } else if (this.dragging) {
                float logicalY = (float) mouseY / this.scale - this.yOffset;
                int raw = (int) ((logicalY - this.rowsTop()) / ROW_STEP);
                this.dragTargetIndex = clamp(raw, 0, this.module.getState().items.size() - 1);
            }
        }

        if (this.confirmDeleteId != null && Minecraft.getSystemTime() > this.confirmDeleteUntilMs) {
            this.confirmDeleteId = null;
        }
    }

    private int stepIconFill(String itemId, RowRegion region, RowRegion hoverRegion) {
        String key = itemId + "#" + region.name();
        int fill = stepFill(this.hoverFill.getOrDefault(key, 0), hoverRegion == region);
        this.hoverFill.put(key, fill);
        return fill;
    }

    private void drawIconCellCover(RowRegion region, int rowY, int fill) {
        int left = this.iconCellLeft(region);
        int cover = fill * ROW_HEIGHT / 75;
        cn.kazer.client189.compat.LegacyGui.drawRect(left, rowY + ROW_HEIGHT - cover, left + ICON_CELL, rowY + ROW_HEIGHT, COLOUR_HOVER_FILL);
    }

    private void drawRowIcons(CustomTextItem item, int rowY, int pencilFill, int cogFill, int deleteFill) {
        boolean deleteConfirming = item.id.equals(this.confirmDeleteId);
        int glyphOffset = (ICON_CELL - (int) (ICON_SCALE * 2)) / 2;
        GL11.glPushMatrix();
        this.drawIcon(PENCIL_ICON, this.iconCellLeft(RowRegion.PENCIL) + glyphOffset, rowY + 5, pencilFill > 0);
        this.drawIcon(COG_ICON, this.iconCellLeft(RowRegion.COG) + glyphOffset, rowY + 5, cogFill > 0);
        if (deleteConfirming) {
            cn.kazer.client189.compat.LegacyGL.glColor4f(1.0F, 0.3F, 0.3F, 1.0F);
        } else {
            tintIcon(deleteFill > 0);
        }
        RenderUtil.drawIcon(DELETE_ICON, ICON_SCALE, this.iconCellLeft(RowRegion.DELETE) + glyphOffset, rowY + 5);
        GL11.glPopMatrix();
    }

    private void drawIcon(ResourceLocation icon, int x, int y, boolean lit) {
        tintIcon(lit);
        RenderUtil.drawIcon(icon, ICON_SCALE, x, y);
    }

    private static void tintIcon(boolean lit) {
        if (lit) {
            cn.kazer.client189.compat.LegacyGL.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
        } else {
            cn.kazer.client189.compat.LegacyGL.glColor4f(0.65F, 0.65F, 0.65F, 1.0F);   // 与 MODS 顶栏齿轮一致的浅灰
        }
    }

    private void resetRowPress() {
        this.pressedRowId = null;
        this.pressedRegion = RowRegion.NONE;
        this.pressStartX = 0;
        this.pressStartY = 0;
        this.dragging = false;
        this.draggingId = null;
        this.dragTargetIndex = -1;
    }

    @Override
    public void handleMouseClick(int mouseX, int mouseY, int button) {
        if (button != 0) return;
        Minecraft mc = Minecraft.getMinecraft();

        for (int i = 0; i < this.module.getState().items.size(); i++) {
            CustomTextItem item = this.module.getState().items.get(i);
            int rowY = this.rowsTop() + i * ROW_STEP;
            RowRegion region = hitRegion(mouseX, mouseY, rowY);
            if (region == RowRegion.NONE) continue;

            switch (region) {
                case TOGGLE:
                    this.module.setInstanceEnabled(item.id, !item.enabled);
                    playClick();
                    return;
                case DELETE:
                    if (item.id.equals(this.confirmDeleteId)
                            && Minecraft.getSystemTime() <= this.confirmDeleteUntilMs) {
                        this.module.removeInstance(item.id);
                        this.confirmDeleteId = null;
                        if (CBModulesGui.instance != null) CBModulesGui.instance.cbzRefreshModuleList();
                    } else {
                        this.confirmDeleteId = item.id;
                        this.confirmDeleteUntilMs = Minecraft.getSystemTime() + DELETE_CONFIRM_MS;
                    }
                    playClick();
                    return;
                case CONTENT:
                case COG:
                case PENCIL:
                default:
                    // 按下仅记录区域与起点，不执行任何页面跳转；松开时才结算。
                    // 避免按下即开子页/选框后，同一次松开又落到新页面上造成二连结算。
                    this.pressedRowId = item.id;
                    this.pressedRegion = region;
                    this.pressStartX = mouseX;
                    this.pressStartY = mouseY;
                    this.dragging = false;
                    this.draggingId = null;
                    this.dragTargetIndex = i;
                    return;
            }
        }

        CustomTextState state = this.module.getState();
        boolean canAdd = state.items.size() < CustomTextState.MAX_ITEMS;
        boolean addHit = canAdd
                && mouseX > (float) this.x * this.scale
                && mouseX < (float) (this.x + addWidth(mc)) * this.scale
                && mouseY > (float) (this.y + this.yOffset) * this.scale
                && mouseY < (float) (this.y + 14 + this.yOffset) * this.scale;
        if (addHit) {
            playClick();
            CustomTextItem created = this.module.addInstance();
            if (created != null && CBModulesGui.instance != null) {
                CBModulesGui.instance.cbzRefreshModuleList();
                CustomTextModule.CustomTextChildUnit unit = this.module.childUnitFor(created.id);
                if (unit != null) {
                    Minecraft.getMinecraft().displayGuiScreen(
                            new CBModulePlaceGui(CBModulesGui.instance, unit, () -> openSubSettingsAfter(unit)));
                }
            }
        }
    }

    private void openSubSettings(CustomTextItem item) {
        CustomTextModule.CustomTextChildUnit unit = this.module.childUnitFor(item.id);
        if (unit != null && CBModulesGui.instance != null) {
            CBModulesGui.instance.cbzOpenModuleSubPage(unit);
        }
    }

    private void openSubSettingsAfter(CustomTextModule.CustomTextChildUnit unit) {
        if (CBModulesGui.instance != null) {
            CBModulesGui.instance.cbzRefreshModuleList();
            CBModulesGui.instance.cbzOpenModuleSubPage(unit);
        }
    }

    private void openContextEdit(CustomTextItem item) {
        if (CBModulesGui.instance == null) return;
        Minecraft.getMinecraft().displayGuiScreen(new CBTextInputOverlay(
                CBModulesGui.instance, "Text: ", item.content,
                text -> this.module.setInstanceContent(item.id, text)));
    }

    public static int dragTargetIndex(float rowsTop, float mouseY, float rowStep, int rowCount) {
        int index = (int) ((mouseY - rowsTop) / rowStep);
        return clamp(index, 0, rowCount - 1);
    }

    public static boolean isDragStart(int pressX, int pressY, int mouseX, int mouseY) {
        return Math.abs(mouseX - pressX) + Math.abs(mouseY - pressY) >= DRAG_THRESHOLD_PX;
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private static void playClick() {
        Minecraft.getMinecraft().getSoundHandler()
                .playSound(PositionedSoundRecord.create(new ResourceLocation("gui.button.press"), 1.0F));
    }
}
