package cn.kazer.client189.input;

public final class ImeKeyboardEvent
{
    private ImeKeyboardEvent()
    {
    }

    public static boolean shouldDispatch(boolean keyDown, int keyCode, char character)
    {
        return keyDown || keyCode == 0
            && character != '\0'
            && !Character.isISOControl(character)
            && !Character.isSurrogate(character)
            && Character.isDefined(character);
    }
}


