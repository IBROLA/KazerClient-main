package com.cheatbreaker.client.module.type;

import com.cheatbreaker.client.config.Setting;
import com.cheatbreaker.client.module.AbstractModule;

/**
 * Enhancement card over the always-on screenshot Patch: the screenshot itself,
 * its chat message, and its [Copy]/[Copy File]/[Open] buttons stay live
 * regardless of this card. The card toggle only gates the Auto Copy and
 * Screenshot Sound sub-options applied on each successfully saved screenshot.
 */
public final class ScreenshotModule extends AbstractModule {

    public static final String AUTO_COPY_IMAGE = "IMAGE";
    public static final String AUTO_COPY_FILE = "FILE";
    public static final String AUTO_COPY_OFF = "OFF";

    private final Setting autoCopy;
    private final Setting screenshotSound;

    public ScreenshotModule() {
        super("Screenshot");
        this.setDefaultState(true);
        this.setDefaultRenderHud(false);
        this.isEditable = false;
        this.setPreviewLabel("Screenshot", 1.0f);

        this.autoCopy = new Setting(this, "Auto Copy")
            .setValue(AUTO_COPY_IMAGE)
            .acceptedValues(AUTO_COPY_IMAGE, AUTO_COPY_FILE, AUTO_COPY_OFF);
        this.screenshotSound = new Setting(this, "Screenshot Sound").setValue(true);
    }

    public Setting getAutoCopySetting() {
        return this.autoCopy;
    }

    public Setting getScreenshotSoundSetting() {
        return this.screenshotSound;
    }

    /**
     * Maps the Auto Copy choice onto the clipboard operation strings used by
     * ScreenShotHelper's action buttons ("copy" / "copy_file"); "Off" and
     * anything unknown mean "leave the clipboard alone".
     */
    public static String autoCopyOperation(String value) {
        if (AUTO_COPY_IMAGE.equalsIgnoreCase(value)) {
            return "copy";
        }
        if (AUTO_COPY_FILE.equalsIgnoreCase(value)) {
            return "copy_file";
        }
        return null;
    }
}
