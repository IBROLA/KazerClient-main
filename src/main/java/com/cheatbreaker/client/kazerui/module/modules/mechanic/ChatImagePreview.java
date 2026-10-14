package com.cheatbreaker.client.kazerui.module.modules.mechanic;

import com.cheatbreaker.client.kazerui.module.hud.Draw;
import com.google.common.cache.*;
import java.awt.image.BufferedImage;
import java.util.*;
import java.util.concurrent.*;
import java.util.regex.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiChat;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.util.IChatComponent;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.input.Keyboard;

/** Source preview interaction with bounded work and main-thread-owned texture lifetime. */
final class ChatImagePreview {
    private static final ThreadPoolExecutor DOWNLOADER=new ThreadPoolExecutor(1,1,30,TimeUnit.SECONDS,
        new ArrayBlockingQueue<Runnable>(4),r->{Thread t=new Thread(r,"Kazer chat image downloader");t.setDaemon(true);return t;});
    private static final ResourceLocation TEXTURE=new ResourceLocation("kazerui","hover_image");
    private static final Pattern URL_PATTERN=Pattern.compile("https://[^\\s\"']+");
    private final LoadingCache<String,Future<BufferedImage>> cache=CacheBuilder.newBuilder().maximumSize(10)
        .expireAfterAccess(5,TimeUnit.MINUTES).build(new CacheLoader<String,Future<BufferedImage>>() {
            @Override public Future<BufferedImage> load(String url) {
                final List<String> allowed=new ArrayList<String>(customHosts);
                try {return DOWNLOADER.submit(()->{
                    try{return ChatImageLoader.load(url,allowed);}catch(Exception failure){return null;}
                });}catch(RejectedExecutionException busy){return CompletableFuture.completedFuture(null);}
            }
        });
    private final List<String> customHosts=new ArrayList<String>(),urls=new ArrayList<String>(2);
    private DynamicTexture texture;
    private Future<BufferedImage> image;
    private String url;
    private boolean showing,shiftDown,expanded;
    private long resourceEpoch=-1,animationStart;
    private float expansion,fromExpansion;
    private int cycle;
    void hosts(String[] domains) {
        customHosts.clear();
        for(String domain:domains)if(!domain.trim().isEmpty()&&customHosts.size()<32)customHosts.add(domain.trim().toLowerCase(Locale.ROOT));
    }
    void render(ModuleChat chat,int mouseX,int mouseY,int screenW,int screenH) {
        if(!chat.hoverImagePreview.on()||!showing||image==null||!image.isDone())return;
        if(!(Minecraft.getMinecraft().currentScreen instanceof GuiChat)){reset();return;}
        try {
            BufferedImage img=image.get();if(img==null)return;
            long epoch=com.cheatbreaker.client.kazerui.KazerResourceEpoch.current();
            if(epoch!=resourceEpoch){releaseTexture();resourceEpoch=epoch;}
            double h=img.getHeight(),w=img.getWidth(),min=chat.minImageSize.value(),max=chat.maxImageSize.value();
            boolean shift=Keyboard.isKeyDown(Keyboard.KEY_LSHIFT);
            long now=System.nanoTime();
            if(shift&&!shiftDown&&chat.fullscreenImage.on()&&max<100){expanded=!expanded;fromExpansion=expansion;animationStart=now;}
            shiftDown=shift;
            float t=Math.min(1,(now-animationStart)/250_000_000F);
            expansion=fromExpansion+((expanded?1:0)-fromExpansion)*(1-(1-t)*(1-t));
            if(chat.fullscreenImage.on())max+=(100-max)*expansion;
            min=Math.min(min,max);double ratio=w/h;
            if(h/screenH>w/screenW){h=clamp(h,min/100*screenH,max/100*screenH);w=h*ratio;}
            else {w=clamp(w,min/100*screenW,max/100*screenW);h=w/ratio;}
            double x=Math.max(0,Math.min(screenW-w,mouseX)),y=Math.max(0,Math.min(screenH-h,mouseY-h));
            if(texture==null){texture=new DynamicTexture(img);Minecraft.getMinecraft().getTextureManager().loadTexture(TEXTURE,texture);}
            GlStateManager.pushMatrix();
            try {GlStateManager.translate(0,0,200);Draw.texture(TEXTURE,(float)x,(float)y,(float)w,(float)h,-1);}
            finally {GlStateManager.popMatrix();}
        }catch(Exception failure){reset();}
    }
    private static double clamp(double value,double min,double max){return Math.max(min,Math.min(max,value));}
    private static String direct(String value){return value.startsWith("https://imgur.com/")?value.replace("https://imgur.com/","https://i.imgur.com/")+".png":value;}
    boolean show(ModuleChat chat,String link,IChatComponent message) {
        if(!chat.hoverImagePreview.on())return false;
        urls.clear();
        if(link!=null)urls.add(direct(link));
        else if(message!=null){Matcher matcher=URL_PATTERN.matcher(ChatText.plain(message));while(urls.size()<16&&matcher.find())urls.add(direct(matcher.group()));}
        if(urls.isEmpty()){reset();return false;}
        String next=urls.get(Math.floorMod(cycle,urls.size()));
        try {
            if(!ChatImageLoader.allowed(new java.net.URL(next),customHosts)){reset();return false;}
            Future<BufferedImage> pending=cache.get(next);
            if(pending.isDone()&&pending.get()==null){reset();return false;}
            if(showing&&next.equals(url))return true;
            if(urls.size()>1&&showing&&image!=null&&image.isDone()&&!pending.isDone())return true;
            releaseTexture();image=pending;showing=true;url=next;return true;
        }catch(Exception failure){reset();return false;}
    }
    void keyPressed(int key,boolean repeat){if(!repeat&&key==Keyboard.KEY_LCONTROL)cycle=cycle==Integer.MAX_VALUE?0:cycle+1;}
    private void releaseTexture(){if(texture!=null){Minecraft.getMinecraft().getTextureManager().deleteTexture(TEXTURE);texture=null;}}
    void reset(){releaseTexture();showing=false;image=null;url=null;expanded=shiftDown=false;expansion=fromExpansion=0;animationStart=0;}
    void clearCache(){reset();for(Future<BufferedImage> pending:cache.asMap().values())if(!pending.isDone())pending.cancel(true);cache.invalidateAll();}
}
