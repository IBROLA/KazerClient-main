package com.cheatbreaker.client.kazerui.module.modules.visual;
import java.awt.image.BufferedImage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.ITextureObject;
import net.minecraft.util.ResourceLocation;
/** Runs only at texture load or an explicit palette change, never per rendered pixel. */
public final class OverlayTexturePalette {
    private static final ResourceLocation[] GUI;
    static {
        String[] names={"inventory","generic_54","furnace","dispenser","crafting_table","anvil","enchanting_table","brewing_stand","beacon","hopper","horse"};
        GUI=new ResourceLocation[names.length+1];GUI[0]=new ResourceLocation("textures/gui/icons.png");
        for(int i=0;i<names.length;i++)GUI[i+1]=new ResourceLocation("textures/gui/container/"+names[i]+".png");
    }
    public static BufferedImage tint(BufferedImage image,ResourceLocation texture) {
        if(image==null||texture==null||!texture.getResourceDomain().equals("minecraft"))return image;
        String path=texture.getResourcePath();
        if(path.equals("textures/gui/icons.png")&&ModuleOverlay.customXp()) {
            int width=image.getWidth(),height=image.getHeight();
            tintRect(image,0,height*64/256,width*182/256,height*74/256,ModuleOverlay.xpColor(0x80FF20));
        }else if(path.startsWith("textures/gui/container/")&&ModuleOverlay.customGui()&&(ModuleOverlay.guiColor()&0xFFFFFF)!=0xFFFFFF)
            tintRect(image,0,0,image.getWidth(),image.getHeight(),ModuleOverlay.guiColor());
        return image;
    }
    private static void tintRect(BufferedImage image,int x0,int y0,int x1,int y1,int color) {
        int r=color>>16&255,g=color>>8&255,b=color&255;
        for(int y=y0;y<y1;y++)for(int x=x0;x<x1;x++){
            int pixel=image.getRGB(x,y),light=Math.max(pixel>>16&255,Math.max(pixel>>8&255,pixel&255));
            image.setRGB(x,y,(pixel&0xFF000000)|(r*light/255)<<16|(g*light/255)<<8|b*light/255);
        }
    }
    public static void reload() {
        ModuleOverlay.paletteApplied();
        Minecraft mc=Minecraft.getMinecraft();if(mc==null||mc.getTextureManager()==null)return;
        for(ResourceLocation texture:GUI) {
            ITextureObject object=mc.getTextureManager().getTexture(texture);if(object==null)continue;
            try{object.loadTexture(mc.getResourceManager());}
            catch(java.io.IOException error){org.apache.logging.log4j.LogManager.getLogger().warn("Could not reload Overlay GUI texture "+texture,error);}
        }
    }
    private OverlayTexturePalette(){}
}
