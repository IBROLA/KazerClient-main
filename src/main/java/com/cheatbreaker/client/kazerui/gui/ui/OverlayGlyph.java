package com.cheatbreaker.client.kazerui.gui.ui;
import java.awt.*;
import java.awt.geom.*;
import java.awt.image.BufferedImage;
/** Owned vector geometry; rasterized once per menu icon size, never in a game render pass. */
public final class OverlayGlyph {
    public static BufferedImage image(int size) {
        BufferedImage image=new BufferedImage(size,size,BufferedImage.TYPE_INT_ARGB);
        Graphics2D g=image.createGraphics();g.scale(size/52d,size/52d);
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
        g.setColor(new Color(0xFFE2E4DE,true));
        for(int layer=2;layer>=0;layer--) {
            float cy=13+layer*11;
            Path2D.Float shape=new Path2D.Float();shape.moveTo(5,cy);shape.lineTo(26,cy-10);
            shape.lineTo(47,cy);shape.quadTo(50,cy+2,47,cy+4);shape.lineTo(26,cy+14);shape.lineTo(5,cy+4);shape.quadTo(2,cy+2,5,cy);
            g.fill(shape);
            if(layer>0){g.setComposite(AlphaComposite.Clear);g.fill(new Rectangle2D.Float(0,cy-15,52,5));g.setComposite(AlphaComposite.SrcOver);}
        }
        g.dispose();return image;
    }
    private OverlayGlyph(){}
}
