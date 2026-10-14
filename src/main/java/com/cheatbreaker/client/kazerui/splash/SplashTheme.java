package com.cheatbreaker.client.kazerui.splash;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.Properties;

/** Read the selected home theme once during loading; never start a panorama. */
public final class SplashTheme {
    public static String background(String selected) {
        if ("vanilla".equals(selected)) return "/assets/minecraft/textures/gui/title/background/panorama_0.png";
        String folder;
        if ("christmas".equals(selected)) folder="christmas";
        else if ("spring".equals(selected) || "summer".equals(selected) || "anniversary".equals(selected)) folder="spring";
        else if ("japan".equals(selected)) folder="japan";
        else folder="classic";
        return "/assets/kazerui/ui/backgrounds/"+folder+"/panorama_0.png";
    }
    public static String selectedBackground(File gameDirectory) {
        File file=new File(gameDirectory,"config/KazerClient/owned-ui.properties");
        Properties values=new Properties();
        if(file.isFile() && file.length()<=1024*1024) {
            try(Reader reader=new InputStreamReader(new FileInputStream(file),StandardCharsets.UTF_8)) {
                values.load(reader);
            } catch(IOException | IllegalArgumentException ignored) { }
        }
        return background(values.getProperty("ui.homeTheme.selected","classic"));
    }
    private SplashTheme() {}
}
