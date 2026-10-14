package com.cheatbreaker.client.kazerui.cosmetics;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;
import org.apache.logging.log4j.LogManager;

public final class CosmeticCatalog {
    private static List<Cosmetic> all;
    private static Map<Integer, Cosmetic> byId;

    private CosmeticCatalog() {}

    public static synchronized List<Cosmetic> all() {
        if (all == null) load();
        return all;
    }

    public static Cosmetic get(int id) { all(); return byId.get(id); }

    private static void load() {
        List<Cosmetic> list = new ArrayList<Cosmetic>();
        Map<Integer, Cosmetic> map = new HashMap<Integer, Cosmetic>();
        Cosmetic builtin=new Cosmetic(900001,"Kazer","builtin:cape",CosmeticType.CLOAK,false,false,"","",false,"");
        list.add(builtin);map.put(builtin.id,builtin);
        builtin=new Cosmetic(900003,"Micet","builtin:micet",CosmeticType.CLOAK,false,false,"","",false,"");
        list.add(builtin);map.put(builtin.id,builtin);
        builtin=new Cosmetic(900002,"Dragon Wings","builtin:wings",CosmeticType.WINGS,false,false,"","",false,"");
        list.add(builtin);map.put(builtin.id,builtin);
        try (InputStreamReader in = new InputStreamReader(
                CosmeticCatalog.class.getResourceAsStream("/assets/kazerui/cosmetics/cosmetics.json"), StandardCharsets.UTF_8)) {
            for (JsonElement e : new JsonParser().parse(in).getAsJsonArray()) {
                JsonArray a = e.getAsJsonArray();
                CosmeticType type = CosmeticType.from(a.get(3).getAsString());

                if (type == null || type == CosmeticType.ITEM || type.parent == CosmeticType.ITEM) continue;
                Cosmetic c = new Cosmetic(a.get(0).getAsInt(), a.get(1).getAsString(), a.get(2).getAsString(), type,
                    a.get(4).getAsInt() != 0, a.get(5).getAsInt() != 0, a.get(6).getAsString(), a.get(7).getAsString(),
                    a.get(8).getAsInt() != 0, a.get(9).getAsString());
                // The only Kazer-branded item is the owned built-in black cloak.
                if (c.name.toLowerCase(Locale.ROOT).contains("kazer")) continue;
                list.add(c); map.put(c.id, c);
            }
        } catch (Exception ex) {
            LogManager.getLogger("Kazer/Cosmetics").error("Could not read the bundled cosmetics.json", ex);
        }
        all = Collections.unmodifiableList(list); byId = map;
    }

    public static int count(CosmeticType type) {
        int n = 0;
        for (Cosmetic c : all()) if (type.covers(c.type)) n++;
        return n;
    }
}
