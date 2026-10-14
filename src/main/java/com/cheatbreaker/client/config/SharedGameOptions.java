package com.cheatbreaker.client.config;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;

/** Shared native options: retain unsupported keys when either MC version saves. */
public final class SharedGameOptions {
    private SharedGameOptions() {}
    private static Path shared(File game) { return game.toPath().resolve("config/KazerClient/game-options.cfg"); }
    public static void beforeLoad(File game) {
        beforeLoad(game,null);
    }
    public static void beforeLoad(File game,String version) {
        try {
            Map<String,String> values = read(shared(game));
            if (values.isEmpty()) return;
            for (String name : new String[]{"options.txt","optionsof.txt"}) {
                Path file = game.toPath().resolve(name);
                Map<String,String> current = read(file);
                for (Map.Entry<String,String> entry : values.entrySet()) {
                    String prefix = name.equals("optionsof.txt") ? "of/" : "mc/";
                    if (entry.getKey().startsWith(prefix)) current.put(entry.getKey().substring(prefix.length()),entry.getValue());
                }
                if(name.equals("options.txt")) {
                    if("1.7.10".equals(version)) {
                        if(values.containsKey("mc/renderClouds"))current.put("clouds",String.valueOf(!"false".equals(values.get("mc/renderClouds"))));
                        if(values.containsKey("mc/modelPart_cape"))current.put("showCape",values.get("mc/modelPart_cape"));
                    } else if("1.8.9".equals(version)) {
                        if(!values.containsKey("mc/renderClouds")&&values.containsKey("mc/clouds"))current.put("renderClouds",values.get("mc/clouds"));
                        if(!values.containsKey("mc/modelPart_cape")&&values.containsKey("mc/showCape"))current.put("modelPart_cape",values.get("mc/showCape"));
                    }
                }
                if (Files.exists(file) && !Files.exists(game.toPath().resolve(name+".kazer-sync-backup")))
                    Files.copy(file,game.toPath().resolve(name+".kazer-sync-backup"));
                write(file,current);
            }
        } catch (IOException error) { System.err.println("[Kazer] Could not load shared game settings: "+error.getMessage()); }
    }
    public static void afterSave(File game) {
        afterSave(game,null);
    }
    public static void afterSave(File game,String version) {
        try {
            Map<String,String> values=read(shared(game));
            for(String name:new String[]{"options.txt","optionsof.txt"}) {
                String prefix=name.equals("optionsof.txt")?"of/":"mc/";
                for(Map.Entry<String,String> entry:read(game.toPath().resolve(name)).entrySet())
                    if (!entry.getKey().equals("version")) values.put(prefix+entry.getKey(),entry.getValue());
            }
            Map<String,String> nativeOptions=read(game.toPath().resolve("options.txt"));
            if("1.7.10".equals(version)) {
                if(nativeOptions.containsKey("clouds")) {
                    String cloud=nativeOptions.get("clouds");
                    // The older boolean cannot represent Fast; retain that newer preference while enabled.
                    values.put("mc/renderClouds","true".equals(cloud)&&"fast".equals(values.get("mc/renderClouds"))?"fast":cloud);
                }
                if(nativeOptions.containsKey("showCape"))values.put("mc/modelPart_cape",nativeOptions.get("showCape"));
            } else if("1.8.9".equals(version)) {
                if(nativeOptions.containsKey("renderClouds"))values.put("mc/clouds",String.valueOf(!"false".equals(nativeOptions.get("renderClouds"))));
                if(nativeOptions.containsKey("modelPart_cape"))values.put("mc/showCape",nativeOptions.get("modelPart_cape"));
            }
            write(shared(game),values);
        } catch(IOException error) {System.err.println("[Kazer] Could not save shared game settings: "+error.getMessage());}
    }
    private static Map<String,String> read(Path file) throws IOException {
        Map<String,String> values=new LinkedHashMap<>();
        if(Files.isRegularFile(file))for(String line:Files.readAllLines(file,StandardCharsets.UTF_8)){
            int split=line.indexOf(':');
            if(split>0&&!line.startsWith("#"))values.put(line.substring(0,split),line.substring(split+1));
        }
        return values;
    }
    private static void write(Path file,Map<String,String> values) throws IOException {
        Files.createDirectories(file.toAbsolutePath().getParent());
        List<String> lines=new ArrayList<>();
        for(Map.Entry<String,String> entry:values.entrySet())lines.add(entry.getKey()+":"+entry.getValue());
        Path temp=file.resolveSibling(file.getFileName()+".kazer.tmp");
        Files.write(temp,lines,StandardCharsets.UTF_8);
        Files.move(temp,file,StandardCopyOption.REPLACE_EXISTING);
    }
}
