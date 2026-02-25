'use client'

import { useState, useEffect } from 'react'
import { AppLayout } from '@/components/AppLayout'
import { shipmentService, Shipment } from '@/lib/services/shipmentService'
import { useRouter, useParams } from 'next/navigation'
import { useAuthStore } from '@/store/authStore'

export default function ShipmentDetailPage() {
  const router = useRouter()
  const params = useParams()
  const { user } = useAuthStore()
  const [shipment, setShipment] = useState<Shipment | null>(null)
  const [isLoading, setIsLoading] = useState(true)
  const [isEditing, setIsEditing] = useState(false)
  const [editData, setEditData] = useState<Partial<Shipment>>({})

  useEffect(() => {
    const loadShipment = async () => {
      if (!user) return
      setIsLoading(true)
      try {
        const shipments = await shipmentService.getShipments(user.uid)
        const found = shipments.find(s => s.id === params.id)
        if (found) {
          setShipment(found)
          setEditData(found)
        }
      } catch (err) {
        console.error('Failed to load shipment:', err)
      } finally {
        setIsLoading(false)
      }
    }
    loadShipment()
  }, [user, params.id])

  const handleUpdate = async () => {
    if (!shipment || !user) return
    try {
      await shipmentService.updateShipment(shipment.id, editData)
      setShipment({ ...shipment, ...editData })
      setIsEditing(false)
    } catch (err) {
      console.error('Failed to update shipment:', err)
    }
  }

  const statusColors: Record<string, string> = {
    'Active': 'bg-blue-100 text-blue-800',
    'In Transit': 'bg-orange-100 text-orange-800',
    'Delivered': 'bg-green-100 text-green-800',
    'Processing': 'bg-yellow-100 text-yellow-800',
  }

  if (isLoading) {
    return (
      <AppLayout>
        <div className="flex justify-center items-center py-20">
          <div className="w-8 h-8 border-4 border-primary border-t-transparent rounded-full animate-spin"></div>
        </div>
      </AppLayout>
    )
  }

  if (!shipment) {
    return (
      <AppLayout>
        <div className="p-4 md:p-6">
          <div className="rounded-lg border border-border bg-card p-8 text-center">
            <p className="text-muted-foreground">Shipment not found</p>
            <button
              onClick={() => router.push('/shipments')}
              className="mt-4 px-4 py-2 bg-primary text-primary-foreground rounded-md hover:bg-primary/90"
            >
              Back to Shipments
            </button>
          </div>
        </div>
      </AppLayout>
    )
  }

  return (
    <AppLayout>
      <div className="p-4 md:p-6 space-y-6 max-w-3xl mx-auto">
        {/* Header */}
        <div className="flex items-start justify-between gap-4">
          <div>
            <button
              onClick={() => router.push('/shipments')}
              className="text-primary hover:underline text-sm mb-2"
            >
              ← Back to Shipments
            </button>
            <h1 className="text-3xl font-bold text-foreground">{shipment.name}</h1>
          </div>
          <span className={`px-4 py-2 rounded-full text-sm font-medium ${statusColors[shipment.status]}`}>
            {shipment.status}
          </span>
        </div>

        {/* Main Details */}
        <div className="rounded-lg border border-border bg-card p-6 space-y-6">
          <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
            <div className="space-y-4">
              <div>
                <p className="text-sm text-muted-foreground">Origin</p>
                {isEditing ? (
                  <input
                    type="text"
                    value={editData.origin || ''}
                    onChange={(e) => setEditData({ ...editData, origin: e.target.value })}
                    className="w-full mt-1 px-3 py-2 border border-input rounded-md bg-background text-foreground"
                  />
                ) : (
                  <p className="text-lg font-semibold text-foreground">{shipment.origin}</p>
                )}
              </div>

              <div>
                <p className="text-sm text-muted-foreground">Weight</p>
                {isEditing ? (
                  <input
                    type="number"
                    value={editData.weight || ''}
                    onChange={(e) => setEditData({ ...editData, weight: Number(e.target.value) })}
                    className="w-full mt-1 px-3 py-2 border border-input rounded-md bg-background text-foreground"
                  />
                ) : (
                  <p className="text-lg font-semibold text-foreground">{shipment.weight} kg</p>
                )}
              </div>

              <div>
                <p className="text-sm text-muted-foreground">Date</p>
                {isEditing ? (
                  <input
                    type="date"
                    value={editData.date || ''}
                    onChange={(e) => setEditData({ ...editData, date: e.target.value })}
                    className="w-full mt-1 px-3 py-2 border border-input rounded-md bg-background text-foreground"
                  />
                ) : (
                  <p className="text-lg font-semibold text-foreground">{new Date(shipment.date).toLocaleDateString()}</p>
                )}
              </div>
            </div>

            <div className="space-y-4">
              <div>
                <p className="text-sm text-muted-foreground">Destination</p>
                {isEditing ? (
                  <input
                    type="text"
                    value={editData.destination || ''}
                    onChange={(e) => setEditData({ ...editData, destination: e.target.value })}
                    className="w-full mt-1 px-3 py-2 border border-input rounded-md bg-background text-foreground"
                  />
                ) : (
                  <p className="text-lg font-semibold text-foreground">{shipment.destination}</p>
                )}
              </div>

              <div>
                <p className="text-sm text-muted-foreground">Status</p>
                {isEditing ? (
                  <select
                    value={editData.status || ''}
                    onChange={(e) => setEditData({ ...editData, status: e.target.value as any })}
                    className="w-full mt-1 px-3 py-2 border border-input rounded-md bg-background text-foreground"
                  >
                    <option value="Active">Active</option>
                    <option value="In Transit">In Transit</option>
                    <option value="Processing">Processing</option>
                    <option value="Delivered">Delivered</option>
                  </select>
                ) : (
                  <p className="text-lg font-semibold text-foreground">{shipment.status}</p>
                )}
              </div>

              <div>
                <p className="text-sm text-muted-foreground">Created</p>
                <p className="text-lg font-semibold text-foreground">
                  {new Date(shipment.createdAt).toLocaleDateString()}
                </p>
              </div>
            </div>
          </div>

          {/* Details */}
          <div className="border-t border-border pt-6">
            <p className="text-sm text-muted-foreground mb-2">Details</p>
            {isEditing ? (
              <textarea
                value={editData.details || ''}
                onChange={(e) => setEditData({ ...editData, details: e.target.value })}
                className="w-full px-3 py-2 border border-input rounded-md bg-background text-foreground resize-none"
                rows={4}
              />
            ) : (
              <p className="text-foreground whitespace-pre-wrap">{shipment.details}</p>
            )}
          </div>

          {/* Actions */}
          <div className="border-t border-border pt-6 flex gap-3">
            {isEditing ? (
              <>
                <button
                  onClick={handleUpdate}
                  className="flex-1 px-4 py-2 bg-primary text-primary-foreground rounded-md hover:bg-primary/90 transition-colors font-medium"
                >
                  Save Changes
                </button>
                <button
                  onClick={() => {
                    setIsEditing(false)
                    setEditData(shipment)
                  }}
                  className="flex-1 px-4 py-2 border border-input rounded-md hover:bg-secondary transition-colors font-medium"
                >
                  Cancel
                </button>
              </>
            ) : (
              <button
                onClick={() => setIsEditing(true)}
                className="flex-1 px-4 py-2 bg-primary text-primary-foreground rounded-md hover:bg-primary/90 transition-colors font-medium"
              >
                Edit Shipment
              </button>
            )}
          </div>
        </div>
      </div>
    </AppLayout>
  )
}
