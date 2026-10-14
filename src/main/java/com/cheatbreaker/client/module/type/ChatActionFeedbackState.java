package com.cheatbreaker.client.module.type;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class ChatActionFeedbackState
{
    public static final long NOTIFICATION_DURATION_MILLIS = 1_500L;
    public static final long NOTIFICATION_FADE_DURATION_MILLIS = 250L;
    private static final int NOTIFICATION_HEIGHT = 20;
    private static final int NOTIFICATION_GAP = 2;
    private static final int MAX_NOTIFICATIONS = 3;

    public enum NotificationType
    {
        INFO,
        ERROR
    }

    public enum NotificationCategory
    {
        VISIBILITY,
        COPY
    }

    public enum CopyResult
    {
        SUCCESS,
        FAILURE,
        NO_MESSAGE
    }

    public static final class Notification
    {
        private final String message;
        private final NotificationType type;
        private final NotificationCategory category;
        private final long createdAt;

        private Notification(String message, NotificationType type, NotificationCategory category, long createdAt)
        {
            this.message = message;
            this.type = type;
            this.category = category;
            this.createdAt = createdAt;
        }

        public String getMessage()
        {
            return this.message;
        }

        public NotificationType getType()
        {
            return this.type;
        }

        public NotificationCategory getCategory()
        {
            return this.category;
        }

        public long getCreatedAt()
        {
            return this.createdAt;
        }

        public float getOpacity(long now)
        {
            long elapsed = Math.max(0L, now - this.createdAt);
            long fadeStart = NOTIFICATION_DURATION_MILLIS - NOTIFICATION_FADE_DURATION_MILLIS;
            if (elapsed <= fadeStart)
            {
                return 1.0F;
            }
            if (elapsed >= NOTIFICATION_DURATION_MILLIS)
            {
                return 0.0F;
            }
            return 1.0F - (float)(elapsed - fadeStart) / (float)NOTIFICATION_FADE_DURATION_MILLIS;
        }
    }

    private final List<Notification> notifications = new ArrayList<>();
    private boolean visibilityNotificationsEnabled = true;
    private boolean copyNotificationsEnabled = true;
    private long copyGeneration;

    public boolean recordVisibility(boolean hidden, long now)
    {
        this.expire(now);
        if (this.visibilityNotificationsEnabled)
        {
            this.addNotification(hidden ? "Chat hidden." : "Chat shown.", NotificationType.INFO,
                NotificationCategory.VISIBILITY, now);
        }
        return true;
    }

    public boolean recordCopy(CopyResult result, long now)
    {
        return this.recordCopy(result, this.copyGeneration, now);
    }

    public long beginCopy()
    {
        return this.copyGeneration;
    }

    public boolean recordCopy(CopyResult result, long generation, long now)
    {
        this.expire(now);
        if (generation != this.copyGeneration)
        {
            return false;
        }
        if (result == CopyResult.NO_MESSAGE)
        {
            return false;
        }
        boolean success = result == CopyResult.SUCCESS;
        if (this.copyNotificationsEnabled)
        {
            this.addNotification(success ? "Chat copied." : "Failed to copy chat message.",
                success ? NotificationType.INFO : NotificationType.ERROR, NotificationCategory.COPY, now);
        }
        return success;
    }

    public List<Notification> getNotifications(long now)
    {
        this.expire(now);
        return Collections.unmodifiableList(new ArrayList<>(this.notifications));
    }

    public boolean isVisibilityNotificationsEnabled()
    {
        return this.visibilityNotificationsEnabled;
    }

    public void setVisibilityNotificationsEnabled(boolean enabled, long now)
    {
        this.expire(now);
        this.visibilityNotificationsEnabled = enabled;
        if (!enabled)
        {
            this.removeCategory(NotificationCategory.VISIBILITY);
        }
    }

    public boolean isCopyNotificationsEnabled()
    {
        return this.copyNotificationsEnabled;
    }

    public void setCopyNotificationsEnabled(boolean enabled, long now)
    {
        this.expire(now);
        this.copyNotificationsEnabled = enabled;
        if (!enabled)
        {
            this.removeCategory(NotificationCategory.COPY);
        }
    }

    public int getStackHeight(long now)
    {
        this.expire(now);
        return this.notifications.size() * (NOTIFICATION_HEIGHT + NOTIFICATION_GAP);
    }

    public void clear(long now)
    {
        ++this.copyGeneration;
        this.expire(now);
        this.notifications.clear();
    }

    private void expire(long now)
    {
        this.notifications.removeIf(notification -> now - notification.createdAt >= NOTIFICATION_DURATION_MILLIS);
    }

    private void addNotification(String message, NotificationType type, NotificationCategory category, long now)
    {
        this.notifications.add(0, new Notification(message, type, category, now));
        if (this.notifications.size() > MAX_NOTIFICATIONS)
        {
            this.notifications.remove(this.notifications.size() - 1);
        }
    }

    private void removeCategory(NotificationCategory category)
    {
        this.notifications.removeIf(notification -> notification.category == category);
    }
}
