package com.cheatbreaker.client.kazerui;

import com.cheatbreaker.client.kazerui.config.LegacyProfileMigration;
import java.io.StringReader;
import org.junit.Test;
import static org.junit.Assert.*;

public class LegacyProfileMigrationTest {
    @Test public void replacementHudKeysPreserveOldOptionsIncludingChildrenEnumsAndRainbow() throws Exception {
        OwnedUiContractTest.Store store=new OwnedUiContractTest.Store();
        LegacyProfileMigration.readProfile(new StringReader("[CPS]\nRight Click CPS=true\nReverse Text=true\nIgnore Cancelled Clicks=true\n"
            +"[Scoreboard]\nHide Numbers=false\nHide Scoreboard=true\nBackground Color=-1;rainbow\n"
            +"[Boss bar]\nRender Text=false\nUse Custom Boss Bar=true\n"
            +"[Potion Effects]\nMode=Compact\nBlink=false\nShow Effect Name=false\nName Color=-1;rainbow\nDuration Color=-65536\n"
            +"[Key Strokes]\nMode=Separated\nShow spacebar=false\nText Color (Pressed)=-1;rainbow\n"
            +"[Armor Status]\nList Mode=horizontal\nDamage Display Type=percent\nShow Boots=false\nProtection=true\nArmor Bars=true\n"),"Default",store);
        store.put("profile.Default.cps.reverseText","false");
        LegacyProfileMigration.finishReplacements(store,java.util.Arrays.asList("Default"));
        assertEquals("true",store.get("profile.Default.cps.rightClick",null));
        assertEquals("false",store.get("profile.Default.cps.reverseText",null));
        assertEquals("true",store.get("profile.Default.cps.ignoreCancelledClicks",null));
        assertEquals("false",store.get("profile.Default.scoreboard.numbers",null));
        assertEquals("true",store.get("profile.Default.scoreboard.hideScoreboard",null));
        assertEquals("FFFFFFFF",store.get("profile.Default.scoreboard.headerColor",null));
        assertEquals("true",store.get("profile.Default.scoreboard.headerColorChroma",null));
        assertEquals("false",store.get("profile.Default.bossbar.renderBossText",null));
        assertEquals("true",store.get("profile.Default.bossbar.customBossBar",null));
        assertEquals("MINIMAL",store.get("profile.Default.potion_effects.potionEffectsMode",null));
        assertEquals("false",store.get("profile.Default.potion_effects.effectName",null));
        assertEquals("false",store.get("profile.Default.potion_effects.potionBlink",null));
        assertEquals("FFFFFFFF",store.get("profile.Default.potion_effects.textColor",null));
        assertEquals("true",store.get("profile.Default.potion_effects.textColorChroma",null));
        assertEquals("FFFF0000",store.get("profile.Default.potion_effects.infoColor",null));
        assertEquals("INDIVIDUAL",store.get("profile.Default.keystrokes.keystrokesMode",null));
        assertEquals("false",store.get("profile.Default.keystrokes.keyStrokesSpacebar",null));
        assertEquals("true",store.get("profile.Default.keystrokes.textPressedColorChroma",null));
        assertEquals("HORIZONTAL",store.get("profile.Default.armorstatus.listMode",null));
        assertEquals("PERCENT",store.get("profile.Default.armorstatus.damageDisplay",null));
        assertEquals("false",store.get("profile.Default.armorstatus_boots_child.enabled",null));
        assertEquals("true",store.get("profile.Default.armorstatus_protection_child.enabled",null));
        assertEquals("true",store.get("profile.Default.armorstatus_bars_child.enabled",null));
        store.remove("profile.Default.armorstatus_boots_child.enabled");
        LegacyProfileMigration.finishReplacements(store,java.util.Arrays.asList("Default"));
        assertNull(store.get("profile.Default.armorstatus_boots_child.enabled",null));
    }
    @Test public void chatReplacementMigratesNotificationsWithoutOverwritingEditsOrReimportingResets() throws Exception {
        OwnedUiContractTest.Store store=new OwnedUiContractTest.Store();
        LegacyProfileMigration.readProfile(new StringReader("[Chat]\nShow Background=false\nShow Copy Notifications=false\nShow Visibility Notifications=false\n"),"Default",store);
        store.put("migration.replacements.v2","true");store.put("migration.chat.v1","true");
        store.put("profile.Default.chat.showVisibilityNotifications","true");
        LegacyProfileMigration.finishReplacements(store,java.util.Arrays.asList("Default"));
        assertEquals("false",store.get("profile.Default.chat.showBackground",null));
        assertEquals("false",store.get("profile.Default.chat.showCopyNotifications",null));
        assertEquals("true",store.get("profile.Default.chat.showVisibilityNotifications",null));
        assertEquals("true",store.get("profile.Default.chat.copyChat",null));
        assertEquals("false",store.get("profile.Default.chat.copyChatRightClick",null));
        assertEquals("false",store.get("legacy.profile.Default.Chat.Show Copy Notifications",null));
        store.remove("profile.Default.chat.showCopyNotifications");
        LegacyProfileMigration.finishReplacements(store,java.util.Arrays.asList("Default"));
        assertNull(store.get("profile.Default.chat.showCopyNotifications",null));
    }
    @Test public void convertsAnchorOffsetsOnceRegardlessOfFieldOrder() throws Exception {
        OwnedUiContractTest.Store store=new OwnedUiContractTest.Store();
        String text="[FPS]\n-Anchor=MIDDLE_BOTTOM_LEFT\n-xTranslation=12\n-yTranslation=4\n[CPS]\n-xTranslation=12\n-Anchor=MIDDLE_BOTTOM_RIGHT\n-yTranslation=4\n[Scoreboard]\n-xTranslation=1\n-yTranslation=3\n-Anchor=RIGHT_MIDDLE\n[Potion Effects]\n-xTranslation=1\n-yTranslation=3\n-Anchor=RIGHT_BOTTOM\n";
        LegacyProfileMigration.readProfile(new StringReader(text),"Default",store);
        assertEquals("-18.0",store.get("profile.Default.fps.x",null));
        assertEquals("42.0",store.get("profile.Default.cps.x",null));
        assertEquals("3.0",store.get("profile.Default.scoreboard.x",null));
        assertEquals("3.0",store.get("profile.Default.potion_effects.x",null));
        assertEquals("5.0",store.get("profile.Default.potion_effects.y",null));
        LegacyProfileMigration.readProfile(new StringReader(text),"Default",store);
        assertEquals("-18.0",store.get("profile.Default.fps.x",null));
        assertEquals("42.0",store.get("profile.Default.cps.x",null));
    }
    @Test public void importsSiblingProfilesReadOnlyAndDoesNotOverwriteNewEdits() throws Exception {
        java.nio.file.Path base = java.nio.file.Paths.get(".target/owned-ui-verification");
        java.nio.file.Files.createDirectories(base);
        java.nio.file.Path directory = java.nio.file.Files.createTempDirectory(base,"legacy-directory-");
        java.nio.file.Path profiles=java.nio.file.Files.createDirectories(directory.resolve("profiles"));
        byte[] global="ActiveProfile=PvP\nFavoriteModules=Nametags\n".getBytes(java.nio.charset.StandardCharsets.UTF_8);
        byte[] profile="[Nametags]\n-State=true\nNametag Opacity=0.75\n[1.7 Visuals]\n-State=false\n".getBytes(java.nio.charset.StandardCharsets.UTF_8);
        java.nio.file.Files.write(directory.resolve("global.cfg"),global);
        java.nio.file.Files.write(profiles.resolve("PvP.cfg"),profile);
        OwnedUiContractTest.Store store=new OwnedUiContractTest.Store();
        LegacyProfileMigration.readDirectory(directory.toFile(),store);
        assertEquals("Default|PvP",store.get("profiles",null));
        assertEquals("PvP",store.get("activeProfile",null));
        assertEquals("false",store.get("profile.PvP.one_seven_visuals.enabled",null));
        store.put("profile.PvP.nametags.enabled","false");
        LegacyProfileMigration.readDirectory(directory.toFile(),store);
        assertEquals("false",store.get("profile.PvP.nametags.enabled",null));
        store.remove("profile.PvP.nametags.enabled");
        LegacyProfileMigration.readDirectory(directory.toFile(),store);
        assertNull("A reset must not re-import stale legacy values on next startup",store.get("profile.PvP.nametags.enabled",null));
        assertArrayEquals(global,java.nio.file.Files.readAllBytes(directory.resolve("global.cfg")));
        assertArrayEquals(profile,java.nio.file.Files.readAllBytes(profiles.resolve("PvP.cfg")));
        try(java.util.stream.Stream<java.nio.file.Path> entries=java.nio.file.Files.list(profiles)) { assertEquals(1,entries.count()); }
    }
    @Test public void migratesSharedValuesAndRetainsUnsupportedLegacyFields() throws Exception {
        OwnedUiContractTest.Store store = new OwnedUiContractTest.Store();
        String text="[FPS]\n-State=true\n-xTranslation=12.5\n-yTranslation=8\n-Anchor=MIDDLE_TOP\n"
            +"Scale=1.25\nText Color=-1;rainbow\nShow Background=false\nUnknown Setting=keep=this\n"
            +"[Nametags]\nNametag Opacity=0.75\n[Unknown old mod]\nData=preserve\n";
        LegacyProfileMigration.readProfile(new StringReader(text),"中文配置",store);
        assertEquals("true",store.get("profile.中文配置.fps.enabled",null));
        assertEquals("topCenter",store.get("profile.中文配置.fps.anchor",null));
        assertEquals("12.5",store.get("profile.中文配置.fps.x",null));
        assertEquals("1.25",store.get("profile.中文配置.fps.scale",null));
        assertEquals("FFFFFFFF",store.get("profile.中文配置.fps.textColor",null));
        assertEquals("true",store.get("profile.中文配置.fps.textColorChroma",null));
        assertEquals("false",store.get("profile.中文配置.fps.background",null));
        assertEquals("keep=this",store.get("legacy.profile.中文配置.FPS.Unknown Setting",null));
        assertEquals("0.75",store.get("legacy.profile.中文配置.Nametags.Nametag Opacity",null));
        assertEquals("preserve",store.get("legacy.profile.中文配置.Unknown old mod.Data",null));
        store.put("profile.中文配置.fps.scale","2");
        LegacyProfileMigration.readProfile(new StringReader(text),"中文配置",store);
        assertEquals("2",store.get("profile.中文配置.fps.scale",null));
        assertEquals("Default|中文配置",store.get("profiles",null));
    }
    @Test public void migratesGlobalFavoritesWithoutChangingUserGameOptions() throws Exception {
        OwnedUiContractTest.Store store = new OwnedUiContractTest.Store();
        LegacyProfileMigration.readGlobal(new StringReader("FavoriteModules=Chat,FPS\nActiveProfile=PvP\nParticle Amount=0.7\n"),store);
        assertEquals("true",store.get("favorite.chat",null));
        assertEquals("true",store.get("favorite.fps",null));
        assertEquals("PvP",store.get("activeProfile",null));
        assertEquals("0.7",store.get("legacy.global.Particle Amount",null));
        assertNull(store.get("gameOptions",null));
    }
}
