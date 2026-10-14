package cn.kazer.client189.runtime;
public final class OverlayAtlasInstaller {
    private static java.lang.reflect.Method setTextureEntry;
    @net.minecraftforge.fml.common.eventhandler.SubscribeEvent
    public void stitch(net.minecraftforge.client.event.TextureStitchEvent.Pre event) {
        try {
            // Forge adds this method to TextureMap; the external vanilla release
            // library does not declare it. Resolve once at atlas stitch, not per frame.
            if(setTextureEntry==null)setTextureEntry=event.map.getClass().getMethod("setTextureEntry",String.class,net.minecraft.client.renderer.texture.TextureAtlasSprite.class);
            setTextureEntry.invoke(event.map,com.cheatbreaker.client.kazerui.module.modules.visual.OverlayWhiteSprite.NAME,
                new com.cheatbreaker.client.kazerui.module.modules.visual.OverlayWhiteSprite());
        }catch(ReflectiveOperationException error){throw new IllegalStateException("Could not register the owned Overlay atlas tile",error);}
    }
}
