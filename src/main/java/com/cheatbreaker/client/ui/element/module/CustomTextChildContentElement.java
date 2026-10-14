package com.cheatbreaker.client.ui.element.module;

import com.cheatbreaker.client.module.type.customtext.CustomTextModule;
import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.PositionedSoundRecord;
import net.minecraft.client.gui.Gui;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.input.Keyboard;

/**
 * Custom Text 子设置页第一行 CONTENT 的自由文本输入（单行、IME 友好、长度上限 256）。
 * 为走全部交 keyboard 会消费的 IME/键事件，用左侧白色小条显示聚焦状态。
 */
public class CustomTextChildContentElement extends com.cheatbreaker.client.ui.element.AbstractModulesGuiElement {
    private static final int CONTENT_MAX_LENGTH = 256;
    private static final int CONTENT_HEIGHT = 14;

    private final CustomTextModule module;
    private final com.cheatbreaker.client.module.type.customtext.CustomTextModule.CustomTextChildUnit unit;
    private final ContentEditor editor = new ContentEditor();

    public CustomTextChildContentElement(CustomTextModule module, CustomTextModule.CustomTextChildUnit unit, float scale) {
        super(scale);
        this.module = module;
        this.unit = unit;
        this.editor.begin(module.getState().byId(unit.getItem().id) != null
                ? module.getState().byId(unit.getItem().id).content : "");
    }

    @Override
    public int getHeight() {
        return 10 + CONTENT_HEIGHT;
    }

    @Override
    public void handleDrawElement(int mouseX, int mouseY, float partialTicks) {
        Minecraft minecraft = Minecraft.getMinecraft();
        int top = this.y;
        minecraft.fontRendererObj.drawString("CONTENT", this.x, top, -6250336);
        top += 10;
        cn.kazer.client189.compat.LegacyGui.drawRect(this.x - 1, top - 1, this.x + this.width + 1, top + CONTENT_HEIGHT + 1, -6250336);
        cn.kazer.client189.compat.LegacyGui.drawRect(this.x, top, this.x + this.width, top + CONTENT_HEIGHT, -16777216);
        int available = this.width - 8;
        this.editor.scrollToCursor(minecraft.fontRendererObj, available);
        String visible = minecraft.fontRendererObj.trimStringToWidth(
                this.editor.value.substring(this.editor.scrollFrom), available);
        minecraft.fontRendererObj.drawString(visible, this.x + 4, top + 3, -2039584);
        if (this.editor.focused && Minecraft.getSystemTime() % 1000L < 500L) {
            int caretX = this.x + 4 + minecraft.fontRendererObj.getStringWidth(
                    visible.substring(0, Math.min(visible.length(), this.editor.cursor - this.editor.scrollFrom)));
            cn.kazer.client189.compat.LegacyGui.drawRect(caretX, top + 2, caretX + 1, top + CONTENT_HEIGHT - 2, -3092272);
        }
    }

    @Override
    public void handleMouseClick(int mouseX, int mouseY, int button) {
        if (button != 0) return;
        if (mouseX > (float) this.x * this.scale
                && mouseX < (float) (this.x + this.width) * this.scale
                && mouseY > (float) (this.y + this.yOffset) * this.scale
                && mouseY < (float) (this.y + this.getHeight() + this.yOffset) * this.scale) {
            this.editor.focus();
            playClick();
        } else {
            this.editor.unfocus();
        }
    }

    @Override
    public boolean handleKeyTyped(char character, int keyCode) {
        if (!this.editor.focused) {
            return false;
        }
        if (this.editor.keyTyped(character, keyCode)) {
            com.cheatbreaker.client.module.type.customtext.CustomTextItem item = this.module.getState().byId(
                    this.unit.getItem().id);
            if (item != null) {
                this.module.setInstanceContent(item.id, this.editor.value);
            }
            return true;
        }
        if (keyCode == Keyboard.KEY_RETURN || keyCode == Keyboard.KEY_ESCAPE) {
            this.editor.unfocus();
            return true;
        }
        return false;
    }

    @Override
    public void onFocusLost() {
        this.editor.unfocus();
    }

    private static void playClick() {
        Minecraft.getMinecraft().getSoundHandler()
                .playSound(PositionedSoundRecord.create(new ResourceLocation("gui.button.press"), 1.0F));
    }

    /**
     * 该元素是自己的子设置页的单行文本框，允许光标、scroll、删除/退格/移动，及 IME commit。
     */
    static final class ContentEditor {
        String value = "";
        int cursor;
        int scrollFrom;
        boolean focused;

        void begin(String content) {
            this.value = content == null ? "" : content;
            this.cursor = this.value.length();
            this.scrollFrom = 0;
            this.focused = false;
        }

        void focus() {
            this.focused = true;
            this.cursor = this.value.length();
        }

        void unfocus() {
            this.focused = false;
        }

        void scrollToCursor(net.minecraft.client.gui.FontRenderer fontRenderer, int available) {
            while (this.scrollFrom < this.cursor
                    && fontRenderer.getStringWidth(this.value.substring(this.scrollFrom, this.cursor)) > available) {
                this.scrollFrom++;
            }
            if (this.scrollFrom > this.cursor) this.scrollFrom = this.cursor;
        }

        boolean keyTyped(char character, int keyCode) {
            if (!this.focused) return false;
            StringBuilder buffer = new StringBuilder(this.value);
            switch (keyCode) {
                case Keyboard.KEY_BACK:
                    if (this.cursor > 0) {
                        buffer.deleteCharAt(this.cursor - 1);
                        this.cursor--;
                        this.value = buffer.toString();
                    }
                    return true;
                case Keyboard.KEY_DELETE:
                    if (this.cursor < this.value.length()) {
                        this.value = buffer.deleteCharAt(this.cursor).toString();
                    }
                    return true;
                case Keyboard.KEY_LEFT:
                    if (this.cursor > 0) this.cursor--;
                    return true;
                case Keyboard.KEY_RIGHT:
                    if (this.cursor < this.value.length()) this.cursor++;
                    return true;
                case Keyboard.KEY_HOME:
                    this.cursor = 0;
                    return true;
                case Keyboard.KEY_END:
                    this.cursor = this.value.length();
                    return true;
                default:
                    if (character >= 32 && character != 127 && this.value.length() < CONTENT_MAX_LENGTH) {
                        this.value = buffer.insert(this.cursor, character).toString();
                        this.cursor++;
                        return true;
                    }
                    return false;
            }
        }
    }
}
