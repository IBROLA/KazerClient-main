package cn.kazer.client189.input;

public final class ImeCaretTracker
{
    private final Positioner positioner;
    private ImeCaretPosition lastPosition;

    public ImeCaretTracker(Positioner positioner)
    {
        this.positioner = positioner;
    }

    public void report(ImeCaretPosition position)
    {
        if (!position.equals(this.lastPosition))
        {
            if (this.positioner.move(position))
            {
                this.lastPosition = position;
            }
        }
    }

    public void suspend()
    {
        this.lastPosition = null;
    }

    public interface Positioner
    {
        boolean move(ImeCaretPosition position);
    }
}


