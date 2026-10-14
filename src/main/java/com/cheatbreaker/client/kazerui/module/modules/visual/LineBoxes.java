package com.cheatbreaker.client.kazerui.module.modules.visual;
import org.lwjgl.opengl.GL11;
/** Immediate primitive bounds avoid temporary vertex arrays and bounding boxes. */
final class LineBoxes {
    static void color(int c){GL11.glColor4f((c>>16&255)/255f,(c>>8&255)/255f,(c&255)/255f,(c>>>24)/255f);}
    static void outline(double x0,double y0,double z0,double x1,double y1,double z1){
        GL11.glBegin(GL11.GL_LINES);
        line(x0,y0,z0,x1,y0,z0);line(x1,y0,z0,x1,y0,z1);line(x1,y0,z1,x0,y0,z1);line(x0,y0,z1,x0,y0,z0);
        line(x0,y1,z0,x1,y1,z0);line(x1,y1,z0,x1,y1,z1);line(x1,y1,z1,x0,y1,z1);line(x0,y1,z1,x0,y1,z0);
        line(x0,y0,z0,x0,y1,z0);line(x1,y0,z0,x1,y1,z0);line(x1,y0,z1,x1,y1,z1);line(x0,y0,z1,x0,y1,z1);GL11.glEnd();
    }
    private static void line(double x,double y,double z,double a,double b,double c){GL11.glVertex3d(x,y,z);GL11.glVertex3d(a,b,c);}
    static void fill(double x,double y,double z,double a,double b,double c){GL11.glBegin(GL11.GL_QUADS);
        GL11.glVertex3d(x,y,z);GL11.glVertex3d(a,y,z);GL11.glVertex3d(a,b,z);GL11.glVertex3d(x,b,z);
        GL11.glVertex3d(a,y,c);GL11.glVertex3d(x,y,c);GL11.glVertex3d(x,b,c);GL11.glVertex3d(a,b,c);
        GL11.glVertex3d(x,y,c);GL11.glVertex3d(x,y,z);GL11.glVertex3d(x,b,z);GL11.glVertex3d(x,b,c);
        GL11.glVertex3d(a,y,z);GL11.glVertex3d(a,y,c);GL11.glVertex3d(a,b,c);GL11.glVertex3d(a,b,z);
        GL11.glVertex3d(x,b,z);GL11.glVertex3d(a,b,z);GL11.glVertex3d(a,b,c);GL11.glVertex3d(x,b,c);
        GL11.glVertex3d(x,y,c);GL11.glVertex3d(a,y,c);GL11.glVertex3d(a,y,z);GL11.glVertex3d(x,y,z);GL11.glEnd();
    }
    private LineBoxes(){}
}
