package com.cheatbreaker.client.module.type;

import com.cheatbreaker.client.config.Setting;
import com.cheatbreaker.client.event.type.GuiDrawEvent;
import com.cheatbreaker.client.event.type.RenderPreviewEvent;
import com.cheatbreaker.client.module.AbstractModule;
import com.cheatbreaker.client.ui.module.CBGuiAnchor;

public final class ChatModule extends AbstractModule
{
    private final ChatVisibilityState visibilityState = new ChatVisibilityState();
    private final ChatActionFeedbackState actionFeedbackState = new ChatActionFeedbackState();
    private final ChatActionNotificationRenderer actionNotificationRenderer = new ChatActionNotificationRenderer();
    public final Setting showVisibilityNotifications;
    public final Setting showCopyNotifications;
    public final Setting showBackground;

    public ChatModule()
    {
        super("Chat");
        this.showBackground = new Setting(this, "Show Background").setValue(true);
        this.showVisibilityNotifications = new Setting(this, "Show Visibility Notifications")
            .setValue(true)
            .onChange(value -> this.actionFeedbackState.setVisibilityNotificationsEnabled((Boolean)value, monotonicMillis()));
        this.showCopyNotifications = new Setting(this, "Show Copy Notifications")
            .setValue(true)
            .onChange(value -> this.actionFeedbackState.setCopyNotificationsEnabled((Boolean)value, monotonicMillis()));
        this.addEvent(GuiDrawEvent.class, this::drawActionNotifications);
        this.addEvent(RenderPreviewEvent.class, this::drawPreview);
        this.setDefaultAnchor(CBGuiAnchor.LEFT_BOTTOM);
        this.setDefaultTranslations(0F, -26F);
        this.setDefaultRenderHud(true);
        this.setDimensions(324F, 90F);
        this.setPreviewLabel("Chat", 1.0F);
        this.setDefaultState(true);
    }

    public boolean toggleHidden()
    {
        if (this.isEnabled())
        {
            this.visibilityState.toggle();
            return this.actionFeedbackState.recordVisibility(this.visibilityState.isHidden(), monotonicMillis());
        }

        return false;
    }

    @Override
    public void setState(boolean state)
    {
        if (!state)
        {
            this.visibilityState.show();
            this.actionFeedbackState.clear(monotonicMillis());
        }

        super.setState(state);
    }

    public boolean isHidden()
    {
        return this.visibilityState.isHidden();
    }

    public long beginCopy()
    {
        return this.actionFeedbackState.beginCopy();
    }

    public boolean recordCopy(long generation, boolean success)
    {
        if (!this.isEnabled())
        {
            return false;
        }

        return this.actionFeedbackState.recordCopy(success ? ChatActionFeedbackState.CopyResult.SUCCESS
            : ChatActionFeedbackState.CopyResult.FAILURE, generation, monotonicMillis());
    }

    public boolean shouldRenderChat(boolean chatOpen)
    {
        return !this.isEnabled() || this.visibilityState.shouldRenderChat(chatOpen);
    }

    public int getActionNotificationStackHeight()
    {
        return this.actionFeedbackState.getStackHeight(monotonicMillis());
    }

    private void drawActionNotifications(GuiDrawEvent event)
    {
        updateChatDimensions();
        long now = monotonicMillis();
        this.actionNotificationRenderer.render(this.actionFeedbackState.getNotifications(now), event.getResolution(), now);
    }

    public void updateChatDimensions() {
        if (minecraft.ingameGUI == null) return;
        net.minecraft.client.gui.GuiNewChat chat = minecraft.ingameGUI.getChatGUI();
        float chatScale = chat.getChatScale();
        int lines = cn.kazer.client189.compat.KazerCompat.visibleChatLines(chat);
        if (lines == 0 && isEditingChat()) lines = 3;
        setDimensions(((float)Math.ceil(chat.getChatWidth() / chatScale) + 4F) * chatScale,
                lines * 9F * chatScale);
    }

    private boolean isEditingChat() {
        return minecraft.currentScreen instanceof com.cheatbreaker.client.ui.module.CBModulePlaceGui
                || minecraft.currentScreen instanceof com.cheatbreaker.client.ui.module.CBModulesGui;
    }

    private void drawPreview(RenderPreviewEvent event) {
        if (!isRenderHud() || !isEditingChat() || minecraft.ingameGUI == null
                || cn.kazer.client189.compat.KazerCompat.visibleChatLines(minecraft.ingameGUI.getChatGUI()) != 0) return;
        updateChatDimensions();
        org.lwjgl.opengl.GL11.glPushMatrix();
        try {
            scaleAndTranslate(event.getResolution());
            if ((Boolean)showBackground.getValue()) cn.kazer.client189.compat.LegacyGui.drawRect(0F, 0F, width, height, 0x80000000);
            org.lwjgl.opengl.GL11.glScalef(minecraft.ingameGUI.getChatGUI().getChatScale(),
                    minecraft.ingameGUI.getChatGUI().getChatScale(), 1F);
            // Chat previews deliberately use the same Minecraft font and line pitch as live chat.
            net.minecraft.client.gui.FontRenderer font = minecraft.fontRendererObj;
            font.drawStringWithShadow("Welcome to Kazer Client!", 0, 0, 0xFFFFFFFF);
            font.drawStringWithShadow("Drag this chat to reposition it.", 0, 9, 0xFFFFFFFF);
            font.drawStringWithShadow("Chat messages appear here.", 0, 18, 0xFFFFFFFF);
        } finally { org.lwjgl.opengl.GL11.glPopMatrix(); }
    }

    private static long monotonicMillis()
    {
        return System.nanoTime() / 1_000_000L;
    }
}
