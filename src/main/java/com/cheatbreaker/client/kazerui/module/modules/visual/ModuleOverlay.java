package com.cheatbreaker.client.kazerui.module.modules.visual;

import com.cheatbreaker.client.kazerui.module.Module;
import com.cheatbreaker.client.kazerui.module.Page;
import com.cheatbreaker.client.kazerui.module.setting.*;
import net.minecraft.block.Block;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemMap;
import net.minecraft.item.ItemStack;

/** Render-only policy. Never changes entity timers, armor stacks, block state or packets. */
public final class ModuleOverlay extends Module {
    private static ModuleOverlay instance;
    private long appliedRevision=Long.MIN_VALUE;
    private int blockSignature,textureSignature;
    private boolean blocksPending,palettePending;
    private long changedAt;
    public enum Glint implements ChoiceSetting.Option {
        ALL("All"),HELD("Items"),ARMOR("Armor"),NONE("None");
        private final String label;Glint(String label){this.label=label;}public String langId(){return label;}
    }
    public enum Portals implements ChoiceSetting.Option {
        NEVER("Never"),ALWAYS("Always"),OVERWORLD("Overworld");
        private final String label;Portals(String label){this.label=label;}public String langId(){return label;}
    }
    public enum Glass implements ChoiceSetting.Option {
        OFF("Off"),REGULAR("Regular"),ALL("All");
        private final String label;Glass(String label){this.label=label;}public String langId(){return label;}
    }
    private final ChoiceSetting<Glint> glint=choice("glint",Glint.ALL);
    private final BoolSetting minimalBob=bool("minimalBob",true),mapBob=bool("noMapBob",false);
    private final NumberSetting fireHeight=decimal("fireHeight",0,0,1),blockFireHeight=decimal("blockFireHeight",0,0,1),pumpkin=decimal("pumpkinOpacity",0,0,1);
    private final BoolSetting foliage=bool("hideFoliage",false),shadows=bool("entityShadows",true),groundArrows=bool("groundArrows",true),stuckArrows=bool("stuckArrows",true),skulls=bool("hideSkulls",false);
    private final ChoiceSetting<Portals> portals=choice("hideEndPortals",Portals.NEVER);
    private final ChoiceSetting<Glass> glass=choice("clearGlass",Glass.OFF);
    private final BoolSetting coloredString=bool("coloredString",true);
    private final ColorSetting stringColor=color("stringColor",0xFFFF0000).noAlpha().noChroma();
    private final BoolSetting oreOutlines=bool("oreOutlines",false);
    private final BoolSetting barrierOutlines=bool("barrierOutlines",false);
    private final ColorSetting barrierColor=color("barrierColor",0xFFFFFFFF).noAlpha().noChroma();
    private final ColorSetting oreColor=color("oreColor",0xFFFFFFFF).noAlpha().noChroma();
    private final BoolSetting xpCustom=bool("customXpColor",false),fishingCustom=bool("customFishingLine",false),guiCustom=bool("customGuiColors",false);
    private final ColorSetting xpColor=color("xpColor",0xFF80FF20).noAlpha().noChroma(),fishingColor=color("fishingColor",0xFF000000).noAlpha(),guiColor=color("guiColor",0xFFFFFFFF).noAlpha().noChroma();
    private final BoolSetting headless=bool("headlessHorse",false),death=bool("disableDeathAnimation",false),damage=bool("disableDamageOverlay",false),fire=bool("disableFireOverlay",true);
    private final BoolSetting helmet=bool("hideHelmet",false),chest=bool("hideChestplate",false),legs=bool("hideLeggings",false),boots=bool("hideBoots",false),self=bool("armorSelfOnly",true);
    public ModuleOverlay() {
        super("OVERLAY",false);instance=this;
        glint.label(()->"Show Enchantment Glint");minimalBob.label(()->"Minimal View Bobbing");mapBob.label(()->"No Map Bobbing");
        fireHeight.label(()->"Fire Height (Overlay)");blockFireHeight.label(()->"Fire Height (Block)");pumpkin.label(()->"Pumpkin Overlay");
        foliage.label(()->"Hide Foliage");shadows.label(()->"Entity Shadows");groundArrows.label(()->"Show Ground Arrows");stuckArrows.label(()->"Show Stuck Arrows");
        skulls.label(()->"Hide Placed Skulls");portals.label(()->"Hide Endportals");glass.label(()->"Clear Glass");coloredString.label(()->"Colored String");stringColor.label(()->"String Color");
        oreOutlines.label(()->"Ore Outlines");oreColor.label(()->"Outline Color");
        oreOutlines.onChange(ModuleOverlay::rebuild);oreColor.onChange(ModuleOverlay::rebuild);
        barrierOutlines.label(()->"Barrier Outlines");barrierColor.label(()->"Barrier Color");
        barrierOutlines.onChange(ModuleOverlay::rebuild);barrierColor.onChange(ModuleOverlay::rebuild);
        xpCustom.label(()->"Custom XP Color");xpColor.label(()->"XP Bar / Level Color");
        fishingCustom.label(()->"Custom Fishing Line");fishingColor.label(()->"Fishing Line Color");
        guiCustom.label(()->"Custom GUI Colors");guiColor.label(()->"Container Texture Color");
        xpCustom.onChange(ModuleOverlay::requestPalette);xpColor.onChange(ModuleOverlay::requestPalette);
        guiCustom.onChange(ModuleOverlay::requestPalette);guiColor.onChange(ModuleOverlay::requestPalette);
        headless.label(()->"Headless Horse");death.label(()->"Disable Death Animation");damage.label(()->"Disable Damage Overlay");fire.label(()->"Disable Fire Overlay");
        helmet.label(()->"Hide Helmet");chest.label(()->"Hide Chestplate");legs.label(()->"Hide Leggings");boots.label(()->"Hide Boots");self.label(()->"Only Hide for Self");
        foliage.onChange(ModuleOverlay::rebuild);glass.onChange(ModuleOverlay::rebuild);blockFireHeight.onChange(ModuleOverlay::rebuild);
        coloredString.onChange(ModuleOverlay::rebuild);stringColor.onChange(ModuleOverlay::rebuild);
    }
    protected void layout(Page page) {
        page.section("First Person Options",s->s.add(glint,minimalBob,mapBob,fireHeight,blockFireHeight,pumpkin));
        page.section("World Options",s->s.add(foliage,shadows,groundArrows,stuckArrows,skulls,portals));
        page.section("Texture Options",s->{s.add(glass);s.group(oreOutlines,g->g.add(oreColor));s.group(coloredString,g->g.add(stringColor));
            if(com.cheatbreaker.client.kazerui.KazerUiBrand.MINECRAFT.equals("1.8.9"))s.group(barrierOutlines,g->g.add(barrierColor));
            s.group(xpCustom,g->g.add(xpColor));s.group(fishingCustom,g->g.add(fishingColor));s.group(guiCustom,g->g.add(guiColor));});
        page.section("Entity Render Options",s->s.add(headless,death,damage,fire));
        page.section("Armor Options",s->s.add(helmet,chest,legs,boots,self));
    }
    public static void migrateLegacy(com.cheatbreaker.client.config.GlobalSettings settings) {
        if(instance==null)return;
        com.cheatbreaker.client.kazerui.gui.ui.UiModel model=com.cheatbreaker.client.kazerui.module.ModuleManager.model();
        String marker="migration.overlay.v1."+model.profile();
        if("true".equals(model.store.get(marker,null)))return;
        seed(model,"glint",(Boolean)settings.enchantmentGlint.getValue()?"ALL":"NONE");
        seed(model,"clearGlass",String.valueOf(settings.clearGlass.getValue()));
        seed(model,"coloredString",String.valueOf(settings.redString.getValue()));
        model.store.put(marker,"true");model.revision++;
    }
    private static void seed(com.cheatbreaker.client.kazerui.gui.ui.UiModel model,String key,String value) {
        if(model.value("overlay",key,null)==null)model.set("overlay",key,value);
    }
    protected void onEnable(){rebuild();requestPalette();}protected void onDisable(){rebuild();requestPalette();}
    private static void rebuild(){
        if(instance!=null){instance.blocksPending=true;instance.changedAt=System.nanoTime();}
    }
    private static void requestPalette(){if(instance!=null){instance.palettePending=true;instance.changedAt=System.nanoTime();}}
    public void tick(com.cheatbreaker.client.kazerui.event.UiTick.ClientTickEvent event) {
        if(event.phase!=com.cheatbreaker.client.kazerui.event.UiTick.Phase.START)return;
        long revision=com.cheatbreaker.client.kazerui.module.ModuleManager.model().revision;
        if(revision!=appliedRevision) {
            appliedRevision=revision;int blocks=blockSignature(),textures=textureSignature();
            if(blocks!=blockSignature){blockSignature=blocks;rebuild();}
            if(textures!=textureSignature){textureSignature=textures;requestPalette();}
        }
        // Coalesce drag/color edits. Never rebuild chunks or decode GUI textures
        // for every slider pixel; closing the GUI flushes the latest state.
        if((blocksPending||palettePending)&&mc()!=null&&(mc().currentScreen==null||System.nanoTime()-changedAt>=150000000L)) {
            if(blocksPending){blocksPending=false;if(mc().theWorld!=null&&mc().renderGlobal!=null)mc().renderGlobal.loadRenderers();}
            if(palettePending){palettePending=false;OverlayTexturePalette.reload();}
        }
    }
    private int blockSignature() {
        if(!isEnabled())return 0;
        int signature=1;signature=signature*31+(foliage.on()?1:0);signature=signature*31+glass.get().ordinal();
        signature=signature*31+Float.floatToIntBits(blockFireHeight.value());
        signature=signature*31+(coloredString.on()?stringColor.argb():0xFFFFFF);
        signature=signature*31+(oreOutlines.on()?oreColor.argb():0);
        return signature*31+(barrierOutlines.on()?barrierColor.argb():0);
    }
    private int textureSignature() {
        if(!isEnabled())return 0;return (xpCustom.on()?xpColor.argb():0)*31+(guiCustom.on()?guiColor.argb():0);
    }
    public static void paletteApplied(){if(instance!=null)instance.textureSignature=instance.textureSignature();}
    private static ModuleOverlay active(){ModuleOverlay m=instance;return m!=null&&m.isEnabled()?m:null;}
    public static boolean glint(boolean original,boolean armor) {
        ModuleOverlay m=active();if(m==null)return ModuleGlintColorizer.visible(original);
        Glint mode=m.glint.get();return ModuleGlintColorizer.visible(mode==Glint.ALL||(armor?mode==Glint.ARMOR:mode==Glint.HELD));
    }
    public static float bobScale(){ModuleOverlay m=active();if(m==null)return 1;
        ItemStack held=mc().thePlayer==null?null:mc().thePlayer.getHeldItem();
        return m.mapBob.on()&&held!=null&&held.getItem() instanceof ItemMap?0:m.minimalBob.on()?.2F:1;
    }
    public static float fireOffset(){ModuleOverlay m=active();return m==null?0:m.fireHeight.value();}
    public static float blockFireOffset(){ModuleOverlay m=active();return m==null?0:m.blockFireHeight.value();}
    public static boolean blockTweaks(){ModuleOverlay m=active();return m!=null&&(m.blockFireHeight.value()>0||m.oreOutlines.on());}
    public static boolean outlines(Block block){ModuleOverlay m=active();return m!=null&&m.oreOutlines.on()&&(block instanceof net.minecraft.block.BlockOre||block==Blocks.lit_redstone_ore||block==Blocks.redstone_ore);}
    public static int outlineColor(){ModuleOverlay m=active();return m==null?0xFFFFFFFF:m.oreColor.color(0);}
    public static boolean barriers(){ModuleOverlay m=active();return m!=null&&m.barrierOutlines.on();}
    public static int barrierColor(){ModuleOverlay m=active();return m==null?0xFFFFFFFF:m.barrierColor.color(0);}
    public static boolean customXp(){ModuleOverlay m=active();return m!=null&&m.xpCustom.on();}
    public static int xpColor(int original){ModuleOverlay m=active();return m!=null&&m.xpCustom.on()?m.xpColor.color(0):original;}
    public static boolean customGui(){ModuleOverlay m=active();return m!=null&&m.guiCustom.on();}
    public static int guiColor(){ModuleOverlay m=active();return m==null?0xFFFFFFFF:m.guiColor.color(0);}
    public static int fishingColor(int original){ModuleOverlay m=active();return m!=null&&m.fishingCustom.on()?m.fishingColor.color(0):original;}
    public static float pumpkinAlpha(){ModuleOverlay m=active();return m==null?1:m.pumpkin.value();}
    public static boolean shadows(){ModuleOverlay m=active();return m==null||m.shadows.on();}
    public static boolean groundArrows(){ModuleOverlay m=active();return m==null||m.groundArrows.on();}
    public static boolean stuckArrows(){ModuleOverlay m=active();return m==null||m.stuckArrows.on();}
    public static boolean hideSkulls(){ModuleOverlay m=active();return m!=null&&m.skulls.on();}
    public static boolean hidePortals(){ModuleOverlay m=active();return m!=null&&(m.portals.get()==Portals.ALWAYS||m.portals.get()==Portals.OVERWORLD&&mc().theWorld!=null&&mc().theWorld.provider.getDimensionId()==0);}
    public static boolean hideBlock(Block block){ModuleOverlay m=active();return m!=null&&m.foliage.on()&&(block instanceof net.minecraft.block.BlockLeaves||block==Blocks.tallgrass||block==Blocks.double_plant||block==Blocks.vine||block==Blocks.deadbush);}
    public static String glass(String original){ModuleOverlay m=active();return m==null?original:m.glass.get().name();}
    public static int stringColor(int original){ModuleOverlay m=active();return m==null?original:m.coloredString.on()?m.stringColor.color(0):0xFFFFFF;}
    public static boolean headless(){ModuleOverlay m=active();return m!=null&&m.headless.on();}
    public static boolean death(){ModuleOverlay m=active();return m!=null&&m.death.on();}
    public static boolean damage(){ModuleOverlay m=active();return m!=null&&m.damage.on();}
    public static boolean fire(){ModuleOverlay m=active();return m!=null&&m.fire.on();}
    public static boolean hideArmor(Entity entity,int slot){ModuleOverlay m=active();if(m==null||slot<0||slot>3||m.self.on()&&entity!=mc().thePlayer)return false;
        return (slot==0?m.helmet:slot==1?m.chest:slot==2?m.legs:m.boots).on();
    }
    public static int renderDeathTime(EntityLivingBase entity){return death()?0:entity.deathTime;}
    public static int renderDamageDeathTime(EntityLivingBase entity){return damage()?0:entity.deathTime;}
    public static int renderHurtTime(EntityLivingBase entity){return damage()?0:entity.hurtTime;}
}
