package com.cheatbreaker.client.kazerui.cosmetics;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.List;
import javax.imageio.ImageIO;
import org.junit.Test;
import static org.junit.Assert.*;

public class CosmeticSafetyTest {
    @Test public void animationTimeCannotOverflowIntoNegativeFrameIndexes() {
        for(long time:new long[]{0,2147483648L,35000000000L,-1,Long.MAX_VALUE})
            for(int count:new int[]{1,2,7,64})for(int delay:new int[]{1,2,100}) {
                int frame=CosmeticTextures.frameIndex(time,delay,count);
                assertTrue(frame>=0 && frame<count);
            }
        assertEquals(1,CosmeticTextures.frameIndex(2147483649L,1,2));
    }
    @Test public void webpDecodesWithoutImageIoServiceRegistration() throws Exception {
        byte[] image = java.util.Base64.getDecoder().decode("UklGRiIAAABXRUJQVlA4IBYAAAAwAQCdASoBAAEADsD+JaQAA3AAAAAA");
        BufferedImage decoded = CosmeticTextures.read(image);
        assertNotNull(decoded);
        assertEquals(1, decoded.getWidth()); assertEquals(1, decoded.getHeight());
    }
    private static byte[] png(int width, int height) throws Exception {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        ImageIO.write(new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB), "png", bytes);
        return bytes.toByteArray();
    }
    private static CosmeticTextures.Frames decode(byte[] image, String metadata) throws Exception {
        Method method = CosmeticTextures.class.getDeclaredMethod("decode", boolean.class, byte[].class, byte[].class);
        method.setAccessible(true);
        return (CosmeticTextures.Frames)method.invoke(null, false, image,
                metadata == null ? null : metadata.getBytes(StandardCharsets.UTF_8));
    }
    @Test public void staticAndAnimatedFramesDecodeWithoutNativeTextures() throws Exception {
        CosmeticTextures.Frames still = decode(png(64, 32), null);
        assertEquals(64, still.width); assertEquals(32, still.height);
        assertEquals(1, still.pixels.length);
        CosmeticTextures.Frames animated = decode(png(32, 64), "{\"animation\":{\"frametime\":2,\"frames\":[1,0]}}");
        assertEquals(32, animated.height); assertEquals(2, animated.pixels.length);
        assertEquals(2, animated.frameTime);
    }
    @Test public void invalidFrameListsAndOversizedImagesAreRejectedBeforeAllocatingFrames() throws Exception {
        byte[] image = png(32, 64);
        assertNull(decode(image, "{\"animation\":{\"frames\":[]}}"));
        assertNull(decode(image, "{\"animation\":{\"frames\":[-1]}}"));
        assertNull(decode(image, "{\"animation\":{\"frames\":[2]}}"));
        assertNull(CosmeticTextures.read(png(4097, 1)));
        assertNull(CosmeticTextures.read(new byte[]{1,2,3}));
    }
    @Test public void loadoutsOnlyAcceptBundledSupportedIdsAndAreImmutable() {
        List<Cosmetic> loadout = Loadout.decode("900001,900002,9999999,http://invalid");
        assertEquals(2, loadout.size());
        assertEquals(900001, loadout.get(0).id);
        assertEquals(900002, loadout.get(1).id);
        try { loadout.clear(); fail("Mutable loadout"); } catch (UnsupportedOperationException expected) { }
        assertEquals(1, Loadout.decode("900001,900001").size());
        assertTrue(Loadout.decode(new String(new char[129])).isEmpty());
        assertTrue(Loadout.decode(null).isEmpty());
    }
}
