'use client'

import { useState } from 'react'
import { AppLayout } from '@/components/AppLayout'
import { ShipmentList } from '@/components/shipments/ShipmentList'
import { ShipmentFormDialog } from '@/components/shipments/ShipmentFormDialog'

export default function ShipmentsPage() {
  const [isFormOpen, setIsFormOpen] = useState(false)

  return (
    <AppLayout>
      <div className="p-4 md:p-6 space-y-6">
        <div className="flex items-center justify-between">
          <div>
            <h1 className="text-3xl font-bold text-foreground">Shipments</h1>
            <p className="text-muted-foreground mt-1">Manage and track your shipments</p>
          </div>
          <button
            onClick={() => setIsFormOpen(true)}
            className="px-4 py-2 bg-primary text-primary-foreground rounded-md hover:bg-primary/90 transition-colors font-medium"
          >
            Create Shipment
          </button>
        </div>

        <ShipmentList />
        <ShipmentFormDialog open={isFormOpen} onOpenChange={setIsFormOpen} />
      </div>
    </AppLayout>
  )
}
