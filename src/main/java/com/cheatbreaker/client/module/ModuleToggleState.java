package com.cheatbreaker.client.module;
/** Non-HUD modules must toggle by enabled state, not the unused HUD flag. */
public final class ModuleToggleState {
    private ModuleToggleState() {}
    public static boolean next(boolean enabled, boolean hasHud, boolean hudVisible) {
        return !enabled || (hasHud && !hudVisible);
    }
}
