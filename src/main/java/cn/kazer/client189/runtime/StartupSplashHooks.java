package cn.kazer.client189.runtime;

import com.cheatbreaker.client.kazerui.splash.KazerSplash;
import com.cheatbreaker.client.kazerui.splash.StartupProgress;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.Iterator;
import net.minecraftforge.fml.common.ProgressManager;

/** Startup callbacks only; after the first GUI this is a single boolean check. */
public final class StartupSplashHooks {
    private static final IdentityHashMap<ProgressManager.ProgressBar,Bar> adapters=new IdentityHashMap<>();
    public static boolean takeOverFromFml(){return KazerSplash.takeOverFromFml();}
    public static boolean onVanillaScreen(){return KazerSplash.onVanillaScreen();}
    public static void onProgress(){
        if(KazerSplash.isFinished())return;
        ArrayList<StartupProgress.ProgressBar> bars=new ArrayList<>();
        IdentityHashMap<ProgressManager.ProgressBar,Bar> live=new IdentityHashMap<>();
        for(Iterator<ProgressManager.ProgressBar> it=ProgressManager.barIterator();it.hasNext();){
            ProgressManager.ProgressBar bar=it.next();
            Bar adapter=adapters.get(bar);if(adapter==null)adapter=new Bar(bar);
            live.put(bar,adapter);bars.add(adapter);
        }
        adapters.clear();adapters.putAll(live);
        StartupProgress.bars(bars);KazerSplash.onProgress();
    }
    public static void finish(){KazerSplash.finish();adapters.clear();}
    private static final class Bar implements StartupProgress.ProgressBar {
        final ProgressManager.ProgressBar bar;
        Bar(ProgressManager.ProgressBar bar){this.bar=bar;}
        public int getSteps(){return bar.getSteps();} public int getStep(){return bar.getStep();}
        public String getMessage(){return bar.getMessage();} public String getTitle(){return bar.getTitle();}
    }
    private StartupSplashHooks(){}
}
