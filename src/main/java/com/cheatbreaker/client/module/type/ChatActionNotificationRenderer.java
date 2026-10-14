package com.cheatbreaker.client.module.type;

import com.cheatbreaker.client.module.type.notifications.CBNotificationRenderer;
import com.cheatbreaker.client.module.type.notifications.CBNotificationType;
import net.minecraft.client.gui.ScaledResolution;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

final class ChatActionNotificationRenderer
{
    private static final float TOP_MARGIN = 5.0F;
    private static final float STEP = 22.0F;
    private final Map<ChatActionFeedbackState.Notification, Float> positions = new HashMap<>();
    private long lastRenderAt = -1L;

    void render(List<ChatActionFeedbackState.Notification> notifications, ScaledResolution resolution, long now)
    {
        this.positions.keySet().retainAll(notifications);
        long frameDelta = this.lastRenderAt < 0L ? 16L : Math.max(1L, Math.min(100L, now - this.lastRenderAt));
        float blend = Math.min(1.0F, (float)frameDelta / 100.0F);

        for (int index = 0; index < notifications.size(); ++index)
        {
            ChatActionFeedbackState.Notification notification = notifications.get(index);
            float target = TOP_MARGIN + index * STEP;
            Float current = this.positions.get(notification);
            if (current == null)
            {
                current = -20.0F;
            }
            current += (target - current) * blend;
            if (Math.abs(target - current) < 0.1F)
            {
                current = target;
            }
            this.positions.put(notification, current);
            CBNotificationRenderer.draw(this.toNativeType(notification.getType()), notification.getMessage(),
                resolution.getScaledWidth(), current, notification.getCreatedAt(),
                ChatActionFeedbackState.NOTIFICATION_DURATION_MILLIS, now, notification.getOpacity(now));
        }
        this.lastRenderAt = now;
    }

    private CBNotificationType toNativeType(ChatActionFeedbackState.NotificationType type)
    {
        return type == ChatActionFeedbackState.NotificationType.ERROR ? CBNotificationType.ERROR : CBNotificationType.INFO;
    }
}
