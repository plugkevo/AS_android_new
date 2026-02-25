'use client'

import { useState } from 'react'
import { shipmentService } from '@/lib/services/shipmentService'
import { useAuthStore } from '@/store/authStore'
import { useShipments } from '@/hooks/useShipments'
import { notificationService } from '@/lib/services/notificationService'

interface ShipmentFormDialogProps {
  open: boolean
  onOpenChange: (open: boolean) => void
}

export function ShipmentFormDialog({ open, onOpenChange }: ShipmentFormDialogProps) {
  const { user } = useAuthStore()
  const { shipments } = useShipments()
  const [isLoading, setIsLoading] = useState(false)
  const [error, setError] = useState('')

  const [formData, setFormData] = useState({
    name: '',
    origin: '',
    destination: '',
    weight: '',
    details: '',
    status: 'Active' as const,
    date: new Date().toISOString().split('T')[0],
  })

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault()
    if (!user) return

    setError('')
    setIsLoading(true)

    try {
      await shipmentService.createShipment(user.uid, {
        ...formData,
        weight: Number(formData.weight),
      })

      // Create a notification
      await notificationService.createNotification(user.uid, {
        message: `New shipment "${formData.name}" created`,
        seen: false,
        type: 'shipment',
      })

      setFormData({
        name: '',
        origin: '',
        destination: '',
        weight: '',
        details: '',
        status: 'Active',
        date: new Date().toISOString().split('T')[0],
      })
      onOpenChange(false)
    } catch (err: any) {
      setError(err.message || 'Failed to create shipment')
    } finally {
      setIsLoading(false)
    }
  }

  if (!open) return null

  return (
    <div className="fixed inset-0 bg-black/50 flex items-center justify-center p-4 z-50">
      <div className="bg-background rounded-lg border border-border max-w-md w-full max-h-[90vh] overflow-y-auto">
        <div className="sticky top-0 border-b border-border bg-background p-4 flex items-center justify-between">
          <h2 className="text-lg font-semibold text-foreground">Create Shipment</h2>
          <button
            onClick={() => onOpenChange(false)}
            className="text-muted-foreground hover:text-foreground"
          >
            ✕
          </button>
        </div>

        <form onSubmit={handleSubmit} className="p-4 space-y-4">
          {error && (
            <div className="rounded-md bg-destructive/10 p-3 text-sm text-destructive">
              {error}
            </div>
          )}

          <div className="space-y-2">
            <label className="text-sm font-medium text-foreground">Shipment Name*</label>
            <input
              type="text"
              value={formData.name}
              onChange={(e) => setFormData({ ...formData, name: e.target.value })}
              required
              className="w-full px-3 py-2 border border-input rounded-md bg-background text-foreground placeholder-muted-foreground focus:outline-none focus:ring-2 focus:ring-primary"
              placeholder="e.g., Electronics Shipment"
            />
          </div>

          <div className="grid grid-cols-2 gap-4">
            <div className="space-y-2">
              <label className="text-sm font-medium text-foreground">Origin*</label>
              <input
                type="text"
                value={formData.origin}
                onChange={(e) => setFormData({ ...formData, origin: e.target.value })}
                required
                className="w-full px-3 py-2 border border-input rounded-md bg-background text-foreground placeholder-muted-foreground focus:outline-none focus:ring-2 focus:ring-primary"
                placeholder="City/Port"
              />
            </div>
            <div className="space-y-2">
              <label className="text-sm font-medium text-foreground">Destination*</label>
              <input
                type="text"
                value={formData.destination}
                onChange={(e) => setFormData({ ...formData, destination: e.target.value })}
                required
                className="w-full px-3 py-2 border border-input rounded-md bg-background text-foreground placeholder-muted-foreground focus:outline-none focus:ring-2 focus:ring-primary"
                placeholder="City/Port"
              />
            </div>
          </div>

          <div className="grid grid-cols-2 gap-4">
            <div className="space-y-2">
              <label className="text-sm font-medium text-foreground">Weight (kg)*</label>
              <input
                type="number"
                value={formData.weight}
                onChange={(e) => setFormData({ ...formData, weight: e.target.value })}
                required
                min="0.1"
                step="0.1"
                className="w-full px-3 py-2 border border-input rounded-md bg-background text-foreground placeholder-muted-foreground focus:outline-none focus:ring-2 focus:ring-primary"
                placeholder="0.0"
              />
            </div>
            <div className="space-y-2">
              <label className="text-sm font-medium text-foreground">Date*</label>
              <input
                type="date"
                value={formData.date}
                onChange={(e) => setFormData({ ...formData, date: e.target.value })}
                required
                className="w-full px-3 py-2 border border-input rounded-md bg-background text-foreground placeholder-muted-foreground focus:outline-none focus:ring-2 focus:ring-primary"
              />
            </div>
          </div>

          <div className="space-y-2">
            <label className="text-sm font-medium text-foreground">Status*</label>
            <select
              value={formData.status}
              onChange={(e) => setFormData({ ...formData, status: e.target.value as any })}
              className="w-full px-3 py-2 border border-input rounded-md bg-background text-foreground focus:outline-none focus:ring-2 focus:ring-primary"
            >
              <option value="Active">Active</option>
              <option value="In Transit">In Transit</option>
              <option value="Processing">Processing</option>
              <option value="Delivered">Delivered</option>
            </select>
          </div>

          <div className="space-y-2">
            <label className="text-sm font-medium text-foreground">Details</label>
            <textarea
              value={formData.details}
              onChange={(e) => setFormData({ ...formData, details: e.target.value })}
              className="w-full px-3 py-2 border border-input rounded-md bg-background text-foreground placeholder-muted-foreground focus:outline-none focus:ring-2 focus:ring-primary resize-none"
              placeholder="Additional details about the shipment"
              rows={3}
            />
          </div>

          <div className="flex gap-3 pt-4">
            <button
              type="button"
              onClick={() => onOpenChange(false)}
              className="flex-1 px-4 py-2 border border-input rounded-md hover:bg-secondary transition-colors text-foreground font-medium"
            >
              Cancel
            </button>
            <button
              type="submit"
              disabled={isLoading}
              className="flex-1 px-4 py-2 bg-primary text-primary-foreground rounded-md hover:bg-primary/90 disabled:opacity-50 transition-colors font-medium"
            >
              {isLoading ? 'Creating...' : 'Create Shipment'}
            </button>
          </div>
        </form>
      </div>
    </div>
  )
}
