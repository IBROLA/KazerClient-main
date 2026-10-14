package com.cheatbreaker.client.kazerui.module.modules.visual;
import com.cheatbreaker.client.kazerui.module.Module;
import com.cheatbreaker.client.kazerui.module.Page;
import com.cheatbreaker.client.kazerui.module.setting.*;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.util.AxisAlignedBB;
import org.lwjgl.opengl.GL11;
public final class ModuleHitbox extends Module {
    public enum Pattern implements ChoiceSetting.Option{SOLID(1,65535),DASHED(3,65280),DOTTED(1,43690);final int factor,mask;Pattern(int f,int m){factor=f;mask=m;}public String langId(){return name().toLowerCase(java.util.Locale.ROOT);}}
    private static ModuleHitbox instance;
    private final BoolSetting players=bool("hitboxPlayerShow",true),items=bool("hitboxItemShow",true),living=bool("hitboxPassiveShow",true),other=bool("hitboxOtherShow",true),limit=bool("maxDistanceToggle",false),damaged=bool("showDamagedColor",false),pointed=bool("showHittableColor",false);
    private final ChoiceSetting<Pattern> pattern=choice("hitboxLinePattern",Pattern.SOLID);
    private final NumberSetting width=decimal("lineWidth",1,1,5),distance=integer("maxDistance",64,1,128);
    private final ColorSetting color=color("lineColor",0xFFFFFFFF),damage=color("damagedColor",0xFFFF5555),target=color("hittableColor",0xFF55FF55);
    public ModuleHitbox(){super("HITBOX",false);instance=this;}
    protected void layout(Page page){page.add(players,items,living,other,pattern,width,color);page.group(limit,s->s.add(distance));page.group(damaged,s->s.add(damage));page.group(pointed,s->s.add(target));}
    public static boolean enabled(boolean original){return original || instance!=null && instance.isEnabled();}
    public static boolean render(Entity e,double x,double y,double z,float yaw,float partial){
        ModuleHitbox m=instance;if(m==null || !m.isEnabled())return false;
        if(e instanceof com.cheatbreaker.client.kazerui.cosmetics.render.Mannequin)return true;
        if(!(e instanceof EntityPlayer?m.players.on():e instanceof EntityItem?m.items.on():e instanceof EntityLivingBase?m.living.on():m.other.on()))return true;
        if(m.limit.on() && mc().thePlayer!=null && e.getDistanceSqToEntity(mc().thePlayer)>m.distance.value()*m.distance.value())return true;
        AxisAlignedBB bb=e.getEntityBoundingBox();
        GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS);
        try{GL11.glDisable(GL11.GL_TEXTURE_2D);GL11.glDisable(GL11.GL_LIGHTING);GL11.glEnable(GL11.GL_BLEND);GL11.glBlendFunc(770,771);GL11.glDepthMask(false);GL11.glLineWidth(m.width.value());Pattern p=m.pattern.get();GL11.glEnable(GL11.GL_LINE_STIPPLE);GL11.glLineStipple(p.factor,(short)p.mask);
            ColorSetting c=m.damaged.on() && e instanceof EntityLivingBase && ((EntityLivingBase)e).hurtTime>0?m.damage:m.pointed.on() && mc().objectMouseOver!=null && mc().objectMouseOver.entityHit==e?m.target:m.color;
            LineBoxes.color(c.color(0));LineBoxes.outline(bb.minX-e.posX+x,bb.minY-e.posY+y,bb.minZ-e.posZ+z,bb.maxX-e.posX+x,bb.maxY-e.posY+y,bb.maxZ-e.posZ+z);
        }finally{GL11.glPopAttrib();}return true;
    }
}
