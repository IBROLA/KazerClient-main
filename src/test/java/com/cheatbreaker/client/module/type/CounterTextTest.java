package com.cheatbreaker.client.module.type;
import org.junit.Test;
import static org.junit.Assert.*;
public class CounterTextTest {
    @Test public void changesOnlyWhenVisibleValueOrFormatChanges() {
        CounterText text=new CounterText("FPS");
        String first=text.text(144,true,false,true);
        assertEquals("144 FPS",first);
        for(int i=0;i<10000;i++)assertSame(first,text.text(144,true,false,true));
        assertEquals("[144 FPS]",text.text(144,false,false,true));
        assertEquals("FPS 144",text.text(144,true,true,true));
        assertEquals("145 FPS",text.text(145,true,false,true));
    }
    @Test public void cpsOptionsMatchOriginalSpacing() {
        CounterText text=new CounterText("CPS");
        assertEquals("9 CPS",text.text(9,true,false,true));
        assertEquals("[CPS 9]",text.text(9,false,true,true));
        assertEquals("[0]",text.text(0,false,true,false));
        assertEquals("12",text.text(12,true,false,false));
    }
}
