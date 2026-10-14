package com.cheatbreaker.client.kazerui.gui.home;

import com.cheatbreaker.client.ui.mainmenu.KazerAccountBridge;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;
import org.lwjgl.input.Mouse;
import java.io.IOException;

/** Owned-theme account flow. Credentials stay exclusively in the launcher bridge. */
public final class KazerAccountScreen extends GuiScreen {
    private final KazerHomeScreen parent;
    private final HomeGfx gfx=new HomeGfx();
    private JsonArray accounts=new JsonArray();
    private boolean busy;
    private String status="Sign in using your Microsoft account.";
    private float left,top,mx,my,k;
    private int scroll;
    private boolean autoLogin;
    public KazerAccountScreen(KazerHomeScreen parent) {this(parent,false);}
    public KazerAccountScreen(KazerHomeScreen parent,boolean autoLogin) {this.parent=parent;this.autoLogin=autoLogin;}
    @Override public void initGui() {
        parent.setWorldAndResolution(mc,width,height);
        KazerAccountBridge.request("/accounts",result -> {
            if(result.has("accounts"))accounts=result.getAsJsonArray("accounts");
        },error -> status=error);
        if(autoLogin) {autoLogin=false;request("/login");}
    }
    @Override public void updateScreen(){KazerHomeScreen.tickPanorama();}
    @Override public void drawScreen(int x,int y,float partial) {
        KazerHomeScreen.drawBehind(parent,this,partial);
        drawRect(0,0,width,height,0x80000000);
        int factor=new ScaledResolution(mc).getScaleFactor();
        gfx.dsf=Math.max(1f,factor/2f);k=gfx.dsf/factor;
        left=(mc.displayWidth/gfx.dsf-440)/2;top=(mc.displayHeight/gfx.dsf-360)/2;
        mx=Mouse.getX()/gfx.dsf;my=(mc.displayHeight-Mouse.getY()-1)/gfx.dsf;
        GlStateManager.pushMatrix();GlStateManager.scale(k,k,1);GlStateManager.disableDepth();
        GlStateManager.disableAlpha();
        try {
            gfx.draw(gfx.box(440,360,10),left,top,0xFF13141A);
            gfx.draw(gfx.text("Accounts",true,22),left+24,top+20,0xFFEDEEF3);
            gfx.roundedRegion(parent.accountSkin(),left+24,top+63,40,40,8,8/64f,8/parent.accountAtlasHeight(),16/64f,16/parent.accountAtlasHeight());
            gfx.roundedRegion(parent.accountSkin(),left+24,top+63,40,40,8,40/64f,8/parent.accountAtlasHeight(),48/64f,16/parent.accountAtlasHeight());
            gfx.draw(gfx.text(mc.getSession().getUsername(),true,18),left+78,top+64,0xFFEDEEF3);
            gfx.draw(gfx.text("Current session",false,14),left+78,top+88,0xFFB1B3C0);
            button("Add Microsoft Account",left+24,top+121,392,!busy,0xFF123142);
            gfx.draw(gfx.text("Saved Accounts",true,14),left+24,top+170,0xFFB1B3C0);
            for(int row=0;row<3 && row+scroll<accounts.size();row++) {
                JsonObject account=accounts.get(row+scroll).getAsJsonObject();
                String name=account.get("username").getAsString();
                button(name,left+24,top+192+row*32,392,!busy,0xFF22232C);
            }
            // Bounded status text; never display credentials or token-bearing exception text.
            String text=status.length()>170?status.substring(0,170):status;
            int split=Math.min(60,text.length());
            gfx.draw(gfx.text(text.substring(0,split),false,12),left+24,top+294,0xFFB1B3C0);
            if(split<text.length())gfx.draw(gfx.text(text.substring(split,Math.min(text.length(),split+60)),false,12),left+24,top+310,0xFFB1B3C0);
            button("Back",left+300,top+326,116,true,0xFF22232C);
        } finally {GlStateManager.enableAlpha();GlStateManager.enableDepth();GlStateManager.popMatrix();}
    }
    private boolean over(float x,float y,float w,float h){return mx>=x&&mx<x+w&&my>=y&&my<y+h;}
    private void button(String label,float x,float y,float w,boolean enabled,int color) {
        gfx.draw(gfx.box(w,28,6),x,y,enabled&&over(x,y,w,28)?0xFF2F313B:color);
        gfx.draw(gfx.text(label,true,14),x+(w-gfx.textWidth(label,true,14))/2,y+5,enabled?0xFFEDEEF3:0xFF797B87);
    }
    @Override protected void mouseClicked(int x,int y,int button)throws IOException {
        if(button!=0)return;
        if(over(left+300,top+326,116,28)){mc.displayGuiScreen(parent);return;}
        if(busy)return;
        if(over(left+24,top+121,392,28)){request("/login");return;}
        for(int row=0;row<3&&row+scroll<accounts.size();row++)if(over(left+24,top+192+row*32,392,28)) {
            String id=accounts.get(row+scroll).getAsJsonObject().get("id").getAsString();
            if(!id.equals(mc.getSession().getPlayerID()))request("/select/"+id);
            return;
        }
    }
    private void request(String route) {
        busy=true;status="Complete sign-in in the Microsoft window.";
        KazerAccountBridge.request(route,result -> {
            busy=false;status=KazerAccountBridge.acceptSession(result);
            if(mc.currentScreen==this)mc.displayGuiScreen(parent);
        },error -> {busy=false;status=error;});
    }
    @Override public void handleMouseInput()throws IOException {
        super.handleMouseInput();int wheel=Mouse.getEventDWheel();
        if(wheel!=0)scroll=Math.max(0,Math.min(Math.max(0,accounts.size()-3),scroll+(wheel<0?1:-1)));
    }
    @Override protected void keyTyped(char character,int key)throws IOException {if(key==1)mc.displayGuiScreen(parent);}
    @Override public void onGuiClosed(){gfx.release();}
    @Override public boolean doesGuiPauseGame(){return false;}
}
