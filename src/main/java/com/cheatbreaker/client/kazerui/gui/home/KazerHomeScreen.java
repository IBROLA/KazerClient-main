package com.cheatbreaker.client.kazerui.gui.home;

import com.cheatbreaker.client.kazerui.KazerUiBrand;
import com.cheatbreaker.client.kazerui.feature.FeatureManager;
import com.cheatbreaker.client.kazerui.gui.KazerMovementScreen;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.minecraft.MinecraftProfileTexture;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import javax.imageio.ImageIO;
import java.util.*;
import net.minecraft.client.audio.PositionedSoundRecord;
import net.minecraft.client.gui.*;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.WorldRenderer;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.client.resources.SkinManager;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.util.MathHelper;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.input.Mouse;
import org.lwjgl.input.Keyboard;
import org.lwjgl.opengl.GL11;
import org.lwjgl.util.glu.Project;

public final class KazerHomeScreen extends GuiScreen {
    private static final int SPACE_1 = 0xFF13141A, SPACE_2 = 0xFF1A1B21, SPACE_3 = 0xFF22232C, SPACE_5 = 0xFF2F313B, SPACE_11 = 0xFFB1B3C0, SPACE_12 = 0xFFEDEEF3;

    private static final int LEGACY_TEXT = 0xFFBEC3BD;
    private static final int FOOTER = 0x5CE3E7FD;
    private static final int TOOLTIP_BG = 0xFF0C0D12, TOOLTIP_SUB = 0xFF797B87, SHADOW = 0x420A0A0A;
    private static final int GHOST = 0x14CACBF5;
    private static final float FACE_ATLAS_HEIGHT = 64f;
    private static final ResourceLocation LOGO = home("logo/kazer.png");
    private static final ResourceLocation ICON_SINGLE = home("home/singleplayer.png"), ICON_MULTI = home("home/multiplayer.png"),
        ICON_STATUS = home("home/account-status.png"), ICON_CHEVRON = home("home/account-chevron.png"),
        ICON_BRUSH = home("home/brush.png"), ICON_CLOSE = home("home/close.png"),
        NAV_LOGO = LOGO, NAV_COG = home("home/nav-cog.png"),
        TYPE_KAZER = home("home/tooltip-kazer-type-icon.png"), TYPE_VANILLA = home("home/tooltip-vanilla-type-icon.png"),
        TYPE_EXTERNAL = home("home/tooltip-external-type-icon.png");

    private static int panoramaTimer;

    private static ResourceLocation home(String path) { return new ResourceLocation(KazerUiBrand.MOD_ID, "ui/" + path); }

    private final FeatureManager features;
    private final KeyBinding menuKey;
    private final HomeGfx gfx = new HomeGfx();
    private final HomeThemes themes;
    private final ThemePicker picker;
    private final Map<String, Fade> fades = new HashMap<String, Fade>();
    private final Star[] stars = new Star[8];
    private final long opened = System.currentTimeMillis();
    // One shared panorama buffer: parent screens can draw behind children, but
    // repeatedly constructing a home screen must not register unbounded textures.
    private static DynamicTexture viewportTexture;
    private static ResourceLocation backgroundTexture;
    private ResourceLocation face;
    private float cssW, cssH, mouseCssX = -1, mouseCssY = -1;
    private final List<Hit> hits = new ArrayList<Hit>();
    private final GuiButton singleButton=new GuiButton(1,0,0,"Singleplayer"), multiButton=new GuiButton(2,0,0,"Multiplayer"),
            optionsButton=new GuiButton(3,0,0,98,20,"Options..."), quitButton=new GuiButton(4,0,0,98,20,"Quit Game");

    private boolean accountOpen;
    private com.google.gson.JsonArray savedAccounts = new com.google.gson.JsonArray();
    private boolean accountBusy;
    private String accountStatus = "Switch Account";
    private int accountScroll;
    private long accountClosedAt;
    private final Fade accountFade = new Fade(150);
    private float popX, popY, popW, popH, accountW;


    private static final class Hit {
        final float x, y, w, h; final Runnable action;
        Hit(float x, float y, float w, float h, Runnable action) { this.x = x; this.y = y; this.w = w; this.h = h; this.action = action; }
    }

    private static final class Star {
        final float duration = (float)(Math.random() * 6 + 3), delay = (float)(Math.random() / 2);
        float opacity(float seconds) {
            float t = seconds - delay;
            if (t <= 0) return 1;
            float cycle = (t % duration) / duration;
            float segment = cycle < 0.5f ? cycle * 2 : (cycle - 0.5f) * 2;
            float eased = cubicBezier(0.42f, 0, 0.58f, 1, segment);
            return cycle < 0.5f ? 1 + (0.15f - 1) * eased : 0.15f + (1 - 0.15f) * eased;
        }
    }

    private static final class Fade {
        boolean on; float from; long start; final float ms;
        Fade() { this(200); }
        Fade(float ms) { this.ms = ms; }
        float value(boolean target) {
            long now = System.currentTimeMillis();
            float current = sample(now);
            if (target != on) { from = current; on = target; start = now; current = sample(now); }
            return current;
        }
        private float sample(long now) {
            float t = Math.min(1, (now - start) / ms);
            float eased = cubicBezier(0.25f, 0.1f, 0.25f, 1, t);
            return from + ((on ? 1 : 0) - from) * eased;
        }
    }

    public KazerHomeScreen(FeatureManager features, KeyBinding menuKey) {
        this.features = features; this.menuKey = menuKey;
        this.themes = new HomeThemes(features);
        this.picker = new ThemePicker(gfx, themes);
        for (int i = 0; i < stars.length; i++) stars[i] = new Star();
    }

    @Override public void initGui() {
        com.cheatbreaker.client.ui.mainmenu.KazerAccountsGui.applyPendingSession();
        if (viewportTexture == null) {
            viewportTexture = new DynamicTexture(256, 256);
            backgroundTexture = mc.getTextureManager().getDynamicTextureLocation("background", viewportTexture);
        }
        if (face == null || !mc.getSession().getPlayerID().equals(faceId)) loadFace();
    }
    private String faceId = "";
    public ResourceLocation accountSkin() { return face; }
    public float accountAtlasHeight() { return FACE_ATLAS_HEIGHT; }
    public float accountUiScale() { return 2f*gfx.dsf/new ScaledResolution(mc).getScaleFactor(); }

    private void loadFace() {
        final String sessionId = mc.getSession().getPlayerID();
        faceId = sessionId;
        GameProfile profile = mc.getSession().getProfile();
        face = DefaultPlayerSkin.getDefaultSkin(profile.getId() == null ? UUID.randomUUID() : profile.getId());
        if (profile.getId() == null) return;
        mc.getSkinManager().loadProfileTextures(profile, new SkinManager.SkinAvailableCallback() {
            @Override public void skinAvailable(MinecraftProfileTexture.Type type, ResourceLocation location, MinecraftProfileTexture texture) {
                if (type == MinecraftProfileTexture.Type.SKIN && sessionId.equals(faceId)) face = location;
            }
        }, true);
    }

    @Override public void updateScreen() { ++panoramaTimer; }

    public static void tickPanorama() { ++panoramaTimer; }

    public static void drawBehind(GuiScreen background, GuiScreen screen, float partialTicks) {
        if (background instanceof KazerHomeScreen) {
            KazerHomeScreen home = (KazerHomeScreen)background;
            home.width = screen.width; home.height = screen.height;
            if (home.viewportTexture != null) {
                cn.kazer.client189.compat.LegacyGL.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS);
                try {
                    HomeGfx.resyncTextureState();
                    GlStateManager.disableAlpha();
                    home.renderSkybox(partialTicks);
                } finally { cn.kazer.client189.compat.LegacyGL.glPopAttrib(); }
                return;
            }
        }
        screen.drawBackground(0);
    }

    @Override public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        cn.kazer.client189.compat.LegacyGL.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS);
        try {
        HomeGfx.resyncTextureState();
        GlStateManager.disableAlpha();
        renderSkybox(partialTicks);
        GlStateManager.enableAlpha();

        int scale = new ScaledResolution(mc).getScaleFactor();
        gfx.dsf = Math.max(1f, scale / 2f);
        cssW = mc.displayWidth / gfx.dsf; cssH = mc.displayHeight / gfx.dsf;
        mouseCssX = Mouse.getX() / gfx.dsf; mouseCssY = (mc.displayHeight - Mouse.getY() - 1) / gfx.dsf;
        hits.clear();
        layoutAccountPopover();

        GlStateManager.pushMatrix();
        float k = gfx.dsf / scale;
        GlStateManager.scale(k, k, 1);
        GlStateManager.disableDepth();

        GlStateManager.disableAlpha();
        drawAccount();
        drawTopButton(cssW - 115, ICON_BRUSH, "brush", new Runnable() { public void run() { picker.show(); } });
        drawTopButton(cssW - 63, ICON_CLOSE, "close", new Runnable() { public void run() { mc.shutdown(); } });
        drawCentre();
        drawNavigation();
        drawFooter(2);
        drawAccountPopover();
        picker.warm(cssW, cssH);
        picker.draw(cssW, cssH, mouseCssX, mouseCssY);
        GlStateManager.enableAlpha();
        GlStateManager.enableDepth();
        GlStateManager.popMatrix();
        } finally { cn.kazer.client189.compat.LegacyGL.glPopAttrib(); }
    }

    private boolean popoverMounted() { return accountOpen || System.currentTimeMillis() - accountClosedAt < 200; }
    private boolean inPopover(float mx, float my) { return popoverMounted() && mx >= popX && my >= popY && mx < popX + popW && my < popY + popH; }
    private boolean over(float x, float y, float w, float h) { return !picker.blocksHome() && !inPopover(mouseCssX, mouseCssY) && overRaw(x, y, w, h); }
    private boolean overRaw(float x, float y, float w, float h) { return mouseCssX >= x && mouseCssY >= y && mouseCssX < x + w && mouseCssY < y + h; }
    private float fade(String key, boolean on) { Fade f = fades.get(key); if (f == null) { f = new Fade(); fades.put(key, f); } return f.value(on); }

    private void drawAccount() {
        String name = mc.getSession().getUsername();
        boolean legacy = false; // Account controls always retain the Modern dark theme.
        float nameW = legacy ? gfx.legacyTextWidth(name, 14) : gfx.textWidth(name, false, 16);
        float w = accountW = Math.max(144, nameW + 96), x = 20, y = 20;
        boolean hover = over(x, y, w, 40);
        float t = fade("account", hover);
        if (legacy) legacyFrame(x, y, w, 40, 8, t);
        else gfx.draw(gfx.box(w, 40, 8), x, y, lerp(SPACE_1, SPACE_2, t));
        int fg = legacy ? lerp(LEGACY_TEXT, 0xFFFFFFFF, t) : lerp(SPACE_11, SPACE_12, t);

        GlStateManager.color(1, 1, 1, 1);
        gfx.roundedRegion(face, 32, 32, 16, 16, 5, 8 / 64f, 8 / FACE_ATLAS_HEIGHT, 16 / 64f, 16 / FACE_ATLAS_HEIGHT);
        gfx.roundedRegion(face, 32, 32, 16, 16, 5, 40 / 64f, 8 / FACE_ATLAS_HEIGHT, 48 / 64f, 16 / FACE_ATLAS_HEIGHT);
        if (legacy) {
            HomeGfx.Tex text = gfx.legacyText(name, 14);
            float ty = y + (40 - text.height) / 2;
            gfx.draw(text, 53, ty + 1, 0x20000000);
            gfx.draw(text, 52, ty, fg);
        } else {
            gfx.draw(LOGO, 53, 31, 18, 18, 0xFF000000 | com.cheatbreaker.client.CheatBreaker.getInstance().getPresenceClient().accountBadgeColor());
            gfx.draw(gfx.text(name, false, 16), 77, 29.5f, fg);
        }
        gfx.draw(ICON_CHEVRON, 85 + nameW, 32, 16, 16, fg);
    }

    private void layoutAccountPopover() {
        float nameW = gfx.textWidth(mc.getSession().getUsername(), false, 18);

        popW = Math.max(240, nameW + 80); popH = 92 + Math.min(4, savedAccounts.size()) * 36; popX = 20;
        popY = 68 - 10 * (1 - accountFade.value(accountOpen));
    }

    private void drawAccountPopover() {
        if (!popoverMounted()) return;
        float t = accountFade.value(accountOpen), x = popX, y = popY, w = popW, h = popH;
        float pad = HomeGfx.shadowPad(24);
        gfx.draw(gfx.shadow(w, h, 8, 8, 24, 0), x - pad, y - pad, withAlpha(0x4D0A0A0A, t));
        gfx.draw(gfx.shadow(w, h, 8, 2, 8, 0), x - HomeGfx.shadowPad(8), y - HomeGfx.shadowPad(8), withAlpha(0x260A0A0A, t));
        gfx.draw(gfx.box(w, h, 8), x, y, withAlpha(SPACE_1, t));
        float cx = x + 16, cy = y + 16;

        gfx.draw(gfx.text(accountBusy ? "Switching account..." : accountStatus, true, 14), cx, cy, withAlpha(SPACE_11, t));
        for (int row=0;row<4 && row+accountScroll<savedAccounts.size();row++) {
            com.google.gson.JsonObject account=savedAccounts.get(row+accountScroll).getAsJsonObject();
            String name=account.get("username").getAsString();float ry=cy+26+row*36;
            boolean current=account.get("id").getAsString().equals(mc.getSession().getPlayerID());
            gfx.draw(gfx.box(w-32,32,7),cx,ry,withAlpha(overRaw(cx,ry,w-32,32)?SPACE_5:SPACE_3,t));
            gfx.draw(gfx.text(name,false,16),cx+12,ry+6,withAlpha(needsVerification(account)?0xFFFF727D:current?0xFF16C7ED:SPACE_12,t));
            if(current)gfx.draw(gfx.text("Active",false,12),cx+w-82,ry+8,withAlpha(SPACE_11,t));
        }
        if(accountBusy) {
            GlStateManager.disableTexture2D();GlStateManager.color(1,1,1,t);GL11.glLineWidth(2);
            GL11.glBegin(GL11.GL_LINE_STRIP);
            double phase=System.nanoTime()/1.0e9*5;
            for(int i=0;i<=20;i++){double a=phase+i*Math.PI/16;GL11.glVertex2d(x+w-24+Math.cos(a)*6,cy+8+Math.sin(a)*6);}
            GL11.glEnd();GlStateManager.enableTexture2D();GlStateManager.color(1,1,1,1);
        }
        float buttonY=y+h-48;
        gfx.draw(gfx.box(w-32,32,7),cx,buttonY,withAlpha(overRaw(cx,buttonY,w-32,32)?0xFF17445A:0xFF123142,t));
        gfx.draw(gfx.text("Add Account",true,14),cx+12,buttonY+7,withAlpha(0xFF16C7ED,t));
    }

    private void toggleAccountPopover() {
        if (accountOpen) closeAccountPopover(); else {
            accountOpen=true;accountStatus="Switch Account";
            com.cheatbreaker.client.ui.mainmenu.KazerAccountBridge.request("/accounts", result -> {
                if(result.has("accounts"))savedAccounts=result.getAsJsonArray("accounts");
            }, error -> accountStatus="Accounts unavailable");
        }
    }
    private void requestAccount(String route) {
        if(accountBusy)return;accountBusy=true;
        com.cheatbreaker.client.ui.mainmenu.KazerAccountBridge.request(route,result -> {
            accountBusy=false;
            com.cheatbreaker.client.ui.mainmenu.KazerAccountBridge.acceptSession(result);
            closeAccountPopover();loadFace();
        }, error -> {
            accountBusy=false;accountStatus="Sign-in cancelled or failed";accountOpen=true;
            com.cheatbreaker.client.ui.mainmenu.KazerAccountBridge.request("/accounts",result -> {
                if(result.has("accounts"))savedAccounts=result.getAsJsonArray("accounts");
            }, ignored -> {});
        });
    }
    private static boolean needsVerification(com.google.gson.JsonObject account) {
        return account.has("needsVerification") && account.get("needsVerification").getAsBoolean();
    }
    private void closeAccountPopover() { if (accountOpen) { accountOpen = false; accountClosedAt = System.currentTimeMillis(); } }

    private static int withAlpha(int argb, float t) { return Math.round((argb >>> 24) * t) << 24 | (argb & 0xFFFFFF); }

    private void drawTopButton(float x, ResourceLocation icon, String key, Runnable action) {
        float t = fade(key, over(x, 20, 42, 40));
        gfx.draw(gfx.box(42, 40, 8), x, 20, lerp(SPACE_1, SPACE_2, t));
        gfx.draw(icon, x + 12, 31, 18, 18, lerp(SPACE_11, SPACE_12, t));
        hits.add(new Hit(x, 20, 42, 40, action));
    }

    private void drawCentre() {
        float top = cssH / 2 - 44, left = cssW / 2 - 200;
        float logoX = cssW / 2 - 62, logoY = top - 152;
        gfx.draw(LOGO, logoX, logoY, 124, 124, 0xFFFFFFFF);
        Runnable single = new Runnable() { public void run() { mc.displayGuiScreen(new GuiSelectWorld(KazerHomeScreen.this)); } };
        Runnable multi = new Runnable() { public void run() { mc.displayGuiScreen(new GuiMultiplayer(KazerHomeScreen.this)); } };
        Runnable options = () -> mc.displayGuiScreen(new GuiOptions(KazerHomeScreen.this, mc.gameSettings));
        Runnable quit = () -> mc.shutdown();
        if (picker.legacyButtons()) {
            nativeButton(singleButton,left,top,single);
            nativeButton(multiButton,left,top+48,multi);
            nativeButton(optionsButton,left,top+112,options);
            nativeButton(quitButton,left+204,top+112,quit);
        } else {
            mainButton(left, top, "Singleplayer", ICON_SINGLE, single);
            mainButton(left, top + 48, "Multiplayer", ICON_MULTI, multi);
        }
    }

    private void nativeButton(GuiButton button,float x,float y,Runnable action) {
        button.xPosition=(int)(x/2);button.yPosition=(int)(y/2);
        GlStateManager.pushMatrix();
        try {GlStateManager.scale(2,2,1);button.drawButton(mc,(int)(mouseCssX/2),(int)(mouseCssY/2));}
        finally {GlStateManager.popMatrix();}
        hits.add(new Hit(x,y,button.width*2,button.height*2,action));
    }

    private void legacyButton(float x, float y, String label, Runnable action) {
        float w = 400, h = 34, r = 8;
        float t = fade("legacy:" + label, over(x, y, w, h));
        legacyFrame(x, y, w, h, r, t);
        String spaced = label.replace("", " ").trim();
        HomeGfx.Tex text = gfx.legacyText(spaced, 14);
        float tx = x + (w - gfx.legacyTextWidth(spaced, 14)) / 2, ty = y + (h - text.height) / 2;
        gfx.draw(text, tx + 1, ty + 1, 0x20000000);
        gfx.draw(text, tx, ty, LEGACY_TEXT);
        hits.add(new Hit(x, y, w, h, action));
    }

    private void legacyFrame(float x, float y, float w, float h, float r, float hover) {
        gfx.draw(gfx.box(w, h, r), x, y, lerp(0x20FFFFFF, 0x45FFFFFF, hover));
        gfx.draw(gfx.ring(w, h, r, 0, 1), x, y, 0x40252525);
        gfx.draw(gfx.ring(w, h, r, 1, 1), x, y, 0x20FFFFFF);
    }

    private void mainButton(float x, float y, String label, ResourceLocation icon, Runnable action) {
        float t = fade(label, over(x, y, 400, 40));
        gfx.draw(gfx.box(400, 40, 8), x, y, lerp(SPACE_1, SPACE_2, t));
        float textW = gfx.textWidth(label, false, 18), contentX = x + 200 - (16 + 8 + textW) / 2;
        gfx.draw(icon, contentX, y + 12, 16, 16, SPACE_11);
        gfx.draw(gfx.text(label, false, 18), contentX + 24, y + 8, SPACE_12);
        hits.add(new Hit(x, y, 400, 40, action));
    }

    private void drawFooter(int navCount) {
        float navW = navCount * 38 + (navCount - 1) * 8, column = (cssW - 30 - navW - 16) / 2;
        List<String> left = wrap(com.cheatbreaker.client.util.KazerBuildInfo.windowTitle(KazerUiBrand.MINECRAFT), column);
        for (int i = 0; i < left.size(); i++)
            gfx.draw(gfx.text(left.get(i), false, 16), 15, cssH - 20 - 21 * (left.size() - i), FOOTER);
        List<String> right = wrap("Not affiliated with Mojang or Microsoft. Do not distribute!", column);
        for (int i = 0; i < right.size(); i++) {
            String line = right.get(i);
            gfx.draw(gfx.text(line, false, 16), cssW - 15 - gfx.textWidth(line, false, 16), cssH - 20 - 21 * (right.size() - i), FOOTER);
        }
    }

    private List<String> wrap(String text, float width) {
        List<String> lines = new ArrayList<String>();
        String line = "";
        for (String word : text.split(" ")) {
            String candidate = line.isEmpty() ? word : line + " " + word;
            if (gfx.textWidth(candidate, false, 16) <= width || line.isEmpty() && width <= 0) { line = candidate; continue; }
            if (!line.isEmpty()) lines.add(line);
            line = word;
            while (width > 0 && gfx.textWidth(line, false, 16) > width && line.length() > 1) {
                int cut = line.length() - 1;
                while (cut > 1 && gfx.textWidth(line.substring(0, cut), false, 16) > width) cut--;
                lines.add(line.substring(0, cut)); line = line.substring(cut);
            }
        }
        if (!line.isEmpty()) lines.add(line);
        return lines;
    }

    private void drawNavigation() {
        String[][] items = {{"Kazer Settings", "Kazer"}, {"Minecraft Settings", "Minecraft"}};
        ResourceLocation[] icons = {NAV_LOGO, NAV_COG};
        ResourceLocation[] types = {TYPE_KAZER, TYPE_VANILLA};
        Runnable[] actions = {
            new Runnable() { public void run() { mc.displayGuiScreen(new KazerMovementScreen(features, menuKey, KazerHomeScreen.this)); } },
            new Runnable() { public void run() { mc.displayGuiScreen(new GuiOptions(KazerHomeScreen.this, mc.gameSettings)); } }};
        float total = items.length * 38 + (items.length - 1) * 8, x0 = cssW / 2 - total / 2, y = cssH - 58;
        int tooltip = -1;
        for (int i = 0; i < items.length; i++) {
            float x = x0 + i * 46;
            boolean hover = over(x, y, 38, 38);
            if (hover) tooltip = i;
            float t = fade("nav" + i, hover);
            gfx.draw(gfx.box(38, 38, 8), x, y, lerp(SPACE_1, SPACE_2, t));
            gfx.draw(icons[i], x + 11, y + 11, 16, 16, 0xA6FFFFFF);
            hits.add(new Hit(x, y, 38, 38, actions[i]));
        }
        if (tooltip >= 0) drawTooltip(x0 + tooltip * 46 + 19, y - 10, items[tooltip][0], items[tooltip][1], types[tooltip]);
    }

    private void drawTooltip(float centreX, float bottom, String title, String type, ResourceLocation typeIcon) {
        float titleW = gfx.textWidth(title, true, 16), rowW = 14 + 4 + gfx.textWidth(type, false, 14);
        float w = Math.max(titleW, rowW) + 24, h = 58, x = centreX - w / 2, y = bottom - h;
        float pad = HomeGfx.shadowPad(15.9f);
        gfx.draw(gfx.shadow(w, h, 8, 7, 15.9f, -5), x - pad, y - pad, SHADOW);
        gfx.draw(gfx.box(w, h, 8), x, y, TOOLTIP_BG);
        gfx.draw(gfx.text(title, true, 16), x + 12, y + 9, SPACE_11);
        gfx.draw(typeIcon, x + 12, y + 34, 14, 14, TOOLTIP_SUB);
        gfx.draw(gfx.text(type, false, 14), x + 30, y + 32, TOOLTIP_SUB);
    }

    @Override protected void mouseClicked(int mouseX, int mouseY, int button) throws IOException {
        if (button != 0) return;
        if (picker.blocksHome()) { picker.mouseClicked(mouseCssX, mouseCssY); return; }

        if (inPopover(mouseCssX, mouseCssY)) {
            if (accountOpen && overRaw(popX+16,popY+popH-48,popW-32,32)) {
                closeAccountPopover();
                requestAccount("/login");
            }
            if(accountOpen && !accountBusy) for(int row=0;row<4 && row+accountScroll<savedAccounts.size();row++)
                if(overRaw(popX+16,popY+42+row*36,popW-32,32)) {
                    com.google.gson.JsonObject account=savedAccounts.get(row+accountScroll).getAsJsonObject();
                    String id=account.get("id").getAsString();
                    if(needsVerification(account))requestAccount("/login");
                    else if(!id.equals(mc.getSession().getPlayerID()))requestAccount("/select/"+id);
                    return;
                }
            return;
        }
        if (overRaw(20, 20, accountW, 40)) { toggleAccountPopover(); return; }
        closeAccountPopover();
        for (Hit hit : hits) {
            if (mouseCssX >= hit.x && mouseCssY >= hit.y && mouseCssX < hit.x + hit.w && mouseCssY < hit.y + hit.h) {
                mc.getSoundHandler().playSound(PositionedSoundRecord.create(new ResourceLocation("gui.button.press"), 1.0F));
                hit.action.run();
                return;
            }
        }
    }

    @Override protected void keyTyped(char typedChar, int keyCode) {
        if(!picker.blocksHome() && keyCode==menuKey.getKeyCode() && !Keyboard.isRepeatEvent()) {
            mc.displayGuiScreen(com.cheatbreaker.client.CheatBreaker.getInstance().ownedUi.hudEditor(this));return;
        }
        picker.keyTyped(typedChar, keyCode);
    }
    @Override public void handleMouseInput() throws IOException {
        super.handleMouseInput();
        int wheel = Mouse.getEventDWheel();
        if(accountOpen && wheel!=0) {accountScroll=Math.max(0,Math.min(Math.max(0,savedAccounts.size()-4),accountScroll+(wheel<0?1:-1)));return;}
        if (wheel != 0) picker.mouseWheel(Integer.signum(wheel));
    }

    @Override public void onGuiClosed() { closeAccountPopover(); }
    public void releaseResources() {
        gfx.release();picker.invalidateWarm();
        if(backgroundTexture!=null)mc.getTextureManager().deleteTexture(backgroundTexture);
        viewportTexture=null;backgroundTexture=null;
    }
    @Override public boolean doesGuiPauseGame() { return false; }

    private void drawPanorama(float partialTicks) {
        HomeThemes.Theme theme = themes.current();
        Tessellator tessellator = Tessellator.getInstance();
        WorldRenderer worldrenderer = tessellator.getWorldRenderer();
        GlStateManager.matrixMode(5889);
        GlStateManager.pushMatrix();
        GlStateManager.loadIdentity();
        Project.gluPerspective(120.0F, 1.0F, 0.05F, 10.0F);
        GlStateManager.matrixMode(5888);
        GlStateManager.pushMatrix();
        GlStateManager.loadIdentity();
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        GlStateManager.rotate(180.0F, 1.0F, 0.0F, 0.0F);
        GlStateManager.rotate(90.0F, 0.0F, 0.0F, 1.0F);
        GlStateManager.enableBlend();
        GlStateManager.disableAlpha();
        GlStateManager.disableCull();
        GlStateManager.depthMask(false);
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
        int i = 8;
        for (int j = 0; j < i * i; ++j) {
            GlStateManager.pushMatrix();
            float f = ((float)(j % i) / (float)i - 0.5F) / 64.0F;
            float f1 = ((float)(j / i) / (float)i - 0.5F) / 64.0F;
            GlStateManager.translate(f, f1, 0.0F);
            GlStateManager.rotate(MathHelper.sin(((float)panoramaTimer + partialTicks) / 400.0F) * 25.0F + 20.0F, 1.0F, 0.0F, 0.0F);
            GlStateManager.rotate(-((float)panoramaTimer + partialTicks) * 0.1F, 0.0F, 1.0F, 0.0F);
            for (int k = 0; k < 6; ++k) {
                GlStateManager.pushMatrix();
                if (k == 1) GlStateManager.rotate(90.0F, 0.0F, 1.0F, 0.0F);
                if (k == 2) GlStateManager.rotate(180.0F, 0.0F, 1.0F, 0.0F);
                if (k == 3) GlStateManager.rotate(-90.0F, 0.0F, 1.0F, 0.0F);
                if (k == 4) GlStateManager.rotate(90.0F, 1.0F, 0.0F, 0.0F);
                if (k == 5) GlStateManager.rotate(-90.0F, 1.0F, 0.0F, 0.0F);
                mc.getTextureManager().bindTexture(theme.panorama(k));
                worldrenderer.begin(7, DefaultVertexFormats.POSITION_TEX_COLOR);
                int l = 255 / (j + 1);
                worldrenderer.pos(-1.0D, -1.0D, 1.0D).tex(0.0D, 0.0D).color(255, 255, 255, l).endVertex();
                worldrenderer.pos(1.0D, -1.0D, 1.0D).tex(1.0D, 0.0D).color(255, 255, 255, l).endVertex();
                worldrenderer.pos(1.0D, 1.0D, 1.0D).tex(1.0D, 1.0D).color(255, 255, 255, l).endVertex();
                worldrenderer.pos(-1.0D, 1.0D, 1.0D).tex(0.0D, 1.0D).color(255, 255, 255, l).endVertex();
                tessellator.draw();
                GlStateManager.popMatrix();
            }
            GlStateManager.popMatrix();
            GlStateManager.colorMask(true, true, true, false);
        }
        worldrenderer.setTranslation(0.0D, 0.0D, 0.0D);
        GlStateManager.colorMask(true, true, true, true);
        GlStateManager.matrixMode(5889);
        GlStateManager.popMatrix();
        GlStateManager.matrixMode(5888);
        GlStateManager.popMatrix();
        GlStateManager.depthMask(true);
        GlStateManager.enableCull();
        GlStateManager.enableDepth();
    }

    private void rotateAndBlurSkybox() {
        mc.getTextureManager().bindTexture(backgroundTexture);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR);
        GL11.glCopyTexSubImage2D(GL11.GL_TEXTURE_2D, 0, 0, 0, 0, 0, 256, 256);
        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
        GlStateManager.colorMask(true, true, true, false);
        Tessellator tessellator = Tessellator.getInstance();
        WorldRenderer worldrenderer = tessellator.getWorldRenderer();
        worldrenderer.begin(7, DefaultVertexFormats.POSITION_TEX_COLOR);
        GlStateManager.disableAlpha();
        int i = 3;
        for (int j = 0; j < i; ++j) {
            float f = 1.0F / (float)(j + 1);
            int k = width, l = height;
            float f1 = (float)(j - i / 2) / 256.0F;
            worldrenderer.pos((double)k, (double)l, (double)zLevel).tex((double)(0.0F + f1), 1.0D).color(1.0F, 1.0F, 1.0F, f).endVertex();
            worldrenderer.pos((double)k, 0.0D, (double)zLevel).tex((double)(1.0F + f1), 1.0D).color(1.0F, 1.0F, 1.0F, f).endVertex();
            worldrenderer.pos(0.0D, 0.0D, (double)zLevel).tex((double)(1.0F + f1), 0.0D).color(1.0F, 1.0F, 1.0F, f).endVertex();
            worldrenderer.pos(0.0D, (double)l, (double)zLevel).tex((double)(0.0F + f1), 0.0D).color(1.0F, 1.0F, 1.0F, f).endVertex();
        }
        tessellator.draw();
        GlStateManager.enableAlpha();
        GlStateManager.colorMask(true, true, true, true);
    }

    private void renderSkybox(float partialTicks) {
        mc.getFramebuffer().unbindFramebuffer();
        GlStateManager.viewport(0, 0, 256, 256);
        drawPanorama(partialTicks);
        for (int pass = 0; pass < 7; pass++) rotateAndBlurSkybox();
        mc.getFramebuffer().bindFramebuffer(true);
        GlStateManager.viewport(0, 0, mc.displayWidth, mc.displayHeight);
        float f = width > height ? 120.0F / (float)width : 120.0F / (float)height;
        float f1 = (float)height * f / 256.0F;
        float f2 = (float)width * f / 256.0F;
        int i = width, j = height;
        Tessellator tessellator = Tessellator.getInstance();
        WorldRenderer worldrenderer = tessellator.getWorldRenderer();
        worldrenderer.begin(7, DefaultVertexFormats.POSITION_TEX_COLOR);
        worldrenderer.pos(0.0D, (double)j, (double)zLevel).tex((double)(0.5F - f1), (double)(0.5F + f2)).color(1.0F, 1.0F, 1.0F, 1.0F).endVertex();
        worldrenderer.pos((double)i, (double)j, (double)zLevel).tex((double)(0.5F - f1), (double)(0.5F - f2)).color(1.0F, 1.0F, 1.0F, 1.0F).endVertex();
        worldrenderer.pos((double)i, 0.0D, (double)zLevel).tex((double)(0.5F + f1), (double)(0.5F - f2)).color(1.0F, 1.0F, 1.0F, 1.0F).endVertex();
        worldrenderer.pos(0.0D, 0.0D, (double)zLevel).tex((double)(0.5F + f1), (double)(0.5F + f2)).color(1.0F, 1.0F, 1.0F, 1.0F).endVertex();
        tessellator.draw();
    }

    static int lerp(int a, int b, float t) {
        int out = 0;
        for (int shift = 0; shift < 32; shift += 8)
            out |= Math.round(((a >>> shift) & 0xFF) * (1 - t) + ((b >>> shift) & 0xFF) * t) << shift;
        return out;
    }

    static float cubicBezier(float x1, float y1, float x2, float y2, float x) {
        if (x <= 0) return 0;
        if (x >= 1) return 1;
        float t = x;
        for (int i = 0; i < 8; i++) {
            float cx = bezier(x1, x2, t) - x, d = bezierSlope(x1, x2, t);
            if (Math.abs(cx) < 1e-5f) break;
            if (Math.abs(d) < 1e-6f) break;
            t -= cx / d;
        }
        if (t < 0 || t > 1 || Math.abs(bezier(x1, x2, t) - x) > 1e-3f) {
            float lo = 0, hi = 1; t = x;
            for (int i = 0; i < 30; i++) { if (bezier(x1, x2, t) < x) lo = t; else hi = t; t = (lo + hi) / 2; }
        }
        return bezier(y1, y2, t);
    }
    private static float bezier(float p1, float p2, float t) { float u = 1 - t; return 3 * u * u * t * p1 + 3 * u * t * t * p2 + t * t * t; }
    private static float bezierSlope(float p1, float p2, float t) { float u = 1 - t; return 3 * u * u * p1 + 6 * u * t * (p2 - p1) + 3 * t * t * (1 - p2); }
}
