package com.cheatbreaker.client.ui.mainmenu;

import com.cheatbreaker.client.CheatBreaker;
import com.cheatbreaker.client.ui.util.RenderUtil;
import com.cheatbreaker.client.ui.util.KazerUiIcons;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiMultiplayer;
import net.minecraft.client.gui.GuiOptions;
import net.minecraft.client.gui.GuiSelectWorld;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.input.Keyboard;
import org.lwjgl.opengl.GL11;


/** Kazer main menu built from the supplied Lunar 1.7.10 layout reference. */
public class MainMenu extends MainMenuBase {
    private static final ResourceLocation KAZER_LOGO = new ResourceLocation("client/kazer_logo.png");
    private GuiButton singleplayer;
    private GuiButton multiplayer;
    private GuiButton options;
    private GuiButton quit;
    private ResourceLocation accountSkin;
    private String accountSkinId = "";

    @Override
    public void initGui() {
        super.initGui();
        KazerAccountBridge.applyPending();
        String sessionId = mc.getSession().getPlayerID();
        if (!sessionId.equals(accountSkinId) && mc.getSession().getProfile().getId() != null) {
            accountSkinId = sessionId;
            accountSkin = null;
            mc.getSkinManager().loadProfileTextures(mc.getSession().getProfile(), (type, location, texture) -> {
                if (type == com.mojang.authlib.minecraft.MinecraftProfileTexture.Type.SKIN
                        && sessionId.equals(accountSkinId)) accountSkin = location;
            }, false);
        }
        int centerX = (int)getScaledWidth() / 2;
        int startY = (int)getScaledHeight() / 2 - 32;
        this.singleplayer = new GuiButton(1, centerX - 100, startY, 200, 20, "Singleplayer");
        this.multiplayer = new GuiButton(2, centerX - 100, startY + 24, 200, 20, "Multiplayer");
        this.options = new GuiButton(3, centerX - 100, startY + 56, 98, 20, "Options...");
        this.quit = new GuiButton(4, centerX + 2, startY + 56, 98, 20, "Quit Game");
    }

    @Override
    public void drawMenu(float mouseX, float mouseY) {
        super.drawMenu(mouseX, mouseY);
        int centerX = (int)getScaledWidth() / 2;
        int startY = (int)getScaledHeight() / 2 - 32;
        int logoSize = Math.max(36, Math.min(48, (int)getScaledHeight() / 6));
        float logoY = Math.max(10F, startY - logoSize - 10F);

        cn.kazer.client189.compat.LegacyGL.glEnable(GL11.GL_TEXTURE_2D);
        cn.kazer.client189.compat.LegacyGL.glEnable(GL11.GL_BLEND);
        cn.kazer.client189.compat.LegacyGL.glDisable(GL11.GL_LIGHTING);
        cn.kazer.client189.compat.LegacyGL.glDisable(GL11.GL_DEPTH_TEST);
        cn.kazer.client189.compat.LegacyGL.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        cn.kazer.client189.compat.LegacyGL.glColor4f(1F, 1F, 1F, 1F);
        RenderUtil.lIIIIlIIllIIlIIlIIIlIIllI(KAZER_LOGO,
                centerX - logoSize / 2F, logoY, logoSize, logoSize);
        cn.kazer.client189.compat.LegacyGL.glColor4f(1F, 1F, 1F, 1F);

        drawAccountControl(mouseX, mouseY);

        draw(singleplayer, mouseX, mouseY);
        draw(multiplayer, mouseX, mouseY);
        draw(options, mouseX, mouseY);
        draw(quit, mouseX, mouseY);
        mc.fontRendererObj.drawStringWithShadow("Kazer Client 1.8.9", 4,
                (int)getScaledHeight() - 12, 0xFFFFFFFF);
        String copyright = "Copyright Mojang AB. Do not distribute!";
        mc.fontRendererObj.drawStringWithShadow(copyright,
                (int)getScaledWidth() - mc.fontRendererObj.getStringWidth(copyright) - 4,
                (int)getScaledHeight() - 12, 0xFFFFFFFF);
    }

    private void draw(GuiButton button, float mouseX, float mouseY) {
        button.drawButton(mc, (int)mouseX, (int)mouseY);
    }

    private void drawAccountControl(float mouseX, float mouseY) {
        int x = 10;
        int y = 10;
        int w = accountControlWidth();
        int h = 20;
        boolean hover = mouseX >= x && mouseX < x + w && mouseY >= y && mouseY < y + h;
        RenderUtil.lIIIIlIIllIIlIIlIIIlIIllI(x, y, x + w, y + h, 4.0D,
                hover ? 0xFF20212B : 0xFF111218);
        cn.kazer.client189.compat.LegacyGL.glColor4f(1F, 1F, 1F, 1F);
        KazerAccountsGui.drawHead(accountSkin, x + 6, y + 6, 8);
        String username = mc.getSession() == null ? "OFFLINE" : mc.getSession().getUsername();
        CheatBreaker.getInstance().playRegular18px.drawMixedString(username, x + 17, y + 6, 0xFFB9BAC6);
        KazerUiIcons.draw(KazerUiIcons.Icon.DOWN, x + w - 13, y + 8, 6, 0xFFB9BAC6);
    }

    int accountControlWidth() {
        return Math.max(63, (int)CheatBreaker.getInstance().playRegular18px.getMixedStringWidth(mc.getSession().getUsername()) + 33);
    }
    ResourceLocation accountSkin() { return accountSkin; }

    @Override
    protected void keyTyped(char typedChar, int keyCode) throws java.io.IOException {
        // Closing a worldless root screen recreates GuiMainMenu and its textures.
        // Child account screens retain their own Escape-to-parent behavior.
        if (keyCode == Keyboard.KEY_ESCAPE) return;
        super.keyTyped(typedChar, keyCode);
    }

    @Override
    public void onMouseClicked(float mouseX, float mouseY, int button) {
        if (button != 0) return;
        int x = (int)mouseX;
        int y = (int)mouseY;
        if (x >= 10 && x < 10 + accountControlWidth() && y >= 10 && y < 30) {
            mc.displayGuiScreen(new KazerAccountsGui(this));
        } else if (singleplayer.mousePressed(mc, x, y)) {
            singleplayer.playPressSound(mc.getSoundHandler());
            mc.displayGuiScreen(new GuiSelectWorld(this));
        } else if (multiplayer.mousePressed(mc, x, y)) {
            multiplayer.playPressSound(mc.getSoundHandler());
            mc.displayGuiScreen(new GuiMultiplayer(this));
        } else if (options.mousePressed(mc, x, y)) {
            options.playPressSound(mc.getSoundHandler());
            mc.displayGuiScreen(new GuiOptions(this, mc.gameSettings));
        } else if (quit.mousePressed(mc, x, y)) {
            quit.playPressSound(mc.getSoundHandler());
            mc.shutdown();
        }
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        super.drawScreen(mouseX, mouseY, partialTicks);
    }
}
