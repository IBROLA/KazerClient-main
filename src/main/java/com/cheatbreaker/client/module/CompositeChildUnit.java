package com.cheatbreaker.client.module;

/**
 * Marker for the per-child editor/settings units a {@link CompositeHudModule} expands into.
 * The settings back button uses {@link #getCompositeParent()} to step back to the composite
 * module's chooser page instead of all the way back to the module list.
 */
public interface CompositeChildUnit {
    AbstractModule getCompositeParent();
}
