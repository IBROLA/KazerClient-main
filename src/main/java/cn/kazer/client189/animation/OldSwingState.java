/* SPDX-License-Identifier: LGPL-3.0-only
 * Adapted from Sk1erLLC/OldAnimations, AnimationHandler.java
 * commit ab10fa4879bf54a899bba777d7a0bb70c946e4fb. See META-INF/licenses/OldAnimations-NOTICE.md.
 * Modifications: isolate the original tick/interpolation algorithm for testing.
 */
package cn.kazer.client189.animation;

public final class OldSwingState {
    private float previous, progress;
    private int ticks;
    private boolean swinging;
    public void reset() { previous=progress=0; ticks=0; swinging=false; }
    public void tick(boolean punchingBlock, int duration) {
        previous=progress;
        if(punchingBlock && (!swinging || ticks >= duration >> 1 || ticks < 0)) {
            swinging=true; ticks=-1;
        }
        if(swinging) {
            ++ticks;
            if(ticks>=duration) { ticks=0; swinging=false; }
        } else ticks=0;
        progress=(float)ticks/(float)duration;
    }
    public float interpolate(float partial, float nativeSwing) {
        if(!swinging) return nativeSwing;
        float delta=progress-previous;
        if(delta<0) ++delta;
        return previous+delta*partial;
    }
    public boolean isSwinging() { return swinging; }
}
