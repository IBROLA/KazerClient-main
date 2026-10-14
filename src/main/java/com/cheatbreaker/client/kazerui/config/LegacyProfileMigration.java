package com.cheatbreaker.client.kazerui.config;

import com.cheatbreaker.client.kazerui.gui.ui.UiStore;
import java.io.*;
import java.util.*;

/** Read-only legacy parser. Callers choose an isolated fixture or their real config;
 * original files are never rewritten or deleted. Unsupported values remain recoverable. */
public final class LegacyProfileMigration {
    private static final Map<String,String> MODULE_IDS = new LinkedHashMap<String,String>();
    private static final Map<String,String> OPTIONS = new HashMap<String,String>();
    private static final Map<String,String> ANCHORS = new HashMap<String,String>();
    static {
        module("Armor Status","armorstatus"); module("ToggleSprint","toggle_sneak");
        module("Toggle Sprint","toggle_sneak"); module("Potion Effects","potion_effects");
        module("Key Strokes","keystrokes"); module("Keystrokes","keystrokes");
        module("FPS","fps"); module("CPS","cps"); module("Nametags","nametags");
        module("Boss bar","bossbar"); module("Motion Blur","motion_blur");
        module("Scoreboard","scoreboard"); module("Chat","chat");
        module("1.7 Visuals","one_seven_visuals");
        module("Freelook","freelook");
        option("Scale","scale"); option("Reverse Order","reverseOrder");
        option("Show Background","background"); option("Background Color","backgroundColor");
        option("Text Color","textColor"); option("Text Shadow","textShadow");
        option("Bracket Color","bracketColor"); option("Show Brackets","brackets");
        option("Right Click","rightClick"); option("Show CPS Text","showCPSText");
        option("Border","border"); option("Border Color","borderColor");
        option("Border Thickness","borderThickness");
        String[] old = {"LEFT_TOP","MIDDLE_TOP","RIGHT_TOP","LEFT_MIDDLE","MIDDLE_MIDDLE","RIGHT_MIDDLE",
            "LEFT_BOTTOM","MIDDLE_BOTTOM_LEFT","MIDDLE_BOTTOM_RIGHT","RIGHT_BOTTOM"};
        String[] own = {"topLeft","topCenter","topRight","middleLeft","middleCenter","middleRight",
            "bottomLeft","bottomCenterLeft","bottomCenterRight","bottomRight"};
        for (int i=0;i<old.length;i++) ANCHORS.put(old[i],own[i]);
    }
    private static void module(String name,String id) { MODULE_IDS.put(name.toLowerCase(Locale.ROOT),id); }
    private static void option(String label,String key) { OPTIONS.put(label.toLowerCase(Locale.ROOT),key); }
    public static String moduleId(String name) { return MODULE_IDS.get(name.toLowerCase(Locale.ROOT)); }
    private static void missing(UiStore store,String key,String value) {
        if (store.get(key,null) == null) store.put(key,value);
    }
    /** Imports only siblings of the new owned config, never traverses game folders. */
    public static void readDirectory(File directory, UiStore store) {
        if (directory == null || !directory.isDirectory()) return;
        if ("true".equals(store.get("migration.legacy.v1",null))) return;
        try {
            File base = directory.getCanonicalFile();
            File global = new File(base, "global.cfg");
            if (global.isFile() && global.getCanonicalFile().getParentFile().equals(base))
                try (Reader input = new InputStreamReader(new FileInputStream(global), java.nio.charset.StandardCharsets.UTF_8)) {
                    readGlobal(input, store);
                }
            File profiles = new File(base, "profiles");
            if (!profiles.getCanonicalFile().getParentFile().equals(base)) return;
            File[] files = profiles.listFiles();
            if (files == null) {
                store.put("migration.legacy.v1","true");
                return;
            }
            Arrays.sort(files, Comparator.comparing(File::getName));
            File profileBase = profiles.getCanonicalFile();
            for (File file : files) {
                String name = file.getName();
                if (!file.isFile() || !name.endsWith(".cfg") || !file.getCanonicalFile().getParentFile().equals(profileBase)) continue;
                String profile = name.substring(0, name.length()-4);
                if (profile.isEmpty() || profile.contains("|")) continue;
                if (profile.equalsIgnoreCase("default")) profile = "Default";
                try (Reader input = new InputStreamReader(new FileInputStream(file), java.nio.charset.StandardCharsets.UTF_8)) {
                    readProfile(input, profile, store);
                }
            }
            store.put("migration.legacy.v1","true");
        } catch (IOException failure) {
            org.apache.logging.log4j.LogManager.getLogger().warn("[KC] Could not read legacy UI configuration; originals remain untouched", failure);
        }
    }
    public static void readProfile(Reader input,String profile,UiStore store) throws IOException {
        if (profile == null || profile.isEmpty() || profile.contains("|")) throw new IllegalArgumentException("Invalid profile name");
        Set<String> profiles = new LinkedHashSet<String>(Arrays.asList(store.get("profiles","Default").split("\\|")));
        profiles.add(profile); store.put("profiles",String.join("|",profiles));
        BufferedReader reader = input instanceof BufferedReader ? (BufferedReader)input : new BufferedReader(input);
        Set<String> positions = new HashSet<String>();
        Map<String,String> anchors = new HashMap<String,String>();
        String line, section = "", id = null;
        while ((line=reader.readLine()) != null) {
            if (line.isEmpty() || line.startsWith("#")) continue;
            if (line.startsWith("[") && line.endsWith("]")) {
                section=line.substring(1,line.length()-1); id=moduleId(section); continue;
            }
            int equal=line.indexOf('='); if (equal<1 || section.isEmpty()) continue;
            String label=line.substring(0,equal), raw=line.substring(equal+1);
            missing(store,"legacy.profile."+profile+"."+section+"."+label,raw);
            if (id == null) continue;
            String prefix="profile."+profile+"."+id+".", key=null, value=raw;
            if (label.equals("-State")) key="enabled";
            else if (label.equals("-RenderHUD")) key="renderHud";
            else if (label.equals("-xTranslation")) key="x";
            else if (label.equals("-yTranslation")) key="y";
            else if (label.equals("-Anchor")) { key="anchor"; value=ANCHORS.get(raw); }
            else if (id.equals("motion_blur") && label.equals("Amount")) {
                key="value";
                try { double amount=Double.parseDouble(raw);value=Double.isNaN(amount)||Double.isInfinite(amount) ? null : String.valueOf(Math.max(1,Math.min(10,(int)Math.round(amount/10)))); }
                catch(NumberFormatException invalid){value=null;}
            }
            else if (id.equals("motion_blur") && label.equals("Mode")) {
                key="type";value=raw.equals("Old") ? "V1" : raw.equals("Phosphor") ? "V2" : "V3";
            }
            else {
                key=OPTIONS.get(label.toLowerCase(Locale.ROOT));
                if (key != null && key.endsWith("Color")) {
                    String[] fields=raw.split(";",2);
                    try { value=String.format(Locale.ROOT,"%08X",Integer.parseInt(fields[0])); }
                    catch (NumberFormatException invalid) { value=null; }
                    if (value != null && fields.length>1 && fields[1].equalsIgnoreCase("rainbow"))
                        missing(store,prefix+key+"Chroma","true");
                }
            }
            if (key != null && value != null) {
                if ((key.equals("x")||key.equals("y")) && store.get(prefix+key,null)==null) positions.add(prefix+key);
                if (key.equals("anchor")) anchors.put(prefix,raw);
                missing(store,prefix+key,value);
            }
        }
        for (Map.Entry<String,String> entry:anchors.entrySet()) {
            float dx=0,dy=0;
            switch(entry.getValue()) {
                case "MIDDLE_BOTTOM_LEFT": dx=-30;break;
                case "MIDDLE_BOTTOM_RIGHT": dx=30;break;
                case "RIGHT_MIDDLE": dx=2;break;
                case "RIGHT_BOTTOM": dx=2;dy=2;break;
                default: break;
            }
            offset(store,positions,entry.getKey()+"x",dx);
            offset(store,positions,entry.getKey()+"y",dy);
        }
    }
    private static void offset(UiStore store,Set<String> positions,String key,float amount) {
        if (amount==0||!positions.contains(key)) return;
        try {
            float old=Float.parseFloat(store.get(key,"0"));
            if (!Float.isNaN(old)&&!Float.isInfinite(old)) store.put(key,String.valueOf(old+amount));
        } catch(NumberFormatException invalid) { /* The original raw value is still preserved. */ }
    }
    public static void readGlobal(Reader input,UiStore store) throws IOException {
        BufferedReader reader=input instanceof BufferedReader ? (BufferedReader)input : new BufferedReader(input);
        String line;
        while ((line=reader.readLine()) != null) {
            int equal=line.indexOf('='); if (equal<1 || line.startsWith("#")) continue;
            String label=line.substring(0,equal), value=line.substring(equal+1);
            missing(store,"legacy.global."+label,value);
            if (label.equals("FavoriteModules")) {
                for (String name:value.split(",")) {
                    String id=moduleId(name.trim()); if (id != null) missing(store,"favorite."+id,"true");
                }
            } else if (label.equals("ActiveProfile")) missing(store,"activeProfile",value.equalsIgnoreCase("default") ? "Default" : value);
        }
    }
    /** New replacement migrations also run for stores whose read-only v1 import
     * already completed. Existing new values win; raw old fields stay recoverable. */
    public static void finishReplacements(UiStore store,java.util.List<String> profiles) {
        finishChat(store,profiles);
        finishHud(store,profiles);
        if("true".equals(store.get("migration.replacements.v2",null)))return;
        String[][] toggles={{"Toggle Sprint","toggleSprint"},{"Toggle Sneak","toggleSneak"},
            {"Double Tap","doubleTap"},{"Fly Boost","flyBoost"},{"Fly Boost Amount","flyBoostAmount"}};
        String[][] hud={{"Show HUD Text","enabled"},{"Show While Typing","showWhileTyping"}};
        for(String profile:profiles) {
            String old="legacy.profile."+profile+".ToggleSprint.",parent="profile."+profile+".toggle_sneak.",child="profile."+profile+".toggle_sneak_hud_child.";
            for(String[] pair:toggles)copyMissing(store,old+pair[0],parent+pair[1]);
            for(String[] pair:hud)copyMissing(store,old+pair[0],child+pair[1]);
            String flying=store.get(old+"Fly String",null);
            if(flying!=null)missing(store,child+"flyingText",unbracket(flying));
            for(String key:new String[]{"x","y","anchor","scale","textColor","textColorChroma","renderHud"})copyMissing(store,parent+key,child+key);
            String blur="profile."+profile+".motion_blur.",legacy="legacy.profile."+profile+".Motion Blur.";
            String mode=store.get(legacy+"Mode",null);
            if(mode!=null)missing(store,blur+"type",mode.equals("Old")?"V1":mode.equals("Phosphor")?"V2":"V3");
            String amount=store.get(legacy+"Amount",null);
            if(amount!=null)try{double n=Double.parseDouble(amount);if(Double.isFinite(n))missing(store,blur+"value",String.valueOf(Math.max(1,Math.min(10,(int)Math.round(n/10)))));}catch(NumberFormatException ignored){}
        }
        store.put("migration.replacements.v2","true");
    }
    private static void copyMissing(UiStore store,String from,String to){String value=store.get(from,null);if(value!=null)missing(store,to,value);}
    private static void map(UiStore store,String old,String target,String[][] fields) {
        for(String[] field:fields)copyMissing(store,old+field[0],target+field[1]);
    }
    private static void color(UiStore store,String old,String target) {
        String raw=store.get(old,null);if(raw==null)return;
        String[] fields=raw.split(";",2);
        try {
            missing(store,target,String.format(Locale.ROOT,"%08X",Integer.parseInt(fields[0])));
            if(fields.length>1&&fields[1].equalsIgnoreCase("rainbow"))missing(store,target+"Chroma","true");
        }catch(NumberFormatException invalid) { /* Keep malformed legacy values in the raw snapshot. */ }
    }
    private static void choice(UiStore store,String old,String target,String... pairs) {
        String raw=store.get(old,null);if(raw==null)return;
        for(int i=0;i<pairs.length;i+=2)if(raw.equalsIgnoreCase(pairs[i])){missing(store,target,pairs[i+1]);return;}
    }
    /** Match semantic setting keys, including renamed fields and child switches.
     * The one-time marker prevents a later user reset from resurrecting old choices. */
    private static void finishHud(UiStore store,java.util.List<String> profiles) {
        if("true".equals(store.get("migration.hud.v1",null)))return;
        for(String profile:profiles) {
            String raw="legacy.profile."+profile+".",prefix="profile."+profile+".";
            map(store,raw+"CPS.",prefix+"cps.",new String[][]{{"Right Click CPS","rightClick"},{"Reverse Text","reverseText"},{"Ignore Cancelled Clicks","ignoreCancelledClicks"}});
            map(store,raw+"Scoreboard.",prefix+"scoreboard.",new String[][]{{"Hide Numbers","numbers"},{"Hide Scoreboard","hideScoreboard"},{"Display Toggle Message","displayToggleMessage"}});
            color(store,raw+"Scoreboard.Background Color",prefix+"scoreboard.headerColor");
            map(store,raw+"Boss bar.",prefix+"bossbar.",new String[][]{{"Render Health","renderBar"},{"Render Text","renderBossText"},{"Use Custom Boss Bar","customBossBar"},{"Use Minecraft GUI Scale","useMinecraftGUIScale"}});
            map(store,raw+"Potion Effects.",prefix+"potion_effects.",new String[][]{
                {"Show While Typing","showWhileTyping"},{"Show Effect Name","effectName"},{"Show Effect Duration","effectDuration"},{"Show Effect Amplifier","effectAmplifier"},
                {"Formatted Durations","formattedDurations"},{"Uppercase Potion Names","uppercasePotionNames"},{"Reversed Text","reversedText"},{"Show In Inventory","showInInventory"},
                {"Hide Potion Status","hidePotionStatus"},{"Effect Duration Bars","effectBars"},{"Blink","potionBlink"},{"Blink Duration","blinkDuration"},
                {"Color Name Based on Effect","colorNameBasedOnEffect"},{"Color Info Based on Effect","colorInfoBasedOnEffect"}});
            color(store,raw+"Potion Effects.Name Color",prefix+"potion_effects.textColor");
            color(store,raw+"Potion Effects.Duration Color",prefix+"potion_effects.infoColor");
            choice(store,raw+"Potion Effects.Mode",prefix+"potion_effects.potionEffectsMode","Normal","NORMAL","Compact","MINIMAL");
            map(store,raw+"Key Strokes.",prefix+"keystrokes.",new String[][]{
                {"Show clicks","keyStrokesClicks"},{"Show movement keys","keyStrokesMovement"},{"Show spacebar","keyStrokesSpacebar"},{"Replace names with arrows","useArrows"},
                {"Animate","animate"},{"Box size","boxSize"},{"Fade Time","keyFadeDelay"}});
            color(store,raw+"Key Strokes.Text Color (Pressed)",prefix+"keystrokes.textPressedColor");
            color(store,raw+"Key Strokes.Background Color (Pressed)",prefix+"keystrokes.backgroundPressedColor");
            choice(store,raw+"Key Strokes.Mode",prefix+"keystrokes.keystrokesMode","Grouped","GROUPED","Separated","INDIVIDUAL");
            map(store,raw+"Armor Status.",prefix+"armorstatus.",new String[][]{
                {"Move Armor Pieces Individually","moveArmorIndividually"},{"Vanilla Mode","vanillaMode"},{"Item Name","itemName"},{"Item Count","itemCount"},{"Show While Typing","showWhileTyping"},
                {"Damage Overlay","damageOverlay"},{"Show Item Damage","itemDamage"},{"Show Armor Damage","armorDamage"},{"Show Max Damage","maxDamage"}});
            choice(store,raw+"Armor Status.List Mode",prefix+"armorstatus.listMode","vertical","VERTICAL","horizontal","HORIZONTAL");
            choice(store,raw+"Armor Status.Damage Display Type",prefix+"armorstatus.damageDisplay","value","VALUE","percent","PERCENT","none","NONE");
            map(store,raw+"Armor Status.",prefix+"armorstatus_",new String[][]{
                {"Show Held Item","held_item_child.enabled"},{"Show Helmet","helmet_child.enabled"},{"Show Chestplate","chestplate_child.enabled"},
                {"Show Leggings","leggings_child.enabled"},{"Show Boots","boots_child.enabled"},{"Protection","protection_child.enabled"},{"Armor Bars","bars_child.enabled"}});
        }
        store.put("migration.hud.v1","true");
    }
    private static void finishChat(UiStore store,java.util.List<String> profiles) {
        if ("true".equals(store.get("migration.chat.v2",null))) return;
        for (String profile : profiles) {
            String old = "legacy.profile." + profile + ".Chat.", prefix = "profile." + profile + ".chat.";
            copyMissing(store,old+"Show Background",prefix+"showBackground");
            copyMissing(store,prefix+"legacy_show_background",prefix+"showBackground");
            copyMissing(store,old+"Show Copy Notifications",prefix+"showCopyNotifications");
            copyMissing(store,old+"Show Visibility Notifications",prefix+"showVisibilityNotifications");
            // Legacy Ctrl-left copy was always available. Preserve it without stealing
            // ordinary left-click server actions; right-click is the new optional path.
            missing(store,prefix+"copyChat","true");missing(store,prefix+"copyChatRightClick","false");
        }
        store.put("migration.chat.v2","true");
    }
    /** Source TextHud owns its brackets; a legacy full label must not nest them. */
    public static String unbracket(String value){return value.length()>=2 && value.startsWith("[") && value.endsWith("]")?value.substring(1,value.length()-1):value;}
    private LegacyProfileMigration() { }
}
