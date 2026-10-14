package com.cheatbreaker.client.kazerui.module.modules.hud;
import net.minecraft.entity.Entity;
/** Observers only: never alter attack packets, reach checks or entity status. */
public final class CombatHud {
    private static ModuleCombo combo;
    private static ModuleReachDisplay reach;
    public static void initialize(ModuleCombo c,ModuleReachDisplay r){combo=c;reach=r;}
    public static void attack(Entity entity){if(combo!=null)combo.attack(entity);if(reach!=null)reach.attack(entity);ModulePvpInfo.attack(entity);}
    public static void status(Entity entity,byte status){if(combo!=null)combo.status(entity,status);if(reach!=null)reach.status(entity,status);ModulePvpInfo.status(entity,status);}
    private CombatHud(){}
}
