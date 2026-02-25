'use client'

import { AppLayout } from '@/components/AppLayout'
import { NotificationsList } from '@/components/notifications/NotificationsList'

export default function NotificationsPage() {
  return (
    <AppLayout>
      <NotificationsList />
    </AppLayout>
  )
}
