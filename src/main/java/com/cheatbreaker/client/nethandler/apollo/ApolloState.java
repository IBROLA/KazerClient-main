package com.cheatbreaker.client.nethandler.apollo;

import com.cheatbreaker.client.util.title.Title;
import com.cheatbreaker.client.kazerui.cosmetics.*;
import java.util.*;

/** Per-connection state; never modifies vanilla/team names or hologram entity text. */
public final class ApolloState {
    private final Map<UUID, List<String>> nametags = new LinkedHashMap<>();
    private final List<Title> titles = new ArrayList<>();
    private final Map<UUID,List<Cosmetic>> npcCosmetics=new LinkedHashMap<>();
    private boolean cosmeticsEnabled=true;
    public List<Cosmetic> cosmetics(UUID npc) {return npcCosmetics.get(npc);}
    private final Map<UUID, List<String>> nametagsView = Collections.unmodifiableMap(nametags);
    private final List<Title> titlesView = Collections.unmodifiableList(titles);
    private boolean nametagsEnabled = true, titlesEnabled = true, clearTitlesOnSwitch = false;

    public Map<UUID, List<String>> nametags() { return nametagsView; }
    public List<Title> titles() { return titlesView; }
    public void disconnect() {
        nametags.clear(); titles.clear(); nametagsEnabled = titlesEnabled = true; clearTitlesOnSwitch = false;
        npcCosmetics.clear();cosmeticsEnabled=true;
    }
    public void serverSwitch() {
        nametags.clear();
        npcCosmetics.clear();
        if (clearTitlesOnSwitch) titles.clear();
    }
    public void tick(long now) { titles.removeIf(t -> t.expired(now)); }

    public void apply(ApolloProtocol.Command c) {
        switch (c.kind) {
            case NPC_EQUIP:
                if(cosmeticsEnabled) {
                    Map<CosmeticType,Cosmetic> slots=new LinkedHashMap<>();
                    List<Cosmetic> existing=npcCosmetics.get(c.player);
                    if(existing!=null)for(Cosmetic item:existing)slots.put(item.slot(),item);
                    if(c.copyLocalCosmetics)for(Cosmetic item:Loadout.equipped())slots.put(item.slot(),item);
                    for(Integer id:c.cosmeticIds) {
                        Cosmetic item=CosmeticCatalog.get(id);
                        if(item!=null && item.renderable() && (slots.containsKey(item.slot()) || slots.size()<8))slots.put(item.slot(),item);
                    }
                    if(npcCosmetics.size()>=256 && !npcCosmetics.containsKey(c.player))npcCosmetics.remove(npcCosmetics.keySet().iterator().next());
                    npcCosmetics.put(c.player,Collections.unmodifiableList(new ArrayList<>(slots.values())));
                }
                break;
            case NPC_UNEQUIP:
                List<Cosmetic> existing=npcCosmetics.get(c.player);
                if(existing!=null) {
                    List<Cosmetic> remaining=new ArrayList<>();for(Cosmetic item:existing)if(!c.cosmeticIds.contains(item.id))remaining.add(item);
                    npcCosmetics.put(c.player,Collections.unmodifiableList(remaining));
                }
                break;
            case NPC_RESET:npcCosmetics.remove(c.player);break;
            case NAMETAG:
                if (nametagsEnabled) {
                    // Preserve wire order; only the renderer owns visual row offsets.
                    List<String> topDown = new ArrayList<>(c.lines);
                    if (nametags.size() >= 4096 && !nametags.containsKey(c.player)) nametags.remove(nametags.keySet().iterator().next());
                    nametags.put(c.player, Collections.unmodifiableList(topDown));
                }
                break;
            case RESET_NAMETAG: nametags.remove(c.player); break;
            case RESET_NAMETAGS: nametags.clear(); break;
            case TITLE:
                if (titlesEnabled) {
                    Title.TitleType type = c.titleType == 1 ? Title.TitleType.title : Title.TitleType.subtitle;
                    titles.removeIf(t -> t.getTitleEnum() == type);
                    titles.add(Title.apollo(c.text, type, c.scale, c.display, c.fadeIn, c.fadeOut,
                            c.interpolationScale, c.interpolationRate));
                }
                break;
            case RESET_TITLES: titles.clear(); break;
            case CONFIG:
                if(c.module.equals("cosmetic")) {cosmeticsEnabled=c.enabled;if(!c.enabled)npcCosmetics.clear();}
                if (c.module.equals("nametag")) { nametagsEnabled = c.enabled; if (!c.enabled) nametags.clear(); }
                if (c.module.equals("title")) {
                    titlesEnabled = c.enabled; clearTitlesOnSwitch = c.clearOnSwitch;
                    if (!c.enabled) titles.clear();
                }
                break;
        }
    }
}
