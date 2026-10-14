package cn.kazer.client189.input;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class WindowsFullscreenImeSupportTest
{
    @Test
    public void testRemovesTopmostFromEveryRecreatedFullscreenWindow()
    {
        // LWJGL2 recreates the window HWND on every fullscreen toggle, so the tweak must
        // not gate on Display.isActive(): focus has not settled on the fresh window yet
        // and the tweak would be skipped with no retry. Focus safety comes from
        // SWP_NOACTIVATE in the SetWindowPos flags, not from skipping the tweak.
        assertTrue(WindowsFullscreenImeSupport.shouldRemoveTopmost(true, true, true));
        assertFalse(WindowsFullscreenImeSupport.shouldRemoveTopmost(false, true, true));
        assertFalse(WindowsFullscreenImeSupport.shouldRemoveTopmost(true, false, true));
        assertFalse(WindowsFullscreenImeSupport.shouldRemoveTopmost(true, true, false));
    }

    @Test
    public void testTickReapplyAlsoWaitsForAnActiveWindow()
    {
        // The per-tick re-application is what actually repairs the window after F11
        // re-entry: the activation/display-mode settle can re-topmost the fresh window
        // after the toggle-time tweak has already run. The tick path waits for the
        // window to be active so it never touches a minimized fullscreen window.
        assertTrue(WindowsFullscreenImeSupport.shouldReapplyOnTick(true, true, true, true));
        assertFalse(WindowsFullscreenImeSupport.shouldReapplyOnTick(false, true, true, true));
        assertFalse(WindowsFullscreenImeSupport.shouldReapplyOnTick(true, false, true, true));
        assertFalse(WindowsFullscreenImeSupport.shouldReapplyOnTick(true, true, false, true));
        assertFalse(WindowsFullscreenImeSupport.shouldReapplyOnTick(true, true, true, false));
    }

    @Test
    public void testNeedsTweakDetectsPopupAndTopmostBits()
    {
        long wsPopup = 0x80000000L;
        long wsExTopmost = 0x00000008L;
        assertTrue(WindowsFullscreenImeSupport.needsTweak(wsPopup, 0L));
        assertTrue(WindowsFullscreenImeSupport.needsTweak(0L, wsExTopmost));
        assertTrue(WindowsFullscreenImeSupport.needsTweak(wsPopup, wsExTopmost));
        assertFalse(WindowsFullscreenImeSupport.needsTweak(0L, 0L));
        assertFalse(WindowsFullscreenImeSupport.needsTweak(0x00000010L, 0x00000020L));
    }

    @Test
    public void testLwjglWindowBridgeMatchesBundledVersion()
    {
        assertTrue(WindowsFullscreenImeSupport.hasLwjglWindowBridge());
    }
}


