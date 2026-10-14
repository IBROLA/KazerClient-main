package cn.kazer.client189.ui;

import com.cheatbreaker.client.CheatBreaker;
import com.cheatbreaker.client.config.Setting;
import net.minecraft.client.gui.*;
import net.minecraft.client.resources.ResourcePackListEntry;
import net.minecraft.client.renderer.GlStateManager;
import org.lwjgl.opengl.GL11;
import java.util.*;
import java.io.IOException;

/** 1.7 Kazer two-column resource-pack layout backed by the official 1.8 repository. */
public final class KazerResourcePacks extends GuiScreenResourcePacks {
    private GuiTextField search;
    private PackList available, selected;
    private final List<ResourcePackListEntry> filtered = new ArrayList<>();
    private volatile boolean repositoryReady;
    private boolean repositoryLoading;
    private static final java.util.concurrent.ExecutorService SCANNER=java.util.concurrent.Executors.newSingleThreadExecutor(task -> {
        Thread thread=new Thread(task,"Kazer resource-pack scan");thread.setDaemon(true);return thread;
    });
    private static java.util.concurrent.CompletableFuture<Void> pendingScan;
    private static synchronized java.util.concurrent.CompletableFuture<Void> scan(net.minecraft.client.resources.ResourcePackRepository repository) {
        if(pendingScan==null || pendingScan.isDone())pendingScan=java.util.concurrent.CompletableFuture.runAsync(repository::updateRepositoryEntriesAll,SCANNER);
        return pendingScan;
    }
    public KazerResourcePacks(GuiScreen parent) { super(parent); }
    @Override public void initGui() {
        if(!repositoryReady) {
            if(!repositoryLoading) {
                repositoryLoading=true;
                final net.minecraft.client.resources.ResourcePackRepository repository=mc.getResourcePackRepository();
                scan(repository).whenComplete((ignored,error) -> mc.addScheduledTask(() -> {
                    repositoryReady=true;
                    if(error!=null)org.apache.logging.log4j.LogManager.getLogger().warn("Resource-pack scan failed",error);
                    if(mc.currentScreen==this)setWorldAndResolution(mc,width,height);
                }));
            }
            return;
        }
        super.initGui();
        buttonList.clear();
        int right = width/2+4;
        buttonList.add(new GuiButton(2, width/2-154, height-48, 150,20,"Open resource pack folder"));
        buttonList.add(new GuiButton(1, right,height-48,200,20,"Done"));
        buttonList.add(new GuiButton(25,right,height-92,200,20,""));
        buttonList.add(new GuiButton(23,right,height-70,112,20,""));
        buttonList.add(new GuiButton(24,right+114,height-70,86,20,""));
        labels();
        search = new GuiTextField(0,fontRendererObj,width/2-204,height-72,200,20);
        search.setMaxStringLength(128);
        refresh();
        available = new PackList(width/2-204,height-76,filtered);
        selected = new PackList(right,height-96,getSelectedResourcePacks());
    }
    private void labels() {
        com.cheatbreaker.client.config.GlobalSettings s = CheatBreaker.getInstance().globalSettings;
        for (GuiButton button : buttonList) {
            if (button.id==25) button.displayString="Background: "+((Boolean)s.transparentBackground.getValue()?"TRANSPARENT":"DEFAULT");
            if (button.id==23) button.displayString="Clear Glass: "+s.clearGlass.getValue();
            if (button.id==24) button.displayString="Red String: "+((Boolean)s.redString.getValue()?"ON":"OFF");
        }
    }
    private void refresh() {
        filtered.clear(); String query=search==null?"":search.getText().toLowerCase(Locale.ROOT);
        for(ResourcePackListEntry entry:getAvailableResourcePacks()) {
            if(query.isEmpty()) {filtered.add(entry);continue;}
            String text="";
            try {
                java.lang.reflect.Method name=ResourcePackListEntry.class.getDeclaredMethod("func_148312_b");
                name.setAccessible(true); text=String.valueOf(name.invoke(entry));
            } catch(ReflectiveOperationException ignored) { }
            if(query.isEmpty() || text.toLowerCase(Locale.ROOT).contains(query)) filtered.add(entry);
        }
    }
    @Override protected void actionPerformed(GuiButton button) throws IOException {
        if(button.id<20) {super.actionPerformed(button); return;}
        com.cheatbreaker.client.config.GlobalSettings settings=CheatBreaker.getInstance().globalSettings;
        Setting setting=button.id==25?settings.transparentBackground:button.id==23?settings.clearGlass:settings.redString;
        if(button.id==23) {
            String value=String.valueOf(setting.getValue()); setting.setValue(value.equals("OFF")?"REGULAR":value.equals("REGULAR")?"ALL":"OFF");
            mc.renderGlobal.loadRenderers();
        } else {
            setting.setValue(!(Boolean)setting.getValue());
            if(button.id==24)mc.renderGlobal.loadRenderers();
        }
        CheatBreaker.getInstance().configManager.writeGlobalConfig(); labels();
    }
    @Override public void updateScreen() {if(search!=null)search.updateCursorCounter();}
    @Override protected void keyTyped(char key,int code) throws IOException {
        if(search==null){if(code==1)super.keyTyped(key,code);return;}
        if(search.textboxKeyTyped(key,code)) {refresh();return;} super.keyTyped(key,code);
    }
    @Override protected void mouseClicked(int x,int y,int button) throws IOException {
        if(search==null)return;
        search.mouseClicked(x,y,button);
        if(button==0) for(GuiButton control:buttonList) if(control.mousePressed(mc,x,y)) {
            control.playPressSound(mc.getSoundHandler());actionPerformed(control);return;
        }
        available.mouseClicked(x,y,button); selected.mouseClicked(x,y,button); refresh();
    }
    @Override protected void mouseReleased(int x,int y,int button) {
        if(available==null)return;
        available.mouseReleased(x,y,button);selected.mouseReleased(x,y,button);
    }
    @Override public void handleMouseInput() throws IOException {
        // The vanilla lists do not exist until the asynchronous repository scan
        // has completed and super.initGui() has run. Its input handler dereferences
        // both lists even for a plain mouse move, so do not delegate while loading.
        if(available==null || selected==null)return;
        super.handleMouseInput(); available.handleMouseInput();selected.handleMouseInput();
    }
    @Override public void drawScreen(int x,int y,float partial) {
        boolean transparent=(Boolean)CheatBreaker.getInstance().globalSettings.transparentBackground.getValue()&&mc.theWorld!=null;
        if(transparent) Gui.drawRect(0,0,width,height,0x70000000); else drawDefaultBackground();
        if(search==null) {
            com.cheatbreaker.client.ui.util.font.CBFontRenderer font=CheatBreaker.getInstance().playRegular18px;
            String text="Loading resource packs...";font.drawMixedString(text,(width-font.getMixedStringWidth(text))/2F,height/2F,-1);return;
        }
        drawCenteredString(fontRendererObj,"Available Resource Packs",width/2-104,17,-1);
        drawCenteredString(fontRendererObj,"Selected Resource Packs",width/2+104,17,-1);
        available.drawScreen(x,y,partial);selected.drawScreen(x,y,partial);
        search.drawTextBox();
        if(search.getText().isEmpty()&&!search.isFocused()) fontRendererObj.drawStringWithShadow("\u00a7oSearch...",width/2-200,height-66,0xAAAAAA);
        for(GuiButton control:buttonList) control.drawButton(mc,x,y);
        drawCenteredString(fontRendererObj,"(Place resource-pack files here)",width/2-80,height-23,0x777777);
    }
    private final class PackList extends GuiListExtended {
        private final List<ResourcePackListEntry> entries;
        PackList(int x,int bottom,List<ResourcePackListEntry> entries) {
            super(KazerResourcePacks.this.mc,200,KazerResourcePacks.this.height,36,bottom,36);
            this.entries=entries;setSlotXBoundsFromLeft(x);setShowSelectionBox(false);
        }
        @Override public IGuiListEntry getListEntry(int index){return entries.get(index);}
        @Override protected int getSize(){return entries.size();}
        @Override protected void drawBackground(){}
        @Override public int getListWidth(){return 200;}
        @Override protected int getScrollBarX(){return right-6;}
        @Override public void drawScreen(int x,int y,float partial) {
            this.mouseX=x;this.mouseY=y;bindAmountScrolled();
            Gui.drawRect(left,top,right,bottom,0x78000000);
            ScaledResolution resolution=new ScaledResolution(mc); int scale=resolution.getScaleFactor();
            GL11.glEnable(GL11.GL_SCISSOR_TEST);GL11.glScissor(left*scale,(resolution.getScaledHeight()-bottom)*scale,200*scale,(bottom-top)*scale);
            for(int i=0;i<entries.size();i++) {
                int row=top+4+i*36-(int)amountScrolled;
                if(row+32<=top||row>=bottom)continue;
                entries.get(i).drawEntry(i,left+2,row,198,32,x,y,x>=left&&x<right&&y>=row&&y<row+36);
            }
            GL11.glDisable(GL11.GL_SCISSOR_TEST);
            int max=func_148135_f();
            if(max>0) {int thumb=Math.max(20,(bottom-top)*(bottom-top)/getContentHeight());
                int sy=top+(int)amountScrolled*(bottom-top-thumb)/max;Gui.drawRect(right-6,sy,right,sy+thumb,0xFFBBBBBB);}
            GlStateManager.color(1,1,1,1);
        }
    }
}
