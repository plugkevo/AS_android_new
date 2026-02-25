import { useEffect, useState } from 'react'
import { notificationService, Notification } from '@/lib/services/notificationService'
import { useAuthStore } from '@/store/authStore'

export function useNotifications() {
  const { user } = useAuthStore()
  const [notifications, setNotifications] = useState<Notification[]>([])
  const [isLoading, setIsLoading] = useState(true)

  useEffect(() => {
    if (!user) return

    setIsLoading(true)
    const unsubscribe = notificationService.subscribeToNotifications(user.uid, (data) => {
      setNotifications(data)
      setIsLoading(false)
    })

    return () => unsubscribe()
  }, [user])

  const unseenCount = notifications.filter(n => !n.seen).length

  return { notifications, isLoading, unseenCount }
}
