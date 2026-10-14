package com.cheatbreaker.client.kazerui.module.modules.visual;
/** Cold meshes warm gradually; cached render calls do not consult this budget. */
final class SkinBuildBudget {
    private boolean started;private long start;private int used;
    boolean acquire(long now){
        if(!started || now-start>=50000000L){started=true;start=now;used=0;}
        if(used>=2)return false;used++;return true;
    }
    void reset(){started=false;used=0;}
}
