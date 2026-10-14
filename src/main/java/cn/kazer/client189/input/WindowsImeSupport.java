package cn.kazer.client189.input;

import com.sun.jna.Memory;
import com.sun.jna.Native;
import com.sun.jna.NativeLibrary;
import com.sun.jna.Pointer;
import com.sun.jna.win32.StdCallLibrary;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiChat;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Util;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.lwjgl.opengl.Display;

public final class WindowsImeSupport implements ImeCaretTracker.Positioner
{
    private static final Logger logger = LogManager.getLogger();
    private static boolean warned;
    private static boolean unavailable;
    private final ImeApi imeApi;

    public WindowsImeSupport(ImeApi imeApi)
    {
        this.imeApi = imeApi;
    }

    @Override
    public boolean move(ImeCaretPosition position)
    {
        return this.imeApi.position(position.getX(), position.getCompositionY(), position.getExcludeLeft(),
            position.getExcludeTop(), position.getExcludeRight(), position.getExcludeBottom());
    }

    public static void updateCaretPosition(int x, int top, int height)
    {
        if (unavailable || Util.getOSType() != Util.EnumOS.WINDOWS)
        {
            return;
        }

        ImeCaretTracker tracker = TrackerHolder.TRACKER;
        Minecraft minecraft = Minecraft.getMinecraft();
        GuiScreen screen = minecraft == null ? null : minecraft.currentScreen;

        if (!Display.isCreated() || !Display.isActive() || screen == null || screen.width <= 0 || screen.height <= 0
            || minecraft.displayWidth <= 0 || minecraft.displayHeight <= 0)
        {
            tracker.suspend();
            return;
        }

        try
        {
            ImeCaretPosition position = screen instanceof GuiChat
                ? ImeCaretPosition.fromGuiAbove(x, top, height, screen.width, screen.height,
                    minecraft.displayWidth, minecraft.displayHeight)
                : ImeCaretPosition.fromGui(x, top, height, screen.width, screen.height,
                    minecraft.displayWidth, minecraft.displayHeight);
            tracker.report(position);
        }
        catch (Throwable error)
        {
            unavailable = true;
            tracker.suspend();

            if (!warned)
            {
                warned = true;
                logger.warn("Could not position the Windows IME candidate window", error);
            }
        }
    }

    public static boolean hasLwjglWindowBridge()
    {
        return WindowsFullscreenImeSupport.hasLwjglWindowBridge();
    }

    public static boolean hasImeWindowBridge()
    {
        try
        {
            NativeLibrary library = NativeLibrary.getInstance("imm32");
            library.getFunction("ImmGetContext");
            library.getFunction("ImmSetCompositionWindow");
            library.getFunction("ImmSetCandidateWindow");
            library.getFunction("ImmReleaseContext");
            return true;
        }
        catch (Throwable ignored)
        {
            return false;
        }
    }

    public interface ImeApi
    {
        boolean position(int x, int y, int excludeLeft, int excludeTop, int excludeRight, int excludeBottom);
    }

    private static final class TrackerHolder
    {
        private static final ImeCaretTracker TRACKER =
            new ImeCaretTracker(new WindowsImeSupport(new NativeImeApi()));
    }

    private static final class NativeImeApi implements ImeApi
    {
        private static final int CFS_POINT = 0x0002;
        private static final int CFS_EXCLUDE = 0x0080;
        private static final int COMPOSITION_FORM_SIZE = 28;
        private static final int CANDIDATE_FORM_SIZE = 32;

        @Override
        public boolean position(int x, int y, int excludeLeft, int excludeTop, int excludeRight, int excludeBottom)
        {
            try
            {
                long hwnd = WindowsFullscreenImeSupport.getWindowHandle();

                if (hwnd == 0L)
                {
                    return false;
                }

                Pointer window = new Pointer(hwnd);
                Pointer context = Imm32.INSTANCE.ImmGetContext(window);

                if (context == null || Pointer.nativeValue(context) == 0L)
                {
                    return false;
                }

                try
                {
                    Memory composition = new Memory(COMPOSITION_FORM_SIZE);
                    composition.clear();
                    composition.setInt(0L, CFS_POINT);
                    composition.setInt(4L, x);
                    composition.setInt(8L, y);

                    Memory candidate = new Memory(CANDIDATE_FORM_SIZE);
                    candidate.clear();
                    candidate.setInt(0L, 0);
                    candidate.setInt(4L, CFS_EXCLUDE);
                    candidate.setInt(8L, x);
                    candidate.setInt(12L, y);
                    candidate.setInt(16L, excludeLeft);
                    candidate.setInt(20L, excludeTop);
                    candidate.setInt(24L, excludeRight);
                    candidate.setInt(28L, excludeBottom);

                    boolean compositionPositioned =
                        Imm32.INSTANCE.ImmSetCompositionWindow(context, composition) != 0;
                    boolean candidatePositioned = Imm32.INSTANCE.ImmSetCandidateWindow(context, candidate) != 0;
                    return compositionPositioned && candidatePositioned;
                }
                finally
                {
                    Imm32.INSTANCE.ImmReleaseContext(window, context);
                }
            }
            catch (Exception error)
            {
                throw new IllegalStateException("Windows IME positioning failed", error);
            }
        }
    }

    private interface Imm32 extends StdCallLibrary
    {
        Imm32 INSTANCE = (Imm32)Native.loadLibrary("imm32", Imm32.class);

        Pointer ImmGetContext(Pointer window);

        int ImmSetCompositionWindow(Pointer context, Pointer compositionForm);

        int ImmSetCandidateWindow(Pointer context, Pointer candidateForm);

        int ImmReleaseContext(Pointer window, Pointer context);
    }
}

