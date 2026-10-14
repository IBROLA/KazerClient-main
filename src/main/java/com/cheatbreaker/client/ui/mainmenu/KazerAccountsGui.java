package com.cheatbreaker.client.ui.mainmenu;

import com.cheatbreaker.client.CheatBreaker;
import com.cheatbreaker.client.ui.util.RenderUtil;
import com.cheatbreaker.client.ui.util.KazerUiIcons;
import com.cheatbreaker.client.ui.util.font.CBFontRenderer;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.minecraft.MinecraftProfileTexture;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.Session;
import org.lwjgl.opengl.GL11;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Main-menu-only dropdown. Official sign-in and account sync use the launcher bridge. */
public final class KazerAccountsGui extends GuiScreen {
    public static void applyPendingSession() { KazerAccountBridge.applyPending(); }
    private static final int X=10, Y=34, W=169;
    private final GuiScreen parent;
    private JsonArray accounts = new JsonArray();
    private final Map<String, ResourceLocation> skins = new HashMap<>();
    private boolean busy, managing, settings;
    private String status = "";
    private int scroll;

    public KazerAccountsGui(GuiScreen parent) { this(parent, false); }
    public KazerAccountsGui(GuiScreen parent, boolean settings) { this.parent=parent; this.settings=settings; }
    private float parentScale() {
        if(parent instanceof MainMenu) return ((MainMenu)parent).getScaleFactor();
        if(parent instanceof com.cheatbreaker.client.kazerui.gui.home.KazerHomeScreen)
            return ((com.cheatbreaker.client.kazerui.gui.home.KazerHomeScreen)parent).accountUiScale();
        return CheatBreaker.getInstance().getScaleFactor();
    }
    private ResourceLocation parentSkin() {
        if(parent instanceof MainMenu) return ((MainMenu)parent).accountSkin();
        if(parent instanceof com.cheatbreaker.client.kazerui.gui.home.KazerHomeScreen)
            return ((com.cheatbreaker.client.kazerui.gui.home.KazerHomeScreen)parent).accountSkin();
        return null;
    }
    @Override public void initGui() {
        KazerAccountBridge.applyPending();
        parent.setWorldAndResolution(mc, width, height);
        refresh();
    }
    private void refresh() {
        KazerAccountBridge.request("/accounts", result -> {
            accounts=result.getAsJsonArray("accounts");
            for (int i=0;i<accounts.size();i++) {
                JsonObject account=accounts.get(i).getAsJsonObject();
                String id=account.get("id").getAsString();
                GameProfile profile=new Session(account.get("username").getAsString(),id,"","mojang").getProfile();
                mc.getSkinManager().loadProfileTextures(profile,(type, location, texture) -> {
                    if(type==MinecraftProfileTexture.Type.SKIN) skins.put(id,location);
                },false);
            }
        }, error -> status=error);
    }
    private List<JsonObject> otherAccounts() {
        List<JsonObject> list=new ArrayList<>();
        for(int i=0;i<accounts.size();i++) {
            JsonObject account=accounts.get(i).getAsJsonObject();
            if(managing || !account.get("id").getAsString().equals(mc.getSession().getPlayerID())) list.add(account);
        }
        return list;
    }
    private int visibleRows() {
        return Math.max(1,Math.min(4,(int)(height/parentScale()-Y-154)/22));
    }
    private int footerY() { return Y+141+Math.max(0,Math.min(visibleRows(),otherAccounts().size())-1)*22; }
    private int panelBottom() { return footerY()+28+(status.isEmpty()?0:31); }
    @Override public boolean doesGuiPauseGame() { return false; }
    @Override public void drawScreen(int mouseX,int mouseY,float partialTicks) {
        parent.drawScreen(-100,-100,partialTicks);
        float scale=parentScale();
        mouseX=(int)(mouseX/scale); mouseY=(int)(mouseY/scale);
        cn.kazer.client189.compat.LegacyGL.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS);
        GL11.glPushMatrix();
        try {
            GL11.glScalef(scale,scale,1);
            RenderUtil.lIIIIlIIllIIlIIlIIIlIIllI(X,Y,X+W,panelBottom(),4D,0xFF111218);
            ResourceLocation skin=skins.get(mc.getSession().getPlayerID());
            if(skin==null) skin=parentSkin();
            drawPortrait(skin,X+8,Y+8);
            RenderUtil.lIIIIlIIllIIlIIlIIIlIIllI(new ResourceLocation("client/kazer_logo.png"),X+60,Y+21,9,9);
            CheatBreaker.getInstance().playRegular18px.drawMixedString(mc.getSession().getUsername(),X+71,Y+21,0xFFF2F2F6);
            font().drawMixedString(accounts.size()>0?"Microsoft account":"Current session",X+60,Y+34,0xFFAEAEBB);
            row("Account Settings",X+8,Y+60,W-16,20,mouseX,mouseY,0xFF22232D,0xFFB7B8C7);
            KazerUiIcons.draw(KazerUiIcons.Icon.GEAR,X+47,Y+66,8,0xFFB7B8C7);
            cn.kazer.client189.compat.LegacyGui.drawRect(X+8,Y+87,X+W-8,Y+87.5F,0xFF1B1C23);
            font().drawMixedString(settings?"Account Details":managing?"Manage Accounts":"Other Accounts",X+8,Y+98,0xFFF1F1F5);
            if(settings) {
                font().drawMixedString("Signed in as "+mc.getSession().getUsername(),X+8,Y+116,0xFFB8BAC7);
            } else {
                KazerUiIcons.draw(KazerUiIcons.Icon.PLUS,X+134,Y+98,7,0xFFB7B8C7);
                font().drawMixedString(busy?"...":"Add",X+145,Y+98,0xFFB7B8C7);
                List<JsonObject> list=otherAccounts();
                for(int row=0;row<visibleRows()&&scroll+row<list.size();row++) {
                    JsonObject account=list.get(scroll+row);
                    int ry=Y+114+row*22;
                    if(inside(mouseX,mouseY,X+8,ry,W-16,20))
                        RenderUtil.lIIIIlIIllIIlIIlIIIlIIllI(X+8,ry,X+W-8,ry+20,3D,0xFF22232D);
                    drawHead(skins.get(account.get("id").getAsString()),X+12,ry+6,8);
                    font().drawMixedString(account.get("username").getAsString(),X+22,ry+5,0xFFB7B8C7);
                }
            }
            row(settings||managing?"Back":"Manage Accounts",X+8,footerY(),W-16,20,mouseX,mouseY,0xFF103243,0xFF16C7F4);
            if(!settings&&!managing) KazerUiIcons.draw(KazerUiIcons.Icon.PLUS,X+37,footerY()+6,8,0xFF16C7F4);
            String remaining=status;
            for(int i=0;i<3&&!remaining.isEmpty();i++) {
                int end=remaining.length();
                while(end>1&&font().getMixedStringWidth(remaining.substring(0,end))>W-16) end--;
                font().drawMixedString(remaining.substring(0,end),X+8,footerY()+25+i*9,0xFFB7B8C7);
                remaining=remaining.substring(end);
            }
        } finally { GL11.glPopMatrix(); cn.kazer.client189.compat.LegacyGL.glPopAttrib(); }
    }
    private CBFontRenderer font() { return CheatBreaker.getInstance().playRegular14px; }
    private void row(String label,int x,int y,int w,int h,int mx,int my,int bg,int fg) {
        RenderUtil.lIIIIlIIllIIlIIlIIIlIIllI(x,y,x+w,y+h,4D,inside(mx,my,x,y,w,h)?0xFF292B36:bg);
        font().drawCenteredInRect(label,x,y,w,h,fg);
    }
    private static boolean inside(int mx,int my,int x,int y,int w,int h) { return mx>=x&&mx<x+w&&my>=y&&my<y+h; }
    public static void drawHead(ResourceLocation skin,int x,int y,int size) {
        net.minecraft.client.Minecraft mc=net.minecraft.client.Minecraft.getMinecraft();
        cn.kazer.client189.compat.LegacyGL.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS);
        try {
            mc.getTextureManager().bindTexture(skin==null?new ResourceLocation("textures/entity/steve.png"):skin);
            int min=GL11.glGetTexParameteri(GL11.GL_TEXTURE_2D,GL11.GL_TEXTURE_MIN_FILTER);
            int mag=GL11.glGetTexParameteri(GL11.GL_TEXTURE_2D,GL11.GL_TEXTURE_MAG_FILTER);
            try {
                cn.kazer.client189.compat.LegacyGL.glEnable(GL11.GL_TEXTURE_2D); cn.kazer.client189.compat.LegacyGL.glEnable(GL11.GL_BLEND);
                cn.kazer.client189.compat.LegacyGL.glBlendFunc(GL11.GL_SRC_ALPHA,GL11.GL_ONE_MINUS_SRC_ALPHA);
                cn.kazer.client189.compat.LegacyGL.glColor4f(1,1,1,1);
                GL11.glTexParameteri(GL11.GL_TEXTURE_2D,GL11.GL_TEXTURE_MIN_FILTER,GL11.GL_NEAREST);
                GL11.glTexParameteri(GL11.GL_TEXTURE_2D,GL11.GL_TEXTURE_MAG_FILTER,GL11.GL_NEAREST);
                Gui.drawScaledCustomSizeModalRect(x,y,8,8,8,8,size,size,64,64);
                if(skin!=null) Gui.drawScaledCustomSizeModalRect(x,y,40,8,8,8,size,size,64,64);
            } finally {
                GL11.glTexParameteri(GL11.GL_TEXTURE_2D,GL11.GL_TEXTURE_MIN_FILTER,min);
                GL11.glTexParameteri(GL11.GL_TEXTURE_2D,GL11.GL_TEXTURE_MAG_FILTER,mag);
            }
        } finally { cn.kazer.client189.compat.LegacyGL.glPopAttrib(); }
    }
    private static void drawPortrait(ResourceLocation skin,int x,int y) {
        RenderUtil.lIIIIlIIllIIlIIlIIIlIIllI(x,y,x+44,y+44,44D,0xFF22232B);
        drawHead(skin,x+11,y+5,22);
        net.minecraft.client.Minecraft.getMinecraft().getTextureManager().bindTexture(
                skin==null?new ResourceLocation("textures/entity/steve.png"):skin);
        int mag=GL11.glGetTexParameteri(GL11.GL_TEXTURE_2D,GL11.GL_TEXTURE_MAG_FILTER);
        try {
            cn.kazer.client189.compat.LegacyGL.glEnable(GL11.GL_TEXTURE_2D); cn.kazer.client189.compat.LegacyGL.glEnable(GL11.GL_BLEND); cn.kazer.client189.compat.LegacyGL.glColor4f(1,1,1,1);
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D,GL11.GL_TEXTURE_MAG_FILTER,GL11.GL_NEAREST);
            portraitPart(x,y,13,27,18,17,20,20,8,8);
            portraitPart(x,y,5,27,8,14,44,20,4,8);
            portraitPart(x,y,31,27,8,14,44,20,4,8);
        } finally { GL11.glTexParameteri(GL11.GL_TEXTURE_2D,GL11.GL_TEXTURE_MAG_FILTER,mag); }
    }
    /** Clip the shoulders/torso to the circular avatar without requiring a stencil buffer. */
    private static void portraitPart(int x,int y,int px,int py,int w,int h,int u,int v,int tw,int th) {
        cn.kazer.client189.compat.Tessellator t=cn.kazer.client189.compat.Tessellator.instance;
        t.startDrawingQuads();
        for(int row=0;row<h;row++) {
            double half=Math.sqrt(Math.max(0,22*22-Math.pow(py+row+.5-22,2)));
            double left=Math.max(px,22-half),right=Math.min(px+w,22+half);
            if(right<=left) continue;
            double u1=(u+(left-px)*tw/w)/64D,u2=(u+(right-px)*tw/w)/64D;
            double v1=(v+(double)row*th/h)/64D,v2=(v+(double)(row+1)*th/h)/64D;
            t.addVertexWithUV(x+left,y+py+row+1,0,u1,v2);
            t.addVertexWithUV(x+right,y+py+row+1,0,u2,v2);
            t.addVertexWithUV(x+right,y+py+row,0,u2,v1);
            t.addVertexWithUV(x+left,y+py+row,0,u1,v1);
        }
        t.draw();
    }
    @Override protected void mouseClicked(int mx,int my,int button) throws java.io.IOException {
        if(button!=0) return;
        mx=(int)(mx/parentScale()); my=(int)(my/parentScale());
        if(!inside(mx,my,X,Y,W,panelBottom()-Y)) { if(!busy) mc.displayGuiScreen(parent); return; }
        if(busy) return;
        if(inside(mx,my,X+8,Y+60,W-16,20)) { settings=!settings; return; }
        if(inside(mx,my,X+8,footerY(),W-16,20)) {
            if(settings||managing) { settings=false; managing=false; } else managing=true;
            scroll=0; return;
        }
        if(settings) return;
        String route=null;
        if(inside(mx,my,X+132,Y+94,29,17)) route="/login";
        List<JsonObject> list=otherAccounts();
        for(int row=0;route==null&&row<visibleRows()&&scroll+row<list.size();row++) {
            if(inside(mx,my,X+8,Y+114+row*22,W-16,20)) {
                String id=list.get(scroll+row).get("id").getAsString();
                if(id.equals(mc.getSession().getPlayerID())) return;
                route="/select/"+id;
            }
        }
        if(route==null) return;
        busy=true; status="Complete sign-in in the Microsoft window.";
        KazerAccountBridge.request(route,result -> {
            busy=false; status=KazerAccountBridge.acceptSession(result);
            parent.initGui(); refresh();
        },error -> { busy=false; status=error; });
    }
    @Override public void handleMouseInput() throws java.io.IOException {
        super.handleMouseInput();
        int wheel=org.lwjgl.input.Mouse.getEventDWheel();
        if(wheel!=0) scroll=Math.max(0,Math.min(Math.max(0,otherAccounts().size()-visibleRows()),scroll+(wheel<0?1:-1)));
    }
    @Override protected void keyTyped(char c,int key) throws java.io.IOException { if(key==1) mc.displayGuiScreen(parent); }
}
