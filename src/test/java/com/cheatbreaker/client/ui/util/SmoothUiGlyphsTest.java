package com.cheatbreaker.client.ui.util;
import org.junit.Test;
import java.awt.image.BufferedImage;
import static org.junit.Assert.*;
public class SmoothUiGlyphsTest {
    @Test public void glyphsHaveAntialiasedEdgesAndTransparentBackground(){
        for(String name:new String[]{"CHAT","VISUALS"}){
            BufferedImage image=SmoothUiGlyphs.mask(name);
            int partial=0,opaque=0;
            for(int y=0;y<128;y++)for(int x=0;x<128;x++){
                int a=image.getRGB(x,y)>>>24;if(a>0&&a<255)partial++;if(a==255)opaque++;
            }
            assertTrue(name,partial>100);assertTrue(name,opaque>100);
            assertEquals(0,image.getRGB(0,0)>>>24);
        }
    }
    @Test public void chatDotsAreTransparentNotPaintedGray(){
        BufferedImage image=SmoothUiGlyphs.mask("CHAT");
        for(int i=0;i<3;i++)assertEquals(0,image.getRGB((int)((6.75+i*5)*128/24),(int)(10.25*128/24))>>>24);
    }
}

