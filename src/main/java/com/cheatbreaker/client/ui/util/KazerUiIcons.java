package com.cheatbreaker.client.ui.util;

import org.lwjgl.opengl.GL11;

/** Resolution-independent UI glyphs; no bitmap enlargement or font fallback. */
public final class KazerUiIcons {
    public enum Icon { SEARCH, CHAT, STAR, LINK, PLUS, GEAR, DOWN, VISUALS }
    private KazerUiIcons() {}

    public static void draw(Icon icon, float x, float y, float size, int color) {
        if (icon == Icon.CHAT || icon == Icon.VISUALS || icon == Icon.STAR) {
            SmoothUiGlyphs.draw(icon.name(),x,y,size,color);
            return;
        }
        cn.kazer.client189.compat.LegacyGL.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS);
        GL11.glPushMatrix();
        try {
            GL11.glTranslatef(x, y, 0);
            GL11.glScalef(size / 24F, size / 24F, 1);
            cn.kazer.client189.compat.LegacyGL.glDisable(GL11.GL_TEXTURE_2D);
            cn.kazer.client189.compat.LegacyGL.glDisable(GL11.GL_CULL_FACE);
            cn.kazer.client189.compat.LegacyGL.glDisable(GL11.GL_DEPTH_TEST);
            cn.kazer.client189.compat.LegacyGL.glDepthMask(false);
            cn.kazer.client189.compat.LegacyGL.glDisable(GL11.GL_ALPHA_TEST);
            cn.kazer.client189.compat.LegacyGL.glEnable(GL11.GL_BLEND);
            cn.kazer.client189.compat.LegacyGL.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
            cn.kazer.client189.compat.LegacyGL.glColor4f((color >> 16 & 255) / 255F, (color >> 8 & 255) / 255F,
                    (color & 255) / 255F, (color >>> 24) / 255F);
            switch (icon) {
                case VISUALS:
                    ring(12,12,10,1.6F);
                    for(int i=0;i<3;i++){
                        double a=i*Math.PI*2/3;
                        float ax=12+(float)Math.cos(a)*10,ay=12+(float)Math.sin(a)*10;
                        stroke(ax,ay,ax-4*(float)Math.cos(a-.7),ay-4*(float)Math.sin(a-.7),2);
                    }
                    ring(12,12,4,1.6F);circle(12,12,1.3F);
                    stroke(5,12,9,9,1.4F);stroke(5,12,9,15,1.4F);
                    stroke(19,12,15,9,1.4F);stroke(19,12,15,15,1.4F);
                    break;
                case SEARCH:
                    ring(9.5F, 9.5F, 7.1F, 2.4F);
                    stroke(15, 15, 21, 21, 2.7F);
                    break;
                case CHAT:
                    rounded(new float[][] {{2,3},{22,3},{22,18},{9,18},{4,22},{4,18},{2,18}}, .7F);
                    // Cut-outs use the card's shade, keeping dots sharp at every GUI scale.
                    cn.kazer.client189.compat.LegacyGL.glColor4f(.25F,.27F,.25F,1);
                    for (int i = 0; i < 3; i++) circle(7 + i * 5, 10.5F, 1.2F);
                    break;
                case STAR:
                    float[][] points = new float[10][2];
                    for (int i = 0; i < 10; i++) {
                        double a = -Math.PI / 2 + i * Math.PI / 5;
                        float radius = i % 2 == 0 ? 10 : 4.6F;
                        points[i][0] = 12 + (float)Math.cos(a) * radius;
                        points[i][1] = 12 + (float)Math.sin(a) * radius;
                    }
                    rounded(points, .8F);
                    break;
                case PLUS: stroke(5,12,19,12,3); stroke(12,5,12,19,3); break;
                case DOWN: stroke(6,9,12,15,2); stroke(12,15,18,9,2); break;
                case LINK:
                    ring(8,15,5,2); ring(16,7,5,2); stroke(8,15,16,7,2); break;
                case GEAR:
                    ring(12,12,6,3);
                    for (int i=0;i<8;i++) {
                        double a=i*Math.PI/4;
                        stroke(12+(float)Math.cos(a)*6,12+(float)Math.sin(a)*6,
                                12+(float)Math.cos(a)*9,12+(float)Math.sin(a)*9,3);
                    }
            }
        } finally { GL11.glPopMatrix(); cn.kazer.client189.compat.LegacyGL.glPopAttrib(); }
    }

    private static void circle(float x, float y, float r) {
        GL11.glBegin(GL11.GL_TRIANGLE_FAN);
        GL11.glVertex2f(x,y);
        for (int i=0;i<=48;i++) GL11.glVertex2d(x+Math.cos(i*Math.PI/24)*r,y+Math.sin(i*Math.PI/24)*r);
        GL11.glEnd();
    }
    private static void ring(float x,float y,float r,float w) {
        GL11.glBegin(GL11.GL_QUAD_STRIP);
        for(int i=0;i<=64;i++) {
            double a=i*Math.PI/32;
            GL11.glVertex2d(x+Math.cos(a)*r,y+Math.sin(a)*r);
            GL11.glVertex2d(x+Math.cos(a)*(r-w),y+Math.sin(a)*(r-w));
        }
        GL11.glEnd();
    }
    private static void stroke(float x,float y,float x2,float y2,float w) {
        double length=Math.hypot(x2-x,y2-y);
        float dx=(float)((y2-y)/length*w/2),dy=(float)((x2-x)/length*w/2);
        GL11.glBegin(GL11.GL_QUADS);
        GL11.glVertex2f(x-dx,y+dy); GL11.glVertex2f(x2-dx,y2+dy);
        GL11.glVertex2f(x2+dx,y2-dy); GL11.glVertex2f(x+dx,y-dy);
        GL11.glEnd(); circle(x,y,w/2); circle(x2,y2,w/2);
    }
    private static void rounded(float[][] p,float trim) {
        GL11.glBegin(GL11.GL_TRIANGLE_FAN);
        GL11.glVertex2f(12,12);
        for(int i=0;i<=p.length;i++) {
            float[] prev=p[(i+p.length-1)%p.length],v=p[i%p.length],next=p[(i+1)%p.length];
            float a=(float)Math.hypot(prev[0]-v[0],prev[1]-v[1]);
            float b=(float)Math.hypot(next[0]-v[0],next[1]-v[1]);
            float sx=v[0]+(prev[0]-v[0])*trim/a,sy=v[1]+(prev[1]-v[1])*trim/a;
            float ex=v[0]+(next[0]-v[0])*trim/b,ey=v[1]+(next[1]-v[1])*trim/b;
            for(int j=0;j<=6;j++) {
                float t=j/6F,u=1-t;
                GL11.glVertex2f(u*u*sx+2*u*t*v[0]+t*t*ex,u*u*sy+2*u*t*v[1]+t*t*ey);
            }
        }
        GL11.glEnd();
    }
}
