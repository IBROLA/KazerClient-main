package com.cheatbreaker.client.kazerui.input;

import com.cheatbreaker.client.kazerui.module.ModuleManager;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.Rectangle;
import org.lwjgl.LWJGLException;
import org.lwjgl.opengl.Display;
import org.lwjgl.opengl.DisplayMode;

/** Transition-only work. Never polls the OS or changes graphics quality per frame. */
public final class WindowMode {
    private static final String UNDECORATED="org.lwjgl.opengl.Window.undecorated";
    private static int windowX,windowY,windowWidth,windowHeight;
    private static boolean saved,borderless;
    private static String originalDecoration;
    public static boolean configured() {
        return Boolean.parseBoolean(ModuleManager.store("global","borderlessFullscreen","true"));
    }
    public static boolean active() { return borderless; }
    public static DisplayMode transition(boolean fullscreen,DisplayMode exclusive,int fallbackWidth,int fallbackHeight) throws LWJGLException {
        if(fullscreen&&!saved) {
            windowX=Display.getX();windowY=Display.getY();
            windowWidth=Math.max(1,Display.getWidth());windowHeight=Math.max(1,Display.getHeight());
            if(Display.isFullscreen()) {
                windowWidth=Math.max(1,fallbackWidth);windowHeight=Math.max(1,fallbackHeight);
                Rectangle monitor=monitor(windowX,windowY);
                windowX=monitor.x+(monitor.width-windowWidth)/2;windowY=monitor.y+(monitor.height-windowHeight)/2;
            }
            originalDecoration=System.getProperty(UNDECORATED);saved=true;
        }
        boolean next=fullscreen&&configured();
        if(next&&!Display.isFullscreen()) {
            DisplayMode inPlace=NativeBorderlessWindow.enter();
            if(inPlace!=null){borderless=true;return inPlace;}
        }
        if(borderless&&NativeBorderlessWindow.restore()&&!fullscreen) {
            borderless=false;saved=false;return new DisplayMode(windowWidth,windowHeight);
        }
        DisplayMode mode;
        int x=windowX,y=windowY;
        if(next) {
            Rectangle monitor=monitor(windowX+windowWidth/2,windowY+windowHeight/2);
            mode=new DisplayMode(monitor.width,monitor.height);x=monitor.x;y=monitor.y;
            System.setProperty(UNDECORATED,"true");
        } else {
            if(originalDecoration==null)System.clearProperty(UNDECORATED);
            else System.setProperty(UNDECORATED,originalDecoration);
            mode=fullscreen?exclusive:new DisplayMode(saved?windowWidth:Math.max(1,fallbackWidth),saved?windowHeight:Math.max(1,fallbackHeight));
        }
        // One combined operation instead of setDisplayMode + setFullscreen.
        // A constructed DisplayMode is windowed; a native mode is exclusive.
        Display.setDisplayModeAndFullscreen(mode);
        if(!fullscreen||next)Display.setLocation(x,y);
        borderless=next;
        if(!fullscreen)saved=false;
        return mode;
    }
    private static Rectangle monitor(int x,int y) {
        GraphicsEnvironment environment=GraphicsEnvironment.getLocalGraphicsEnvironment();
        for(GraphicsDevice device:environment.getScreenDevices()) {
            Rectangle bounds=device.getDefaultConfiguration().getBounds();
            if(bounds.contains(x,y))return bounds;
        }
        return environment.getDefaultScreenDevice().getDefaultConfiguration().getBounds();
    }
    private WindowMode() {}
}
