package com.cheatbreaker.client.kazerui.cosmetics;

/** Stable wire permission bits shared with the presence service. */
public enum CosmeticCategory {
    CLOAKS("Cloaks",1), HEADWEAR("Headwear",2), PETS("Pets",4), BODYWEAR("Bodywear",8), AURAS("Auras",16);
    public final String label;
    public final int bit;
    CosmeticCategory(String label,int bit){this.label=label;this.bit=bit;}
    private static final CosmeticCategory[] VISIBLE={CLOAKS,HEADWEAR,PETS};
    public static CosmeticCategory[] visible() { return VISIBLE.clone(); }
    public static CosmeticCategory of(CosmeticType type) {
        if(type==CosmeticType.CLOAK)return CLOAKS;
        if(CosmeticType.HEADWEAR.covers(type))return HEADWEAR;
        if(type==CosmeticType.PET || type==CosmeticType.COMPANION)return PETS;
        if(type==CosmeticType.AURAS)return AURAS;
        return BODYWEAR;
    }
}
