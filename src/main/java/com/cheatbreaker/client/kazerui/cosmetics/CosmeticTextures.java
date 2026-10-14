package com.cheatbreaker.client.kazerui.cosmetics;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.*;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.MemoryCacheImageInputStream;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.util.ResourceLocation;
import org.apache.logging.log4j.LogManager;

public final class CosmeticTextures {
    private static final int MAX_FRAME = 256;
    private static final int CACHE = 16;
    private static final long PIXEL_BUDGET = 32L * 1024 * 1024;
    private static final long ENTRY_PIXEL_BUDGET = 8L * 1024 * 1024;
    private static final ExecutorService DECODE = new ThreadPoolExecutor(1, 1, 0, TimeUnit.SECONDS, new ArrayBlockingQueue<Runnable>(16), r -> {
        Thread t = new Thread(r, "Kazer cosmetic decode"); t.setDaemon(true); return t;
    });
    private static final Map<String, Entry> ENTRIES = new LinkedHashMap<String, Entry>(16, .75f, true);

    private CosmeticTextures() {}

    private static final class Entry {
        volatile Future<Frames> loading;
        Frames frames;
        boolean failed;
        long retryAt;
        DynamicTexture texture;
        ResourceLocation location;
        int shownFrame = -1;
    }

    static final class Frames {
        final int width, height, frameTime;
        final int[][] pixels;
        Frames(int width, int height, int frameTime, int[][] pixels) { this.width = width; this.height = height; this.frameTime = frameTime; this.pixels = pixels; }
    }

    private static final ResourceLocation CAPE=new ResourceLocation("kazerui","cosmetics/kazer-cape.png"),WINGS=new ResourceLocation("textures/entity/enderdragon/dragon.png");
    private static final ResourceLocation MICET=new ResourceLocation("kazerui","cosmetics/micet-cape.png");
    public static ResourceLocation get(Cosmetic c) {
        if(c.resource.equals("builtin:cape"))return CAPE;if(c.resource.equals("builtin:wings"))return WINGS;
        if(c.resource.equals("builtin:micet"))return MICET;
        return get(c.resource, c.type == CosmeticType.CLOAK);
    }

    public static ResourceLocation get(final String path, final boolean cloak) {
        Entry e = ENTRIES.get(path);
        if (e == null) {
            e = new Entry();
            ENTRIES.put(path, e);
            final Future<byte[]> image = KazerCdn.fetch(path);
            final Future<byte[]> meta = KazerCdn.has(path + ".mcmeta") ? KazerCdn.fetch(path + ".mcmeta") : null;
            try { e.loading = DECODE.submit(() -> decode(cloak, image.get(), meta == null ? null : meta.get())); }
            catch (RejectedExecutionException full) { ENTRIES.remove(path); return null; }
            evict(e);
        }
        if (e.failed) {
            if(System.nanoTime()<e.retryAt)return null;
            ENTRIES.remove(path);return get(path,cloak);
        }
        if (e.frames == null) {
            if (!e.loading.isDone()) return null;
            try { e.frames = e.loading.get(); } catch (Exception ex) { e.frames = null; }
            e.loading = null;
            if (e.frames == null) { e.failed = true;e.retryAt=System.nanoTime()+30_000_000_000L;return null; }
            evict(e);
        }
        if (e.texture == null) {
            e.texture = new DynamicTexture(e.frames.width, e.frames.height);
            e.location = Minecraft.getMinecraft().getTextureManager().getDynamicTextureLocation("kazer_cosmetic_" + Integer.toHexString(path.hashCode()), e.texture);
        }
        int frame = frameIndex(Minecraft.getMinecraft().theWorld == null ? System.currentTimeMillis() / 50 : Minecraft.getMinecraft().theWorld.getTotalWorldTime(),e.frames.frameTime,e.frames.pixels.length);
        if (frame != e.shownFrame) {
            System.arraycopy(e.frames.pixels[frame], 0, e.texture.getTextureData(), 0, e.frames.pixels[frame].length);
            e.texture.updateDynamicTexture();
            e.shownFrame = frame;
        }
        return e.location;
    }
    static int frameIndex(long ticks,int frameTime,int count) {
        // Wall/world time must not narrow to int before modulo: it can become
        // negative and crash animated cosmetics on the home menu.
        return (int)Math.floorMod(ticks / Math.max(1,frameTime),(long)count);
    }

    private static long bytes(Entry e) {
        if (e.frames == null) return 0;
        return (long)e.frames.width * e.frames.height * 4 * (e.frames.pixels.length + 1);
    }

    private static void evict(Entry keep) {
        long used = 0;
        for (Entry entry : ENTRIES.values()) {
            // Completed futures retain decoded arrays too. Harvest them before budgeting.
            if (entry.frames == null && entry.loading != null && entry.loading.isDone()) {
                try { entry.frames = entry.loading.get(); } catch (Exception ignored) { entry.failed = true; }
                entry.loading = null;
                if (entry.frames == null) { entry.failed = true;entry.retryAt=System.nanoTime()+30_000_000_000L; }
            }
            used += bytes(entry);
        }
        Iterator<Map.Entry<String, Entry>> it = ENTRIES.entrySet().iterator();
        while ((ENTRIES.size() > CACHE || used > PIXEL_BUDGET) && it.hasNext()) {
            Entry old = it.next().getValue();
            if (old == keep) continue;
            if (old.location != null) Minecraft.getMinecraft().getTextureManager().deleteTexture(old.location);
            if (old.loading != null) old.loading.cancel(true);
            used -= bytes(old);
            it.remove();
        }
    }

    private static Frames decode(boolean cloak, byte[] data, byte[] meta) throws Exception {
        if (data == null) return null;
        BufferedImage img = read(data);
        if (img == null) return null;
        int w = img.getWidth(), frameH = img.getHeight(), frameTime = 1;
        if(w>4096 || frameH>4096)return null;
        int[] order = null;
        if (meta != null) {
            JsonObject anim = new JsonParser().parse(new String(meta, StandardCharsets.UTF_8)).getAsJsonObject().getAsJsonObject("animation");
            if (anim != null) {
                if (anim.has("frametime")) frameTime = Math.max(1, anim.get("frametime").getAsInt());

                if (anim.has("width") && anim.has("height")) frameH = Math.round(w * anim.get("height").getAsFloat() / anim.get("width").getAsFloat());
                else frameH = w;
                if (anim.has("frames")) {
                    if (anim.getAsJsonArray("frames").size() > 64) return null;
                    List<Integer> list = new ArrayList<Integer>();
                    for (JsonElement f : anim.getAsJsonArray("frames"))
                        list.add(f.isJsonObject() ? f.getAsJsonObject().get("index").getAsInt() : f.getAsInt());
                    order = new int[list.size()];
                    for (int i = 0; i < order.length; i++) order[i] = list.get(i);
                }
            }
        }
        frameH = Math.max(1, Math.min(frameH, img.getHeight()));
        int count = Math.max(1, img.getHeight() / frameH);
        if(count>64 || (order!=null && (order.length==0 || order.length>64)))return null;
        if (order == null) { order = new int[count]; for (int i = 0; i < count; i++) order[i] = i; }
        BufferedImage[] frames = new BufferedImage[count];
        for (int i = 0; i < count; i++) frames[i] = img.getSubimage(0, i * frameH, w, frameH);

        int outW, outH;
        if (cloak) {
            boolean full = Math.abs((float)w / frameH - 2f) < .01f;
            outW = full ? w : Math.round(w * 64f / 22f);
            outH = full ? frameH : Math.round(frameH * 32f / 17f);
        } else { outW = w; outH = frameH; }
        float shrink = Math.min(1f, (float)MAX_FRAME * (cloak ? 64f / 22f : 1f) / Math.max(outW, outH));
        int tw = Math.max(1, Math.round(outW * shrink)), th = Math.max(1, Math.round(outH * shrink));
        if ((long)tw * th * 4 * (order.length + 1) > ENTRY_PIXEL_BUDGET) return null;
        for (int frame : order) if (frame < 0 || frame >= count) return null;
        int[][] pixels = new int[order.length][];
        for (int i = 0; i < order.length; i++) {
            BufferedImage out = new BufferedImage(tw, th, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g = out.createGraphics();
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, shrink < 1 ? RenderingHints.VALUE_INTERPOLATION_BILINEAR : RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
            g.drawImage(frames[Math.min(order[i], count - 1)], 0, 0, Math.round(w * shrink), Math.round(frameH * shrink), null);
            g.dispose();
            pixels[i] = out.getRGB(0, 0, tw, th, null, 0, tw);
        }
        return new Frames(tw, th, frameTime, pixels);
    }

    static BufferedImage read(byte[] data) throws Exception {
        // Inspect dimensions before allocating a decoded image.
        try (MemoryCacheImageInputStream input = new MemoryCacheImageInputStream(new ByteArrayInputStream(data))) {
            boolean webp = data.length > 12 && data[0]=='R' && data[1]=='I' && data[2]=='F' && data[3]=='F' && data[8]=='W' && data[9]=='E' && data[10]=='B' && data[11]=='P';
            ImageReader reader;
            if (webp) reader = new com.twelvemonkeys.imageio.plugins.webp.WebPImageReaderSpi().createReaderInstance(null);
            else {
                Iterator<ImageReader> readers = ImageIO.getImageReaders(input);
                if (!readers.hasNext()) return null;
                reader = readers.next();
            }
            try {
                reader.setInput(input, true, true);
                int width = reader.getWidth(0), height = reader.getHeight(0);
                if (width < 1 || height < 1 || width > 4096 || height > 4096 || (long)width * height > 4L * 1024 * 1024) return null;
                return reader.read(0);
            } finally { reader.dispose(); }
        }
    }
}
