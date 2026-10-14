package com.cheatbreaker.client.kazerui.cosmetics;

import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;
import java.util.concurrent.*;

/** The owned catalog's content-addressed assets. Only indexed hashes can be requested. */
public final class KazerCdn {
    // At most eight 4 MiB downloads are retained; cosmetic pixels have a separate budget.
    private static final int MAX_PENDING = 8;
    private static final Map<String,String> INDEX=new HashMap<String,String>();
    private static final Map<String,Future<byte[]>> PENDING=new LinkedHashMap<String,Future<byte[]>>(16,.75F,true);
    private static final ExecutorService POOL=new ThreadPoolExecutor(2,2,0,TimeUnit.SECONDS,new ArrayBlockingQueue<Runnable>(32),r -> {
        Thread t=new Thread(r,"Kazer cosmetic download");t.setDaemon(true);return t;
    });
    private static boolean loaded;
    private static boolean warming;
    private static synchronized void load() {
        if(loaded)return;loaded=true;
        try(BufferedReader reader=new BufferedReader(new InputStreamReader(KazerCdn.class.getResourceAsStream("/assets/kazerui/cosmetics/jit_index"),StandardCharsets.UTF_8))) {
            for(String line;(line=reader.readLine())!=null;) {
                int split=line.indexOf(' ');if(split<1)continue;String hash=line.substring(split+1).trim();
                if(hash.matches("[a-f0-9]{40}"))INDEX.put(line.substring(0,split),hash);
            }
        }catch(IOException | RuntimeException failure) { org.apache.logging.log4j.LogManager.getLogger().warn("[KC] Cosmetic index unavailable",failure); }
    }
    public static boolean has(String path) { load();return INDEX.containsKey(path); }
    /** Start once, off the render thread. Only indexed assets enter the persistent cache. */
    public static synchronized void preload() {
        if(warming)return;warming=true;
        Thread loader=new Thread(() -> {
            CosmeticCatalog.all();load();
            java.util.Set<String> paths=preloadPaths(CosmeticCatalog.all(),INDEX.keySet());
            java.util.Set<String> hashes=new java.util.LinkedHashSet<String>();
            for(String resource:paths){String hash=INDEX.get(resource);if(hash!=null)hashes.add(hash);}
            for(String hash:hashes) {
                if(Thread.currentThread().isInterrupted())return;
                // Download one at a time, reserving the other worker for the selected item.
                java.nio.file.Path file=cacheFile(hash);
                if(!java.nio.file.Files.isRegularFile(file))download(hash);
            }
        },"Kazer cosmetic preload");loader.setDaemon(true);loader.start();
    }
    private static java.nio.file.Path cacheFile(String hash) {
        return new File(net.minecraft.client.Minecraft.getMinecraft().mcDataDir,"kazer/cosmetic-assets/"+hash).toPath();
    }
    /** Expand indexed folders once, instead of rescanning the full index per item. */
    static Set<String> preloadPaths(Collection<Cosmetic> cosmetics,Collection<String> index) {
        Set<String> paths=new HashSet<String>();TreeSet<String> folders=new TreeSet<String>();
        for(Cosmetic cosmetic:cosmetics) {
            String resource=cosmetic.resource;
            if(resource.startsWith("builtin:"))continue;
            paths.add(resource);
            if(cosmetic.type!=CosmeticType.CLOAK) {
                String folder=resource.substring(0,resource.lastIndexOf('/')+1);
                int textures=folder.indexOf("/textures/");if(textures>=0)folder=folder.substring(0,textures+1);
                if(!folder.isEmpty())folders.add(folder);
            }
        }
        TreeSet<String> roots=new TreeSet<String>();String previous=null;
        for(String folder:folders)if(previous==null || !folder.startsWith(previous)) {roots.add(folder);previous=folder;}
        for(String candidate:index) {String prefix=roots.floor(candidate);if(prefix!=null && candidate.startsWith(prefix))paths.add(candidate);}
        return paths;
    }
    private static boolean verified(String hash,byte[] bytes)throws Exception {
        byte[] digest=MessageDigest.getInstance("SHA-1").digest(bytes);
        final char[] hex="0123456789abcdef".toCharArray();
        for(int i=0;i<digest.length;i++)if(hash.charAt(i*2)!=hex[(digest[i]>>4)&15] || hash.charAt(i*2+1)!=hex[digest[i]&15])return false;
        return true;
    }
    public static synchronized Future<byte[]> fetch(String path) {
        load();String hash=INDEX.get(path);if(hash==null)return CompletableFuture.completedFuture(null);
        Future<byte[]> prior=PENDING.get(hash);
        if(prior!=null) {
            if(!prior.isDone())return prior;
            try {if(prior.get()!=null)return prior;}catch(Exception ignored) { }
            PENDING.remove(hash); // A transient network failure must not poison the session cache.
        }
        Iterator<Map.Entry<String,Future<byte[]>>> iterator=PENDING.entrySet().iterator();
        while(PENDING.size()>=MAX_PENDING && iterator.hasNext()) { Map.Entry<String,Future<byte[]>> entry=iterator.next();if(entry.getValue().isDone())iterator.remove(); }
        if(PENDING.size()>=MAX_PENDING)return CompletableFuture.completedFuture(null);
        try {
            Future<byte[]> task=POOL.submit(() -> download(hash));PENDING.put(hash,task);return task;
        }catch(RejectedExecutionException ignored) { return CompletableFuture.completedFuture(null); }
    }
    private static byte[] download(String hash) {
        HttpURLConnection connection=null;
        try {
            java.nio.file.Path file=cacheFile(hash);
            if(java.nio.file.Files.isRegularFile(file) && java.nio.file.Files.size(file)<=4*1024*1024) {
                byte[] bytes=java.nio.file.Files.readAllBytes(file);
                if(verified(hash,bytes))return bytes;
            }
            connection=(HttpURLConnection)new URL("https://textures.lunarclientcdn.com/file/"+hash).openConnection();
            connection.setInstanceFollowRedirects(false);
            connection.setConnectTimeout(4000);connection.setReadTimeout(6000);connection.setRequestProperty("User-Agent","Kazer Client");
            if(connection.getResponseCode()!=200 || connection.getContentLengthLong()>4*1024*1024)return null;
            ByteArrayOutputStream bytes=new ByteArrayOutputStream();byte[] buffer=new byte[8192];
            try(InputStream input=connection.getInputStream()) {
                for(int size;(size=input.read(buffer))!=-1;) { if(bytes.size()+size>4*1024*1024)return null;bytes.write(buffer,0,size); }
            }
            byte[] result=bytes.toByteArray();if(!verified(hash,result))return null;
            try {
                java.nio.file.Files.createDirectories(file.getParent());
                java.nio.file.Path temporary=java.nio.file.Files.createTempFile(file.getParent(),hash,".part");
                try {java.nio.file.Files.write(temporary,result);java.nio.file.Files.move(temporary,file,java.nio.file.StandardCopyOption.REPLACE_EXISTING);}
                finally {java.nio.file.Files.deleteIfExists(temporary);}
            }catch(IOException ignored){ /* Read-only installations can still use verified memory assets. */ }
            return result;
        }catch(Exception failure) { return null; }
        finally { if(connection!=null)connection.disconnect(); }
    }
    private KazerCdn() {}
}
