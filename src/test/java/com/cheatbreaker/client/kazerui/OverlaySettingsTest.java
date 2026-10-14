package com.cheatbreaker.client.kazerui;
import com.cheatbreaker.client.kazerui.gui.ui.*;
import com.cheatbreaker.client.kazerui.module.ModuleManager;
import com.cheatbreaker.client.kazerui.module.modules.visual.ModuleOverlay;
import java.lang.reflect.Field;
import java.util.*;
import org.junit.Test;
import static org.junit.Assert.*;
public class OverlaySettingsTest {
    @Test public void defaultsStayInactiveAndEveryExposedOptionHasARealLabel() throws Exception {
        Field modelField=ModuleManager.class.getDeclaredField("model");modelField.setAccessible(true);Object old=modelField.get(null);
        try {
            UiModel model=new UiModel(new OwnedUiContractTest.Store());modelField.set(null,model);
            ModuleOverlay overlay=new ModuleOverlay();
            assertFalse(overlay.isEnabled());assertTrue(ModuleOverlay.glint(true,false));assertFalse(ModuleOverlay.glint(false,false));
            assertEquals("REGULAR",ModuleOverlay.glass("REGULAR"));assertEquals(0,ModuleOverlay.fireOffset(),0);
            assertEquals(1,ModuleOverlay.pumpkinAlpha(),0);assertFalse(ModuleOverlay.fire());assertFalse(ModuleOverlay.death());
            List<OptionCatalog.Node> nodes=OptionCatalog.flatten(overlay.page().options);
            Set<String> names=new HashSet<String>();for(OptionCatalog.Node node:nodes){assertTrue(names.add(node.key));assertNotNull(node.text);assertFalse(node.text.trim().isEmpty());}
            assertTrue(names.containsAll(Arrays.asList("glint","minimalBob","noMapBob","fireHeight","blockFireHeight","pumpkinOpacity","hideFoliage","entityShadows","groundArrows","stuckArrows","hideSkulls","hideEndPortals","clearGlass","coloredString","stringColor","headlessHorse","disableDeathAnimation","disableDamageOverlay","disableFireOverlay","hideHelmet","hideChestplate","hideLeggings","hideBoots","armorSelfOnly")));
            model.set("overlay","enabled","true");model.set("overlay","glint","ARMOR");
            assertTrue(ModuleOverlay.glint(false,true));assertFalse(ModuleOverlay.glint(true,false));
            model.set("overlay","glint","HELD");assertFalse(ModuleOverlay.glint(true,true));assertTrue(ModuleOverlay.glint(false,false));
            model.set("overlay","fireHeight","9");assertEquals(1,ModuleOverlay.fireOffset(),0);
            model.set("overlay","fireHeight","NaN");assertEquals(0,ModuleOverlay.fireOffset(),0);
            model.set("overlay","blockFireHeight","0.5");assertEquals(.5F,ModuleOverlay.blockFireOffset(),0);
            model.set("overlay","disableFireOverlay","false");assertFalse(ModuleOverlay.fire());
            model.set("overlay","enabled","false");assertEquals(0,ModuleOverlay.blockFireOffset(),0);
        }finally{modelField.set(null,old);}
    }
    @Test public void controlsResetRequestsAndGeneralFlagsDoNotRewriteOtherProfiles() {
        OwnedUiContractTest.Store store=new OwnedUiContractTest.Store();UiModel model=new UiModel(store);
        model.set("global","showCloaks","false");model.set("global","borderlessFullscreen","true");
        model.action("profile:Second");assertFalse(model.flag("global","showCloaks",true));assertTrue(model.flag("global","borderlessFullscreen",false));
        model.action("resetBinding:freelook");assertEquals("freelook",model.resetBindingRequested);assertFalse(model.captureKey);
        model.action("rebindZoom");assertTrue(model.captureKey);assertEquals("zoom",model.captureTarget);
    }
    @Test public void iconControlsAreSingleProfileAwareAliases() {
        UiModel model=new UiModel(new OwnedUiContractTest.Store());
        model.set("global","showTabIcon","false");
        assertFalse(model.flag("tab","tabLogo",true));assertFalse(model.flag("nametags","legacy_show_kazer_icon_in_tab",true));
        model.set("global","showNametagIcon","false");assertFalse(model.flag("nametags","legacy_show_nametag_icon",true));
        model.set("tab","tabLogo","true");assertTrue(model.flag("global","showTabIcon",false));
        model.action("profile:Second");assertTrue(model.flag("global","showNametagIcon",true));
    }
    @Test public void texturePaletteChangesOnlyRequestedPixelsAndPreservesAlpha() throws Exception {
        Field modelField=ModuleManager.class.getDeclaredField("model");modelField.setAccessible(true);Object old=modelField.get(null);
        try {
            UiModel model=new UiModel(new OwnedUiContractTest.Store());modelField.set(null,model);new ModuleOverlay();
            java.awt.image.BufferedImage image=new java.awt.image.BufferedImage(256,256,java.awt.image.BufferedImage.TYPE_INT_ARGB);
            for(int y=0;y<256;y++)for(int x=0;x<256;x++)image.setRGB(x,y,0x80204060);
            net.minecraft.util.ResourceLocation icons=new net.minecraft.util.ResourceLocation("textures/gui/icons.png");
            model.set("overlay","enabled","true");model.set("overlay","customXpColor","true");model.set("overlay","xpColor","FFFF0000");
            assertSame(image,com.cheatbreaker.client.kazerui.module.modules.visual.OverlayTexturePalette.tint(image,icons));
            assertEquals(0x80600000,image.getRGB(20,65));assertEquals(0x80204060,image.getRGB(183,65));assertEquals(0x80204060,image.getRGB(20,75));
            model.set("overlay","customGuiColors","true");model.set("overlay","guiColor","FF00FF00");
            java.awt.image.BufferedImage container=new java.awt.image.BufferedImage(1,1,java.awt.image.BufferedImage.TYPE_INT_ARGB);container.setRGB(0,0,0xAABBCCDD);
            com.cheatbreaker.client.kazerui.module.modules.visual.OverlayTexturePalette.tint(container,new net.minecraft.util.ResourceLocation("textures/gui/container/inventory.png"));
            assertEquals(0xAA00DD00,container.getRGB(0,0));
            model.set("overlay","enabled","false");container.setRGB(0,0,0xAABBCCDD);
            com.cheatbreaker.client.kazerui.module.modules.visual.OverlayTexturePalette.tint(container,new net.minecraft.util.ResourceLocation("textures/gui/container/inventory.png"));assertEquals(0xAABBCCDD,container.getRGB(0,0));
            model.set("overlay","enabled","true");
            com.cheatbreaker.client.kazerui.module.modules.visual.OverlayTexturePalette.tint(container,new net.minecraft.util.ResourceLocation("kazerui:textures/gui/container/inventory.png"));assertEquals(0xAABBCCDD,container.getRGB(0,0));
        }finally{modelField.set(null,old);}
    }
}
