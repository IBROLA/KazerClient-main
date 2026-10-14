package cn.kazer.client189.input;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class ImeKeyboardEventTest
{
    @Test
    public void testDispatchesKeyDownAndImeCommitCharacters()
    {
        assertTrue(ImeKeyboardEvent.shouldDispatch(true, 30, 'a'));
        assertTrue(ImeKeyboardEvent.shouldDispatch(false, 0, '\u4F60'));
    }

    @Test
    public void testRejectsOrdinaryKeyUpAndInvalidImeCharacters()
    {
        assertFalse(ImeKeyboardEvent.shouldDispatch(false, 30, 'a'));
        assertFalse(ImeKeyboardEvent.shouldDispatch(false, 0, '\0'));
        assertFalse(ImeKeyboardEvent.shouldDispatch(false, 0, '\n'));
        assertFalse(ImeKeyboardEvent.shouldDispatch(false, 0, '\u0378'));
        assertFalse(ImeKeyboardEvent.shouldDispatch(false, 0, '\uD83D'));
    }
}


