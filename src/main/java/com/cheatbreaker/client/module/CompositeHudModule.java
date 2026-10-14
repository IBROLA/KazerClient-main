package com.cheatbreaker.client.module;

import java.util.List;

/**
 * A Module that exposes more than one independently placeable HUD unit to the RShift editor.
 * The card still appears once in the module list (driven by {@link AbstractModule}); this
 * interface lets {@code CBModulesGui} expand the module into its child units only for the
 * HUD drag canvas, so each Child HUD can be dragged, scaled and closed independently.
 */
public interface CompositeHudModule {
    List<AbstractModule> getChildHudUnits();
}
