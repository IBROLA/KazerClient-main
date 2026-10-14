package cn.kazer.client189.input;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class ImeCaretPositionTest
{
    @Test
    public void testScalesGuiCaretToWindowsClientPixels()
    {
        ImeCaretPosition position = ImeCaretPosition.fromGui(100, 520, 9, 960, 540, 1920, 1080);

        assertEquals(200, position.getX());
        assertEquals(1040, position.getTop());
        assertEquals(1058, position.getBottom());
    }

    @Test
    public void testKeepsCaretInsideWindowsClientArea()
    {
        ImeCaretPosition position = ImeCaretPosition.fromGui(960, 539, 9, 960, 540, 1920, 1080);

        assertEquals(1919, position.getX());
        assertEquals(1078, position.getTop());
        assertEquals(1079, position.getBottom());
    }
}


