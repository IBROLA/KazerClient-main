package com.cheatbreaker.client.util;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ServerData;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Lightweight same-server presence used only for the K nametag badge.  It
 * intentionally sends no access token, session token, username or hardware ID.
 */
public final class KazerPresenceClient {
    public static final String ENDPOINT = "http://45.125.44.66:10034/v1/presence";
    public static final UUID OWNER_UUID = KazerBadgeColors.CANRX_UUID;
    private static final long REFRESH_MS = 10_000L;

    private final ExecutorService executor = Executors.newSingleThreadExecutor(runnable -> {
        Thread thread = new Thread(runnable, "Kazer Presence");
        thread.setDaemon(true);
        return thread;
    });
    private final AtomicBoolean requestRunning = new AtomicBoolean();
    private volatile Map<UUID, Integer> sameServerUsers = Collections.emptyMap();
    private volatile Map<UUID,java.util.List<com.cheatbreaker.client.kazerui.cosmetics.Cosmetic>> sameServerCosmetics=Collections.emptyMap();
    private volatile String localPlayerId, activeServer;
    private volatile UUID localPlayerUuid;
    private String lastSessionId;
    private volatile long lastSuccess;
    private volatile long nextRefresh;
    private long nextPermissionRefresh;
    private final AtomicBoolean permissionRunning=new AtomicBoolean();
    private static final class AccountPalette {
        final String id; final int color;
        AccountPalette(String id,int color){this.id=id;this.color=color;}
    }
    private volatile AccountPalette accountPalette;

    public void tick() {
        Minecraft minecraft = Minecraft.getMinecraft();
        String sessionId = minecraft.getSession().getPlayerID();
        if (!java.util.Objects.equals(sessionId, lastSessionId)) {
            String normalized = normalizeUuid(sessionId);
            lastSessionId = sessionId;
            localPlayerId = normalized;
            localPlayerUuid = normalized == null ? null : uuidKey(normalized);
            nextPermissionRefresh=0;
        }
        long permissionNow=System.currentTimeMillis();
        if(localPlayerId!=null && permissionNow>=nextPermissionRefresh && permissionRunning.compareAndSet(false,true)) {
            nextPermissionRefresh=permissionNow+60000;
            final String id=localPlayerId;
            executor.execute(()->{
                HttpURLConnection request=null;
                try {
                    request=(HttpURLConnection)new URL(ENDPOINT.replace("/v1/presence","/v1/cosmetic-permissions?uuid=")+id).openConnection();
                    request.setConnectTimeout(2500);request.setReadTimeout(2500);request.setUseCaches(false);
                    if(request.getResponseCode()==200)try(BufferedReader reader=new BufferedReader(new InputStreamReader(request.getInputStream(),StandardCharsets.UTF_8))) {
                        Integer color=KazerBadgeColors.parse(request.getHeaderField("X-Kazer-Badge-Color"));
                        if(color!=null)accountPalette=new AccountPalette(id,color);
                        String value=reader.readLine();
                        if(value!=null && value.matches("[0-9]{1,2}"))com.cheatbreaker.client.kazerui.cosmetics.CosmeticPermissions.accept(id,Integer.parseInt(value));
                    }
                }catch(Exception ignored){}finally{if(request!=null)request.disconnect();permissionRunning.set(false);}
            });
        }
        ServerData data = minecraft.getCurrentServerData();
        if (minecraft.theWorld == null || minecraft.thePlayer == null || data == null
                || data.serverIP == null || data.serverIP.trim().isEmpty()) {
            sameServerUsers = Collections.emptyMap();
            sameServerCosmetics = Collections.emptyMap();
            activeServer = null;
            nextRefresh = 0L;
            return;
        }

        long now = System.currentTimeMillis();
        String currentServer = data.serverIP.trim().toLowerCase(java.util.Locale.ROOT);
        if (!currentServer.equals(activeServer)) {
            activeServer = currentServer;
            sameServerUsers = Collections.emptyMap();
            sameServerCosmetics = Collections.emptyMap();
            nextRefresh = 0L;
        }
        if (now - lastSuccess > 25000L) { sameServerUsers = Collections.emptyMap();sameServerCosmetics=Collections.emptyMap(); }
        if (now < nextRefresh || !requestRunning.compareAndSet(false, true)) return;
        nextRefresh = now + REFRESH_MS;

        final String uuid = normalizeUuid(minecraft.getSession().getPlayerID());
        final String server = currentServer;
        if (uuid == null) {
            requestRunning.set(false);
            return;
        }
        executor.execute(() -> refresh(uuid, server));
    }

    public boolean isKazerClient(UUID uuid) {
        if (uuid == null) return false;
        return uuid.equals(localPlayerUuid) || sameServerUsers.containsKey(uuid);
    }

    public boolean isOwner(UUID uuid) {
        return OWNER_UUID.equals(uuid);
    }
    public java.util.List<com.cheatbreaker.client.kazerui.cosmetics.Cosmetic> cosmetics(UUID uuid) {
        java.util.List<com.cheatbreaker.client.kazerui.cosmetics.Cosmetic> list=sameServerCosmetics.get(uuid);
        return list==null?Collections.emptyList():list;
    }

    public int badgeColor(UUID uuid) {
        if (uuid == null) return KazerBadgeColors.WHITE;
        Integer color = sameServerUsers.get(uuid);
        return color == null ? KazerBadgeColors.defaultColor(uuid) : color;
    }
    /** Account-only palette survives leaving a server; it never grants presence. */
    public int accountBadgeColor() {
        AccountPalette palette=accountPalette;
        if(palette!=null && palette.id.equals(localPlayerId))return palette.color;
        return badgeColor(localPlayerUuid);
    }

    private void refresh(String uuid, String server) {
        try {
            if (!request(uuid, server, "3") && !request(uuid,server,"2")) request(uuid, server, "1");
        } finally {
            requestRunning.set(false);
        }
    }

    private boolean request(String uuid, String server, String protocol) {
        HttpURLConnection connection = null;
        try {
            String body = "uuid=" + URLEncoder.encode(uuid, "UTF-8")
                    + "&server=" + URLEncoder.encode(server, "UTF-8")
                    + ("3".equals(protocol)?"&cosmetics="+URLEncoder.encode(com.cheatbreaker.client.kazerui.cosmetics.Loadout.wire(),"UTF-8"):"");
            byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
            connection = (HttpURLConnection)new URL(ENDPOINT).openConnection();
            connection.setRequestMethod("POST");
            connection.setConnectTimeout(2500);
            connection.setReadTimeout(2500);
            connection.setDoOutput(true);
            connection.setUseCaches(false);
            connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8");
            connection.setRequestProperty("X-Kazer-Protocol", protocol);
            connection.setFixedLengthStreamingMode(bytes.length);
            try (OutputStream output = connection.getOutputStream()) {
                output.write(bytes);
            }
            int status = connection.getResponseCode();
            if (status == 400 && !"1".equals(protocol)) return false; // Old service compatibility.
            if (status != 200) return true;

            Map<String, Integer> resolved = new HashMap<>();
            Map<UUID,java.util.List<com.cheatbreaker.client.kazerui.cosmetics.Cosmetic>> cosmetics=new HashMap<>();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(
                    connection.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                int rows=0;
                while ((line = reader.readLine()) != null && rows++<2048) {
                    if(line.length()>200)continue;String[] fields=line.split("\\t",-1);
                    KazerBadgeColors.acceptLine(resolved, fields.length>1?fields[0]+"\t"+fields[1]:line);
                    if(fields.length==3 && normalizeUuid(fields[0])!=null)
                        cosmetics.put(uuidKey(normalizeUuid(fields[0])),com.cheatbreaker.client.kazerui.cosmetics.Loadout.decode(fields[2]));
                }
            }
            if (server.equals(activeServer)) {
                Map<UUID, Integer> indexed = new HashMap<>();
                for (Map.Entry<String, Integer> entry : resolved.entrySet())
                    indexed.put(uuidKey(entry.getKey()), entry.getValue());
                sameServerUsers = Collections.unmodifiableMap(indexed);
                sameServerCosmetics=Collections.unmodifiableMap(cosmetics);
                lastSuccess = System.currentTimeMillis();
            }
        } catch (Exception ignored) {
            // Presence is cosmetic and must never interrupt networking or rendering.
        } finally {
            if (connection != null) connection.disconnect();
        }
        return true;
    }

    private static String normalizeUuid(String raw) {
        return KazerBadgeColors.normalizeUuid(raw);
    }
    private static UUID uuidKey(String id) {
        return UUID.fromString(id.substring(0,8)+"-"+id.substring(8,12)+"-"
                +id.substring(12,16)+"-"+id.substring(16,20)+"-"+id.substring(20));
    }
}
