package cn.kazer.client189.compat;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.item.ItemStack;

public final class RenderItem {
    public float zLevel;
    public void renderItemAndEffectIntoGUI(FontRenderer font, TextureManager texture, ItemStack item, int x, int y) {
        net.minecraft.client.renderer.entity.RenderItem renderer = Minecraft.getMinecraft().getRenderItem();
        float old = renderer.zLevel;
        renderer.zLevel = zLevel;
        try { renderer.renderItemAndEffectIntoGUI(item, x, y); }
        finally { renderer.zLevel = old; }
    }
    public void renderItemIntoGUI(FontRenderer font, TextureManager texture, ItemStack item, int x, int y) {
        renderItemAndEffectIntoGUI(font, texture, item, x, y);
    }
}
