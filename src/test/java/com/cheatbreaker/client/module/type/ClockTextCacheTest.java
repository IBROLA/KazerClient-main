package com.cheatbreaker.client.module.type;
import org.junit.Test;
import java.time.*;
import static org.junit.Assert.*;
public class ClockTextCacheTest {
    @Test public void formattingOptionsInvalidateCacheImmediately() {
        ClockText clock=new ClockText(Clock.fixed(Instant.parse("2026-10-08T05:06:07Z"),ZoneId.of("Asia/Shanghai")));
        String first=clock.format(true,true);
        assertEquals("13:06:07",first);
        for(int i=0;i<10000;i++)assertSame(first,clock.format(true,true));
        assertEquals("01:06:07 PM",clock.format(false,true));
        assertEquals("13:06",clock.format(true,false));
    }
}
