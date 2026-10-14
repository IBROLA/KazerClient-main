package cn.kazer.client189.runtime;

import net.minecraft.client.resources.model.IBakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.ItemCameraTransforms;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.util.EnumFacing;
import net.minecraftforge.client.event.ModelBakeEvent;
import java.util.*;

/** Version adapter for the reference's red tripwire without editing OptiFine or block textures. */
public final class RedStringModels implements IBakedModel {
    private final IBakedModel original;
    private static final class Cache {
        final Map<List<BakedQuad>,List<BakedQuad>> lists=new IdentityHashMap<>();
        int color=-1;
    }
    // OptiFine may compile several chunks concurrently. Cached immutable lists
    // stay thread-local, with no locks or shared-map mutations in workers.
    private final ThreadLocal<Cache> local=new ThreadLocal<Cache>(){protected Cache initialValue(){return new Cache();}};
    private RedStringModels(IBakedModel model){original=model;}
    public static final class Installer {
        @net.minecraftforge.fml.common.eventhandler.SubscribeEvent public void bake(ModelBakeEvent event){install(event);}
    }
    public static void install(ModelBakeEvent event) {
        if(!(event.modelRegistry instanceof net.minecraft.util.RegistrySimple))return;
        for(ModelResourceLocation key:((net.minecraft.util.RegistrySimple<ModelResourceLocation,IBakedModel>)event.modelRegistry).getKeys())
            if(key.getResourceDomain().equals("minecraft")&&key.getResourcePath().equals("tripwire")) {
                IBakedModel model=event.modelRegistry.getObject(key);
                if(!(model instanceof RedStringModels)) event.modelRegistry.putObject(key,new RedStringModels(model));
            }
    }
    private List<BakedQuad> color(List<BakedQuad> quads){
        com.cheatbreaker.client.CheatBreaker core=com.cheatbreaker.client.CheatBreaker.getInstance();
        int color=com.cheatbreaker.client.kazerui.module.modules.visual.ModuleOverlay.stringColor(core!=null&&(Boolean)core.globalSettings.redString.getValue()?0xFF0000:0xFFFFFF);
        if(color==0xFFFFFF)return quads;
        Cache cache=local.get();
        if(cache.color!=color){cache.lists.clear();cache.color=color;}
        List<BakedQuad> existing=cache.lists.get(quads);if(existing!=null)return existing;
        List<BakedQuad> result=new ArrayList<>(quads.size());
        for(BakedQuad quad:quads){
            int[] data=quad.getVertexData().clone();int stride=data.length/4;
            int packed=(color&0xFF)<<16|(color&0xFF00)|(color>>16&0xFF);
            if(stride<7||data.length%4!=0){result.add(quad);continue;}
            for(int i=0;i<4;i++)data[i*stride+3]=(data[i*stride+3]&0xFF000000)|packed;
            BakedQuad red=new BakedQuad(data,quad.getTintIndex(),quad.getFace());
            result.add(red);
        }
        List<BakedQuad> immutable=Collections.unmodifiableList(result);
        if(cache.lists.size()>=64)cache.lists.clear();cache.lists.put(quads,immutable);return immutable;
    }
    @Override public List<BakedQuad> getFaceQuads(EnumFacing face){return color(original.getFaceQuads(face));}
    @Override public List<BakedQuad> getGeneralQuads(){return color(original.getGeneralQuads());}
    @Override public boolean isAmbientOcclusion(){return original.isAmbientOcclusion();}
    @Override public boolean isGui3d(){return original.isGui3d();}
    @Override public boolean isBuiltInRenderer(){return original.isBuiltInRenderer();}
    @Override public TextureAtlasSprite getParticleTexture(){return original.getParticleTexture();}
    @Override public ItemCameraTransforms getItemCameraTransforms(){return original.getItemCameraTransforms();}
}
