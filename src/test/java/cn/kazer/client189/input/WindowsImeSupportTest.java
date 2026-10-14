package cn.kazer.client189.input;

import cn.kazer.client189.input.ImeCaretPosition;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class WindowsImeSupportTest
{
    @Test
    public void testBundledRuntimeExposesLwjglAndWindowsImeBridges()
    {
        assertTrue(WindowsImeSupport.hasLwjglWindowBridge());
        assertTrue(WindowsImeSupport.hasImeWindowBridge());
    }

    @Test
    public void testPositionsImeBelowCaretAndExcludesTextLine()
    {
        RecordingImeApi imeApi = new RecordingImeApi();
        WindowsImeSupport support = new WindowsImeSupport(imeApi);

        boolean positioned = support.move(ImeCaretPosition.fromGui(100, 520, 9, 960, 540, 1920, 1080));

        assertTrue(positioned);
        assertEquals(200, imeApi.x);
        assertEquals(1058, imeApi.y);
        assertEquals(200, imeApi.excludeLeft);
        assertEquals(1040, imeApi.excludeTop);
        assertEquals(201, imeApi.excludeRight);
        assertEquals(1058, imeApi.excludeBottom);
    }

    @Test
    public void testPositionsChatCompositionInsideFieldAndCandidateAbove()
    {
        RecordingImeApi imeApi = new RecordingImeApi();
        WindowsImeSupport support = new WindowsImeSupport(imeApi);

        boolean positioned = support.move(
            ImeCaretPosition.fromGuiAbove(4, 347, 11, 640, 360, 1920, 1080));

        assertTrue(positioned);
        assertEquals(12, imeApi.x);
        assertEquals(1041, imeApi.y);
        assertEquals(0, imeApi.excludeLeft);
        assertEquals(1041, imeApi.excludeTop);
        assertEquals(1920, imeApi.excludeRight);
        assertEquals(1080, imeApi.excludeBottom);
    }

    private static final class RecordingImeApi implements WindowsImeSupport.ImeApi
    {
        private int x;
        private int y;
        private int excludeLeft;
        private int excludeTop;
        private int excludeRight;
        private int excludeBottom;

        @Override
        public boolean position(int x, int y, int excludeLeft, int excludeTop, int excludeRight, int excludeBottom)
        {
            this.x = x;
            this.y = y;
            this.excludeLeft = excludeLeft;
            this.excludeTop = excludeTop;
            this.excludeRight = excludeRight;
            this.excludeBottom = excludeBottom;
            return true;
        }
    }
}


