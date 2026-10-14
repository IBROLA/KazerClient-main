package com.cheatbreaker.client.kazerui.module.modules.mechanic;
import com.cheatbreaker.client.kazerui.module.Module;
import com.cheatbreaker.client.kazerui.module.Page;
import com.cheatbreaker.client.kazerui.module.setting.*;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.WorldRenderer;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.opengl.GL11;
/** Health-driven visual overlay; never changes damage or health. */
public final class ModuleDamageTint extends Module {
    private static ModuleDamageTint instance;
    private static final ResourceLocation VIGNETTE=new ResourceLocation("textures/misc/vignette.png");
    private final ColorSetting color=color("vignetteColor",0xFFFF0000);
    private final NumberSetting intensity=decimal("vignetteIntensity",1,0,1),threshold=integer("showVignetteBelow",50,0,100);
    public ModuleDamageTint(){super("DAMAGE_TINT",false);instance=this;}
    protected void layout(Page page){page.add(color,intensity,threshold);}
    public static void render(ScaledResolution res) {
        ModuleDamageTint m=instance;if(m==null || !m.isEnabled() || mc().thePlayer==null || mc().currentScreen!=null || mc().thePlayer.capabilities.isCreativeMode)return;
        float limit=m.threshold.value()/100f,ratio=mc().thePlayer.getHealth()/mc().thePlayer.getMaxHealth();if(limit<=0 || ratio<=0 || ratio>=limit)return;
        float alpha=(1-ratio/limit)*m.intensity.value();int c=m.color.color(0);
        cn.kazer.client189.compat.LegacyGL.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS);
        try {
            mc().getTextureManager().bindTexture(VIGNETTE);GlStateManager.enableBlend();GlStateManager.disableDepth();GlStateManager.depthMask(false);
            GlStateManager.tryBlendFuncSeparate(0,769,0,769);GlStateManager.color(1-(c>>16&255)/255f*alpha,1-(c>>8&255)/255f*alpha,1-(c&255)/255f*alpha,1);
            WorldRenderer wr=Tessellator.getInstance().getWorldRenderer();int w=res.getScaledWidth(),h=res.getScaledHeight();
            wr.begin(7,DefaultVertexFormats.POSITION_TEX);wr.pos(0,h,0).tex(0,1).endVertex();wr.pos(w,h,0).tex(1,1).endVertex();wr.pos(w,0,0).tex(1,0).endVertex();wr.pos(0,0,0).tex(0,0).endVertex();Tessellator.getInstance().draw();
        }finally{cn.kazer.client189.compat.LegacyGL.glPopAttrib();}
    }
}
