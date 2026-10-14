package cn.kazer.client189.compat;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ChatLine;
import net.minecraft.client.gui.GuiNewChat;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.Session;
import net.minecraftforge.fml.relauncher.ReflectionHelper;

import java.util.List;

public final class KazerCompat {
    public static float partialTicks;
    private static final VignetteSession VIGNETTE = new VignetteSession();
    private static KeyBinding fallbackZoom;
    private static final java.nio.FloatBuffer NAMEPLATE = org.lwjgl.BufferUtils.createFloatBuffer(16);
    private KazerCompat() { }

    public static VignetteSession vignette() { return VIGNETTE; }
    public static void drawVignette(int width, int height, float progress) {
        com.cheatbreaker.client.config.GlobalSettings settings = com.cheatbreaker.client.CheatBreaker.getInstance().globalSettings;
        if (!(Boolean)settings.containerVignette.getValue()) return;
        int strength = (Integer)settings.containerVignetteStrength.getValue();
        int alpha = Math.min(255, Math.round(184 * strength / 50F * Math.max(0, Math.min(1, progress))));
        LegacyGui.drawRect(0, 0, width, height, alpha << 24 | 0x101010);
    }
    public static boolean beginTextNameplate() {
        return beginTextNameplate(false);
    }
    public static boolean beginTextNameplate(boolean preserveScale) {
        boolean cull = org.lwjgl.opengl.GL11.glIsEnabled(org.lwjgl.opengl.GL11.GL_CULL_FACE);
        org.lwjgl.opengl.GL11.glDisable(org.lwjgl.opengl.GL11.GL_CULL_FACE);
        NAMEPLATE.clear();
        org.lwjgl.opengl.GL11.glGetFloat(org.lwjgl.opengl.GL11.GL_MODELVIEW_MATRIX, NAMEPLATE);
        float scale = preserveScale ? (float)Math.sqrt(NAMEPLATE.get(0)*NAMEPLATE.get(0)
                + NAMEPLATE.get(1)*NAMEPLATE.get(1) + NAMEPLATE.get(2)*NAMEPLATE.get(2)) : 1F;
        if(preserveScale && Minecraft.getMinecraft().currentScreen instanceof net.minecraft.client.gui.inventory.GuiContainer) {
            int top=ReflectionHelper.getPrivateValue(net.minecraft.client.gui.inventory.GuiContainer.class,
                    (net.minecraft.client.gui.inventory.GuiContainer)Minecraft.getMinecraft().currentScreen,"guiTop","field_147009_r");
            // Pointer-driven model tilt must not push the name above its portrait frame.
            NAMEPLATE.put(13,Math.max(NAMEPLATE.get(13),top+9F));
        }
        for (int i = 0; i < 12; i++) NAMEPLATE.put(i, 0);
        NAMEPLATE.put(0, -scale); NAMEPLATE.put(5, preserveScale ? -scale : scale); NAMEPLATE.put(10, scale); NAMEPLATE.put(15, 1);
        NAMEPLATE.rewind();
        org.lwjgl.opengl.GL11.glLoadMatrix(NAMEPLATE);
        return cull;
    }
    public static void endTextNameplate(boolean cull) {
        if (cull) org.lwjgl.opengl.GL11.glEnable(org.lwjgl.opengl.GL11.GL_CULL_FACE);
        else org.lwjgl.opengl.GL11.glDisable(org.lwjgl.opengl.GL11.GL_CULL_FACE);
    }
    public static void setSession(Session session) {
        ReflectionHelper.setPrivateValue(Minecraft.class, Minecraft.getMinecraft(), session, "session", "field_71449_j");
    }
    public static KeyBinding zoomKey() {
        for (KeyBinding key : Minecraft.getMinecraft().gameSettings.keyBindings) {
            if ("of.key.zoom".equals(key.getKeyDescription()) || "Zoom".equals(key.getKeyDescription())) return key;
        }
        if (fallbackZoom == null) fallbackZoom = new KeyBinding("of.key.zoom", 46, "key.categories.misc");
        return fallbackZoom;
    }
    public static int fontColor(int index) {
        int[] colors = ReflectionHelper.getPrivateValue(net.minecraft.client.gui.FontRenderer.class,
                Minecraft.getMinecraft().fontRendererObj, "colorCode", "field_78285_g");
        return colors[index];
    }
    public static int visibleChatLines(GuiNewChat chat) {
        List<ChatLine> lines = ReflectionHelper.getPrivateValue(GuiNewChat.class, chat, "drawnChatLines", "field_146253_i");
        int count = 0;
        int now = Minecraft.getMinecraft().ingameGUI.getUpdateCounter();
        for (ChatLine line : lines) {
            if (count >= chat.getLineCount()) break;
            if (chat.getChatOpen() || now - line.getUpdatedCounter() < 200) count++;
        }
        return count;
    }
    public static void loadGuiBlur() {
        Minecraft.getMinecraft().entityRenderer.loadShader(new ResourceLocation("shaders/post/blur.json"));
    }
    public static org.lwjgl.util.vector.Matrix4f shaderMatrix(javax.vecmath.Matrix4f source) {
        org.lwjgl.util.vector.Matrix4f target = new org.lwjgl.util.vector.Matrix4f();
        // ShaderUniform.set(Matrix4f) serializes named fields in row order and uploads with transpose=true.
        // This is deliberately not an LWJGL column-major matrix conversion.
        target.m00 = source.m00; target.m01 = source.m01; target.m02 = source.m02; target.m03 = source.m03;
        target.m10 = source.m10; target.m11 = source.m11; target.m12 = source.m12; target.m13 = source.m13;
        target.m20 = source.m20; target.m21 = source.m21; target.m22 = source.m22; target.m23 = source.m23;
        target.m30 = source.m30; target.m31 = source.m31; target.m32 = source.m32; target.m33 = source.m33;
        return target;
    }
}
