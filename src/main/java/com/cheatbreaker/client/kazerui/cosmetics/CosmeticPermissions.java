package com.cheatbreaker.client.kazerui.cosmetics;

import net.minecraft.client.Minecraft;

/** All five categories are public; badges remain independent of equipment. */
public final class CosmeticPermissions {
    private static volatile String subject="";
    private static volatile int grants=1;
    public static volatile long revision;
    private static net.minecraft.util.Session session;
    private static String localId="";
    public static int mask() {
        return 31;
    }
    public static boolean allows(CosmeticCategory category){return (mask() & category.bit)!=0;}
    public static boolean allows(Cosmetic item){return allows(CosmeticCategory.of(item.type));}
    public static void accept(String uuid,int mask) {
        String id=uuid.replace("-","").toLowerCase(java.util.Locale.ROOT);
        int next=(mask & 31)|1;
        if(!id.equals(subject)||next!=grants){subject=id;grants=next;revision++;}
    }
    private CosmeticPermissions(){}
}
