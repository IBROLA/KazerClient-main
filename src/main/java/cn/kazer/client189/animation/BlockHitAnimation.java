package cn.kazer.client189.animation;

/** Module switch only. Animation state is owned by the OldAnimations adapter. */
public final class BlockHitAnimation {
    private static volatile boolean enabled = true;
    private BlockHitAnimation() { }
    public static void setEnabled(boolean value) { enabled=value; if(!value) OldAnimationsAdapter.reset(); }
    public static boolean isEnabled() { return enabled; }
    static BlockHitModule module() {
        com.cheatbreaker.client.CheatBreaker core=com.cheatbreaker.client.CheatBreaker.getInstance();
        return core==null||core.moduleManager==null?null:core.moduleManager.blockHitAnimation;
    }
}
