'use client'

import { useNotifications } from '@/hooks/useNotifications'
import { notificationService } from '@/lib/services/notificationService'
import { useAuthStore } from '@/store/authStore'

export function NotificationsList() {
  const { user } = useAuthStore()
  const { notifications, isLoading } = useNotifications()

  const handleMarkAsRead = async (id: string) => {
    try {
      await notificationService.markAsRead(id)
    } catch (err) {
      console.error('Failed to mark as read:', err)
    }
  }

  const handleMarkAllAsRead = async () => {
    if (!user) return
    try {
      await notificationService.markAllAsRead(user.uid)
    } catch (err) {
      console.error('Failed to mark all as read:', err)
    }
  }

  const getTypeIcon = (type: string) => {
    switch (type) {
      case 'shipment':
        return '📦'
      case 'payment':
        return '💳'
      case 'system':
        return '⚙️'
      default:
        return '🔔'
    }
  }

  return (
    <div className="p-4 md:p-6 space-y-6">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-3xl font-bold text-foreground">Notifications</h1>
          <p className="text-muted-foreground mt-1">Stay updated on your activities</p>
        </div>
        {notifications.some(n => !n.seen) && (
          <button
            onClick={handleMarkAllAsRead}
            className="px-4 py-2 text-sm bg-secondary text-foreground rounded-md hover:bg-secondary/80 transition-colors"
          >
            Mark all as read
          </button>
        )}
      </div>

      {isLoading ? (
        <div className="flex justify-center items-center py-12">
          <div className="w-8 h-8 border-4 border-primary border-t-transparent rounded-full animate-spin"></div>
        </div>
      ) : notifications.length === 0 ? (
        <div className="rounded-lg border border-border bg-card p-8 text-center">
          <p className="text-muted-foreground text-lg">No notifications yet</p>
        </div>
      ) : (
        <div className="space-y-3">
          {notifications.map((notification) => (
            <div
              key={notification.id}
              onClick={() => !notification.seen && handleMarkAsRead(notification.id)}
              className={`p-4 rounded-lg border cursor-pointer transition-all ${
                notification.seen
                  ? 'border-border bg-background hover:bg-secondary/50'
                  : 'border-primary/50 bg-primary/5 hover:bg-primary/10'
              }`}
            >
              <div className="flex items-start gap-3">
                <span className="text-2xl">{getTypeIcon(notification.type)}</span>
                <div className="flex-1 min-w-0">
                  <p className={`text-sm ${notification.seen ? 'text-muted-foreground' : 'text-foreground font-semibold'}`}>
                    {notification.message}
                  </p>
                  <p className="text-xs text-muted-foreground mt-1">
                    {new Date(notification.timestamp).toLocaleString()}
                  </p>
                </div>
                {!notification.seen && (
                  <div className="w-2 h-2 rounded-full bg-primary mt-1 flex-shrink-0"></div>
                )}
              </div>
            </div>
          ))}
        </div>
      )}
    </div>
  )
}
