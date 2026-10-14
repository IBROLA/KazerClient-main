package cn.kazer.client189.compat;

import net.minecraft.client.renderer.GlStateManager;
import org.lwjgl.opengl.GL11;

/** Preserves the float-coordinate rectangles used by the reference UI. */
public final class LegacyGui {
    private LegacyGui() { }

    public static void drawBoxWithOutLine(float l, float t, float r, float b, float size, int border, int fill) {
        drawRectWithOutline(l, t, r, b, size, border, fill);
    }
    public static void drawRectWithOutline(float l, float t, float r, float b, float size, int border, int fill) {
        drawRect(l, t, r, t + size, border);
        drawRect(l, b - size, r, b, border);
        drawRect(l, t + size, l + size, b - size, border);
        drawRect(r - size, t + size, r, b - size, border);
        drawRect(l + size, t + size, r - size, b - size, fill);
    }
    public static void drawGradientRect(float l, float t, float r, float b, int top, int bottom) {
        GlStateManager.disableTexture2D();
        GlStateManager.enableBlend();
        GlStateManager.disableAlpha();
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
        GL11.glShadeModel(GL11.GL_SMOOTH);
        GL11.glBegin(GL11.GL_QUADS);
        GL11.glColor4f((top >> 16 & 255) / 255F, (top >> 8 & 255) / 255F, (top & 255) / 255F, (top >>> 24) / 255F);
        GL11.glVertex2f(r, t); GL11.glVertex2f(l, t);
        GL11.glColor4f((bottom >> 16 & 255) / 255F, (bottom >> 8 & 255) / 255F, (bottom & 255) / 255F, (bottom >>> 24) / 255F);
        GL11.glVertex2f(l, b); GL11.glVertex2f(r, b);
        GL11.glEnd();
        GL11.glShadeModel(GL11.GL_FLAT);
        GlStateManager.disableBlend(); GlStateManager.enableAlpha(); GlStateManager.enableTexture2D();
        // Immediate gradient colors bypass the cache, including a fully transparent final vertex.
        GlStateManager.resetColor();
        GlStateManager.color(1, 1, 1, 1);
    }

    public static void drawRect(float left, float top, float right, float bottom, int color) {
        GlStateManager.enableBlend();
        GlStateManager.disableTexture2D();
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
        GlStateManager.color((color >> 16 & 255) / 255F, (color >> 8 & 255) / 255F,
                (color & 255) / 255F, (color >>> 24) / 255F);
        GL11.glBegin(GL11.GL_QUADS);
        GL11.glVertex2f(left, bottom);
        GL11.glVertex2f(right, bottom);
        GL11.glVertex2f(right, top);
        GL11.glVertex2f(left, top);
        GL11.glEnd();
        GlStateManager.enableTexture2D();
        GlStateManager.disableBlend();
        GlStateManager.color(1, 1, 1, 1);
    }
}
