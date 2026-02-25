import { useEffect, useState } from 'react'
import { shipmentService, Shipment } from '@/lib/services/shipmentService'
import { useAuthStore } from '@/store/authStore'

export function useShipments() {
  const { user } = useAuthStore()
  const [shipments, setShipments] = useState<Shipment[]>([])
  const [isLoading, setIsLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    if (!user) return

    setIsLoading(true)
    const unsubscribe = shipmentService.subscribeToShipments(user.uid, (data) => {
      setShipments(data)
      setIsLoading(false)
    })

    return () => unsubscribe()
  }, [user])

  return { shipments, isLoading, error }
}
