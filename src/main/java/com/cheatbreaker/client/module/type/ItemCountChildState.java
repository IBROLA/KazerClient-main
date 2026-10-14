package com.cheatbreaker.client.module.type;

import com.cheatbreaker.client.ui.module.CBGuiAnchor;

public class ItemCountChildState {
    public static final float DEFAULT_TOP_OFFSET = 60.0F;
    public String id;
    public boolean enabled;
    public boolean renderHud = true;
    public String anchor = CBGuiAnchor.MIDDLE_TOP.name();
    public float xTranslation;
    public float yTranslation;
    public float scale = 1.0F;
    public String displayMode;
    public Integer textColor = 0xFFFFFFFF;
    public Boolean showBackground = false;
    public Integer backgroundColor = 0x6F000000;

    public ItemCountChildState() {
    }

    public ItemCountChildState(ItemCountType type) {
        this.id = type.id();
        this.enabled = type == ItemCountType.HEALING_POT || type == ItemCountType.GOLDEN_APPLE;
        this.displayMode = ItemCountContent.defaultDisplayMode(type).name();
        if (type == ItemCountType.HEALING_POT) {
            this.anchor = CBGuiAnchor.RIGHT_TOP.name();
            this.xTranslation = -1.0F;
            this.yTranslation = 11.0F;
        } else if (type == ItemCountType.GOLDEN_APPLE) {
            this.anchor = CBGuiAnchor.RIGHT_TOP.name();
            this.xTranslation = -1.0F;
            this.yTranslation = 19.0F;
        } else {
            this.yTranslation = DEFAULT_TOP_OFFSET;
        }
    }
}
