package cn.kazer.client189.input;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class ImeCaretTrackerTest
{
    @Test
    public void testDoesNotMoveNativeImeTwiceForSameCaret()
    {
        RecordingPositioner positioner = new RecordingPositioner();
        ImeCaretTracker tracker = new ImeCaretTracker(positioner);

        tracker.report(ImeCaretPosition.fromGui(100, 520, 9, 960, 540, 1920, 1080));
        tracker.report(ImeCaretPosition.fromGui(100, 520, 9, 960, 540, 1920, 1080));

        assertEquals(1, positioner.moveCount);
    }

    @Test
    public void testReappliesCaretAfterDisplayFocusReturns()
    {
        RecordingPositioner positioner = new RecordingPositioner();
        ImeCaretTracker tracker = new ImeCaretTracker(positioner);
        ImeCaretPosition position = ImeCaretPosition.fromGui(100, 520, 9, 960, 540, 1920, 1080);

        tracker.report(position);
        tracker.suspend();
        tracker.report(position);

        assertEquals(2, positioner.moveCount);
    }

    @Test
    public void testRetriesSameCaretWhenNativeImeContextWasUnavailable()
    {
        UnavailableOncePositioner positioner = new UnavailableOncePositioner();
        ImeCaretTracker tracker = new ImeCaretTracker(positioner);
        ImeCaretPosition position = ImeCaretPosition.fromGui(100, 520, 9, 960, 540, 1920, 1080);

        tracker.report(position);
        tracker.report(position);

        assertEquals(2, positioner.moveCount);
    }

    private static final class RecordingPositioner implements ImeCaretTracker.Positioner
    {
        private int moveCount;

        @Override
        public boolean move(ImeCaretPosition position)
        {
            ++this.moveCount;
            return true;
        }
    }

    private static final class UnavailableOncePositioner implements ImeCaretTracker.Positioner
    {
        private int moveCount;

        @Override
        public boolean move(ImeCaretPosition position)
        {
            return ++this.moveCount > 1;
        }
    }
}


