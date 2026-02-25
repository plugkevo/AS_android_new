'use client'

import { useShipments } from '@/hooks/useShipments'
import { useRouter } from 'next/navigation'
import { useState } from 'react'

const statusColors: Record<string, string> = {
  'Active': 'bg-blue-100 text-blue-800',
  'In Transit': 'bg-orange-100 text-orange-800',
  'Delivered': 'bg-green-100 text-green-800',
  'Processing': 'bg-yellow-100 text-yellow-800',
}

export function ShipmentList() {
  const router = useRouter()
  const { shipments, isLoading } = useShipments()
  const [filter, setFilter] = useState('')
  const [statusFilter, setStatusFilter] = useState('')

  const filtered = shipments.filter(
    (s) =>
      (s.name.toLowerCase().includes(filter.toLowerCase()) ||
        s.origin.toLowerCase().includes(filter.toLowerCase()) ||
        s.destination.toLowerCase().includes(filter.toLowerCase())) &&
      (!statusFilter || s.status === statusFilter)
  )

  if (isLoading) {
    return (
      <div className="flex justify-center items-center py-12">
        <div className="w-8 h-8 border-4 border-primary border-t-transparent rounded-full animate-spin"></div>
      </div>
    )
  }

  return (
    <div className="space-y-4">
      {/* Filters */}
      <div className="flex flex-col gap-4 md:flex-row md:items-center">
        <input
          type="text"
          placeholder="Search shipments..."
          value={filter}
          onChange={(e) => setFilter(e.target.value)}
          className="flex-1 px-3 py-2 border border-input rounded-md bg-background text-foreground placeholder-muted-foreground focus:outline-none focus:ring-2 focus:ring-primary"
        />
        <select
          value={statusFilter}
          onChange={(e) => setStatusFilter(e.target.value)}
          className="px-3 py-2 border border-input rounded-md bg-background text-foreground focus:outline-none focus:ring-2 focus:ring-primary"
        >
          <option value="">All Status</option>
          <option value="Active">Active</option>
          <option value="In Transit">In Transit</option>
          <option value="Processing">Processing</option>
          <option value="Delivered">Delivered</option>
        </select>
      </div>

      {/* List */}
      <div className="space-y-3">
        {filtered.length === 0 ? (
          <div className="rounded-lg border border-border bg-card p-8 text-center">
            <p className="text-muted-foreground">No shipments found</p>
          </div>
        ) : (
          filtered.map((shipment) => (
            <div
              key={shipment.id}
              onClick={() => router.push(`/shipments/${shipment.id}`)}
              className="p-4 rounded-lg border border-border bg-card hover:shadow-md transition-all cursor-pointer"
            >
              <div className="flex items-start justify-between gap-4">
                <div className="flex-1 min-w-0">
                  <h3 className="font-semibold text-foreground truncate">{shipment.name}</h3>
                  <p className="text-sm text-muted-foreground mt-1">
                    {shipment.origin} → {shipment.destination}
                  </p>
                  <div className="flex items-center gap-2 mt-2 flex-wrap">
                    <span className="text-xs text-muted-foreground">{shipment.weight}kg</span>
                    <span className="text-xs text-muted-foreground">•</span>
                    <span className="text-xs text-muted-foreground">
                      {new Date(shipment.date).toLocaleDateString()}
                    </span>
                  </div>
                </div>
                <div className="flex flex-col items-end gap-2">
                  <span className={`px-3 py-1 rounded-full text-xs font-medium whitespace-nowrap ${statusColors[shipment.status]}`}>
                    {shipment.status}
                  </span>
                </div>
              </div>
            </div>
          ))
        )}
      </div>
    </div>
  )
}
