package com.cheatbreaker.client.module.type;

public final class ChatVisibilityState
{
    private boolean hidden;

    public void toggle()
    {
        this.hidden = !this.hidden;
    }

    public boolean isHidden()
    {
        return this.hidden;
    }

    public void show()
    {
        this.hidden = false;
    }

    public boolean shouldRenderChat(boolean chatOpen)
    {
        return !this.hidden || chatOpen;
    }
}
