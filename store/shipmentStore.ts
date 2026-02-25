import { create } from 'zustand'
import { Shipment, shipmentService } from '@/lib/services/shipmentService'

interface ShipmentState {
  shipments: Shipment[]
  filteredShipments: Shipment[]
  isLoading: boolean
  filters: {
    status?: string
    origin?: string
    destination?: string
  }
  setShipments: (shipments: Shipment[]) => void
  setFilters: (filters: ShipmentState['filters']) => void
  addShipment: (shipment: Shipment) => void
  updateShipment: (id: string, shipment: Partial<Shipment>) => void
  deleteShipment: (id: string) => void
  setLoading: (loading: boolean) => void
}

export const useShipmentStore = create<ShipmentState>((set) => ({
  shipments: [],
  filteredShipments: [],
  isLoading: false,
  filters: {},

  setShipments: (shipments: Shipment[]) => {
    set((state) => {
      const filtered = applyFilters(shipments, state.filters)
      return { shipments, filteredShipments: filtered }
    })
  },

  setFilters: (filters) => {
    set((state) => {
      const filtered = applyFilters(state.shipments, filters)
      return { filters, filteredShipments: filtered }
    })
  },

  addShipment: (shipment: Shipment) => {
    set((state) => {
      const updated = [...state.shipments, shipment]
      const filtered = applyFilters(updated, state.filters)
      return { shipments: updated, filteredShipments: filtered }
    })
  },

  updateShipment: (id: string, updates: Partial<Shipment>) => {
    set((state) => {
      const updated = state.shipments.map(s => s.id === id ? { ...s, ...updates } : s)
      const filtered = applyFilters(updated, state.filters)
      return { shipments: updated, filteredShipments: filtered }
    })
  },

  deleteShipment: (id: string) => {
    set((state) => {
      const updated = state.shipments.filter(s => s.id !== id)
      const filtered = applyFilters(updated, state.filters)
      return { shipments: updated, filteredShipments: filtered }
    })
  },

  setLoading: (loading: boolean) => {
    set({ isLoading: loading })
  },
}))

function applyFilters(shipments: Shipment[], filters: ShipmentState['filters']): Shipment[] {
  return shipments.filter(shipment => {
    if (filters.status && shipment.status !== filters.status) return false
    if (filters.origin && !shipment.origin.toLowerCase().includes(filters.origin.toLowerCase())) return false
    if (filters.destination && !shipment.destination.toLowerCase().includes(filters.destination.toLowerCase())) return false
    return true
  })
}
