package com.cheatbreaker.client.kazerui.cosmetics.render;
import org.junit.Test;
import static org.junit.Assert.*;
public class PetMotionSampleTest {
    @Test public void jumpVelocityMovesContinuouslyBetweenTicks() {
        PetMotionSample sample=new PetMotionSample();sample.update(1,0,0,0);sample.update(2,2,8,4);
        assertEquals(0,sample.y(0),0);assertEquals(4,sample.y(.5f),0);assertEquals(8,sample.y(1),0);
        assertEquals(1,sample.x(.5f),0);assertEquals(2,sample.z(.5f),0);
        sample.update(2,100,100,100);assertEquals(4,sample.y(.5f),0);
        sample.update(3,0,-2,0);assertEquals(8,sample.y(0),0);assertEquals(3,sample.y(.5f),0);
    }
    @Test public void discontinuitiesAndTeleportsResetRatherThanTrailAcrossWorld() {
        PetMotionSample sample=new PetMotionSample();sample.update(1,0,0,0);sample.update(8,2,4,6);
        assertEquals(4,sample.y(0),0);sample.update(9,0,200,0);assertEquals(200,sample.y(0),0);
    }
    @Test public void yawWrapUsesTheShortArc() {
        assertEquals(180,PetMotionSample.angle(179,-179,.5f),0);
        assertEquals(-180,PetMotionSample.angle(-179,179,.5f),0);
    }
}
