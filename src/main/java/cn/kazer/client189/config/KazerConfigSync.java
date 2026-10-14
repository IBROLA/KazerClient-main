package cn.kazer.client189.config;

import cn.kazer.client189.animation.BlockHitAnimation;
import cn.kazer.client189.hud.CpsHud;
import cn.kazer.client189.hud.FpsHud;
import cn.kazer.client189.hud.KeystrokesHud;
import cn.kazer.client189.hud.PotionEffectsHud;
import net.minecraft.client.Minecraft;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/** Reloads shared profiles after 1.7 or an external editor changes them. */
public final class KazerConfigSync {
    private final Path gameDirectory;
    private FpsHud fps;
    private CpsHud cps;
    private KeystrokesHud keystrokes;
    private PotionEffectsHud potions;
    private long globalStamp = Long.MIN_VALUE;
    private long profileStamp = Long.MIN_VALUE;
    private long animationStamp = Long.MIN_VALUE;
    private String activeProfile;
    private int ticks;

    public KazerConfigSync(Path gameDirectory) {
        this.gameDirectory = gameDirectory;
    }

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END || ++ticks % 100 != 0) return;
        if (Minecraft.getMinecraft().thePlayer == null) return;
        reload(false);
    }

    public void reload(boolean force) {
        try {
            Path globalPath = KazerConfigPaths.global(gameDirectory);
            Path animationPath = KazerConfigPaths.animation(gameDirectory);
            long nextGlobalStamp = stamp(globalPath);
            KazerConfigDocument global = KazerConfigDocument.read(globalPath);
            String nextActive = global.get("", "ActiveProfile");
            if (nextActive == null || nextActive.trim().isEmpty()) nextActive = "default";
            Path profilePath = nextActive.equalsIgnoreCase("default")
                    ? null : KazerConfigPaths.profile(gameDirectory, nextActive);
            long nextProfileStamp = profilePath == null ? -1 : stamp(profilePath);
            long nextAnimationStamp = stamp(animationPath);
            if (force || nextGlobalStamp != globalStamp || nextProfileStamp != profileStamp
                    || !nextActive.equals(activeProfile)) {
                KazerConfigDocument profile = profilePath == null ? null : KazerConfigDocument.read(profilePath);
                FpsHud nextFps = new FpsHud();
                CpsHud nextCps = new CpsHud();
                KeystrokesHud nextKeystrokes = new KeystrokesHud();
                PotionEffectsHud nextPotions = new PotionEffectsHud();
                nextFps.load(profile);
                nextCps.load(profile);
                nextKeystrokes.load(profile);
                nextPotions.load(profile);
                if (fps != null) MinecraftForge.EVENT_BUS.unregister(fps);
                if (cps != null) MinecraftForge.EVENT_BUS.unregister(cps);
                if (keystrokes != null) MinecraftForge.EVENT_BUS.unregister(keystrokes);
                if (potions != null) MinecraftForge.EVENT_BUS.unregister(potions);
                fps = nextFps;
                cps = nextCps;
                keystrokes = nextKeystrokes;
                potions = nextPotions;
                MinecraftForge.EVENT_BUS.register(fps);
                MinecraftForge.EVENT_BUS.register(cps);
                MinecraftForge.EVENT_BUS.register(keystrokes);
                MinecraftForge.EVENT_BUS.register(potions);
                globalStamp = nextGlobalStamp;
                profileStamp = nextProfileStamp;
                activeProfile = nextActive;
                System.out.println("[KC 1.8.9] Shared profile: " + nextActive);
            }
            if (force || nextAnimationStamp != animationStamp) {
                String blockHit = KazerConfigDocument.read(animationPath).get("", "BlockHitAnimation");
                BlockHitAnimation.setEnabled(blockHit == null || Boolean.parseBoolean(blockHit));
                animationStamp = nextAnimationStamp;
            }
        } catch (IOException | IllegalArgumentException exception) {
            System.err.println("[KC 1.8.9] Could not reload shared config: " + exception.getMessage());
        }
    }

    private static long stamp(Path file) throws IOException {
        return Files.exists(file) ? Files.getLastModifiedTime(file).toMillis() : -1;
    }
}
