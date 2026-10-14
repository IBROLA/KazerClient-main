package com.cheatbreaker.client.kazerui;

import com.cheatbreaker.client.kazerui.config.Configuration;
import com.cheatbreaker.client.kazerui.gui.ui.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import org.junit.Test;
import static org.junit.Assert.*;

public class OwnedUiContractTest {
    static final class Store implements UiStore {
        final Map<String,String> values = new LinkedHashMap<String,String>();
        public String get(String key,String fallback) { return values.containsKey(key) ? values.get(key) : fallback; }
        public void put(String key,String value) { values.put(key,value); }
        public void remove(String key) { values.remove(key); }
        public Collection<String> keys() { return new ArrayList<String>(values.keySet()); }
        public void save() { }
    }
    @Test public void adaptedModulesAreVisibleWithoutHypixelOrEmotes() {
        assertEquals(KazerUiBrand.MINECRAFT.equals("1.8.9") ? 47 : 46, ModuleCatalog.ALL.size());
        for(String id:new String[]{"particle_changer","glint_colorizer","scrollable_tooltips","stopwatch","item_physics","light_overlay","weather_changer"})
            assertNotNull(ModuleCatalog.find(id));
        Set<String> ids = new HashSet<String>();
        for (ModuleCatalog.Module module : ModuleCatalog.ALL) assertTrue(ids.add(module.id));
        assertTrue(ids.contains("nametags"));
        assertTrue(ids.contains("overlay"));
        for(String id:new String[]{"coordinates","combo","reach_display"}) assertTrue(id,ids.contains(id));
        for(String id:new String[]{"clock","memory","day_counter","potion_counter","server_address","direction_hud"})assertTrue(id,ids.contains(id));
        assertFalse(ids.contains("hypixel_mods"));
        assertFalse(ids.contains("hypixel_bedwars"));
        for(String id:new String[]{"ping","freelook","fov","block_outline","shiny_pots","2d_items","3d_skins","hitbox","lighting","menu_blur","pvp_info","saturation","tab","titles","zoom","hit_color","color_saturation","damage_tint"})assertTrue(id,ids.contains(id));
        assertFalse(ids.contains("emotes"));
        for (ModuleCatalog.Module module : ModuleCatalog.ALL) for (int size : new int[]{20,52}) {
            String asset = "/assets/kazerui/ui/icons/features/" + module.icon + "-" + size + "x" + size + ".png";
            if(module.id.equals("overlay")) {
                BufferedImage glyph=OverlayGlyph.image(size);assertEquals(size,glyph.getWidth());assertEquals(size,glyph.getHeight());
                int pixels=0;for(int y=0;y<size;y++)for(int x=0;x<size;x++)if((glyph.getRGB(x,y)>>>24)>0)pixels++;
                assertTrue("Owned overlay geometry must visibly render",pixels>size*size/5);
                assertEquals("Transparent corner",0,glyph.getRGB(0,0));continue;
            }
            assertNotNull(asset, getClass().getResource(asset));
        }
    }
    @Test public void profilesFavoritesAndSearchRemainIndependent() {
        Store store = new Store();
        store.put("profiles","Default|中文配置");
        UiModel model = new UiModel(store);
        model.set("fps","enabled","true");
        model.action("profile:中文配置");
        assertFalse(model.flag("fps","enabled",false));
        model.set("fps","enabled","false");
        model.action("profile:Default");
        assertTrue(model.flag("fps","enabled",false));
        model.action("favorite:chat");
        assertTrue(model.favorite(ModuleCatalog.find("chat")));
        assertEquals("chat",model.visibleModules().get(0).id);
        model.setSearch("chat");
        assertEquals("chat", model.visibleModules().get(0).id);
    }
    @Test public void nativeGlobalOptionsDoNotChangeWithProfilesOrReset() {
        Store store=new Store(); store.put("profiles","Default|Other");
        UiModel model=new UiModel(store);
        model.set("global","legacy_particle_amount","40");
        model.action("profile:Other");
        assertEquals("40",model.value("global","legacy_particle_amount",null));
        model.resetProfile("Other");
        assertEquals("40",model.value("global","legacy_particle_amount",null));
        model.action("profile:Default");
        assertEquals("40",model.value("global","legacy_particle_amount",null));
    }
    @Test public void zoomBindingIsSharedWithGeneralAndSurvivesProfileSwitch(){
        Store store=new Store();store.put("profiles","Default|Other");UiModel model=new UiModel(store);
        model.set("zoom","zoomKeybind","Z");assertEquals("Z",store.get("zoomKeyName",null));
        model.action("profile:Other");assertEquals("Z",model.value("zoom","zoomKeybind",null));
        model.resetProfile("Other");assertEquals("Z",model.value("zoom","zoomKeybind",null));
    }
    @Test public void storeUsesUtf8AtomicSavesAndDoesNotRewriteUnchangedValues() throws Exception {
        Path base = Paths.get(System.getProperty("basedir", "."), ".target", "owned-ui-verification");
        Files.createDirectories(base);
        Path dir = Files.createTempDirectory(base, "owned-ui-store-");
        File file = dir.resolve("ui.properties").toFile();
        Configuration config = new Configuration(file);
        config.get("ui","name","中文配置").set("中文配置");
        assertTrue(config.hasChanged()); config.save(); assertFalse(config.hasChanged());
        byte[] before = Files.readAllBytes(file.toPath());
        config.get("ui","name", "fallback").set("中文配置");
        config.save(); assertArrayEquals(before, Files.readAllBytes(file.toPath()));
        Configuration loaded = new Configuration(file); loaded.load();
        assertEquals("中文配置", loaded.getCategory("ui").get("name").getString());
        loaded.getCategory("ui").remove("name"); assertTrue(loaded.hasChanged()); loaded.save();
        assertFalse(loaded.hasKey("ui","name"));
        try (java.util.stream.Stream<Path> paths = Files.list(dir)) { assertEquals(1, paths.count()); }
    }
    @Test public void viewportRoundTripsCoordinates() {
        UiViewport view = new UiViewport(); view.resize(320,240);
        assertEquals(100,view.x(view.left + 100 * view.scale),.0001);
        assertEquals(80,view.y(view.top + 80 * view.scale),.0001);
        assertTrue(view.scale > 0 && view.scale <= 1);
    }
    @Test public void reopenedMenusReuseBoundedReadOnlyGeometryWithoutSharingCanvases() throws Exception {
        UiModel model = new UiModel(new Store());
        UiRenderer first = new UiRenderer();
        BufferedImage image = first.render(model,-1,-1,2,false);
        int[] pixels = image.getRGB(0,0,image.getWidth(),image.getHeight(),null,0,image.getWidth());
        java.lang.reflect.Field field = UiRenderer.class.getDeclaredField("rings");
        field.setAccessible(true);
        assertTrue(java.lang.reflect.Modifier.isStatic(field.getModifiers()));
        Map<?,?> cache = (Map<?,?>)field.get(null);
        Map<?,?> before = new HashMap<Object,Object>(cache);
        assertFalse(before.isEmpty());
        BufferedImage reopened = new UiRenderer().render(model,-1,-1,2,false);
        assertNotSame(image,reopened);
        assertArrayEquals(pixels,reopened.getRGB(0,0,reopened.getWidth(),reopened.getHeight(),null,0,reopened.getWidth()));
        for (Object key : before.keySet()) assertSame(before.get(key),cache.get(key));
        assertTrue(cache.size()<=128);
    }
    @Test public void painterReusesCanvasClearsTransparencyAndReallocatesOnlyOnResize() throws Exception {
        UiModel model = new UiModel(new Store());
        UiRenderer renderer = new UiRenderer();
        BufferedImage first = renderer.render(model,-1,-1,2,false);
        int[] before = first.getRGB(0,0,first.getWidth(),first.getHeight(),null,0,first.getWidth());
        BufferedImage second = renderer.render(model,-1,-1,2,false);
        assertSame(first,second);
        assertArrayEquals(before,second.getRGB(0,0,second.getWidth(),second.getHeight(),null,0,second.getWidth()));
        BufferedImage resized = renderer.render(model,-1,-1,3,false);
        assertNotSame(first,resized);
        assertEquals((UiRenderer.WIDTH + 2 * UiRenderer.PAD) * 3,resized.getWidth());
        File output = new File(".target/owned-ui-verification"); assertTrue(output.isDirectory() || output.mkdirs());
        ImageIO.write(resized,"png",new File(output,"modules-"+KazerUiBrand.MINECRAFT+".png"));
        model.tab = "SETTINGS"; model.settingsTab = "CONTROLS";
        ImageIO.write(renderer.render(model,-1,-1,3,false),"png",new File(output,"controls-"+KazerUiBrand.MINECRAFT+".png"));
    }
}
