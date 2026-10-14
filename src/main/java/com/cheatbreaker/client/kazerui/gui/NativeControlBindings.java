package com.cheatbreaker.client.kazerui.gui;

import com.cheatbreaker.client.CheatBreaker;
import java.util.*;
import net.minecraft.client.settings.KeyBinding;

/** Presentation-only filtering; bindings stay registered and their saved values stay intact. */
public final class NativeControlBindings {
    public static KeyBinding[] visible(KeyBinding[] bindings) {
        List<KeyBinding> shown=new ArrayList<>();
        for(KeyBinding binding:bindings) {
            String category=binding.getKeyCategory().replace(" ","").toLowerCase(Locale.ROOT);
            if(category.equals("kazerclient") || category.equals("cheatbreaker")
                    || category.equals("key.categories.kazer") || category.equals("key.categories.cheatbreaker"))continue;
            CheatBreaker client=CheatBreaker.getInstance();
            if(client!=null && client.globalSettings!=null && client.globalSettings.keyBindings.manages(binding))continue;
            shown.add(binding);
        }
        return shown.toArray(new KeyBinding[shown.size()]);
    }
    private NativeControlBindings(){}
    public static Set<String> categories() {
        Set<String> categories=new HashSet<>();
        for(KeyBinding binding:visible(net.minecraft.client.Minecraft.getMinecraft().gameSettings.keyBindings))
            categories.add(binding.getKeyCategory());
        return categories;
    }
}
