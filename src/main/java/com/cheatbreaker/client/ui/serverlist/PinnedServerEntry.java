package com.cheatbreaker.client.ui.serverlist;

import com.cheatbreaker.client.ui.util.RenderUtil;
import com.google.common.base.Charsets;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufInputStream;
import io.netty.buffer.Unpooled;
import io.netty.handler.codec.base64.Base64;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiListExtended;
import net.minecraft.client.gui.GuiMultiplayer;
import net.minecraft.client.multiplayer.ServerData;
import cn.kazer.client189.compat.Tessellator;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.renderer.texture.ITextureObject;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.ResourceLocation;
import org.apache.commons.lang3.Validate;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.lwjgl.opengl.GL11;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.util.List;

public class PinnedServerEntry implements GuiListExtended.IGuiListEntry {
    private static final ResourceLocation SERVER_SELECTION_BUTTONS = new ResourceLocation("textures/gui/resource_packs.png");
    private static final ResourceLocation UNKNOWN_SERVER = new ResourceLocation("textures/misc/unknown_pack.png");
    private static final Logger logger = LogManager.getLogger();
    private final GuiMultiplayer guiMultiplayer;
    private final Minecraft mc;
    private final ServerData server;
    private long field_148298_f;
    private String lastIconB64;
    private DynamicTexture icon;
    private ResourceLocation serverIcon;
    private ResourceLocation starIcon = new ResourceLocation("client/icons/star-64.png");
    private ResourceLocation cbIcon = new ResourceLocation("client/icons/cb.png");

    public PinnedServerEntry(GuiMultiplayer p_i45048_1_, ServerData serverIn) {
        this.guiMultiplayer = p_i45048_1_;
        this.server = serverIn;
        this.mc = Minecraft.getMinecraft();
        this.serverIcon = new ResourceLocation("servers/" + serverIn.serverIP + "/icon");
        this.icon = (DynamicTexture)this.mc.getTextureManager().getTexture(this.serverIcon);
    }

    @Override
    public void drawEntry(int n, int n2, int n3, int n4, int n5, int n6, int n7, boolean bl) {
        String string;
        int n8;
        cn.kazer.client189.compat.LegacyGL.glColor4f(1.0f, 0.40909088f * 2.2f, 0.0f, 1.0f);
        RenderUtil.drawIcon(this.starIcon, (float)5, (float)(n2 - 17), (float)(n3 + 12));
        cn.kazer.client189.compat.LegacyGL.glColor4f(11.5f * 0.073913045f, 0.2857143f * 2.975f, 3.0f * 0.28333333f, 1.0f);
        if ("kazer.cn".equalsIgnoreCase(this.server.serverIP)) {
            float f = 16;
            float f2 = 8;
            float f3 = 0.0f;
            float f4 = 0.0f;
            float f5 = n2 - 20;
            float f6 = n3 + 20;
            cn.kazer.client189.compat.LegacyGL.glEnable(3042);
            Minecraft.getMinecraft().renderEngine.bindTexture(this.cbIcon);
            GL11.glBegin(7);
            GL11.glTexCoord2d(f3 / (float)5, f4 / (float)5);
            GL11.glVertex2d(f5, f6);
            GL11.glTexCoord2d(f3 / (float)5, (f4 + (float)5) / (float)5);
            GL11.glVertex2d(f5, f6 + f2);
            GL11.glTexCoord2d((f3 + (float)5) / (float)5, (f4 + (float)5) / (float)5);
            GL11.glVertex2d(f5 + f, f6 + f2);
            GL11.glTexCoord2d((f3 + (float)5) / (float)5, f4 / (float)5);
            GL11.glVertex2d(f5 + f, f6);
            GL11.glEnd();
            cn.kazer.client189.compat.LegacyGL.glDisable(3042);
        }
        boolean outOfDateClient = this.server.version > 5;
        boolean outOfDateServer = this.server.version < 5;
        boolean outOfDate = (outOfDateClient || outOfDateServer) && this.server.version != -1332;
        this.mc.fontRendererObj.drawString(this.server.serverName, n2 + 32 + 3, n3 + 1, 0xFFFFFF);
        List<String> list = this.mc.fontRendererObj.listFormattedStringToWidth(this.server.serverMOTD, n4 - 32 - 2);
        for (int i = 0; i < Math.min(list.size(), 2); ++i) {
            this.mc.fontRendererObj.drawString((String)list.get(i), n2 + 32 + 3, n3 + 12 + this.mc.fontRendererObj.FONT_HEIGHT * i, 0x808080);
        }
        String string2 = outOfDate ? (Object)((Object)EnumChatFormatting.DARK_RED) + this.server.gameVersion : this.server.populationInfo;
        int n9 = this.mc.fontRendererObj.getStringWidth(string2);
        this.mc.fontRendererObj.drawString(string2, n2 + n4 - n9 - 15 - 2, n3 + 1, 0x808080);
        int n10 = 0;
        String string3 = null;
        if (outOfDate) {
            n8 = 5;
            string = outOfDateClient ? "Client out of date!" : "Server out of date!";
            string3 = this.server.populationInfo;
        } else if (this.server.field_78841_f && this.server.pingToServer != -2L) {
            n8 = this.server.pingToServer < 0L ? 5 : (this.server.pingToServer < 150L ? 0 : (this.server.pingToServer < 300L ? 1 : (this.server.pingToServer < 600L ? 2 : (this.server.pingToServer < 1000L ? 3 : 4))));
            if (this.server.pingToServer < 0L) {
                string = "(no connection)";
            } else {
                string = this.server.pingToServer + "ms";
                string3 = this.server.populationInfo;
            }
        } else {
            n10 = 1;
            n8 = (int)(Minecraft.getSystemTime() / 100L + (long)(n * 2) & 7L);
            if (n8 > 4) {
                n8 = 8 - n8;
            }
            string = "Pinging...";
        }
        cn.kazer.client189.compat.LegacyGL.glColor4f(1.0f, 1.0f, 1.0f, 1.0f);
        this.mc.getTextureManager().bindTexture(Gui.icons);
        Gui.drawModalRectWithCustomSizedTexture(n2 + n4 - 15, n3, n10 * 10, 176 + n8 * 8, 10, 8, 256, 256);
        if (this.server.getBase64EncodedIconData() != null && !this.server.getBase64EncodedIconData().equals(this.lastIconB64)) {
            this.lastIconB64 = this.server.getBase64EncodedIconData();
            this.prepareServerIcon();
            this.guiMultiplayer.getServerList().saveServerList();
        }
        if (this.icon != null) {
            this.mc.getTextureManager().bindTexture(this.serverIcon);
        } else {
            this.mc.getTextureManager().bindTexture(UNKNOWN_SERVER);
        }
        Gui.drawModalRectWithCustomSizedTexture(n2, n3, 0.0f, 0.0f, 32, 32, 32.0F, 32.0F);

        int n11 = n6 - n2;
        int n12 = n7 - n3;
        if (n11 >= n4 - 15 && n11 <= n4 - 5 && n12 >= 0 && n12 <= 8) {
            this.guiMultiplayer.setHoveringText(string);
        } else if (n11 >= n4 - n9 - 15 - 2 && n11 <= n4 - 15 - 2 && n12 >= 0 && n12 <= 8) {
            this.guiMultiplayer.setHoveringText(string3);
        }
        Minecraft minecraft = Minecraft.getMinecraft();
        if (minecraft.gameSettings.touchscreen || bl) {
            minecraft.getTextureManager().bindTexture(SERVER_SELECTION_BUTTONS);
            cn.kazer.client189.compat.LegacyGui.drawRect(n2, n3, n2 + 32, n3 + 32, -1601138544);
            cn.kazer.client189.compat.LegacyGL.glColor4f(1.0f, 1.0f, 1.0f, 1.0f);
            int n13 = n6 - n2;
            if (n13 < 32) {
                Gui.drawModalRectWithCustomSizedTexture(n2, n3, 0.0f, 32, 32, 32, 256, 256);
            } else {
                Gui.drawModalRectWithCustomSizedTexture(n2, n3, 0.0f, 0.0f, 32, 32, 256, 256);
            }
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    private void prepareServerIcon() {
        if (this.server.getBase64EncodedIconData() == null) {
            this.mc.getTextureManager().deleteTexture(this.serverIcon);
            this.icon = null;
        } else {
            BufferedImage bufferedImage;
            block8: {
                ByteBuf byteBuf = Unpooled.copiedBuffer(this.server.getBase64EncodedIconData(), Charsets.UTF_8);
                ByteBuf byteBuf2 = Base64.decode(byteBuf);
                try {
                    bufferedImage = ImageIO.read(new ByteBufInputStream(byteBuf2));
                    Validate.validState(bufferedImage.getWidth() == 64, "Must be 64 pixels wide", new Object[0]);
                    Validate.validState(bufferedImage.getHeight() == 64, "Must be 64 pixels high", new Object[0]);
                    break block8;
                } catch (Exception exception) {
                    logger.error("Invalid icon for server " + this.server.serverName + " (" + this.server.serverIP + ")", (Throwable)exception);
                    this.server.setBase64EncodedIconData(null);
                }
                finally {
                    byteBuf.release();
                    byteBuf2.release();
                }
                return;
            }
            if (this.icon == null) {
                this.icon = new DynamicTexture(bufferedImage.getWidth(), bufferedImage.getHeight());
                this.mc.getTextureManager().loadTexture(this.serverIcon, (ITextureObject)this.icon);
            }
            bufferedImage.getRGB(0, 0, bufferedImage.getWidth(), bufferedImage.getHeight(), this.icon.getTextureData(), 0, bufferedImage.getWidth());
            this.icon.updateDynamicTexture();
        }
    }

    @Override
    public boolean mousePressed(int n, int n2, int n3, int n4, int n5, int n6) {
        this.guiMultiplayer.selectServer(n);
        if (Minecraft.getSystemTime() - this.field_148298_f < 250L) {
            this.guiMultiplayer.connectToSelected();
        }
        this.field_148298_f = Minecraft.getSystemTime();
        if (n5 <= 32) {
            if (n5 < 32) {
                this.guiMultiplayer.connectToSelected();
                return true;
            }
        }
        return false;
    }

    @Override
    public void mouseReleased(int n, int n2, int n3, int n4, int n5, int n6) {
    }

    public ServerData getServer() {
        return this.server;
    }

    @Override
    public void setSelected(int index, int x, int y) { }

}
