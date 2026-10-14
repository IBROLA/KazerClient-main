package com.cheatbreaker.client.ui.module;

import com.cheatbreaker.client.CheatBreaker;
import com.cheatbreaker.client.config.ConfigManager;
import com.cheatbreaker.client.config.Profile;
import com.cheatbreaker.client.config.ProfileNameValidator;
import com.cheatbreaker.client.ui.element.profile.ProfileElement;
import com.cheatbreaker.client.ui.element.profile.ProfilesListElement;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.util.EnumChatFormatting;
import org.lwjgl.input.Keyboard;
import org.lwjgl.opengl.GL11;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

public class CBProfileCreateGui
        extends GuiScreen {
    private final GuiScreen guiScreen;
    private final float IlllIIIlIlllIllIlIIlllIlI;
    private final int IIIIllIlIIIllIlllIlllllIl;
    private final ProfilesListElement parent;
    private GuiTextField lIIIIIIIIIlIllIIllIlIIlIl = null;
    private String IlIlIIIlllIIIlIlllIlIllIl = "";
    private boolean IIIllIllIlIlllllllIlIlIII = false;
    private Profile profile;

    public CBProfileCreateGui(Profile profile, GuiScreen guiScreen, ProfilesListElement parent, int n, float f) {
        this(guiScreen, parent, n, f);
        this.profile = profile;
    }

    public CBProfileCreateGui(GuiScreen guiScreen, ProfilesListElement parent, int n, float f) {
        this.guiScreen = guiScreen;
        this.IlllIIIlIlllIllIlIIlllIlI = f;
        this.parent = parent;
        this.IIIIllIlIIIllIlllIlllllIl = n;
        this.IIIllIllIlIlllllllIlIlIII = true;
    }

    @Override
    public void updateScreen() {
        this.lIIIIIIIIIlIllIIllIlIIlIl.updateCursorCounter();
    }

    @Override
    public void onGuiClosed() {
        Keyboard.enableRepeatEvents(false);
    }

    @Override
    public void initGui() {
        Keyboard.enableRepeatEvents(true);
        if (!this.IIIllIllIlIlllllllIlIlIII) {
            this.mc.displayGuiScreen(this.guiScreen);
            ((CBModulesGui) this.guiScreen).currentScrollableElement = ((CBModulesGui) this.guiScreen).profilesElement;
        } else {
            this.IIIllIllIlIlllllllIlIlIII = false;
            this.lIIIIIIIIIlIllIIllIlIIlIl = new GuiTextField(0, this.mc.fontRendererObj, this.width / 2 - 70, this.height / 2 - 6, 140, 10);
            if (this.profile != null) {
                this.lIIIIIIIIIlIllIIllIlIIlIl.setText(this.profile.getName());
            }
            this.lIIIIIIIIIlIllIIllIlIIlIl.setFocused(true);
        }
    }

    @Override
    public void drawScreen(int n, int n2, float f) {
        this.guiScreen.drawScreen(n, n2, f);
        cn.kazer.client189.compat.LegacyGui.drawRect(0, 0, this.width, this.height, -1072689136);
        float panelLeft = this.width / 2f - 73;
        float panelTop = this.height / 2f - 19;
        float panelRight = this.width / 2f + 73;
        float panelBottom = this.height / 2f + (this.IlIlIIIlllIIIlIlllIlIllIl.isEmpty() ? 8 : 22);
        cn.kazer.client189.compat.LegacyGui.drawRect(panelLeft, panelTop, panelRight, panelBottom, -11250604);
        cn.kazer.client189.compat.LegacyGui.drawRect(panelLeft + 1, panelTop + 1, panelRight - 1, panelBottom - 1, -3881788);
        if (!this.IlIlIIIlllIIIlIlllIlIllIl.isEmpty()) {
            cn.kazer.client189.compat.LegacyGui.drawRect(panelLeft + 1, this.height / 2f + 8, panelRight - 1, panelBottom - 1, 0x66000000);
        }
        GL11.glPushMatrix();
        GL11.glScalef(this.IlllIIIlIlllIllIlIIlllIlI, this.IlllIIIlIlllIllIlIIlllIlI, this.IlllIIIlIlllIllIlIIlllIlI);
        int n3 = (int) ((float) this.width / this.IlllIIIlIlllIllIlIIlllIlI);
        int n4 = (int) ((float) this.height / this.IlllIIIlIlllIllIlIIlllIlI);
        CheatBreaker.getInstance().ubuntuMedium16px.drawString("Profile Name: ", (float) (n3 / 2) - (float) 70 / this.IlllIIIlIlllIllIlIIlllIlI, (float) (n4 / 2) - (float) 17 / this.IlllIIIlIlllIllIlIIlllIlI, 0x6F000000);
        CheatBreaker.getInstance().ubuntuMedium16px.drawString(this.IlIlIIIlllIIIlIlllIlIllIl, (float) (n3 / 2) - (float) 72 / this.IlllIIIlIlllIllIlIIlllIlI, (float) (n4 / 2) + (float) 8 / this.IlllIIIlIlllIllIlIIlllIlI, -1358954496);
        GL11.glPopMatrix();
        this.lIIIIIIIIIlIllIIllIlIIlIl.drawTextBox();
    }

    @Override
    public void mouseClicked(int n, int n2, int n3) throws java.io.IOException {
        super.mouseClicked(n, n2, n3);
        this.lIIIIIIIIIlIllIIllIlIIlIl.mouseClicked(n, n2, n3);
    }

    @Override
    public void mouseReleased(int n, int n2, int n3) {
        super.mouseReleased(n, n2, n3);
    }

    @Override
    public void keyTyped(char c, int n) throws java.io.IOException {
        switch (n) {
            case 1: {
                this.mc.displayGuiScreen(this.guiScreen);
                ((CBModulesGui) this.guiScreen).currentScrollableElement = ((CBModulesGui) this.guiScreen).profilesElement;
                break;
            }
            case Keyboard.KEY_RETURN:
            case Keyboard.KEY_NUMPADENTER: {
                String profileName = this.lIIIIIIIIIlIllIIllIlIIlIl.getText().trim();
                if (profileName.length() < 3) {
                    this.IlIlIIIlllIIIlIlllIlIllIl = EnumChatFormatting.RED + "Name must be at least 3 characters long.";
                    break;
                }
                if (profileName.equalsIgnoreCase("default")) {
                    this.IlIlIIIlllIIIlIlllIlIllIl = EnumChatFormatting.RED + "That name is already in use.";
                    break;
                }
                if (!ProfileNameValidator.isWindowsSafeFileName(profileName)) {
                    this.IlIlIIIlllIIIlIlllIlIllIl = EnumChatFormatting.RED + "Illegal characters in name.";
                    break;
                }
                for (Profile existingProfile : CheatBreaker.getInstance().profiles) {
                    if (existingProfile != this.profile && existingProfile.getName().equalsIgnoreCase(profileName)) {
                        this.IlIlIIIlllIIIlIlllIlIllIl = EnumChatFormatting.RED + "That name is already in use.";
                        return;
                    }
                }
                if (this.profile != null && this.profile.isEditable()) {
                    File file = new File(ConfigManager.profilesDir, this.profile.getName() + ".cfg");
                    File file2 = new File(ConfigManager.profilesDir, profileName + ".cfg");
                    try {
                        if (!file.exists()) {
                            CheatBreaker.getInstance().configManager.writeProfile(this.profile.getName());
                        }
                        if (!file.exists()) {
                            this.IlIlIIIlllIIIlIlllIlIllIl = EnumChatFormatting.RED + "Could not save profile.";
                            break;
                        }
                        Files.move(file.toPath(), file2.toPath(), StandardCopyOption.REPLACE_EXISTING);
                        this.profile.setName(profileName);
                        CheatBreaker.getInstance().configManager.write();
                        this.mc.displayGuiScreen(this.guiScreen);
                        ((CBModulesGui) this.guiScreen).currentScrollableElement = ((CBModulesGui) this.guiScreen).profilesElement;
                    } catch (Exception exception) {
                        this.IlIlIIIlllIIIlIlllIlIllIl = EnumChatFormatting.RED + "Could not save profile.";
                        exception.printStackTrace();
                    }
                    break;
                }
                CheatBreaker.getInstance().configManager.writeProfile(CheatBreaker.getInstance().activeProfile.getName());
                Profile ilIIlIIlIIlllIlIIIlIllIIl3 = new Profile(profileName, true);
                CheatBreaker.getInstance().profiles.add(ilIIlIIlIIlllIlIIIlIllIIl3);
                CheatBreaker.getInstance().activeProfile = ilIIlIIlIIlllIlIIIlIllIIl3;
                this.parent.lIIIIlIIllIIlIIlIIIlIIllI.add(new ProfileElement(this.parent, this.IIIIllIlIIIllIlllIlllllIl, ilIIlIIlIIlllIlIIIlIllIIl3, this.IlllIIIlIlllIllIlIIlllIlI));
                CheatBreaker.getInstance().configManager.writeProfile(CheatBreaker.getInstance().activeProfile.getName());
                this.mc.displayGuiScreen(this.guiScreen);
                ((CBModulesGui) this.guiScreen).currentScrollableElement = ((CBModulesGui) this.guiScreen).profilesElement;
                break;
            }
            default: {
                this.lIIIIIIIIIlIllIIllIlIIlIl.textboxKeyTyped(c, n);
            }
        }
    }
}
