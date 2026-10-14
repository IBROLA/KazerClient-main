package cn.kazer.client189.runtime;

import com.cheatbreaker.client.kazerui.module.modules.visual.*;
import com.cheatbreaker.client.kazerui.module.modules.mechanic.ModuleWeatherChanger;
import java.util.List;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.resources.model.IBakedModel;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.item.ItemStack;

/** Small typed Forge host adapters. No reflection, world writes or resource reloads. */
public final class SevenVisualHooks {
    private SevenVisualHooks() {}
    public static void armorColor(float r, float g, float b, float a) {
        if (r != 1 || g != 1 || b != 1) {
            r = ModuleGlintColorizer.channel(r, true, 16);
            g = ModuleGlintColorizer.channel(g, true, 8);
            b = ModuleGlintColorizer.channel(b, true, 0);
        }
        GlStateManager.color(r, g, b, a);
    }
    public static float rainSoundStrength(float original) {
        return ModuleWeatherChanger.strength(original);
    }
    public static boolean muteSound(net.minecraft.client.audio.ISound sound) {
        return sound != null && ModuleWeatherChanger.muteSound(sound.getSoundLocation().getResourcePath());
    }
    public static void itemTranslate(float x, float y, float z, EntityItem entity, IBakedModel model, double renderY) {
        if (ModuleItemPhysics.active())
            y = (float)renderY + (model.isGui3d() ? .25f * model.getItemCameraTransforms().ground.scale.y : .065f);
        GlStateManager.translate(x, y, z);
    }
    public static void itemRotate(float angle, float x, float y, float z, EntityItem entity, float partial, IBakedModel model) {
        if (!ModuleItemPhysics.active()) { GlStateManager.rotate(angle, x, y, z); return; }
        GlStateManager.rotate(entity.rotationYaw, 0, 1, 0);
        if (!model.isGui3d()) GlStateManager.rotate(ModuleItemPhysics.angle(entity, entity.getAge() + partial), 1, 0, 0);
    }
    public static void tooltip(GuiScreen gui, List<String> lines, int x, int y, FontRenderer font, ItemStack stack) {
        if (!ModuleScrollableTooltips.draw(gui, stack, lines, x, y, font))
            net.minecraftforge.fml.client.config.GuiUtils.drawHoveringText(lines, x, y, gui.width, gui.height, -1, font);
    }
}
