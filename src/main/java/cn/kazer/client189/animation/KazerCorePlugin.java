package cn.kazer.client189.animation;

import net.minecraftforge.fml.relauncher.IFMLLoadingPlugin;

import java.util.Map;

/** Loads a visual-only renderer transformer after Forge's Minecraft mappings. */
@IFMLLoadingPlugin.MCVersion("1.8.9")
@IFMLLoadingPlugin.SortingIndex(1001)
public final class KazerCorePlugin implements IFMLLoadingPlugin {
    @Override public String[] getASMTransformerClass() {
        return new String[] { BlockHitTransformer.class.getName(), "cn.kazer.client189.runtime.RuntimeTransformer", "cn.kazer.client189.runtime.OverlayTransformer", "cn.kazer.client189.runtime.SevenVisualTransformer", "cn.kazer.client189.runtime.StartupSplashTransformer" };
    }

    @Override public String getModContainerClass() { return null; }

    @Override public String getSetupClass() { return null; }

    @Override public void injectData(Map<String, Object> data) { }

    @Override public String getAccessTransformerClass() { return null; }
}
