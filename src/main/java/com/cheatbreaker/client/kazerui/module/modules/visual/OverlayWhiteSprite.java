package com.cheatbreaker.client.kazerui.module.modules.visual;
import java.util.*;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.IResourceManager;
import net.minecraft.util.ResourceLocation;
/** Owned white atlas tile, generated at reload; no third-party texture copied. */
public final class OverlayWhiteSprite extends TextureAtlasSprite {
    public static final String NAME="kazerui:overlay_white";
    public static OverlayWhiteSprite current;
    public OverlayWhiteSprite(){super(NAME);current=this;}
    public boolean hasCustomLoader(IResourceManager manager,ResourceLocation location){return true;}
    public boolean load(IResourceManager manager,ResourceLocation location) {
        setIconWidth(16);setIconHeight(16);
        int[][] levels=new int[5][];
        for(int level=0;level<5;level++){int side=16>>level;levels[level]=new int[side*side];Arrays.fill(levels[level],0xFFFFFFFF);}
        List<int[][]> frames=new ArrayList<int[][]>(1);frames.add(levels);setFramesTextureData(frames);return false;
    }
}
