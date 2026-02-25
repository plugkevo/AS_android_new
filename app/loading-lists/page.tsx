'use client'

import { AppLayout } from '@/components/AppLayout'
import { LoadingListManagement } from '@/components/loading-lists/LoadingListManagement'

export default function LoadingListsPage() {
  return (
    <AppLayout>
      <LoadingListManagement />
    </AppLayout>
  )
}
