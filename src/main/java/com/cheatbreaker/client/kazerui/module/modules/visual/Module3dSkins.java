package com.cheatbreaker.client.kazerui.module.modules.visual;
import com.cheatbreaker.client.kazerui.KazerResourceEpoch;
import com.cheatbreaker.client.kazerui.module.Module;
import com.cheatbreaker.client.kazerui.module.Page;
import com.cheatbreaker.client.kazerui.module.setting.*;
import net.minecraft.client.entity.AbstractClientPlayer;
import net.minecraft.client.model.ModelBiped;
import net.minecraft.client.model.ModelRenderer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.texture.ITextureObject;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.opengl.GL11;
import java.nio.ByteBuffer;
import java.util.LinkedHashMap;

/** Bounded skin meshes; pixel readback and voxel construction occur on cache misses. */
public final class Module3dSkins extends Module {
    private static Module3dSkins instance;
    private final BoolSetting[] parts={bool("enableHat",true),bool("enableLeftPants",true),bool("enableRightPants",true),bool("enableLeftSleeve",true),bool("enableRightSleeve",true),bool("enableJacket",true)};
    private final BoolSetting others=bool("showOthers",true);
    private final NumberSetting distance=integer("renderDistanceLod",14,5,40),headSize=decimal("headVoxelSize",1.18f,1.001f,1.25f),bodySize=decimal("baseVoxelSize",1.15f,1.001f,1.4f);
    private final LinkedHashMap<ResourceLocation,Entry> cache=new LinkedHashMap<ResourceLocation,Entry>(128,.75f,true);
    private final SkinBuildBudget builds=new SkinBuildBudget();
    private long epoch=-1;
    private static final class Entry {
        final int texture;final boolean slim;final SkinVoxels.Mesh head;final SkinVoxels.Mesh[] body;final long retry;
        Entry(int texture,boolean slim,SkinVoxels.Mesh head,SkinVoxels.Mesh[] body){this.texture=texture;this.slim=slim;this.head=head;this.body=body;retry=System.currentTimeMillis()+1000;}
        void delete(){if(head!=null)head.delete();if(body!=null)for(SkinVoxels.Mesh mesh:body)mesh.delete();}
    }
    public Module3dSkins(){super("3D_SKINS",false);instance=this;}
    protected void layout(Page page){page.section("performanceOptions",s->s.add(distance,others));page.section("playerModelOptions",s->{s.add(parts);s.add(headSize,bodySize);});}
    public static boolean active(){return instance!=null && instance.isEnabled();}
    protected void onDisable(){clear();}
    private void clear(){for(Entry entry:cache.values())entry.delete();cache.clear();builds.reset();}
    private boolean shown(AbstractClientPlayer player){return isEnabled() && !player.isInvisible() && (others.on() || player==mc().thePlayer) && (mc().thePlayer==null || player.getDistanceSqToEntity(mc().thePlayer)<=distance.intValue()*distance.intValue());}
    private Entry entry(AbstractClientPlayer player) {
        if(epoch!=KazerResourceEpoch.current()){clear();epoch=KazerResourceEpoch.current();}
        ResourceLocation skin=player.getLocationSkin();ITextureObject texture=mc().getTextureManager().getTexture(skin);if(texture==null)return null;
        int id=texture.getGlTextureId();boolean slim=SkinParts.slim(player);Entry cached=cache.get(skin);
        if(cached!=null && cached.texture==id && cached.slim==slim && (cached.head!=null || System.currentTimeMillis()<cached.retry))return cached;
        // Preserve native flat skin while waiting. Limit only cold GPU readback /
        // mesh builds, never visibility, distance or the user's detail settings.
        if(!builds.acquire(System.nanoTime()))return null;
        if(cached!=null)cached.delete();
        SkinVoxels.Image image=null;
        cn.kazer.client189.compat.LegacyGL.glPushAttrib(GL11.GL_TEXTURE_BIT|GL11.GL_PIXEL_MODE_BIT);
        try {
            mc().getTextureManager().bindTexture(skin);
            int width=GL11.glGetTexLevelParameteri(GL11.GL_TEXTURE_2D,0,GL11.GL_TEXTURE_WIDTH),height=GL11.glGetTexLevelParameteri(GL11.GL_TEXTURE_2D,0,GL11.GL_TEXTURE_HEIGHT);
            if(width==64 && (height==32 || height==64)) {
                ByteBuffer pixels=org.lwjgl.BufferUtils.createByteBuffer(width*height*4);GL11.glGetTexImage(GL11.GL_TEXTURE_2D,0,GL11.GL_RGBA,GL11.GL_UNSIGNED_BYTE,pixels);
                final byte[] alpha=new byte[width*height];for(int i=0;i<alpha.length;i++)alpha[i]=pixels.get(i*4+3);
                image=new SkinVoxels.Image(){public int width(){return 64;}public int height(){return height;}public boolean present(int x,int y){return x>=0 && x<64 && y>=0 && y<height && (alpha[y*64+x]&255)>0;}public boolean solid(int x,int y){return present(x,y) && (alpha[y*64+x]&255)==255;}};
            }
        }finally{cn.kazer.client189.compat.LegacyGL.glPopAttrib();}
        Entry next=new Entry(id,slim,image==null?null:SkinVoxels.head(image),image==null || image.height()<64?null:SkinVoxels.body(image,slim));cache.put(skin,next);
        if(cache.size()>128){ResourceLocation first=cache.keySet().iterator().next();cache.remove(first).delete();}
        return next;
    }
    public static void hideFlat(AbstractClientPlayer player,ModelBiped model) {
        Module3dSkins m=instance;if(m==null || !m.shown(player))return;Entry entry=m.entry(player);if(entry==null || entry.head==null)return;
        int mask=0;for(int i=0;i<6;i++)if(m.parts[i].on() && SkinParts.shown(player,i) && (i==0 || entry.body!=null))mask|=1<<i;
        SkinParts.hide(model,mask);
    }
    public static void render(AbstractClientPlayer player,ModelBiped model) {
        Module3dSkins m=instance;if(m==null || !m.shown(player))return;Entry entry=m.entry(player);if(entry==null || entry.head==null)return;
        mc().getTextureManager().bindTexture(player.getLocationSkin());GlStateManager.color(1,1,1,1);GlStateManager.pushMatrix();
        try {
            if(player.isSneaking())GlStateManager.translate(0,.2f,0);
            if(m.parts[0].on() && SkinParts.shown(player,0)){entry.head.copy(model.bipedHead);float scale=m.headSize.value();entry.head.render(.0625f,0,0,scale,scale,scale);}
            if(entry.body!=null) {
                for(int i=0;i<5;i++)if(m.parts[i+1].on() && SkinParts.shown(player,i+1)) {
                    ModelRenderer part=i==0?model.bipedLeftLeg:i==1?model.bipedRightLeg:i==2?model.bipedLeftArm:i==3?model.bipedRightArm:model.bipedBody;
                    entry.body[i].copy(part);float scale=m.bodySize.value();entry.body[i].render(.0625f,0,0,scale,1,scale);
                }
            }
        }finally{GlStateManager.popMatrix();}
    }
}
