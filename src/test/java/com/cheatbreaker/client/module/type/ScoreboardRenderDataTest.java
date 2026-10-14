package com.cheatbreaker.client.module.type;

import org.junit.Test;
import java.util.concurrent.atomic.AtomicInteger;
import static org.junit.Assert.*;

public class ScoreboardRenderDataTest {
    @Test public void preservesLegacyPaddedWidthAndTitlePosition() {
        ScoreboardRenderData data = new ScoreboardRenderData("Title",
                new String[]{"Alice","Bob"}, new String[]{"10","2"}, String::length);
        assertEquals(9, data.width);
        assertEquals(21, data.right);
        assertEquals(5, data.titleWidth);
        assertArrayEquals(new int[]{2,1}, data.numberWidths);
        assertEquals("Alice",data.names[0]);
    }
    @Test public void oneSnapshotMeasuresOnlyOnceAndReplacementUpdatesData() {
        AtomicInteger calls = new AtomicInteger();
        ScoreboardRenderData first = new ScoreboardRenderData("HP",
                new String[]{"CANRX"},new String[]{"20"},text -> {calls.incrementAndGet();return text.length();});
        assertEquals(3,calls.get());
        for(int frame=0;frame<10000;frame++)assertEquals(9,first.width);
        assertEquals(3,calls.get());
        ScoreboardRenderData next = new ScoreboardRenderData("Health",
                new String[]{"[Mod Mode] CANRX"},new String[]{"19"},String::length);
        assertEquals("19",next.numbers[0]);assertTrue(next.width>first.width);
    }
    @Test public void emptyRowsRetainTitleGeometryWithoutInvalidArrayAccess() {
        ScoreboardRenderData empty = new ScoreboardRenderData("Title",new String[0],new String[0],String::length);
        assertEquals(21,empty.right);assertEquals(0,empty.numberWidths.length);
    }
}
