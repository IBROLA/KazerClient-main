package com.cheatbreaker.client.kazerui.cosmetics.render;

/** Visual-only tick samples: no player state mutation and no frame allocations. */
public final class PetMotionSample {
    private int tick=Integer.MIN_VALUE;
    private double px,py,pz,x,y,z;
    public void update(int nextTick,double dx,double dy,double dz) {
        if(nextTick==tick)return;
        boolean reset=tick==Integer.MIN_VALUE || nextTick!=tick+1
            || Math.abs(dx)>160 || Math.abs(dy)>160 || Math.abs(dz)>160;
        px=reset?dx:x;py=reset?dy:y;pz=reset?dz:z;
        x=dx;y=dy;z=dz;tick=nextTick;
    }
    public double x(float partial){return lerp(px,x,partial);}
    public double y(float partial){return lerp(py,y,partial);}
    public double z(float partial){return lerp(pz,z,partial);}
    private static double lerp(double before,double after,float partial) {
        return before+(after-before)*Math.max(0,Math.min(1,partial));
    }
    public static float angle(float before,float after,float partial) {
        float difference=(after-before)%360;
        if(difference>=180)difference-=360;
        if(difference< -180)difference+=360;
        return before+difference*Math.max(0,Math.min(1,partial));
    }
}
