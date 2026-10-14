package com.cheatbreaker.client.module.type;

import com.cheatbreaker.client.config.Setting;
import com.cheatbreaker.client.event.type.GuiDrawEvent;
import com.cheatbreaker.client.event.type.RenderPreviewEvent;
import com.cheatbreaker.client.module.AbstractModule;
import com.cheatbreaker.client.ui.module.CBGuiAnchor;
import com.cheatbreaker.client.ui.module.CBModulePlaceGui;
import com.cheatbreaker.client.ui.module.CBModulesGui;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.client.renderer.texture.TextureUtil;
import net.minecraft.client.resources.ResourcePackRepository;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.opengl.GL11;

import java.io.IOException;

public final class PackDisplayModule extends AbstractModule {
    private final Setting showPackImage;
    private final Setting showBackground;
    private final Setting backgroundColor;
    private ResourceLocation defaultIconLocation;

    public PackDisplayModule() {
        super("Pack Display");
        this.setDefaultAnchor(CBGuiAnchor.LEFT_TOP);
        this.setDefaultTranslations(0.0F, 12.0F);
        this.setDefaultState(true);
        this.showPackImage = new Setting(this, "Show Pack Image").setValue(false);
        this.showBackground = new Setting(this, "Show Background").setValue(false);
        this.backgroundColor = new Setting(this, "Background Color").setValue(0x6F000000)
                .setMinMax(Integer.MIN_VALUE, Integer.MAX_VALUE).setParent(this.showBackground);
        this.setPreviewLabel("[Default]", 1.5F);
        this.addEvent(GuiDrawEvent.class, this::renderHud);
        this.addEvent(RenderPreviewEvent.class, this::renderPreview);
    }

    Setting getShowPackImage() {
        return this.showPackImage;
    }

    private void renderHud(GuiDrawEvent event) {
        if (!this.isRenderHud() || this.minecraft.thePlayer == null
                || this.minecraft.currentScreen instanceof CBModulesGui
                || this.minecraft.currentScreen instanceof CBModulePlaceGui) {
            return;
        }
        this.render(event);
    }

    private void renderPreview(RenderPreviewEvent event) {
        if (!this.isRenderHud()) {
            return;
        }
        if (this.minecraft.currentScreen instanceof CBModulePlaceGui
                || this.minecraft.currentScreen instanceof CBModulesGui) {
            this.render(event);
        }
    }

    private void render(GuiDrawEvent event) {
        boolean image = (Boolean) this.showPackImage.getValue();
        boolean background = (Boolean) this.showBackground.getValue();
        ResourcePackRepository.Entry top = (ResourcePackRepository.Entry) PackDisplayContent.topOrNull(
                this.minecraft.getResourcePackRepository().getRepositoryEntries());
        String text = PackDisplayContent.resolveName(top == null ? null : top.getResourcePackName());
        int textWidth = this.minecraft.fontRendererObj.getStringWidth(text);
        float[] dimensions = PackDisplayContent.computeDimensions(image, true, background, textWidth,
                this.minecraft.fontRendererObj.FONT_HEIGHT);
        this.setDimensions(dimensions[0], dimensions[1]);
        boolean depthEnabled = GL11.glIsEnabled(GL11.GL_DEPTH_TEST);
        boolean blendEnabled = GL11.glIsEnabled(GL11.GL_BLEND);
        GL11.glPushMatrix();
        cn.kazer.client189.compat.LegacyGL.glDisable(GL11.GL_DEPTH_TEST);
        try {
            this.scaleAndTranslate(event.getResolution());
            if (background) {
                cn.kazer.client189.compat.LegacyGui.drawRect(0.0F, 0.0F, dimensions[0], dimensions[1], this.backgroundColor.getColorValue());
            }
            if (image) {
                this.bindPackIcon(top);
                cn.kazer.client189.compat.LegacyGL.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
                cn.kazer.client189.compat.LegacyGL.glEnable(GL11.GL_BLEND);
                OpenGlHelper.glBlendFunc(770, 771, 1, 0);
                Gui.drawModalRectWithCustomSizedTexture(0, 0, 0.0F, 0.0F,
                        PackDisplayContent.ICON_SIZE, PackDisplayContent.ICON_SIZE,
                        (float) PackDisplayContent.ICON_SIZE, (float) PackDisplayContent.ICON_SIZE);
            }
            int textX = PackDisplayContent.textX(image, background);
            int textY = PackDisplayContent.textY(image, this.minecraft.fontRendererObj.FONT_HEIGHT, background);
            // Reset the draw color so the pack icon bind cannot tint the name; the
            // pack's own § color codes drive the text, defaulting to white.
            cn.kazer.client189.compat.LegacyGL.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
            if (background) {
                this.minecraft.fontRendererObj.drawString(text, textX, textY, 0xFFFFFFFF);
            } else {
                this.minecraft.fontRendererObj.drawString(text, textX, textY, 0xFFFFFFFF, true);
            }
        } finally {
            GL11.glPopMatrix();
            if (depthEnabled) {
                cn.kazer.client189.compat.LegacyGL.glEnable(GL11.GL_DEPTH_TEST);
            }
            if (!blendEnabled) {
                cn.kazer.client189.compat.LegacyGL.glDisable(GL11.GL_BLEND);
            }
        }
    }

    private void bindPackIcon(ResourcePackRepository.Entry top) {
        TextureManager textureManager = this.minecraft.getTextureManager();
        if (top != null) {
            top.bindTexturePackIcon(textureManager);
            return;
        }
        if (this.defaultIconLocation == null) {
            DynamicTexture texture;
            try {
                texture = new DynamicTexture(
                        this.minecraft.getResourcePackRepository().rprDefaultResourcePack.getPackImage());
            } catch (IOException var3) {
                texture = TextureUtil.missingTexture;
            }
            this.defaultIconLocation = textureManager.getDynamicTextureLocation("texturepackicon", texture);
        }
        textureManager.bindTexture(this.defaultIconLocation);
    }
}
