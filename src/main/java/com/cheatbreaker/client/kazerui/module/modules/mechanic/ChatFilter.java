package com.cheatbreaker.client.kazerui.module.modules.mechanic;

import com.cheatbreaker.client.kazerui.module.ChatEvent;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import net.minecraft.client.Minecraft;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.ChatStyle;
import net.minecraft.util.IChatComponent;
import net.minecraft.util.ResourceLocation;
import org.apache.logging.log4j.LogManager;

final class ChatFilter {
    private static List<Pattern> normal, high;
    private static Pattern normalWords, highWords;

    private List<Pattern> patterns;
    private Pattern custom = Pattern.compile("(?!)");
    private static long resourceEpoch=Long.MIN_VALUE;
    private long selectedEpoch=Long.MIN_VALUE;
    private String playerName;
    private Pattern playerPattern;
    private java.util.concurrent.Future<Load> pending;
    private boolean loadSucceeded;
    private static final Pattern EMPTY = Pattern.compile("(?!)");
    private static final int MAX_CUSTOM_BYTES = 65536;
    private static final class Load {
        final Pattern pattern; final boolean success;
        Load(Pattern pattern, boolean success) { this.pattern=pattern;this.success=success; }
    }
    private static final java.util.concurrent.ThreadPoolExecutor FILES = new java.util.concurrent.ThreadPoolExecutor(
            1, 1, 0L, java.util.concurrent.TimeUnit.MILLISECONDS, new java.util.concurrent.ArrayBlockingQueue<Runnable>(2),
            task -> { Thread thread = new Thread(task, "Kazer-Chat-Filter"); thread.setDaemon(true); return thread; },
            new java.util.concurrent.ThreadPoolExecutor.AbortPolicy());

    void request(File file, ModuleChat.Profanity mode) {
        if (pending != null) pending.cancel(true);
        FILES.purge();
        custom = EMPTY; loadSucceeded=false; select(mode);
        try { pending = FILES.submit(() -> readCustom(file)); }
        catch (java.util.concurrent.RejectedExecutionException busy) { pending = java.util.concurrent.CompletableFuture.completedFuture(new Load(EMPTY,false)); }
    }
    boolean poll(ModuleChat.Profanity mode) {
        if (pending == null || !pending.isDone()) return false;
        try { Load loaded=pending.get();custom=loaded.pattern;loadSucceeded=loaded.success; }
        catch (Exception failed) { custom = EMPTY;loadSucceeded=false; }
        pending = null; select(mode); return true;
    }
    boolean loadSucceeded() { return loadSucceeded; }

    private static synchronized void loadLists() {
        long epoch=com.cheatbreaker.client.kazerui.KazerResourceEpoch.current();
        if (normal != null && resourceEpoch==epoch) return;
        resourceEpoch=epoch;normalWords=highWords=null;
        normal = new ArrayList<Pattern>();
        high = new ArrayList<Pattern>();
        try (InputStream in = Minecraft.getMinecraft().getResourceManager()
                .getResource(new ResourceLocation("kazerui", "profanity/profanity_filter.json")).getInputStream()) {
            JsonObject root = new JsonParser().parse(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject();
            JsonObject regex = root.getAsJsonObject("regex"), words = root.getAsJsonObject("words");
            for (JsonElement e : regex.getAsJsonArray("normal")) normal.add(Pattern.compile(e.getAsString(), Pattern.CASE_INSENSITIVE));
            for (JsonElement e : regex.getAsJsonArray("high")) high.add(Pattern.compile(e.getAsString(), Pattern.CASE_INSENSITIVE));
            normalWords = Pattern.compile("\\b(" + join(words.getAsJsonArray("normal")) + ")\\b", Pattern.CASE_INSENSITIVE);
            highWords = Pattern.compile("\\b(" + join(words.getAsJsonArray("high")) + ")\\b", Pattern.CASE_INSENSITIVE);
        } catch (Exception e) {
            LogManager.getLogger("KazerForge").warn("Could not find profanity filter json file: kazer:profanity/profanity_filter.json");
        }
    }

    private static String join(JsonArray words) {
        StringBuilder b = new StringBuilder();
        for (JsonElement e : words) { if (b.length() > 0) b.append('|'); b.append(e.getAsString()); }
        return b.toString();
    }

    static boolean profane(String text) {
        loadLists();
        for (Pattern p : high) if (p.matcher(text).find()) return true;
        return highWords!=null&&highWords.matcher(text).find();
    }

    boolean apply(ModuleChat chat, ChatEvent message) {
        if(selectedEpoch!=com.cheatbreaker.client.kazerui.KazerResourceEpoch.current())select(chat.profanity.get());
        ModuleChat.ChatColor color = chat.chatNameColor.get();
        boolean bold = chat.chatNameBold.on(), italic = chat.chatNameItalic.on(), underline = chat.chatNameUnderline.on();
        boolean strike = chat.chatNameStrikethrough.on(), obfuscated = chat.chatNameObfuscated.on();
        if (color != ModuleChat.ChatColor.OFF || bold || italic || underline || strike || obfuscated) {
            String name = Minecraft.getMinecraft().thePlayer.getName();
            IChatComponent before = message.component();
            if (!name.equals(playerName)) { playerName=name; playerPattern=Pattern.compile(Pattern.quote(name)); }
            IChatComponent after = ChatText.transform(before, playerPattern, (match, codes) -> {
                ChatStyle style = ChatText.styleOf(codes);
                if (color.formatting != null) style.setColor(color.formatting);
                if (bold) style.setBold(true);
                if (italic) style.setItalic(true);
                if (underline) style.setUnderlined(true);
                if (strike) style.setStrikethrough(true);
                if (obfuscated) style.setObfuscated(true);
                ChatComponentText t = new ChatComponentText(match);
                t.setChatStyle(style);
                return t;
            });
            if (after != before) message.set(after);
        }
        IChatComponent filtered = filter(message.component());
        if (filtered != null) {
            message.filtered = true;
            message.set(filtered);
        }
        if (message.filtered && chat.stopProfaneMessages.on()) {
            message.cancelled = true;
            return true;
        }
        return false;
    }

    private IChatComponent filter(IChatComponent component) {
        if (patterns == null) return null;
        IChatComponent out = component;
        for (Pattern p : patterns) out = ChatText.mask(out, p, "*");
        return out == component ? null : out;
    }

    private static List<Pattern> usable(List<Pattern> list) {
        List<Pattern> out = new ArrayList<Pattern>();
        for (Pattern p : list) if (!p.toString().isEmpty() && !p.toString().contains("()")) out.add(p);
        return out;
    }

    void select(ModuleChat.Profanity mode) {
        selectedEpoch=com.cheatbreaker.client.kazerui.KazerResourceEpoch.current();
        if(mode==ModuleChat.Profanity.NORMAL || mode==ModuleChat.Profanity.HIGH)loadLists();
        List<Pattern> list;
        switch (mode) {
            case NORMAL:
                list = new ArrayList<Pattern>(normal);
                if (normalWords != null) list.add(normalWords);
                list.add(custom);
                break;
            case HIGH:
                list = new ArrayList<Pattern>(high);
                if (highWords != null) list.add(highWords);
                list.add(custom);
                break;
            case CUSTOM:
                list = new ArrayList<Pattern>();
                list.add(custom);
                break;
            default:
                list = null;
        }
        patterns = list == null ? null : usable(list);
    }

    void load(File file, ModuleChat.Profanity mode) {
        Load loaded=readCustom(file);custom=loaded.pattern;loadSucceeded=loaded.success;select(mode);
    }
    /** Reads a bounded stream, not a size check followed by an unbounded read:
     * the file can grow or be replaced between those two operations. */
    static String readCustomText(InputStream input) throws java.io.IOException {
        java.io.ByteArrayOutputStream bytes=new java.io.ByteArrayOutputStream();
        byte[] buffer=new byte[4096];int count,total=0;
        while((count=input.read(buffer))!=-1) {
            total+=count;
            if(total>MAX_CUSTOM_BYTES)throw new java.io.IOException("Custom chat filter exceeds 64 KiB");
            if(Thread.currentThread().isInterrupted())throw new java.io.InterruptedIOException("Filter loading cancelled");
            bytes.write(buffer,0,count);
        }
        return new String(bytes.toByteArray(),StandardCharsets.UTF_8);
    }
    private static Load readCustom(File file) {
        try {
            if (!file.exists()) {
                if(file.getParentFile()!=null)file.getParentFile().mkdirs();
                file.createNewFile();
            }
            if (file.length()>65536) throw new java.io.IOException("Custom chat filter exceeds 64 KiB");
            String text;
            try(InputStream input=new java.io.FileInputStream(file)){text=readCustomText(input);}
            StringBuilder b = new StringBuilder();
            for (String line : text.split("\\r?\\n")) {
                if (line.trim().isEmpty()) continue;
                if (b.length()>0) b.append('|');
                b.append('(').append(Pattern.quote(line)).append(')');
            }
            return new Load(b.length()==0 ? EMPTY : Pattern.compile("(" + b + ")", Pattern.CASE_INSENSITIVE),true);
        } catch (Exception e) {
            LogManager.getLogger("KazerForge").warn("Loading ChatMod", e);
            return new Load(EMPTY,false); // Never retain a previous profile's words.
        }
    }
}
