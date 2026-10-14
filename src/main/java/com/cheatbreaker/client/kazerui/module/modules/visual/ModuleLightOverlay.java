package com.cheatbreaker.client.kazerui.module.modules.visual;

import com.cheatbreaker.client.kazerui.module.Module;
import com.cheatbreaker.client.kazerui.module.Page;
import com.cheatbreaker.client.kazerui.module.setting.*;
import com.cheatbreaker.client.kazerui.event.UiTick;
import net.minecraft.entity.Entity;
import net.minecraft.util.MathHelper;
import net.minecraft.world.EnumSkyBlock;
import net.minecraft.util.BlockPos;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.WorldRenderer;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import org.lwjgl.opengl.GL11;

/** Loaded full-block floor markers; brightness hint, not a claim of exact mob spawning rules. */
public final class ModuleLightOverlay extends Module {
    private static ModuleLightOverlay instance;
    private final NumberSetting radius = integer("renderRadius", 8, 1, 16);
    private final NumberSetting threshold = integer("lightThreshold", 7, 0, 15);
    private final BoolSetting includeSky = bool("includeSkyLight", true), darkOnly = bool("onlyDarkMarkers", false);
    private final ColorSetting bright = color("brightColor", 0xFF55FF55).noAlpha().noChroma();
    private final ColorSetting dark = color("darkColor", 0xFFFF5555).noAlpha().noChroma();
    private final LightOverlayCache cache = new LightOverlayCache();
    private final BlockPos.MutableBlockPos position = new BlockPos.MutableBlockPos();
    private final LightOverlayCache.Reader reader = this::readLight;
    public ModuleLightOverlay() { super("LIGHT_OVERLAY", false); instance = this; labels(); }
    private void labels() {
        for (Setting<?> setting : settings()) {
            switch (setting.key) {
                case "renderRadius": setting.label(() -> "Marker Radius (Blocks)"); break;
                case "lightThreshold": setting.label(() -> "Dark Light Threshold"); break;
                case "includeSkyLight": setting.label(() -> "Include Sky Light"); break;
                case "onlyDarkMarkers": setting.label(() -> "Show Only Dark Markers"); break;
                case "brightColor": setting.label(() -> "Bright Marker Color"); break;
                case "darkColor": setting.label(() -> "Dark Marker Color"); break;
                default: break;
            }
        }
    }
    @Override protected void layout(Page p) { p.add(radius, includeSky, threshold, darkOnly, bright, dark); }
    @Override protected void onDisable() { cache.clear(); }
    @Override public void tick(UiTick.ClientTickEvent event) {
        if (event.phase != UiTick.Phase.END) return;
        if (!isEnabled() || mc().theWorld == null || mc().thePlayer == null) { cache.clear(); return; }
        Entity player = mc().thePlayer;
        cache.update(mc().theWorld, MathHelper.floor_double(player.posX), MathHelper.floor_double(player.posY),
            MathHelper.floor_double(player.posZ), radius.intValue(), includeSky.on(), reader);
    }
    private int readLight(int x, int y, int z, boolean sky) {
        position.set(x, y, z);
        if (!mc().theWorld.isBlockLoaded(position)) return -1;
        position.set(x, y - 1, z);
        if (!mc().theWorld.getBlockState(position).getBlock().isOpaqueCube()) return -1;
        position.set(x, y, z);
        if (!mc().theWorld.isAirBlock(position)) return -1;
        position.set(x, y + 1, z);
        if (!mc().theWorld.isAirBlock(position)) return -1;
        position.set(x, y, z);
        int block = mc().theWorld.getLightFor(EnumSkyBlock.BLOCK, position);
        return sky ? Math.max(block, mc().theWorld.getLightFor(EnumSkyBlock.SKY, position) - mc().theWorld.getSkylightSubtracted()) : block;
    }
    public static void render(float partial) {
        ModuleLightOverlay m = instance;
        if (m == null || !m.isEnabled() || m.cache.count() == 0 || mc().theWorld == null || mc().thePlayer == null) return;
        Entity view = mc().getRenderViewEntity();
        if (view == null) return;
        double vx = view.lastTickPosX + (view.posX - view.lastTickPosX) * partial;
        double vy = view.lastTickPosY + (view.posY - view.lastTickPosY) * partial;
        double vz = view.lastTickPosZ + (view.posZ - view.lastTickPosZ) * partial;
        cn.kazer.client189.compat.LegacyGL.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS);
        GL11.glPushMatrix();
        try {
            GL11.glDisable(GL11.GL_TEXTURE_2D); GL11.glDisable(GL11.GL_LIGHTING);
            GL11.glDisable(GL11.GL_BLEND); GL11.glDisable(GL11.GL_FOG);
            GL11.glDepthMask(false); GL11.glLineWidth(1);
            GL11.glTranslated(-vx, -vy, -vz);
            WorldRenderer buffer = Tessellator.getInstance().getWorldRenderer(); buffer.begin(GL11.GL_LINES, DefaultVertexFormats.POSITION_COLOR);
            int threshold = m.threshold.intValue(), bright = m.bright.argb(), dark = m.dark.argb();
            boolean darkOnly = m.darkOnly.on();
            for (int n = 0; n < m.cache.count(); n++) {
                boolean low = m.cache.light(n) <= threshold;
                if (!low && darkOnly) continue;
                double x = m.cache.x(n), y = m.cache.y(n) + .015, z = m.cache.z(n);
                int color = low ? dark : bright;
                int red = color >>> 16 & 255, green = color >>> 8 & 255, blue = color & 255;
                buffer.pos(x + .2, y, z + .2).color(red, green, blue, 255).endVertex();
                buffer.pos(x + .8, y, z + .8).color(red, green, blue, 255).endVertex();
                buffer.pos(x + .8, y, z + .2).color(red, green, blue, 255).endVertex();
                buffer.pos(x + .2, y, z + .8).color(red, green, blue, 255).endVertex();
            }
            Tessellator.getInstance().draw();
        } finally { GL11.glPopMatrix(); cn.kazer.client189.compat.LegacyGL.glPopAttrib(); }
    }
}
