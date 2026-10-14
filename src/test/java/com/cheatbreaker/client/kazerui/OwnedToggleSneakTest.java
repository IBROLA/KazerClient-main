package com.cheatbreaker.client.kazerui;

import com.cheatbreaker.client.kazerui.module.modules.mechanic.ModuleToggleSneak;
import com.cheatbreaker.client.kazerui.config.LegacyProfileMigration;
import java.io.StringReader;
import java.util.Arrays;
import org.junit.Test;
import static org.junit.Assert.*;

public class OwnedToggleSneakTest {
    @Test public void fullLegacyFlightLabelDoesNotNestSourceHudBrackets() throws Exception {
        OwnedUiContractTest.Store store=new OwnedUiContractTest.Store();
        LegacyProfileMigration.readProfile(new StringReader("[ToggleSprint]\nFly String=[My Flight]\n"),"PvP",store);
        LegacyProfileMigration.finishReplacements(store,Arrays.asList("PvP"));
        assertEquals("My Flight",store.get("profile.PvP.toggle_sneak_hud_child.flyingText",null));
        assertEquals("[My Flight]",store.get("legacy.profile.PvP.ToggleSprint.Fly String",null));
        assertEquals("No brackets",LegacyProfileMigration.unbracket("No brackets"));
    }
    @Test public void shortReleaseDoesNotToggleAfterMenuResetOrLongHold() {
        assertTrue(ModuleToggleSneak.shortPress(true,1000,1400,400));
        assertFalse(ModuleToggleSneak.shortPress(true,1000,1401,400));
        assertTrue(ModuleToggleSneak.shortPress(true,1000,1200,200));
        assertFalse(ModuleToggleSneak.shortPress(true,1000,1201,200));
        assertFalse(ModuleToggleSneak.shortPress(false,1000,1100,400));
        assertFalse(ModuleToggleSneak.shortPress(true,1000,900,400));
    }
    @Test public void restoresSourceHudAndOptionsForAlreadyImportedProfilesOnce() throws Exception {
        OwnedUiContractTest.Store store=new OwnedUiContractTest.Store();
        LegacyProfileMigration.readProfile(new StringReader("[ToggleSprint]\nToggle Sprint=false\nToggle Sneak=true\nDouble Tap=false\nFly Boost Amount=8\nShow HUD Text=false\n-xTranslation=12\n-yTranslation=8\n-Anchor=RIGHT_TOP\nText Color=-1\n"),"PvP",store);
        store.put("migration.legacy.v1","true");
        LegacyProfileMigration.finishReplacements(store,Arrays.asList("PvP"));
        assertEquals("false",store.get("profile.PvP.toggle_sneak.toggleSprint",null));
        assertEquals("true",store.get("profile.PvP.toggle_sneak.toggleSneak",null));
        assertEquals("false",store.get("profile.PvP.toggle_sneak.doubleTap",null));
        assertEquals("8",store.get("profile.PvP.toggle_sneak.flyBoostAmount",null));
        assertEquals("false",store.get("profile.PvP.toggle_sneak_hud_child.enabled",null));
        assertEquals("12",store.get("profile.PvP.toggle_sneak_hud_child.x",null));
        assertEquals("topRight",store.get("profile.PvP.toggle_sneak_hud_child.anchor",null));
        assertEquals("FFFFFFFF",store.get("profile.PvP.toggle_sneak_hud_child.textColor",null));
        store.remove("profile.PvP.toggle_sneak.doubleTap");
        LegacyProfileMigration.finishReplacements(store,Arrays.asList("PvP"));
        assertNull("Profile reset must not re-seed stale values",store.get("profile.PvP.toggle_sneak.doubleTap",null));
    }
}
