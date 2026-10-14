package cn.kazer.client189.runtime;
import com.cheatbreaker.client.kazerui.module.modules.visual.ModuleOverlay;
import com.cheatbreaker.client.kazerui.module.ModuleManager;
import com.cheatbreaker.client.kazerui.KazerResourceEpoch;
import java.util.*;
import net.minecraft.client.resources.model.IBakedModel;
import net.minecraft.client.renderer.block.model.*;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.util.EnumFacing;

/** Compile-time quad variants only. Never edits shared resource-pack vertex arrays. */
public final class OverlayModelCache {
    private static final class Cache {
        long revision=Long.MIN_VALUE,epoch=Long.MIN_VALUE;
        final IdentityHashMap<IBakedModel,IBakedModel[]> variants=new IdentityHashMap<IBakedModel,IBakedModel[]>();
    }
    private static final ThreadLocal<Cache> CACHE=new ThreadLocal<Cache>(){protected Cache initialValue(){return new Cache();}};
    private static volatile IBakedModel barrier;
    private static long barrierEpoch=Long.MIN_VALUE;private static int barrierColor;
    public static boolean renderBarrier(IBlockState state,net.minecraft.util.BlockPos pos,net.minecraft.world.IBlockAccess world,net.minecraft.client.renderer.WorldRenderer renderer) {
        if(!ModuleOverlay.barriers()||state.getBlock()!=Blocks.barrier||com.cheatbreaker.client.kazerui.module.modules.visual.OverlayWhiteSprite.current==null)return false;
        long epoch=KazerResourceEpoch.current();int color=ModuleOverlay.barrierColor();IBakedModel model=barrier;
        if(model==null||epoch!=barrierEpoch||color!=barrierColor)model=barrierModel(epoch,color);
        net.minecraft.client.Minecraft.getMinecraft().getBlockRendererDispatcher().getBlockModelRenderer().renderModel(world,model,state,pos,renderer,true);
        return true; // A fully occluded barrier must not fall through to the missing-texture model.
    }
    private static synchronized IBakedModel barrierModel(long epoch,int color) {
        if(barrier!=null&&barrierEpoch==epoch&&barrierColor==color)return barrier;
        barrier=new BarrierModel(color);barrierEpoch=epoch;barrierColor=color;return barrier;
    }
    private static final class BarrierModel implements IBakedModel {
        final List<BakedQuad>[] faces;
        @SuppressWarnings("unchecked") BarrierModel(int color) {
            faces=(List<BakedQuad>[])new List[6];
            for(EnumFacing face:EnumFacing.values()) {
                int[] data=new int[28];
                for(int v=0;v<4;v++) {
                    float u=v==1||v==2?1:0,t=v>=2?1:0,x,y,z;
                    switch(face){case DOWN:x=u;y=0;z=t;break;case UP:x=u;y=1;z=1-t;break;
                        case NORTH:x=1-u;y=t;z=0;break;case SOUTH:x=u;y=t;z=1;break;
                        case WEST:x=0;y=t;z=u;break;default:x=1;y=t;z=1-u;}
                    data[v*7]=Float.floatToRawIntBits(x);data[v*7+1]=Float.floatToRawIntBits(y);data[v*7+2]=Float.floatToRawIntBits(z);data[v*7+3]=-1;
                }
                List<BakedQuad> quads=new ArrayList<BakedQuad>(4);outlines(quads,new BakedQuad(data,-1,face),color);
                faces[face.ordinal()]=Collections.unmodifiableList(quads);
            }
        }
        public List<BakedQuad> getFaceQuads(EnumFacing face){return faces[face.ordinal()];}
        public List<BakedQuad> getGeneralQuads(){return Collections.emptyList();}
        public boolean isAmbientOcclusion(){return false;}public boolean isGui3d(){return true;}public boolean isBuiltInRenderer(){return false;}
        public TextureAtlasSprite getParticleTexture(){return com.cheatbreaker.client.kazerui.module.modules.visual.OverlayWhiteSprite.current;}
        public ItemCameraTransforms getItemCameraTransforms(){return ItemCameraTransforms.DEFAULT;}
    }
    public static IBakedModel adjust(IBakedModel model,IBlockState state) {
        if(!ModuleOverlay.blockTweaks()||model==null||state==null)return model;
        boolean fire=state.getBlock()==Blocks.fire;
        boolean outline=ModuleOverlay.outlines(state.getBlock());
        if(!fire&&!outline)return model;
        float shrink=fire?1-ModuleOverlay.blockFireOffset():1;
        int color=outline?ModuleOverlay.outlineColor():0xFFFFFF;
        if(!outline&&shrink==1)return model;
        Cache cache=CACHE.get();long revision=ModuleManager.model().revision,epoch=KazerResourceEpoch.current();
        if(cache.revision!=revision||cache.epoch!=epoch){cache.variants.clear();cache.revision=revision;cache.epoch=epoch;}
        IBakedModel[] pair=cache.variants.get(model);
        if(pair==null){if(cache.variants.size()>=512)cache.variants.clear();pair=new IBakedModel[2];cache.variants.put(model,pair);}
        int index=fire?0:1;if(pair[index]==null)pair[index]=new Variant(model,shrink,color,outline);
        return pair[index];
    }
    private static final class Variant implements IBakedModel {
        final IBakedModel original;final List<BakedQuad>[] faces;final List<BakedQuad> general;
        @SuppressWarnings("unchecked") Variant(IBakedModel original,float shrink,int color,boolean outline) {
            this.original=original;faces=(List<BakedQuad>[])new List[6];
            for(EnumFacing face:EnumFacing.values())faces[face.ordinal()]=quads(original.getFaceQuads(face),shrink,color,outline);
            general=quads(original.getGeneralQuads(),shrink,color,outline);
        }
        public List<BakedQuad> getFaceQuads(EnumFacing face){return faces[face.ordinal()];}
        public List<BakedQuad> getGeneralQuads(){return general;}
        public boolean isAmbientOcclusion(){return original.isAmbientOcclusion();}
        public boolean isGui3d(){return original.isGui3d();}
        public boolean isBuiltInRenderer(){return original.isBuiltInRenderer();}
        public TextureAtlasSprite getParticleTexture(){return original.getParticleTexture();}
        public ItemCameraTransforms getItemCameraTransforms(){return original.getItemCameraTransforms();}
    }
    private static List<BakedQuad> quads(List<BakedQuad> source,float shrink,int color,boolean outline) {
        List<BakedQuad> result=new ArrayList<BakedQuad>(source.size());
        for(BakedQuad quad:source) {
            if(outline){result.add(quad);outlines(result,quad,color);continue;}
            int[] data=quad.getVertexData().clone();int stride=data.length/4;
            if(stride<7||data.length%4!=0){result.add(quad);continue;}
            int packed=java.nio.ByteOrder.nativeOrder()==java.nio.ByteOrder.LITTLE_ENDIAN?
                (color&0xFF)<<16|(color&0xFF00)|(color>>16&0xFF):color;
            for(int v=0;v<4;v++){data[v*stride+1]=Float.floatToRawIntBits(Float.intBitsToFloat(data[v*stride+1])*shrink);if(color!=0xFFFFFF)data[v*stride+3]=(data[v*stride+3]&0xFF000000)|packed;}
            result.add(new BakedQuad(data,quad.getTintIndex(),quad.getFace()));
        }
        return Collections.unmodifiableList(result);
    }
    private static void outlines(List<BakedQuad> out,BakedQuad quad,int color) {
        com.cheatbreaker.client.kazerui.module.modules.visual.OverlayWhiteSprite white=com.cheatbreaker.client.kazerui.module.modules.visual.OverlayWhiteSprite.current;
        if(white==null)return;
        int[] original=quad.getVertexData();int stride=original.length/4;if(stride<7||original.length%4!=0)return;
        float cx=0,cy=0,cz=0;for(int v=0;v<4;v++){cx+=Float.intBitsToFloat(original[v*stride]);cy+=Float.intBitsToFloat(original[v*stride+1]);cz+=Float.intBitsToFloat(original[v*stride+2]);}
        cx/=4;cy/=4;cz/=4;EnumFacing face=quad.getFace();
        int packed=0xFF000000|(color&0xFF)<<16|(color&0xFF00)|(color>>16&0xFF);
        for(int edge=0;edge<4;edge++) {
            int next=(edge+1)%4;int[] data=original.clone();
            for(int v=0;v<4;v++) {
                int src=(v==0||v==3?edge:next)*stride,dst=v*stride;
                System.arraycopy(original,src,data,dst,stride);float blend=v>=2?.05F:0;
                float x=Float.intBitsToFloat(original[src]),y=Float.intBitsToFloat(original[src+1]),z=Float.intBitsToFloat(original[src+2]);
                data[dst]=Float.floatToRawIntBits(x+(cx-x)*blend+face.getFrontOffsetX()*.0002F);
                data[dst+1]=Float.floatToRawIntBits(y+(cy-y)*blend+face.getFrontOffsetY()*.0002F);
                data[dst+2]=Float.floatToRawIntBits(z+(cz-z)*blend+face.getFrontOffsetZ()*.0002F);
                data[dst+3]=packed;data[dst+4]=Float.floatToRawIntBits(white.getInterpolatedU(8));data[dst+5]=Float.floatToRawIntBits(white.getInterpolatedV(8));
            }
            out.add(new BakedQuad(data,-1,face));
        }
    }
    private OverlayModelCache(){}
}
