package cn.kazer.client189;

import cn.kazer.client189.config.KazerConfigSync;
import net.minecraft.client.Minecraft;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventHandler;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/** Entry point for the separate 1.8.9 extension; official OptiFine is external. */
@Mod(modid = KazerClient189.MOD_ID, name = "Kazer Client", version = KazerClient189.VERSION,
        acceptedMinecraftVersions = "[1.8.9]")
public final class KazerClient189 {
    public static final String MOD_ID = "kazerclient";
    public static final String VERSION = "0.1.37";

    @EventHandler @SideOnly(Side.CLIENT)
    public void preInit(net.minecraftforge.fml.common.event.FMLPreInitializationEvent event) {
        for(net.minecraftforge.fml.common.ModContainer mod:net.minecraftforge.fml.common.Loader.instance().getActiveModList())
            if(!allowedMod(mod.getModId()))throw new IllegalStateException("Kazer 1.8.9 does not support extra mods: "+mod.getName());
        net.minecraftforge.common.MinecraftForge.EVENT_BUS.register(new cn.kazer.client189.runtime.RedStringModels.Installer());
        net.minecraftforge.common.MinecraftForge.EVENT_BUS.register(new cn.kazer.client189.runtime.OverlayAtlasInstaller());
    }
    static boolean allowedMod(String id) {
        String value=id.toLowerCase(java.util.Locale.ROOT);
        return value.equals("minecraft") || value.equals("mcp") || value.equals("fml") || value.equals("forge")
            || value.equals(MOD_ID) || value.equals("optifine");
    }

    @EventHandler
    @SideOnly(Side.CLIENT)
    public void init(FMLInitializationEvent event) {
        com.cheatbreaker.client.ui.mainmenu.LoadingScreen loading=new com.cheatbreaker.client.ui.mainmenu.LoadingScreen(3);
        loading.newMessage("Mods");org.lwjgl.opengl.Display.update();
        com.cheatbreaker.client.CheatBreaker client = new com.cheatbreaker.client.CheatBreaker();
        client.initialize();
        loading.newMessage("Configuration");org.lwjgl.opengl.Display.update();
        client.moduleManager.blockHitAnimation.load();
        net.minecraft.client.settings.KeyBinding.resetKeyBindingArrayAndHash();
        cn.kazer.client189.runtime.KazerRuntime runtime = new cn.kazer.client189.runtime.KazerRuntime();
        net.minecraftforge.common.MinecraftForge.EVENT_BUS.register(runtime);
        FMLCommonHandler.instance().bus().register(runtime);
        org.lwjgl.opengl.Display.setTitle(com.cheatbreaker.client.util.KazerBuildInfo.windowTitle("1.8.9"));
        loading.newMessage("Ready");org.lwjgl.opengl.Display.update();
        cn.kazer.client189.runtime.ParitySmoke.startup(loading);
    }
}
