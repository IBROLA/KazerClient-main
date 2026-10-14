package com.cheatbreaker.client.kazerui.module.modules.mechanic;
import org.junit.Test;
import static org.junit.Assert.*;

public class OwnedChatGeometryTest {
    @Test public void inverseCoordinatesRemainStableAtAllGuiAndHudScales() {
        for (int guiScale = 1; guiScale <= 4; guiScale++) for (float hudScale : new float[]{.75f,1,1.25f}) {
            float origin = 82, chatScale = .8f, combined = hudScale * chatScale;
            float display = (origin + 8.25f * combined) * guiScale;
            assertEquals(8, ChatGeometry.coordinate(display,guiScale,origin,combined));
            assertEquals(-1, ChatGeometry.coordinate((origin-.1f)*guiScale,guiScale,origin,combined));
        }
    }
    @Test public void rowBoundsScrollAndHeadGutterCannotSelectAnotherLine() {
        assertEquals(4,ChatGeometry.row(12,8,324,20,4,50));
        assertEquals(5,ChatGeometry.row(12,9,324,20,4,50));
        assertEquals(-1,ChatGeometry.row(-1,8,324,20,0,50));
        assertEquals(-1,ChatGeometry.row(324,8,324,20,0,50));
        assertEquals(-1,ChatGeometry.row(12,180,324,20,0,50));
        assertEquals(-1,ChatGeometry.row(12,9,324,20,49,50));
    }
    @Test public void anchorsClampWithoutResettingTheSavedOffset() {
        assertEquals(82,ChatGeometry.origin(80,2,530,324),0);
        assertEquals(206,ChatGeometry.origin(999,2,530,324),0);
        assertEquals(0,ChatGeometry.origin(-999,2,530,324),0);
    }
    @Test public void historyTrimIsBoundedAndKeepsNewestMessageIdentity() {
        java.util.List<Object> messages=new java.util.ArrayList<Object>();
        Object newest=new Object();messages.add(newest);
        for(int i=0;i<1200;i++)messages.add(new Object());
        ChatRuntime.trim(messages,1000);assertEquals(1000,messages.size());assertSame(newest,messages.get(0));
        ChatRuntime.trim(messages,100);assertEquals(100,messages.size());assertSame(newest,messages.get(0));
    }
}
