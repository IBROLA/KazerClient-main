package com.cheatbreaker.client.ui.mainmenu;

import com.cheatbreaker.client.ui.AbstractGui;
import com.cheatbreaker.client.kazerui.splash.KazerSplash;
import com.cheatbreaker.client.kazerui.splash.StartupProgress;

/** Existing bootstrap API backed by the shared donor loading renderer. */
public final class LoadingScreen extends AbstractGui {
    private final int capacity;
    private int step;
    public LoadingScreen(int capacity){this.capacity=Math.max(1,capacity);}
    public void newMessage(String message){
        KazerSplash.caption(message); ++step;
    }
    @Override public void drawMenu(float mouseX,float mouseY){KazerSplash.onVanillaScreen();}
    @Override protected void onMouseClicked(float mouseX,float mouseY,int button){}
    @Override public void onMouseReleased(float mouseX,float mouseY,int button){}
}
