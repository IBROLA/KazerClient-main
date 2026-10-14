package cn.kazer.client189.runtime;

import com.cheatbreaker.client.kazerui.cosmetics.render.CosmeticRender;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.AbstractClientPlayer;
import net.minecraft.client.renderer.entity.RenderPlayer;
import net.minecraft.client.renderer.entity.layers.LayerRenderer;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.opengl.GL11;

/** Accessories live in native player layers, never in a through-wall overlay pass. */
public final class KazerCosmeticLayer implements LayerRenderer<AbstractClientPlayer> {
    private static final java.util.Map<RenderPlayer,int[]> VISIBILITY=new java.util.IdentityHashMap<>();
    private final RenderPlayer renderer;
    private final Runnable drawCape;
    private KazerCosmeticLayer(RenderPlayer renderer) { this.renderer=renderer;drawCape=()->renderer.getMainModel().renderCape(.0625F); }
    public static void install() {
        // Forge adds this accessor after vanilla SRG mapping; resolve once at
        // initialization, never from the player render hot path.
        Object manager=Minecraft.getMinecraft().getRenderManager();
        try {
            java.util.Map<?,?> skins=(java.util.Map<?,?>)manager.getClass().getMethod("getSkinMap").invoke(manager);
            for(Object renderer:skins.values()) {
                RenderPlayer player=(RenderPlayer)renderer;
                VISIBILITY.put(player,new int[2]);player.addLayer(new KazerCosmeticLayer(player));
            }
        } catch(ReflectiveOperationException error) {
            throw new IllegalStateException("Forge player skin renderers unavailable",error);
        }
    }
    public static boolean customCape(AbstractClientPlayer player) { return CosmeticRender.cape(player)!=null; }
    public static void hide(net.minecraftforge.client.event.RenderLivingEvent.Pre event) {
        if(!(event.renderer instanceof RenderPlayer) || !(event.entity instanceof AbstractClientPlayer))return;
        RenderPlayer renderer=(RenderPlayer)event.renderer;
        int[] saved=VISIBILITY.get(renderer);if(saved==null)return;
        restore(renderer);
        java.util.List<com.cheatbreaker.client.kazerui.cosmetics.Cosmetic> worn=CosmeticRender.worn((AbstractClientPlayer)event.entity);
        if(worn.isEmpty() && !com.cheatbreaker.client.kazerui.module.modules.visual.Module3dSkins.active())return;
        saved[0]=com.cheatbreaker.client.kazerui.cosmetics.render.GeckoCosmetics.visibility(renderer.getMainModel());saved[1]=1;
        com.cheatbreaker.client.kazerui.cosmetics.render.GeckoCosmetics.hideParts((AbstractClientPlayer)event.entity,renderer.getMainModel(),worn);
        com.cheatbreaker.client.kazerui.module.modules.visual.Module3dSkins.hideFlat((AbstractClientPlayer)event.entity,renderer.getMainModel());
    }
    public static void restore(RenderPlayer renderer) {
        int[] saved=VISIBILITY.get(renderer);
        if(saved!=null && saved[1]!=0){com.cheatbreaker.client.kazerui.cosmetics.render.GeckoCosmetics.visibility(renderer.getMainModel(),saved[0]);saved[1]=0;}
    }
    @Override public void doRenderLayer(AbstractClientPlayer player,float a,float b,float partial,float d,float e,float f,float g) {
        restore(renderer);
        if(player.isInvisible() || (CosmeticRender.worn(player).isEmpty() && !com.cheatbreaker.client.kazerui.module.modules.visual.Module3dSkins.active()))return;
        cn.kazer.client189.compat.LegacyGL.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS);
        try {
            ResourceLocation cape=CosmeticRender.cape(player);
            if(cape!=null) { Minecraft.getMinecraft().getTextureManager().bindTexture(cape);CosmeticRender.cloak(player,partial,drawCape); }
            CosmeticRender.accessories(player,renderer.getMainModel(),partial);
        }finally { cn.kazer.client189.compat.LegacyGL.glPopAttrib(); }
    }
    @Override public boolean shouldCombineTextures() { return false; }
}
