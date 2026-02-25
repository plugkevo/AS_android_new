'use client'

import { AppLayout } from '@/components/AppLayout'
import { PaymentManagement } from '@/components/payments/PaymentManagement'

export default function PaymentsPage() {
  return (
    <AppLayout>
      <PaymentManagement />
    </AppLayout>
  )
}
