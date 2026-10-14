package cn.kazer.client189.input;

public final class ImeCaretAnchor
{
    private final int x;
    private final int top;
    private final int height;

    private ImeCaretAnchor(int x, int top, int height)
    {
        this.x = x;
        this.top = top;
        this.height = height;
    }

    public static ImeCaretAnchor forSign(int screenWidth, int screenHeight, int line, int textWidth, int height)
    {
        int currentLine = Math.max(0, Math.min(3, line));
        return forCenteredTextEnd(screenWidth / 2, 70 + currentLine * 10, textWidth, height);
    }

    public static ImeCaretAnchor forWrappedTextEnd(int left, int top, int lastLineWidth, int lineCount,
        int lineHeight)
    {
        return new ImeCaretAnchor(left + lastLineWidth,
            top + (Math.max(1, lineCount) - 1) * lineHeight, lineHeight);
    }

    public static ImeCaretAnchor forCenteredTextEnd(int centerX, int top, int textWidth, int height)
    {
        return new ImeCaretAnchor(centerX + textWidth / 2, top, height);
    }

    public int getX()
    {
        return this.x;
    }

    public int getTop()
    {
        return this.top;
    }

    public int getHeight()
    {
        return this.height;
    }
}


