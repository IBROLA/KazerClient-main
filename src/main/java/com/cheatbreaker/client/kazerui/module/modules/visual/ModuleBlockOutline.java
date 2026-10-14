package com.cheatbreaker.client.kazerui.module.modules.visual;
import com.cheatbreaker.client.kazerui.module.Module;
import com.cheatbreaker.client.kazerui.module.Page;
import com.cheatbreaker.client.kazerui.module.setting.*;
import net.minecraft.util.AxisAlignedBB;
import org.lwjgl.opengl.GL11;
public final class ModuleBlockOutline extends Module {
    private static ModuleBlockOutline instance;
    private final BoolSetting outline=bool("outline",true),fill=bool("overlay",false);
    private final NumberSetting width=decimal("outlineThickness",2,1,5);
    private final ColorSetting lineColor=color("outlineColor",0xCCFFFFFF),fillColor=color("overlayColor",0x3055AAFF);
    public ModuleBlockOutline(){super("BLOCK_OUTLINE",false);instance=this;}
    protected void layout(Page page){page.group(outline,s->s.add(width,lineColor));page.group(fill,s->s.add(fillColor));}
    public static boolean draw(AxisAlignedBB bb){ModuleBlockOutline m=instance;if(m==null || !m.isEnabled())return false;
        GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS);
        try{GL11.glDisable(GL11.GL_TEXTURE_2D);GL11.glDisable(GL11.GL_LIGHTING);GL11.glEnable(GL11.GL_BLEND);GL11.glBlendFunc(770,771);GL11.glDepthMask(false);
            if(m.fill.on()){GL11.glDisable(GL11.GL_CULL_FACE);LineBoxes.color(m.fillColor.color(0));LineBoxes.fill(bb.minX,bb.minY,bb.minZ,bb.maxX,bb.maxY,bb.maxZ);}
            if(m.outline.on()){GL11.glLineWidth(m.width.value());LineBoxes.color(m.lineColor.color(0));LineBoxes.outline(bb.minX,bb.minY,bb.minZ,bb.maxX,bb.maxY,bb.maxZ);}
        }finally{GL11.glPopAttrib();}return true;
    }
}
