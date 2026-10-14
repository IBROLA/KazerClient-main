package com.cheatbreaker.client.module.type.nametag;

import com.cheatbreaker.client.config.Setting;
import com.cheatbreaker.client.module.AbstractModule;

/** User-facing controls for vanilla and server-provided player nameplates. */
public final class NametagModule extends AbstractModule {
    public final Setting textShadow;
    public final Setting thirdPersonNametag;
    public final Setting displayToggleMessage;
    public final Setting hideInFirstPerson;
    public final Setting opacity;
    public final Setting replaceOwnColor;
    public final Setting showBadgeInTab;
    public final Setting showNametagIcon;

    public NametagModule() {
        super("Nametags");
        setDefaultState(true);
        textShadow = new Setting(this, "Nametag Text Shadow").setValue(false);
        thirdPersonNametag = new Setting(this, "Third Person Nametag").setValue(true);
        displayToggleMessage = new Setting(this, "Display Toggle Nametags Message").setValue(true);
        hideInFirstPerson = new Setting(this, "Hide Nametags in F1").setValue(false);
        opacity = new Setting(this, "Nametag Opacity").setValue(1.0F).setMinMax(0.1F, 1.0F);
        replaceOwnColor = new Setting(this, "Replace Own Nametag Color").setValue(false);
        showBadgeInTab = new Setting(this, "Show Kazer Icon in TAB").setValue(true);
        showNametagIcon = new Setting(this, "Show Nametag Icon").setValue(true);
        setPreviewLabel("NAME", 1.6F);
    }
}
