package com.cheatbreaker.client.kazerui.splash;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;

/** Typed startup-only bridge; no Forge dependency in the native client. */
public final class StartupProgress {
    public interface ProgressBar {
        int getSteps(); int getStep(); String getMessage(); String getTitle();
    }
    private static List<ProgressBar> bars=Collections.emptyList();
    public static Iterator<ProgressBar> barIterator(){return bars.iterator();}
    public static void bars(List<ProgressBar> value){bars=value;}
    private static final NativeBar nativeBar=new NativeBar();
    public static void nativeStep(String message,int step,int total){
        nativeBar.message=message;nativeBar.step=step;nativeBar.total=Math.max(1,total);
        bars=Collections.<ProgressBar>singletonList(nativeBar);
        KazerSplash.onProgress();
    }
    public static void clear(){bars=Collections.emptyList();}
    private static final class NativeBar implements ProgressBar {
        String message="Initializing Minecraft Engine";int step,total=1;
        public int getSteps(){return total;} public int getStep(){return step;}
        public String getMessage(){return message;} public String getTitle(){return "Minecraft";}
    }
    private StartupProgress(){}
}
