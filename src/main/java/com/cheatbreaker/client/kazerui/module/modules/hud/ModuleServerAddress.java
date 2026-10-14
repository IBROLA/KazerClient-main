package com.cheatbreaker.client.kazerui.module.modules.hud;

import com.cheatbreaker.client.kazerui.feature.HudAnchor;
import com.cheatbreaker.client.kazerui.module.Module;
import com.cheatbreaker.client.kazerui.module.Page;
import com.cheatbreaker.client.kazerui.module.hud.Draw;
import com.cheatbreaker.client.kazerui.module.hud.TextHud;
import com.cheatbreaker.client.kazerui.module.setting.BoolSetting;
import com.google.common.base.Charsets;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufInputStream;
import io.netty.buffer.Unpooled;
import io.netty.handler.codec.base64.Base64;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.Optional;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.renderer.texture.TextureUtil;
import net.minecraft.util.ResourceLocation;

import com.cheatbreaker.client.kazerui.event.UiTick;

public final class ModuleServerAddress extends Module {
    private static final ResourceLocation UNKNOWN_SERVER = new ResourceLocation("minecraft", "textures/misc/unknown_server.png");
    private final BoolSetting serverIcon = bool("serverIcon", true);

    private ResourceLocation iconTexture;

    private BufferedImage iconImage;
    private ServerData lastServer;

    public ModuleServerAddress() {
        super("SERVER_ADDRESS", false);
        hud(new Hud());
    }

    @Override protected void onEnable() { loadIcon(); }

    
    public void tick(UiTick.ClientTickEvent event) {
        if (event.phase != UiTick.Phase.END) return;
        ServerData current=mc().theWorld==null?null:mc().getCurrentServerData();
        if(current!=lastServer) {
            lastServer=current;
            releaseIcon();
            if(current!=null && isEnabled())loadIcon();
        }
        if (!isEnabled()) { if(iconTexture!=null)releaseIcon(); return; }
        if (iconImage == null) return;
        if (iconTexture != null) mc().getTextureManager().deleteTexture(iconTexture);
        iconTexture = mc().getTextureManager().getDynamicTextureLocation("server-icon-thumbnail", new DynamicTexture(iconImage));
        iconImage = null;
    }

    
    private void releaseIcon() {
        iconImage=null;
        if (iconTexture != null) {
            mc().getTextureManager().deleteTexture(iconTexture);
            iconTexture = null;
        }
    }

    private void loadIcon() {
        ServerData data = mc().getCurrentServerData();
        if (data == null || data.getBase64EncodedIconData() == null || data.getBase64EncodedIconData().isEmpty()) {
            iconTexture = null;
        } else {
            decodeIcon(data.getBase64EncodedIconData()).ifPresent(image -> iconImage = image);
        }
    }

    private static Optional<BufferedImage> decodeIcon(String base64) {
        if(base64==null || base64.length()>262144)return Optional.empty();
        ByteBuf encoded = Unpooled.copiedBuffer(base64, Charsets.UTF_8);
        ByteBuf decoded = null;
        try {
            decoded=Base64.decode(encoded);
            BufferedImage image=javax.imageio.ImageIO.read(new ByteBufInputStream(decoded));
            return image!=null && image.getWidth()==64 && image.getHeight()==64?Optional.of(image):Optional.empty();
        } catch (IOException | IllegalArgumentException e) {
            return Optional.empty();
        } finally {
            encoded.release();
            if(decoded!=null)decoded.release();
        }
    }

    @Override protected void layout(Page page) {
        page.section("settings", s -> s.add(serverIcon));
    }

    private final class Hud extends TextHud {
        private String line;
        private String shownLine;
        private boolean brackets;

        Hud() { super(ModuleServerAddress.this, 0, 0, HudAnchor.TOP_LEFT, sizes(10, 18, 22, 40, 56, 62)); }

        @Override protected Boolean staticWidthFor(String text) { return Boolean.FALSE; }

        private String shown() { return shownLine; }

        @Override protected String text(boolean preview) {
            try {
                ServerData data = mc().getCurrentServerData();

                return data == null ? (preview ? "kazer.cn" : "") : data.serverIP;
            } catch (NullPointerException e) {
                return preview ? "kazer.cn" : "";
            }
        }

        @Override public boolean visible(boolean preview) {
            String next=text(preview);boolean nextBrackets=hasBrackets();
            if(!java.util.Objects.equals(line,next) || brackets!=nextBrackets) {
                line=next;brackets=nextBrackets;shownLine=brackets?"["+line+"]":line;
            }
            if (line == null || line.isEmpty()) return false;
            size(Draw.width(shown()) + 8.0f + (serverIcon.on() ? backgroundHeight.intValue() : 0), backgroundHeight.intValue());
            return true;
        }

        @Override public void render(boolean preview) {
            if (line == null || line.isEmpty()) return;
            boolean bg = hasBackground(), withBrackets = hasBrackets();
            float textWidth = Draw.width(shown());
            float w = width(), h = height();
            float x = 0.0f, y = 0.0f;
            boolean icon = serverIcon.on();
            if (icon) {
                x += h;
                w -= h;
            }
            if (bg) {
                float offset = icon ? h : 0.0f;
                Draw.fill(backgroundColor, x - offset, y, w + offset, h);
                if (border.on()) Draw.border(borderColor, x - offset, y, w + offset, h, borderThickness.value());
            }
            if (icon) {
                Draw.texture(iconTexture == null ? UNKNOWN_SERVER : iconTexture, (int)(x - h), (int)y, (int)h, (int)h, 0xFFFFFFFF);
            }
            float textY = y + (h / 1.88f - Draw.fontHeight() / 2.0f + 0.5f);
            drawText(line, align(x, w, textWidth, bg), textY, withBrackets, textShadow.on());
        }
    }
}
