package com.cheatbreaker.client.kazerui.input;
import com.sun.jna.*;
import java.lang.reflect.Method;
import org.lwjgl.opengl.Display;
import org.lwjgl.opengl.DisplayMode;

/** Windows-only in-place borderless transitions; keeps the HWND and GL context. */
final class NativeBorderlessWindow {
    private static boolean unavailable,saved;
    private static long savedHandle,savedStyle;
    private static int x,y,width,height;
    private static final org.apache.logging.log4j.Logger LOG=org.apache.logging.log4j.LogManager.getLogger();
    static DisplayMode enter() {
        if(unavailable||!System.getProperty("os.name","").startsWith("Windows")||Display.isFullscreen())return null;
        try {
            long handle=Bridge.handle();if(handle==0)return null;Pointer window=new Pointer(handle);
            if(!saved) {
                if(Bridge.RECT.invokeInt(new Object[]{window,Bridge.bounds})==0)throw new IllegalStateException("GetWindowRect failed");
                savedStyle=Bridge.style(window);savedHandle=handle;
                x=Bridge.bounds.getInt(0);y=Bridge.bounds.getInt(4);width=Bridge.bounds.getInt(8)-x;height=Bridge.bounds.getInt(12)-y;saved=true;
            }
            Pointer monitor=Bridge.MONITOR.invokePointer(new Object[]{window,2});Bridge.monitorInfo.setInt(0,40);
            if(Bridge.INFO.invokeInt(new Object[]{monitor,Bridge.monitorInfo})==0)throw new IllegalStateException("GetMonitorInfo failed");
            int mx=Bridge.monitorInfo.getInt(4),my=Bridge.monitorInfo.getInt(8),mw=Bridge.monitorInfo.getInt(12)-mx,mh=Bridge.monitorInfo.getInt(16)-my;
            long style=(savedStyle&~(0x00C00000L|0x00040000L|0x00020000L|0x00010000L|0x01000000L))|0x80000000L;
            Bridge.style(window,style);Bridge.position(window,mx,my,mw,mh);
            Display.processMessages();return new DisplayMode(mw,mh);
        }catch(Throwable error){unavailable=true;LOG.warn("In-place borderless mode unavailable; using LWJGL fallback",error);return null;}
    }
    static boolean restore() {
        if(!saved)return false;
        try {
            long handle=Bridge.handle();if(handle!=savedHandle){saved=false;return false;}
            Pointer window=new Pointer(handle);Bridge.style(window,savedStyle);Bridge.position(window,x,y,width,height);
            Display.processMessages();saved=false;return true;
        }catch(Throwable error){unavailable=true;LOG.warn("Could not restore borderless window bounds",error);return false;}
    }
    private static final class Bridge {
        static final NativeLibrary USER=NativeLibrary.getInstance("user32");
        static final Function GET=USER.getFunction(Native.POINTER_SIZE==8?"GetWindowLongPtrW":"GetWindowLongW",Function.ALT_CONVENTION);
        static final Function SET=USER.getFunction(Native.POINTER_SIZE==8?"SetWindowLongPtrW":"SetWindowLongW",Function.ALT_CONVENTION);
        static final Function RECT=USER.getFunction("GetWindowRect",Function.ALT_CONVENTION);
        static final Function POS=USER.getFunction("SetWindowPos",Function.ALT_CONVENTION);
        static final Function MONITOR=USER.getFunction("MonitorFromWindow",Function.ALT_CONVENTION);
        static final Function INFO=USER.getFunction("GetMonitorInfoW",Function.ALT_CONVENTION);
        static final Memory bounds=new Memory(16),monitorInfo=new Memory(40);
        static final Method implementation,hwnd;
        static {
            try{implementation=Display.class.getDeclaredMethod("getImplementation");implementation.setAccessible(true);
                Class<?> nativeDisplay=Class.forName("org.lwjgl.opengl.WindowsDisplay");hwnd=nativeDisplay.getDeclaredMethod("getHwnd");hwnd.setAccessible(true);
            }catch(Exception error){throw new ExceptionInInitializerError(error);}
        }
        static long handle() throws Exception{return ((Number)hwnd.invoke(implementation.invoke(null))).longValue();}
        static long style(Pointer window){Object[] args={window,-16};return Native.POINTER_SIZE==8?GET.invokeLong(args):GET.invokeInt(args)&0xFFFFFFFFL;}
        static void style(Pointer window,long style) {
            if(Native.POINTER_SIZE==8)SET.invokeLong(new Object[]{window,-16,style});else SET.invokeInt(new Object[]{window,-16,(int)style});
        }
        static void position(Pointer window,int x,int y,int width,int height) {
            // Never activate/steal focus or put the game into the topmost band.
            if(POS.invokeInt(new Object[]{window,Pointer.createConstant(-2),x,y,width,height,0x0010|0x0020|0x0400})==0)throw new IllegalStateException("SetWindowPos failed");
        }
    }
    private NativeBorderlessWindow(){}
}
