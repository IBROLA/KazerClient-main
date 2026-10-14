package com.cheatbreaker.client.kazerui;

import com.cheatbreaker.client.kazerui.gui.ui.*;
import com.cheatbreaker.client.kazerui.module.Module;
import com.cheatbreaker.client.kazerui.module.ModuleManager;
import com.cheatbreaker.client.kazerui.module.modules.hud.*;
import com.cheatbreaker.client.kazerui.module.modules.visual.*;
import com.cheatbreaker.client.kazerui.module.modules.mechanic.ModuleWeatherChanger;
import java.lang.reflect.Field;
import java.util.*;
import org.junit.Test;
import static org.junit.Assert.*;

public class SevenModulesTest {
    private static volatile double blackhole;
    @Test public void steadyVisualHelpersAndBoundedLightSweepDoNotAllocatePerParticle() throws Exception {
        Field field = ModuleManager.class.getDeclaredField("model"); field.setAccessible(true); Object old = field.get(null);
        try {
            UiModel model = new UiModel(new OwnedUiContractTest.Store()); field.set(null,model);
            new ModuleParticleChanger();new ModuleGlintColorizer();new ModuleWeatherChanger();
            model.set("particle_changer","enabled","true");model.set("particle_changer","particleAmount","50");
            model.set("particle_changer","overrideParticleColor","true");
            model.set("glint_colorizer","enabled","true");model.set("weather_changer","enabled","true");
            Object particle = new Object();
            com.sun.management.ThreadMXBean bean=(com.sun.management.ThreadMXBean)java.lang.management.ManagementFactory.getThreadMXBean();
            assertTrue(bean.isThreadAllocatedMemorySupported());bean.setThreadAllocatedMemoryEnabled(true);
            for(int i=0;i<100000;i++)visualHelpers(particle);
            long thread=Thread.currentThread().getId(), before=bean.getThreadAllocatedBytes(thread);
            long start=System.nanoTime();for(int i=0;i<100000;i++)visualHelpers(particle);
            long elapsed=System.nanoTime()-start, allocated=bean.getThreadAllocatedBytes(thread)-before;
            // Allow VM bookkeeping, but not an object/buffer per particle.
            assertTrue("Steady particle/glint/weather helper allocation: "+allocated,allocated<=4096);
            LightOverlayCache cache=new LightOverlayCache();Object world=new Object();
            LightOverlayCache.Reader reader=(x,y,z,sky)->y==64?7:-1;
            for(int i=0;i<10000;i++)cache.update(world,0,64,0,8,true,reader);
            before=bean.getThreadAllocatedBytes(thread);
            for(int i=0;i<10000;i++)cache.update(world,0,64,0,8,true,reader);
            long scanBytes=bean.getThreadAllocatedBytes(thread)-before;
            assertTrue("Light sweeps allocated per checked block: "+scanBytes,scanBytes<=4096);
            System.out.println("SEVEN_HELPER_ALLOCATIONS calls=100000 bytes="+allocated+" nanos="+elapsed+" lightTicks=10000 lightBytes="+scanBytes+" budget=128");
        }finally{field.set(null,old);}
    }
    private static void visualHelpers(Object particle) {
        blackhole=ModuleParticleChanger.scale(.1f,particle)+ModuleParticleChanger.channel(.2f,particle,16)
            +(ModuleParticleChanger.keep(particle)?1:0)+ModuleGlintColorizer.itemColor(123)
            +ModuleWeatherChanger.strength(.4f)+ModuleWeatherChanger.temperature(.5f);
    }
    @Test public void stopwatchMonotonicPauseResetAndFormatting() {
        StopwatchClock clock = new StopwatchClock();
        assertEquals(0, clock.elapsed(10));
        clock.toggle(100);
        assertEquals(0, clock.elapsed(50));
        assertEquals(125, clock.elapsed(225));
        clock.pause(225); assertFalse(clock.running());
        assertEquals(125, clock.elapsed(10000));
        clock.toggle(20000); assertEquals(250, clock.elapsed(20125));
        clock.reset(30000); assertFalse(clock.running()); assertEquals(0, clock.elapsed(40000));
        assertEquals("00:00.0", StopwatchClock.format(-1, true));
        assertEquals("01:01.2", StopwatchClock.format(61234, true));
        assertEquals("1:00:01", StopwatchClock.format(3601000, false));
    }
    @Test public void lightQueriesAreBoundedDoubleBufferedAndWorldScoped() {
        LightOverlayCache cache = new LightOverlayCache();
        Object world = new Object();
        int[] queried = {0};
        LightOverlayCache.Reader reader = (x,y,z,sky) -> { queried[0]++; return y == 64 ? 7 : -1; };
        cache.update(world, 0, 64, 0, 16, true, reader);
        assertEquals(0, cache.count());
        assertEquals(LightOverlayCache.TICK_BUDGET, queried[0]);
        for (int tick = 0; tick < 80; tick++) {
            queried[0] = 0; cache.update(world, 0, 64, 0, 16, true, reader);
            assertTrue(queried[0] <= LightOverlayCache.TICK_BUDGET);
        }
        assertEquals(1089, cache.count()); assertEquals(7, cache.light(0));
        int publishedX = cache.x(0);
        cache.update(world, 9, 64, 0, 16, true, reader);
        assertEquals(publishedX, cache.x(0)); // Partial sweeps never replace the visible buffer.
        cache.update(new Object(), 9, 64, 0, 16, true, reader);
        assertEquals(0, cache.count()); // No old-world markers.
        cache.update(null, 0, 0, 0, 16, true, reader); assertEquals(0, cache.count());
    }
    @Test public void tooltipOffsetsStayWithinActualOverflow() {
        assertEquals(0, ModuleScrollableTooltips.clampOffset(-10, 100), 0);
        assertEquals(100, ModuleScrollableTooltips.clampOffset(130, 100), 0);
        assertEquals(20, ModuleScrollableTooltips.clampOffset(20, 100), 0);
        assertEquals(0, ModuleScrollableTooltips.clampOffset(20, -1), 0);
    }
    @Test public void modulesDefaultOffHaveRealLabelsAndComposeWithoutConfigMutations() throws Exception {
        Field field = ModuleManager.class.getDeclaredField("model"); field.setAccessible(true); Object old = field.get(null);
        try {
            UiModel model = new UiModel(new OwnedUiContractTest.Store()); field.set(null, model);
            Module[] modules = {new ModuleParticleChanger(), new ModuleGlintColorizer(), new ModuleScrollableTooltips(),
                new ModuleStopwatch(), new ModuleItemPhysics(), new ModuleLightOverlay(), new ModuleWeatherChanger()};
            for (Module m : modules) {
                assertFalse(m.id, m.defaultEnabled()); assertFalse(m.id, m.isEnabled());
                Set<String> keys = new HashSet<String>();
                for (OptionCatalog.Node n : OptionCatalog.flatten(m.page().options)) {
                    assertTrue(m.id + ":" + n.key, keys.add(n.key));
                    // Standard HUD style labels come from the pre-existing language catalog.
                    if (!m.id.equals("STOPWATCH") || Arrays.asList("timerMode","showTenths","pauseInMenu","countdownSeconds","startPauseKey","resetKey","startPause","resetTimer").contains(n.key))
                        assertNotNull(m.id + ":" + n.key, n.text);
                }
            }
            assertEquals(.4f, ModuleWeatherChanger.strength(.4f), 0);
            model.set("weather_changer","enabled","true");
            assertEquals(0, ModuleWeatherChanger.strength(.4f), 0);
            model.set("weather_changer","weatherMode","SNOW");
            assertEquals(1, ModuleWeatherChanger.strength(0), 0);
            assertEquals(0, ModuleWeatherChanger.temperature(.7f), 0);
            assertTrue(ModuleWeatherChanger.precipitation(false));
            model.set("weather_changer","weatherMode","NATURAL"); assertEquals(.3f, ModuleWeatherChanger.strength(.3f), 0);
            assertTrue(ModuleParticleChanger.keep(new Object()));
            model.set("particle_changer","enabled","true"); model.set("particle_changer","particleAmount","25");
            Object particle = new Object(); int kept=0; for(int i=0;i<100;i++) if(ModuleParticleChanger.keep(particle)) kept++;
            assertEquals(25, kept);
            model.set("particle_changer","particleSize","2"); assertEquals(.2f, ModuleParticleChanger.scale(.1f,particle),0);
            model.set("glint_colorizer","enabled","true"); model.set("glint_colorizer","itemColor","FFFF0000");
            assertEquals(0xFFFF0000,ModuleGlintColorizer.itemColor(0));
            model.set("glint_colorizer","showGlint","false"); assertFalse(ModuleGlintColorizer.visible(true));
            assertEquals(123,ModuleGlintColorizer.itemColor(123));
            model.set("2d_items","enabled","true"); model.set("item_physics","enabled","true");
            assertTrue(ModuleItemPhysics.active()); assertTrue(model.flag("2d_items","enabled",false));
            model.set("item_physics","enabled","false"); assertTrue(model.flag("2d_items","enabled",false));
            model.action("profile:Other");
            assertFalse(ModuleItemPhysics.active()); assertFalse(modules[0].isEnabled()); assertEquals(.1f,ModuleParticleChanger.scale(.1f,particle),0);
        } finally { field.set(null, old); }
    }
}
