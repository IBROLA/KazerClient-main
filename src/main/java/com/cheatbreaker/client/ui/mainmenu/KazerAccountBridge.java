package com.cheatbreaker.client.ui.mainmenu;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Session;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.function.Consumer;

public final class KazerAccountBridge {
    private static Session pendingSession;

    public static void applyPending() {
        Minecraft mc = Minecraft.getMinecraft();
        if (pendingSession != null && mc.theWorld == null) {
            cn.kazer.client189.compat.KazerCompat.setSession(pendingSession);
            pendingSession = null;
        }
    }

    public static void request(String route, Consumer<JsonObject> complete, Consumer<String> failed) {
        if ("/login".equals(route)) grantLauncherForeground();
        Thread worker = new Thread(() -> {
            HttpURLConnection connection = null;
            try {
                String endpoint = System.getProperty("kazer.accounts.url", "");
                String secret = System.getProperty("kazer.accounts.secret", "");
                if (!endpoint.matches("http://127\\.0\\.0\\.1:[0-9]{1,5}") || !secret.matches("[a-f0-9]{64}"))
                    throw new IllegalStateException("Start this client from Kazer Launcher to manage accounts.");
                connection = (HttpURLConnection)new URL(endpoint + route).openConnection();
                connection.setRequestMethod(route.equals("/accounts") ? "GET" : "POST");
                connection.setRequestProperty("Authorization", "Bearer " + secret);
                connection.setConnectTimeout(5000);
                connection.setReadTimeout(300000);
                try (InputStream stream = connection.getInputStream();
                     InputStreamReader reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
                    JsonObject result = new JsonParser().parse(reader).getAsJsonObject();
                    Minecraft.getMinecraft().addScheduledTask(() -> complete.accept(result));
                }
            } catch (Exception error) {
                String message = error instanceof IllegalStateException ? error.getMessage()
                        : "Login failed or was cancelled. Please try again.";
                Minecraft.getMinecraft().addScheduledTask(() -> failed.accept(message));
            } finally { if (connection != null) connection.disconnect(); }
        }, "Kazer account login");
        worker.setDaemon(true);
        worker.start();
    }

    private static void grantLauncherForeground() {
        // Called on the user's click thread, before asynchronous OAuth work.
        // Grant only the launcher PID supplied by its authenticated launch, never ASFW_ANY.
        if (!System.getProperty("os.name", "").startsWith("Windows")) return;
        try {
            String endpoint=System.getProperty("kazer.accounts.url", ""), secret=System.getProperty("kazer.accounts.secret", "");
            if (!endpoint.matches("http://127\\.0\\.0\\.1:[0-9]{1,5}") || !secret.matches("[a-f0-9]{64}")) return;
            int pid=Integer.parseInt(System.getProperty("kazer.accounts.pid", "0"));
            if (pid<=0) return;
            com.sun.jna.NativeLibrary.getInstance("user32").getFunction("AllowSetForegroundWindow", com.sun.jna.Function.ALT_CONVENTION).invokeInt(new Object[]{pid});
        } catch (RuntimeException unavailable) { /* Keep ordinary login usable on restricted desktops. */ }
        catch (LinkageError unavailable) { /* Native API unavailable; launcher still performs a temporary lift. */ }
    }

    public static String acceptSession(JsonObject result) {
        Session session = new Session(result.get("name").getAsString(), result.get("id").getAsString(),
                result.get("accessToken").getAsString(), "mojang");
        if (Minecraft.getMinecraft().theWorld == null) {
            cn.kazer.client189.compat.KazerCompat.setSession(session);
            return "Account added and synced to Kazer Launcher.";
        }
        pendingSession = session;
        return "Account synced. It will switch after you leave this world.";
    }
}
