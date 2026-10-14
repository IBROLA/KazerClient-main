package cn.kazer.client189.input;

public final class ImeCaretPosition
{
    private final int x;
    private final int top;
    private final int bottom;
    private final int clientWidth;
    private final int clientHeight;
    private final boolean above;

    private ImeCaretPosition(int x, int top, int bottom, int clientWidth, int clientHeight, boolean above)
    {
        this.x = x;
        this.top = top;
        this.bottom = bottom;
        this.clientWidth = clientWidth;
        this.clientHeight = clientHeight;
        this.above = above;
    }

    public static ImeCaretPosition fromGui(int x, int top, int height, int guiWidth, int guiHeight,
        int clientWidth, int clientHeight)
    {
        return fromGui(x, top, height, guiWidth, guiHeight, clientWidth, clientHeight, false);
    }

    public static ImeCaretPosition fromGuiAbove(int x, int top, int height, int guiWidth, int guiHeight,
        int clientWidth, int clientHeight)
    {
        return fromGui(x, top, height, guiWidth, guiHeight, clientWidth, clientHeight, true);
    }

    private static ImeCaretPosition fromGui(int x, int top, int height, int guiWidth, int guiHeight,
        int clientWidth, int clientHeight, boolean above)
    {
        return new ImeCaretPosition(scale(x, guiWidth, clientWidth), scale(top, guiHeight, clientHeight),
            scale(top + height, guiHeight, clientHeight), clientWidth, clientHeight, above);
    }

    private static int scale(int coordinate, int guiSize, int clientSize)
    {
        int scaled = (int)((long)coordinate * (long)clientSize / (long)guiSize);
        return Math.max(0, Math.min(clientSize - 1, scaled));
    }

    public int getX()
    {
        return this.x;
    }

    public int getTop()
    {
        return this.top;
    }

    public int getBottom()
    {
        return this.bottom;
    }

    public int getCompositionY()
    {
        return this.above ? this.top : this.bottom;
    }

    public int getExcludeLeft()
    {
        return this.above ? 0 : this.x;
    }

    public int getExcludeTop()
    {
        return this.top;
    }

    public int getExcludeRight()
    {
        return this.above ? this.clientWidth : this.x + 1;
    }

    public int getExcludeBottom()
    {
        return this.above ? this.clientHeight : this.bottom;
    }

    @Override
    public boolean equals(Object other)
    {
        if (this == other)
        {
            return true;
        }

        if (!(other instanceof ImeCaretPosition))
        {
            return false;
        }

        ImeCaretPosition position = (ImeCaretPosition)other;
        return this.x == position.x && this.top == position.top && this.bottom == position.bottom
            && this.clientWidth == position.clientWidth && this.clientHeight == position.clientHeight
            && this.above == position.above;
    }

    @Override
    public int hashCode()
    {
        int result = this.x;
        result = 31 * result + this.top;
        result = 31 * result + this.bottom;
        result = 31 * result + this.clientWidth;
        result = 31 * result + this.clientHeight;
        return 31 * result + (this.above ? 1 : 0);
    }
}


