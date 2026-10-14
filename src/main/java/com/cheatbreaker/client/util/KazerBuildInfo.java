package com.cheatbreaker.client.util;

import java.io.InputStream;
import java.util.Properties;

/** Both client versions read the same release identity, independent of game version. */
public final class KazerBuildInfo {
    private static final Properties BUILD = load();
    private KazerBuildInfo() {}
    private static Properties load() {
        Properties properties=new Properties();
        try (InputStream input=KazerBuildInfo.class.getResourceAsStream("/client/kazer-build.properties")) {
            if(input!=null)properties.load(input);
        } catch(Exception ignored) { }
        return properties;
    }
    public static String release() { return "v"+BUILD.getProperty("version","dev")+"-"+BUILD.getProperty("buildId","0000"); }
    public static String windowTitle(String minecraft) { return "Kazer Client "+minecraft+" ("+release()+")"; }
    public static String footer(String minecraft) { return "Version("+minecraft+" "+release()+")"; }
}

