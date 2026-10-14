package com.cheatbreaker.client.util;

import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.nio.ByteBuffer;
import javax.imageio.ImageIO;
import org.lwjgl.opengl.Display;

/** Set vanilla game icons once. Launcher/server branding is a separate asset. */
public final class MinecraftWindowIcon {
    public static void apply() {
        if (!Display.isCreated()) return;
        try {
            Display.setIcon(new ByteBuffer[]{read("icon-1.png"),read("icon-2.png")});
        } catch (Exception error) {
            System.err.println("[Kazer] Could not set Minecraft window icon: " + error.getClass().getSimpleName());
        }
    }
    private static ByteBuffer read(String name) throws Exception {
        try (InputStream stream=MinecraftWindowIcon.class.getResourceAsStream("/assets/minecraft/client/"+name)) {
            if(stream==null)throw new java.io.IOException("Missing vanilla icon");
            BufferedImage image=ImageIO.read(stream);
            if(image==null)throw new java.io.IOException("Unreadable vanilla icon");
            ByteBuffer pixels=ByteBuffer.allocateDirect(image.getWidth()*image.getHeight()*4);
            for(int y=0;y<image.getHeight();y++)for(int x=0;x<image.getWidth();x++) {
                int argb=image.getRGB(x,y);
                pixels.put((byte)(argb>>16)).put((byte)(argb>>8)).put((byte)argb).put((byte)(argb>>24));
            }
            pixels.flip();return pixels;
        }
    }
    private MinecraftWindowIcon(){}
}
