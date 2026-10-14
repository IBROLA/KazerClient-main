package cn.kazer.client189.input;

import java.lang.reflect.Method;
import net.minecraft.util.Util;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.lwjgl.opengl.Display;

public final class WindowsFullscreenImeSupport
{
    private static final Logger logger = LogManager.getLogger();
    private static boolean unavailable;
    private static boolean warned;

    private WindowsFullscreenImeSupport()
    {
    }

    public static void updateWindowLayer()
    {
        applyIfNeeded(shouldRemoveTopmost(Util.getOSType() == Util.EnumOS.WINDOWS, Display.isCreated(),
            Display.isFullscreen()));
    }

    public static void onClientTick()
    {
        applyIfNeeded(shouldReapplyOnTick(Util.getOSType() == Util.EnumOS.WINDOWS, Display.isCreated(),
            Display.isFullscreen(), Display.isActive()));
    }

    static boolean shouldRemoveTopmost(boolean windows, boolean displayCreated, boolean fullscreen)
    {
        return windows && displayCreated && fullscreen;
    }

    static boolean shouldReapplyOnTick(boolean windows, boolean displayCreated, boolean fullscreen, boolean active)
    {
        return windows && displayCreated && fullscreen && active;
    }

    static boolean needsTweak(long style, long extendedStyle)
    {
        return (style & NativeBridge.WS_POPUP) != 0L || (extendedStyle & NativeBridge.WS_EX_TOPMOST) != 0L;
    }

    private static void applyIfNeeded(boolean applicable)
    {
        if (unavailable || !applicable)
        {
            return;
        }

        try
        {
            if (NativeBridge.tweakFullscreenStyle())
            {
                logger.info("Adjusted Windows fullscreen style for the IME candidate window");
            }
        }
        catch (Throwable error)
        {
            unavailable = true;

            if (!warned)
            {
                warned = true;
                logger.warn("Could not expose the Windows IME candidate window in fullscreen", error);
            }
        }
    }

    static boolean hasLwjglWindowBridge()
    {
        return NativeBridge.isAvailable();
    }

    static long getWindowHandle() throws Exception
    {
        return NativeBridge.getWindowHandle();
    }

    private static final class NativeBridge
    {
        private static final int SWP_NOSIZE = 0x0001;
        private static final int SWP_NOMOVE = 0x0002;
        private static final int SWP_NOACTIVATE = 0x0010;
        private static final int SWP_FRAMECHANGED = 0x0020;
        private static final int SWP_NOSENDCHANGING = 0x0400;
        private static final int GWL_STYLE = -16;
        private static final int GWL_EXSTYLE = -20;
        private static final long WS_POPUP = 0x80000000L;
        private static final long WS_EX_TOPMOST = 0x00000008L;
        private static final long HWND_NOTOPMOST = -2L;
        private static final Class<?> WINDOWS_DISPLAY;
        private static final Method GET_IMPLEMENTATION;
        private static final Method GET_HWND;
        private static final Method GET_WINDOW_LONG_PTR;
        private static final Method SET_WINDOW_LONG_PTR;
        private static final Method SET_WINDOW_POS;

        static
        {
            try
            {
                WINDOWS_DISPLAY = Class.forName("org.lwjgl.opengl.WindowsDisplay", false, Display.class.getClassLoader());
                GET_IMPLEMENTATION = Display.class.getDeclaredMethod("getImplementation");
                GET_HWND = WINDOWS_DISPLAY.getDeclaredMethod("getHwnd");
                GET_WINDOW_LONG_PTR = WINDOWS_DISPLAY.getDeclaredMethod("getWindowLongPtr", long.class, int.class);
                SET_WINDOW_LONG_PTR = WINDOWS_DISPLAY.getDeclaredMethod("setWindowLongPtr", long.class, int.class, long.class);
                SET_WINDOW_POS = WINDOWS_DISPLAY.getDeclaredMethod("setWindowPos", long.class, long.class, int.class, int.class, int.class, int.class, long.class);
                GET_IMPLEMENTATION.setAccessible(true);
                GET_HWND.setAccessible(true);
                GET_WINDOW_LONG_PTR.setAccessible(true);
                SET_WINDOW_LONG_PTR.setAccessible(true);
                SET_WINDOW_POS.setAccessible(true);
            }
            catch (Exception var0)
            {
                throw new IllegalStateException("Unsupported LWJGL Windows display bridge", var0);
            }
        }

        private NativeBridge()
        {
        }

        private static boolean isAvailable()
        {
            return true;
        }

        private static boolean tweakFullscreenStyle() throws Exception
        {
            long var1 = getWindowHandle();

            if (var1 == 0L)
            {
                return false;
            }

            long var3 = ((Long)GET_WINDOW_LONG_PTR.invoke(null, var1, GWL_STYLE)).longValue();
            long var5 = ((Long)GET_WINDOW_LONG_PTR.invoke(null, var1, GWL_EXSTYLE)).longValue();

            if (!needsTweak(var3, var5))
            {
                return false;
            }

            if ((var3 & WS_POPUP) != 0L)
            {
                SET_WINDOW_LONG_PTR.invoke(null, var1, GWL_STYLE, var3 & ~WS_POPUP);
            }

            if (!((Boolean)SET_WINDOW_POS.invoke(null, var1, HWND_NOTOPMOST, 0, 0, 0, 0,
                (long)(SWP_NOSIZE | SWP_NOMOVE | SWP_NOACTIVATE | SWP_FRAMECHANGED | SWP_NOSENDCHANGING))).booleanValue())
            {
                throw new IllegalStateException("LWJGL setWindowPos failed");
            }

            return true;
        }

        private static long getWindowHandle() throws Exception
        {
            Object display = GET_IMPLEMENTATION.invoke(null);
            return WINDOWS_DISPLAY.isInstance(display) ? ((Long)GET_HWND.invoke(display)).longValue() : 0L;
        }
    }
}


