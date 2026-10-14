package cn.kazer.client189.runtime;
import com.cheatbreaker.client.kazerui.module.modules.visual.ModuleOverlay;
import net.minecraft.client.model.ModelRenderer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.item.ItemStack;
public final class OverlayHooks {
    public static boolean skipShadow(){return !ModuleOverlay.shadows();}
    public static boolean skipArrows(){return !ModuleOverlay.stuckArrows();}
    public static boolean skipPumpkin(){return ModuleOverlay.pumpkinAlpha()<=0;}
    public static boolean skipGroundArrow(Entity entity){return !ModuleOverlay.groundArrows()&&entity instanceof ArrowGroundState&&((ArrowGroundState)entity).kazerInGround();}
    public static ItemStack armor(ItemStack stack,EntityLivingBase entity,int slot){return ModuleOverlay.hideArmor(entity,4-slot)?null:stack;}
    public static ItemStack headArmor(ItemStack stack,EntityLivingBase entity){return ModuleOverlay.hideArmor(entity,0)?null:stack;}
    public static void bobTranslate(float x,float y,float z){float scale=ModuleOverlay.bobScale();GlStateManager.translate(x*scale,y*scale,z*scale);}
    public static void bobRotate(float angle,float x,float y,float z){GlStateManager.rotate(angle*ModuleOverlay.bobScale(),x,y,z);}
    public static void fireTranslate(float x,float y,float z){GlStateManager.translate(x,y-ModuleOverlay.fireOffset(),z);}
    public static void pumpkinColor(float r,float g,float b,float a){GlStateManager.color(r,g,b,a*ModuleOverlay.pumpkinAlpha());}
    public static void horseHead(ModelRenderer model,float scale){if(!ModuleOverlay.headless())model.render(scale);}
    public static boolean armorGlint(){return ModuleOverlay.glint(com.cheatbreaker.client.CheatBreaker.getInstance()==null||(Boolean)com.cheatbreaker.client.CheatBreaker.getInstance().globalSettings.enchantmentGlint.getValue(),true);}
    public static boolean skipArmorGlint(){return !armorGlint();}
    public static int barrierType(int original){return ModuleOverlay.barriers()?3:original;}
    public static net.minecraft.client.renderer.WorldRenderer lineColor(net.minecraft.client.renderer.WorldRenderer renderer,int r,int g,int b,int a) {
        int color=ModuleOverlay.fishingColor(r<<16|g<<8|b);return renderer.color(color>>16&255,color>>8&255,color&255,a);
    }
    public static void packIcon(int x,int y,float u,float v,int width,int height,float textureWidth,float textureHeight) {
        if(textureWidth!=32||textureHeight!=32||com.cheatbreaker.client.kazerui.module.NativeGlobalSettings.packPreviews())
            net.minecraft.client.gui.Gui.drawModalRectWithCustomSizedTexture(x,y,u,v,width,height,textureWidth,textureHeight);
    }
    private OverlayHooks(){}
}
