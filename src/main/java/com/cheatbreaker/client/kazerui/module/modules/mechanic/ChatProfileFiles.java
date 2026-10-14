package com.cheatbreaker.client.kazerui.module.modules.mechanic;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

/** Profile names are data, never paths. Both tracks use the same safe encoding. */
final class ChatProfileFiles {
    static File filter(File configRoot, String profile) {
        String encoded=Base64.getUrlEncoder().withoutPadding().encodeToString(profile.getBytes(StandardCharsets.UTF_8));
        return new File(new File(new File(configRoot,"chat-filters"),encoded),"profanity_filter.txt");
    }
    private ChatProfileFiles() { }
}
