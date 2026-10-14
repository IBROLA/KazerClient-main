package com.cheatbreaker.client.ui.util;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.opengl.GL11;
import java.awt.*;
import java.awt.geom.*;
import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.Map;

/** Code-native paths are rasterized once at high resolution, then drawn with linear filtering. */
public final class SmoothUiGlyphs {
    private static final Map<String, ResourceLocation> TEXTURES = new HashMap<>();
    private SmoothUiGlyphs() {}
    public static BufferedImage mask(String name) {
        BufferedImage image=new BufferedImage(128,128,BufferedImage.TYPE_INT_ARGB);
        Graphics2D g=image.createGraphics();
        try {
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
            g.scale(128D/24,128D/24);
            g.setColor(Color.WHITE);
            if ("CHAT".equals(name)) {
                g.fill(new RoundRectangle2D.Double(2,3,20,15,4,4));
                Path2D tail=new Path2D.Double();tail.moveTo(4,16);tail.lineTo(4,21);
                tail.quadTo(4,22,5,21.3);tail.lineTo(10,17);tail.closePath();g.fill(tail);
                g.setComposite(AlphaComposite.Clear);
                for(int i=0;i<3;i++)g.fill(new Ellipse2D.Double(5.5+i*5,9,2.5,2.5));
            } else if ("STAR".equals(name)) {
                double[][] p = new double[10][2];
                for (int i=0;i<10;i++) {
                    double a=-Math.PI/2+i*Math.PI/5, r=i%2==0?10:4.9;
                    p[i][0]=12+Math.cos(a)*r;p[i][1]=12+Math.sin(a)*r;
                }
                Path2D star=new Path2D.Double();
                for (int i=0;i<10;i++) {
                    double[] prev=p[(i+9)%10], v=p[i], next=p[(i+1)%10];
                    double sx=v[0]+(prev[0]-v[0])*.18, sy=v[1]+(prev[1]-v[1])*.18;
                    double ex=v[0]+(next[0]-v[0])*.18, ey=v[1]+(next[1]-v[1])*.18;
                    if(i==0)star.moveTo(sx,sy);else star.lineTo(sx,sy);
                    star.quadTo(v[0],v[1],ex,ey);
                }
                star.closePath();g.fill(star);
            } else {
                g.setStroke(new BasicStroke(1.7F,BasicStroke.CAP_ROUND,BasicStroke.JOIN_ROUND));
                for(int i=0;i<3;i++) {
                    g.draw(new Arc2D.Double(2.5,2.5,19,19,i*120+20,88,Arc2D.OPEN));
                    double a=Math.toRadians(i*120+108);
                    double x=12+9.5*Math.cos(a),y=12-9.5*Math.sin(a);
                    double tx=-Math.sin(a),ty=-Math.cos(a),nx=Math.cos(a),ny=-Math.sin(a);
                    Path2D arrow=new Path2D.Double();arrow.moveTo(x-tx*3+nx*2,y-ty*3+ny*2);
                    arrow.lineTo(x,y);arrow.lineTo(x-tx*3-nx*2,y-ty*3-ny*2);g.draw(arrow);
                }
                g.draw(new Ellipse2D.Double(7.5,7.5,9,9));
                g.fill(new Ellipse2D.Double(10.75,10.75,2.5,2.5));
            }
        } finally { g.dispose(); }
        return image;
    }
    public static void draw(String name,float x,float y,float size,int color) {
        cn.kazer.client189.compat.LegacyGL.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS);
        try {
            ResourceLocation texture=TEXTURES.get(name);
            if(texture==null) {
                texture=Minecraft.getMinecraft().getTextureManager().getDynamicTextureLocation(
                        "kazer-glyph-"+name.toLowerCase(java.util.Locale.ROOT),new DynamicTexture(mask(name)));
                TEXTURES.put(name,texture);
            }
            cn.kazer.client189.compat.LegacyGL.glEnable(GL11.GL_TEXTURE_2D);cn.kazer.client189.compat.LegacyGL.glDisable(GL11.GL_LIGHTING);
            cn.kazer.client189.compat.LegacyGL.glDisable(GL11.GL_CULL_FACE);cn.kazer.client189.compat.LegacyGL.glDisable(GL11.GL_DEPTH_TEST);
            cn.kazer.client189.compat.LegacyGL.glDepthMask(false);cn.kazer.client189.compat.LegacyGL.glDisable(GL11.GL_ALPHA_TEST);cn.kazer.client189.compat.LegacyGL.glEnable(GL11.GL_BLEND);
            cn.kazer.client189.compat.LegacyGL.glBlendFunc(GL11.GL_SRC_ALPHA,GL11.GL_ONE_MINUS_SRC_ALPHA);
            Minecraft.getMinecraft().getTextureManager().bindTexture(texture);
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D,GL11.GL_TEXTURE_MIN_FILTER,GL11.GL_LINEAR);
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D,GL11.GL_TEXTURE_MAG_FILTER,GL11.GL_LINEAR);
            cn.kazer.client189.compat.LegacyGL.glColor4f((color>>16&255)/255F,(color>>8&255)/255F,(color&255)/255F,(color>>>24)/255F);
            GL11.glBegin(GL11.GL_QUADS);
            GL11.glTexCoord2f(0,0);GL11.glVertex2f(x,y);
            GL11.glTexCoord2f(0,1);GL11.glVertex2f(x,y+size);
            GL11.glTexCoord2f(1,1);GL11.glVertex2f(x+size,y+size);
            GL11.glTexCoord2f(1,0);GL11.glVertex2f(x+size,y);
            GL11.glEnd();
        } finally { cn.kazer.client189.compat.LegacyGL.glPopAttrib(); }
    }
}
