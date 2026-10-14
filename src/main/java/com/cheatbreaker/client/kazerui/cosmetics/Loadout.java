package com.cheatbreaker.client.kazerui.cosmetics;

import com.cheatbreaker.client.kazerui.gui.ui.UiStore;
import java.util.*;

/** Immutable per-loadout lists and wire strings; nothing is rebuilt per rendered player. */
public final class Loadout {
    private static UiStore store;
    private static volatile List<Cosmetic> equipped=Collections.emptyList();
    private static volatile String wire="";
    private static int permissionMask=-1;
    public static void initialize(UiStore settings) { store=settings;permissionMask=-1;refreshPermissions(); }
    public static List<Cosmetic> equipped() { return equipped; }
    public static String wire() { return wire; }
    public static void refreshPermissions() {
        int mask=CosmeticPermissions.mask();if(permissionMask==mask || store==null)return;
        permissionMask=mask;
        List<Cosmetic> next=new ArrayList<Cosmetic>();
        for(Cosmetic item:decode(store.get("cosmetics.equipped","")))if(CosmeticPermissions.allows(item))next.add(item);
        equipped=Collections.unmodifiableList(next);wire=encode(next);
    }
    public static boolean isEquipped(Cosmetic cosmetic) { for(Cosmetic item:equipped)if(item.id==cosmetic.id)return true;return false; }
    public static void toggle(Cosmetic cosmetic) {
        refreshPermissions();
        if(store==null || !cosmetic.renderable() || !CosmeticPermissions.allows(cosmetic))return;
        List<Cosmetic> next=new ArrayList<Cosmetic>();boolean remove=isEquipped(cosmetic);
        for(Cosmetic item:equipped)if(item.slot()!=cosmetic.slot())next.add(item);
        if(!remove && next.size()<8)next.add(cosmetic);
        equipped=Collections.unmodifiableList(next);wire=encode(next);store.put("cosmetics.equipped",wire);
    }
    public static void clear() { equipped=Collections.emptyList();wire="";if(store!=null)store.put("cosmetics.equipped",""); }
    public static List<Cosmetic> decode(String value) {
        if(value==null || value.length()>128)return Collections.emptyList();
        Map<CosmeticType,Cosmetic> slots=new LinkedHashMap<CosmeticType,Cosmetic>();
        for(String id:value.split(",")) try {
            if(slots.size()>=8)break;Cosmetic item=CosmeticCatalog.get(Integer.parseInt(id));
            if(item!=null && item.renderable())slots.put(item.slot(),item);
        }catch(NumberFormatException ignored){}
        return Collections.unmodifiableList(new ArrayList<Cosmetic>(slots.values()));
    }
    private static String encode(List<Cosmetic> list) { StringBuilder result=new StringBuilder();for(Cosmetic item:list) { if(result.length()>0)result.append(',');result.append(item.id); }return result.toString(); }
    private Loadout() {}
}
