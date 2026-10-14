package com.cheatbreaker.client.kazerui.cosmetics.render;
import com.cheatbreaker.client.kazerui.cosmetics.gecko.Molang;
import java.io.*;
import java.nio.charset.StandardCharsets;
import org.junit.Test;
import static org.junit.Assert.*;

public class PetConstantsTest {
    private static String resource(String name)throws Exception {
        try(InputStream in=PetConstantsTest.class.getResourceAsStream("/assets/kazerui/cosmetics/"+name)) {
            ByteArrayOutputStream out=new ByteArrayOutputStream();byte[] buffer=new byte[4096];
            for(int n;(n=in.read(buffer))>0;)out.write(buffer,0,n);return new String(out.toByteArray(),StandardCharsets.UTF_8);
        }
    }
    @Test public void realConstantsCompileAndCountPausedAndDuplicateFramesOnlyOnce()throws Exception {
        Molang.loadFunctions(resource("functions.molang"));
        Molang.Expr constants=Molang.compile(GeckoCosmetics.petConstantSource(resource("constants.molang")));
        CosmeticAnimationClock clock=new CosmeticAnimationClock();Molang.Scope sc=new Molang.Scope();
        sc.queries=name->"life_time".equals(name)?clock.time():"is_on_ground".equals(name)?1:0;
        for(int i=0;i<=120;i++) {
            clock.sample(9000+i/120.0);sc.interpolationSeconds=clock.delta();
            if(clock.advanced())constants.eval(sc);
            for(int duplicate=0;duplicate<5;duplicate++) {
                clock.sample(9000+i/120.0);assertFalse(clock.advanced());
            }
        }
        assertEquals(1,sc.vars.get("context.afk_timer"),1e-8);
        assertEquals(1,sc.vars.get("context.last_frame"),1e-8);
        assertEquals(0,sc.vars.get("context.distance_moved"),0);
        assertEquals(20,sc.vars.get("context.current_tick"),1e-8);
    }
    @Test public void wrappedYawDoesNotCreateThreeHundredSixtyDegreePetOffsets()throws Exception {
        Molang.Expr constants=Molang.compile(GeckoCosmetics.petConstantSource(resource("constants.molang")));
        Molang.Scope sc=new Molang.Scope();sc.interpolationSeconds=1.0/60;
        sc.vars.put("context.entity_y_rotation_smoothed",179.0);
        sc.vars.put("context.last_head_y_rotation",179.0);
        sc.queries=name->"entity_y_rotation".equals(name)||"head_y_rotation".equals(name)?-179:0;
        constants.eval(sc);
        assertTrue(Math.abs(sc.vars.get("context.entity_y_rotation_smoothed_diff"))<2.01);
        assertEquals(-2,sc.vars.get("context.head_y_rotation_diff"),0);
    }
}
