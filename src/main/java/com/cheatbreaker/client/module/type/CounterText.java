package com.cheatbreaker.client.module.type;

/** Constant-value counters retain their text instead of allocating at render frequency. */
public final class CounterText {
    private final String label;
    private int lastValue, lastFlags;
    private String cached;
    public CounterText(String label){this.label=label;}
    public String text(int value, boolean background, boolean reverse, boolean showLabel) {
        int flags=(background?1:0)|(reverse?2:0)|(showLabel?4:0);
        if(cached==null||lastValue!=value||lastFlags!=flags) {
            String suffix=showLabel?label:"";
            String content=(reverse?suffix+" "+value:value+" "+suffix).trim();
            cached=background?content:"["+content+"]";
            lastValue=value;lastFlags=flags;
        }
        return cached;
    }
}
