package com.cheatbreaker.client.nethandler.apollo;

import com.google.gson.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

/** Wire fields verified against the local Apollo 1.2.8 / apollo-protos 0.2.0 implementation. */
public final class ApolloProtocol {
    public static final String CHANNEL = "lunar:apollo";
    public static final String JSON_CHANNEL = "apollo:json";
    private static final String PREFIX = "type.googleapis.com/lunarclient.apollo.";
    public enum Kind { NAMETAG, RESET_NAMETAG, RESET_NAMETAGS, TITLE, RESET_TITLES, CONFIG, NPC_EQUIP, NPC_UNEQUIP, NPC_RESET }

    public static final class Command {
        public Kind kind;
        public UUID player;
        public List<String> lines;
        public String text, module;
        public int titleType;
        public float scale, interpolationScale, interpolationRate;
        public long fadeIn, display, fadeOut;
        public boolean enabled, clearOnSwitch;
        public boolean copyLocalCosmetics;
        public List<Integer> cosmeticIds;
    }

    private ApolloProtocol() { }
    public static byte[] handshake() {
        byte[] minecraft = new ProtoWire.Writer().string(1, "V1_8").build();
        // This field is named LunarClientVersion in the protocol, but we identify as Kazer.
        byte[] client = new ProtoWire.Writer().string(1, "kazer").string(3, cn.kazer.client189.KazerClient189.VERSION).build();
        return envelope("player.v1.PlayerHandshakeMessage", new ProtoWire.Writer()
                .bytes(1, minecraft).bytes(2, client).number(4, 1).build());
    }
    static byte[] envelope(String type, byte[] message) {
        return new ProtoWire.Writer().string(1, PREFIX + type).bytes(2, message).build();
    }

    public static List<Command> decode(String channel, byte[] bytes) {
        if (bytes == null || bytes.length > 32767) throw new IllegalArgumentException("Invalid Apollo payload");
        JsonObject packet;
        if (JSON_CHANNEL.equals(channel)) packet = parseJson(new String(bytes, StandardCharsets.UTF_8)).getAsJsonObject();
        else if (CHANNEL.equals(channel)) {
            ProtoWire any = new ProtoWire(bytes);
            packet = binary(any.string(1), any.bytes(2));
            if (packet == null) return Collections.emptyList();
        } else return Collections.emptyList();
        String type = string(packet, "@type", "");
        if (!type.startsWith(PREFIX)) return Collections.emptyList();
        String name = type.substring(PREFIX.length());
        Command c = new Command();
        switch (name) {
            case "cosmetic.v1.EquipNpcCosmeticsMessage":
            case "cosmetic.v1.UnequipNpcCosmeticsMessage":
            case "cosmetic.v1.ResetNpcCosmeticsMessage":
                c.player=uuid(field(packet,"npc_uuid"));
                if(c.player.getLeastSignificantBits()!=0)throw new IllegalArgumentException("Cosmetic target is not an NPC");
                c.kind=name.contains("Unequip")?Kind.NPC_UNEQUIP:name.contains("Reset")?Kind.NPC_RESET:Kind.NPC_EQUIP;
                c.copyLocalCosmetics=field(packet,"copy_local_cosmetics")!=null && field(packet,"copy_local_cosmetics").getAsBoolean();
                c.cosmeticIds=new ArrayList<>();
                JsonElement cosmetics=field(packet,c.kind==Kind.NPC_UNEQUIP?"cosmetic_ids":"cosmetics");
                int cosmeticCount=0;
                if(cosmetics!=null)for(JsonElement item:cosmetics.getAsJsonArray()) {
                    if(++cosmeticCount>8)throw new IllegalArgumentException("Too many NPC cosmetics");
                    // Optional custom shape/placement controls are not supported
                    // by the retained default-model renderer; do not fake them.
                    if(item.isJsonObject() && item.getAsJsonObject().entrySet().size()!=1)continue;
                    String encodedId=(item.isJsonObject()?item.getAsJsonObject().get("id"):item).getAsString();
                    if(!encodedId.matches("[0-9]{1,10}"))throw new IllegalArgumentException("Invalid cosmetic ID");
                    long id=Long.parseLong(encodedId);
                    if(id<=0 || id>Integer.MAX_VALUE)throw new IllegalArgumentException("Invalid cosmetic ID");
                    c.cosmeticIds.add((int)id);
                }
                break;
            case "nametag.v1.OverrideNametagMessage":
                c.kind = Kind.NAMETAG;
                c.player = uuid(field(packet, "player_uuid"));
                c.lines = new ArrayList<>();
                JsonElement lines = field(packet, "adventure_json_lines");
                if (lines != null) for (JsonElement line : lines.getAsJsonArray()) {
                    if (c.lines.size() >= 64) throw new IllegalArgumentException("Too many nametag lines");
                    c.lines.add(AdventureText.legacy(line.getAsString()));
                }
                // Empty overrides intentionally hide the vanilla name.
                break;
            case "nametag.v1.ResetNametagMessage":
                c.kind = Kind.RESET_NAMETAG; c.player = uuid(field(packet, "player_uuid")); break;
            case "nametag.v1.ResetNametagsMessage": c.kind = Kind.RESET_NAMETAGS; break;
            case "title.v1.DisplayTitleMessage":
                c.kind = Kind.TITLE;
                JsonElement titleType = field(packet, "title_type");
                String t = titleType == null ? "0" : titleType.getAsString();
                c.titleType = t.equals("TITLE_TYPE_TITLE") ? 1 : t.equals("TITLE_TYPE_SUBTITLE") ? 2 : Integer.parseInt(t);
                if (c.titleType != 1 && c.titleType != 2) return Collections.emptyList();
                c.text = AdventureText.legacy(string(packet, "adventure_json_message", "\"\""));
                c.scale = positive(packet, "scale");
                c.interpolationScale = positive(packet, "interpolation_scale");
                c.interpolationRate = positive(packet, "interpolation_rate");
                c.fadeIn = duration(field(packet, "fade_in_time"));
                c.display = duration(field(packet, "display_time"));
                c.fadeOut = duration(field(packet, "fade_out_time"));
                break;
            case "title.v1.ResetTitlesMessage": c.kind = Kind.RESET_TITLES; break;
            case "configurable.v1.OverrideConfigurableSettingsMessage":
                List<Command> settings = new ArrayList<>();
                JsonElement values = field(packet, "configurable_settings");
                if (values != null) for (JsonElement value : values.getAsJsonArray()) {
                    JsonObject setting = value.getAsJsonObject();
                    String module = string(setting, "apollo_module", "");
                    if (!module.equals("title") && !module.equals("nametag") && !module.equals("cosmetic")) continue;
                    Command config = new Command(); config.kind = Kind.CONFIG; config.module = module;
                    config.enabled = field(setting, "enable") != null && field(setting, "enable").getAsBoolean();
                    JsonElement props = field(setting, "properties");
                    config.clearOnSwitch = props != null && props.isJsonObject()
                            && props.getAsJsonObject().has("clear-title-on-server-switch")
                            && props.getAsJsonObject().get("clear-title-on-server-switch").getAsBoolean();
                    settings.add(config);
                }
                return settings;
            default: return Collections.emptyList();
        }
        return Collections.singletonList(c);
    }

    static JsonElement parseJson(String text) {
        // Bound nesting before invoking the recursive Gson parser (untrusted server input).
        int depth = 0; boolean quoted = false, escape = false;
        for (int i = 0; i < text.length(); i++) {
            char ch = text.charAt(i);
            if (escape) { escape = false; continue; }
            if (quoted && ch == '\\') { escape = true; continue; }
            if (ch == '"') quoted = !quoted;
            if (!quoted && (ch == '{' || ch == '[') && ++depth > 40)
                throw new IllegalArgumentException("JSON nesting limit");
            if (!quoted && (ch == '}' || ch == ']')) depth--;
        }
        return new JsonParser().parse(text);
    }

    private static JsonElement field(JsonObject o, String snake) {
        if (o.has(snake)) return o.get(snake);
        StringBuilder camel = new StringBuilder(); boolean upper = false;
        for (char ch : snake.toCharArray()) {
            if (ch == '_') upper = true;
            else { camel.append(upper ? Character.toUpperCase(ch) : ch); upper = false; }
        }
        return o.get(camel.toString());
    }
    private static String string(JsonObject o, String name, String fallback) {
        JsonElement e = field(o, name); return e == null ? fallback : e.getAsString();
    }
    private static UUID uuid(JsonElement value) {
        if (value == null || !value.isJsonObject()) throw new IllegalArgumentException("Missing player UUID");
        JsonObject u = value.getAsJsonObject();
        return new UUID(Long.parseUnsignedLong(string(u, "high64", "0")), Long.parseUnsignedLong(string(u, "low64", "0")));
    }
    private static float positive(JsonObject o, String name) {
        JsonElement v = field(o, name);
        float f = v == null ? 0 : v.getAsFloat();
        if (Float.isNaN(f) || Float.isInfinite(f) || f < 0 || f > 100) throw new IllegalArgumentException("Invalid title scale");
        return f;
    }
    private static long duration(JsonElement value) {
        if (value == null) return 0;
        double millis;
        if (value.isJsonObject()) {
            JsonObject d = value.getAsJsonObject();
            millis = Double.parseDouble(string(d, "seconds", "0")) * 1000
                    + Double.parseDouble(string(d, "nanos", "0")) / 1000000;
        } else {
            String s = value.getAsString();
            if (!s.endsWith("s")) throw new IllegalArgumentException("Invalid Duration");
            millis = Double.parseDouble(s.substring(0, s.length() - 1)) * 1000;
        }
        if (Double.isNaN(millis) || Double.isInfinite(millis) || millis < 0 || millis > 86400000)
            throw new IllegalArgumentException("Invalid title duration");
        return Math.round(millis);
    }

    private static JsonObject binary(String type, byte[] bytes) {
        if (!type.startsWith(PREFIX)) return null;
        String name = type.substring(PREFIX.length());
        if (!name.equals("nametag.v1.OverrideNametagMessage") && !name.equals("nametag.v1.ResetNametagMessage")
                && !name.equals("nametag.v1.ResetNametagsMessage") && !name.equals("title.v1.DisplayTitleMessage")
                && !name.equals("title.v1.ResetTitlesMessage") && !name.equals("configurable.v1.OverrideConfigurableSettingsMessage")
                && !name.equals("cosmetic.v1.EquipNpcCosmeticsMessage") && !name.equals("cosmetic.v1.UnequipNpcCosmeticsMessage")
                && !name.equals("cosmetic.v1.ResetNpcCosmeticsMessage")) return null;
        ProtoWire p = new ProtoWire(bytes);
        JsonObject o = new JsonObject(); o.addProperty("@type", type);
        if(name.startsWith("cosmetic.")) {
            if(!p.has(1))throw new IllegalArgumentException("Missing NPC UUID");
            ProtoWire u=p.message(1);JsonObject id=new JsonObject();
            id.addProperty("high64",Long.toUnsignedString(u.number(1)));id.addProperty("low64",Long.toUnsignedString(u.number(2)));o.add("npc_uuid",id);
            JsonArray ids=new JsonArray();
            if(name.contains("EquipNpc")) {
                for(Object item:p.all(2)) {
                    ProtoWire cosmetic=new ProtoWire((byte[])item);JsonObject value=new JsonObject();value.addProperty("id",cosmetic.number(1));
                    if(cosmetic.has(2)||cosmetic.has(3)||cosmetic.has(4)||cosmetic.has(5))value.addProperty("unsupported_options",true);
                    ids.add(value);
                }
                o.add("cosmetics",ids);o.addProperty("copy_local_cosmetics",p.number(3)!=0);
            } else if(name.contains("UnequipNpc")) {
                for(Long value:p.numbers(2))ids.add(new JsonPrimitive(value));o.add("cosmetic_ids",ids);
            }
        } else if (name.startsWith("nametag.") && !name.endsWith("ResetNametagsMessage")) {
            if (!p.has(1)) throw new IllegalArgumentException("Missing player UUID");
            ProtoWire u = p.message(1); JsonObject id = new JsonObject();
            id.addProperty("high64", Long.toUnsignedString(u.number(1)));
            id.addProperty("low64", Long.toUnsignedString(u.number(2))); o.add("player_uuid", id);
            JsonArray lines = new JsonArray();
            if (p.has(3)) for (Object line : p.all(3)) lines.add(new JsonPrimitive(new String((byte[])line, StandardCharsets.UTF_8)));
            else for (Object line : p.all(2)) lines.add(new JsonPrimitive(legacyComponent(new ProtoWire((byte[])line), 0).toString()));
            o.add("adventure_json_lines", lines);
        } else if (name.equals("title.v1.DisplayTitleMessage")) {
            o.addProperty("title_type", p.number(1)); o.addProperty("scale", p.decimal(3));
            o.addProperty("adventure_json_message", p.has(7) ? p.string(7) : legacyComponent(p.message(2), 0).toString());
            o.addProperty("interpolation_scale", p.decimal(8)); o.addProperty("interpolation_rate", p.decimal(9));
            String[] names = {"fade_in_time", "display_time", "fade_out_time"};
            for (int i = 0; i < names.length; i++) {
                ProtoWire d = p.message(4 + i); JsonObject duration = new JsonObject();
                duration.addProperty("seconds", d.number(1)); duration.addProperty("nanos", d.number(2)); o.add(names[i], duration);
            }
        } else if (name.equals("configurable.v1.OverrideConfigurableSettingsMessage")) {
            JsonArray settings = new JsonArray();
            for (Object entry : p.all(1)) {
                ProtoWire setting = new ProtoWire((byte[])entry); JsonObject value = new JsonObject();
                value.addProperty("apollo_module", setting.string(2)); value.addProperty("enable", setting.number(3) != 0);
                JsonObject properties = new JsonObject();
                for (Object property : setting.message(4).all(1)) {
                    ProtoWire pair = new ProtoWire((byte[])property);
                    if (pair.string(1).equals("clear-title-on-server-switch"))
                        properties.addProperty(pair.string(1), pair.message(2).number(4) != 0);
                }
                value.add("properties", properties); settings.add(value);
            }
            o.add("configurable_settings", settings);
        }
        return o;
    }

    private static JsonObject legacyComponent(ProtoWire p, int depth) {
        if (depth > 32) throw new IllegalArgumentException("Component nesting limit");
        JsonObject c = new JsonObject(); c.addProperty("text", p.string(1));
        if (p.has(2)) c.addProperty("color", String.format("#%06x", p.message(2).number(1) & 0xffffff));
        String[] flags = {"", "obfuscated", "bold", "strikethrough", "underlined", "italic"};
        for (Object decorator : p.all(3)) {
            if (decorator instanceof Long) {
                int n = ((Long)decorator).intValue(); if (n > 0 && n < flags.length) c.addProperty(flags[n], true);
            } else {
                // Packed enum values in the deprecated Component representation.
                for (byte n : (byte[])decorator) if (n > 0 && n < flags.length) c.addProperty(flags[n], true);
            }
        }
        JsonArray children = new JsonArray();
        for (Object child : p.all(4)) children.add(legacyComponent(new ProtoWire((byte[])child), depth + 1));
        c.add("extra", children); return c;
    }
}
