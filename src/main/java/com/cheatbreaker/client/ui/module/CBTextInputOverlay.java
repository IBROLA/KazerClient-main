package com.cheatbreaker.client.ui.module;

import com.cheatbreaker.client.CheatBreaker;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiTextField;
import org.lwjgl.input.Keyboard;
import org.lwjgl.opengl.GL11;

import java.util.function.Consumer;

/**
 * 通用初等文字输入选框，从 CBProfileCreateGui 提取。显示为一个带 CB Filter 色调的
 * 全屏蒙层 + 白底"标题： 文本输入框"，Enter 提交（onConfirm）、Esc 取消（回到
 * previous screen）。原场所见完成后仍由调用方负责刷新各自页面。
 */
public class CBTextInputOverlay extends GuiScreen {
    private static final int CONTENT_MAX_LENGTH = 256;
    private static final String DEFAULT_LABEL = "Edit: ";

    private final GuiScreen previous;
    private final String label;
    private final String initial;
    private final Consumer<String> onConfirm;
    private final float scaleFactor;
    private final int maxLength;
    private final Runnable onCancel;
    private final Runnable afterClose;
    private String errorMessage = "";
    private GuiTextField textField;

    public CBTextInputOverlay(GuiScreen previous, String label, String initial, Consumer<String> onConfirm) {
        this(previous, label, initial, onConfirm, 1.0f);
    }

    public CBTextInputOverlay(GuiScreen previous, String label, String initial, Consumer<String> onConfirm, float scaleFactor) {
        this(previous, label, initial, onConfirm, scaleFactor, CONTENT_MAX_LENGTH);
    }

    public CBTextInputOverlay(GuiScreen previous, String label, String initial, Consumer<String> onConfirm,
                              float scaleFactor, int maxLength) {
        this(previous, label, initial, onConfirm, scaleFactor, maxLength, null, null);
    }

    /**
     * @param onCancel   Esc 取消时回调（与 onConfirm 并用）
     * @param afterClose 关闭后回调（在 previous 屏幕重新 init 之后执行，用于恢复页面状态）
     */
    public CBTextInputOverlay(GuiScreen previous, String label, String initial, Consumer<String> onConfirm,
                              float scaleFactor, int maxLength, Runnable onCancel, Runnable afterClose) {
        this.previous = previous;
        this.label = label == null ? DEFAULT_LABEL : label;
        this.initial = initial == null ? "" : initial;
        this.onConfirm = onConfirm;
        this.scaleFactor = scaleFactor;
        this.maxLength = maxLength;
        this.onCancel = onCancel;
        this.afterClose = afterClose;
    }

    @Override
    public void initGui() {
        Keyboard.enableRepeatEvents(true);
        this.textField = new GuiTextField(0, this.mc.fontRendererObj, this.width / 2 - 70, this.height / 2 - 6, 140, 10);
        this.textField.setText(this.initial);
        this.textField.setFocused(true);
        this.textField.setMaxStringLength(this.maxLength);
    }

    @Override
    public void updateScreen() {
        this.textField.updateCursorCounter();
    }

    @Override
    public void onGuiClosed() {
        Keyboard.enableRepeatEvents(false);
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        if (this.previous != null) {
            this.previous.drawScreen(mouseX, mouseY, partialTicks);
        }
        cn.kazer.client189.compat.LegacyGui.drawRect(0, 0, this.width, this.height, -1072689136);
        float panelLeft = this.width / 2f - 73;
        float panelTop = this.height / 2f - 19;
        float panelRight = this.width / 2f + 73;
        float panelBottom = this.height / 2f + (this.errorMessage.isEmpty() ? 8 : 22);
        cn.kazer.client189.compat.LegacyGui.drawRect(panelLeft, panelTop, panelRight, panelBottom, -11250604);
        cn.kazer.client189.compat.LegacyGui.drawRect(panelLeft + 1, panelTop + 1, panelRight - 1, panelBottom - 1, -3881788);
        if (!this.errorMessage.isEmpty()) {
            cn.kazer.client189.compat.LegacyGui.drawRect(panelLeft + 1, this.height / 2f + 8, panelRight - 1, panelBottom - 1, 0x66000000);
        }
        GL11.glPushMatrix();
        GL11.glScalef(this.scaleFactor, this.scaleFactor, this.scaleFactor);
        int panelWidth = (int) ((float) this.width / this.scaleFactor);
        int panelHeight = (int) ((float) this.height / this.scaleFactor);
        CheatBreaker.getInstance().ubuntuMedium16px.drawString(this.label,
                (float) (panelWidth / 2) - (float) 70 / this.scaleFactor,
                (float) (panelHeight / 2) - (float) 17 / this.scaleFactor, 0x6F000000);
        if (!this.errorMessage.isEmpty()) {
            CheatBreaker.getInstance().ubuntuMedium16px.drawString(this.errorMessage,
                    (float) (panelWidth / 2) - (float) 72 / this.scaleFactor,
                    (float) (panelHeight / 2) + (float) 8 / this.scaleFactor, -1358954496);
        }
        GL11.glPopMatrix();
        this.textField.drawTextBox();
    }

    @Override
    public void mouseClicked(int x, int y, int button) throws java.io.IOException {
        super.mouseClicked(x, y, button);
        this.textField.mouseClicked(x, y, button);
    }

    @Override
    public void keyTyped(char character, int keyCode) throws java.io.IOException {
        switch (keyCode) {
            case Keyboard.KEY_ESCAPE:
                if (this.onCancel != null) {
                    this.onCancel.run();
                }
                close();
                break;
            case Keyboard.KEY_RETURN:
            case Keyboard.KEY_NUMPADENTER:
                String text = this.textField.getText();
                if (this.onConfirm != null) {
                    this.onConfirm.accept(text);
                }
                close();
                break;
            default:
                this.textField.textboxKeyTyped(character, keyCode);
        }
    }

    protected void close() {
        this.mc.displayGuiScreen(this.previous);
        if (this.afterClose != null) {
            this.afterClose.run();
        }
    }
}
