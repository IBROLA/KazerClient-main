package com.cheatbreaker.client.kazerui.module.hud;
import java.util.Objects;
/** One bounded label, keyed by native state/config/resource generations.
 * Lookup does not format, box, allocate a key or retain previous labels. */
public final class HudTextCache {
    private int first,second;
    private long revision,resources;
    private String identity,value;
    public String get(int first,int second,long revision,long resources,String identity) {
        return value!=null && this.first==first && this.second==second && this.revision==revision
            && this.resources==resources && Objects.equals(this.identity,identity)?value:null;
    }
    public void put(int first,int second,long revision,long resources,String identity,String value) {
        this.first=first;this.second=second;this.revision=revision;this.resources=resources;this.identity=identity;this.value=value;
    }
}
