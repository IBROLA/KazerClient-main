package com.cheatbreaker.client.config;
import org.junit.Test;
import org.junit.Rule;
import org.junit.rules.TemporaryFolder;
import java.nio.file.*;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import static org.junit.Assert.*;
public class SharedGameOptionsTest {
    @Rule public TemporaryFolder temporary=new TemporaryFolder();
    @Test public void savesKeysAndOptionsWithoutErasingOtherVersionFields() throws Exception {
        java.io.File game=temporary.newFolder();
        Path options=game.toPath().resolve("options.txt"),of=game.toPath().resolve("optionsof.txt");
        Files.write(options,Arrays.asList("key_key.forward:30","key_key.attack:-100","fov:0.3","modelPart_jacket:true","version:9"),StandardCharsets.UTF_8);
        Files.write(of,Arrays.asList("ofCustomSky:true","ofRenderRegions:true"),StandardCharsets.UTF_8);
        SharedGameOptions.afterSave(game);
        Files.write(options,Arrays.asList("key_key.forward:17","fov:0.7","version:7"),StandardCharsets.UTF_8);
        Files.write(of,Arrays.asList("ofCustomSky:false"),StandardCharsets.UTF_8);
        SharedGameOptions.afterSave(game);
        SharedGameOptions.beforeLoad(game);
        java.util.List<String> values=Files.readAllLines(options,StandardCharsets.UTF_8);
        assertTrue(values.contains("key_key.forward:17"));
        assertTrue(values.contains("key_key.attack:-100"));
        assertTrue(values.contains("modelPart_jacket:true"));
        assertTrue(values.contains("fov:0.7"));
        assertTrue(values.contains("version:7"));
        assertTrue(Files.readAllLines(of,StandardCharsets.UTF_8).contains("ofRenderRegions:true"));
        assertTrue(Files.exists(game.toPath().resolve("options.txt.kazer-sync-backup")));
    }
    @Test public void defaultHudFlagDoesNotPreventDisablingAnEffect() {
        assertFalse(com.cheatbreaker.client.module.ModuleToggleState.next(true,false,false));
        assertTrue(com.cheatbreaker.client.module.ModuleToggleState.next(false,false,false));
        assertTrue(com.cheatbreaker.client.module.ModuleToggleState.next(true,true,false));
        assertFalse(com.cheatbreaker.client.module.ModuleToggleState.next(true,true,true));
    }
    @Test public void mapsRenamedCloudAndCapeSettingsFromOlderVersion() throws Exception {
        java.io.File game=temporary.newFolder();Path file=game.toPath().resolve("options.txt");
        Files.write(file,Arrays.asList("clouds:false","showCape:false"),StandardCharsets.UTF_8);
        SharedGameOptions.afterSave(game,"1.7.10");
        Files.write(file,Arrays.asList("renderClouds:true","modelPart_cape:true"),StandardCharsets.UTF_8);
        SharedGameOptions.beforeLoad(game,"1.8.9");
        assertTrue(Files.readAllLines(file,StandardCharsets.UTF_8).contains("renderClouds:false"));
        assertTrue(Files.readAllLines(file,StandardCharsets.UTF_8).contains("modelPart_cape:false"));
    }
    @Test public void mapsNewerSettingsAndRetainsFastCloudPreferenceAcrossOldSaves() throws Exception {
        java.io.File game=temporary.newFolder();Path file=game.toPath().resolve("options.txt");
        Files.write(file,Arrays.asList("renderClouds:fast","modelPart_cape:false"),StandardCharsets.UTF_8);
        SharedGameOptions.afterSave(game,"1.8.9");
        Files.write(file,Arrays.asList("clouds:false","showCape:true"),StandardCharsets.UTF_8);
        SharedGameOptions.beforeLoad(game,"1.7.10");
        assertTrue(Files.readAllLines(file,StandardCharsets.UTF_8).contains("clouds:true"));
        assertTrue(Files.readAllLines(file,StandardCharsets.UTF_8).contains("showCape:false"));
        Files.write(file,Arrays.asList("clouds:true","showCape:false"),StandardCharsets.UTF_8);
        SharedGameOptions.afterSave(game,"1.7.10");SharedGameOptions.beforeLoad(game,"1.8.9");
        assertTrue(Files.readAllLines(file,StandardCharsets.UTF_8).contains("renderClouds:fast"));
    }
}
