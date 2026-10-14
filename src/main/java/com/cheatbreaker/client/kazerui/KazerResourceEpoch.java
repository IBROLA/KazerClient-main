package com.cheatbreaker.client.kazerui;
/** Render-thread resource generation; text/layout caches invalidate on F3+T/language/font reload. */
public final class KazerResourceEpoch {
    private static long generation;
    public static long current(){return generation;}
    public static void reloaded(){generation++;}
    private KazerResourceEpoch(){}
}
