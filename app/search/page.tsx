'use client'

import { useState, useEffect } from 'react'
import { AppLayout } from '@/components/AppLayout'
import { shipmentService } from '@/lib/services/shipmentService'
import { loadingListService } from '@/lib/services/loadingListService'
import { useAuthStore } from '@/store/authStore'
import { useRouter } from 'next/navigation'

export default function SearchPage() {
  const router = useRouter()
  const { user } = useAuthStore()
  const [searchQuery, setSearchQuery] = useState('')
  const [shipmentResults, setShipmentResults] = useState<any[]>([])
  const [listResults, setListResults] = useState<any[]>([])
  const [isLoading, setIsLoading] = useState(false)

  useEffect(() => {
    const performSearch = async () => {
      if (!user || !searchQuery.trim()) {
        setShipmentResults([])
        setListResults([])
        return
      }

      setIsLoading(true)
      try {
        const query = searchQuery.toLowerCase()

        // Search shipments
        const shipments = await shipmentService.getShipments(user.uid)
        const filteredShipments = shipments.filter(
          s =>
            s.name.toLowerCase().includes(query) ||
            s.origin.toLowerCase().includes(query) ||
            s.destination.toLowerCase().includes(query) ||
            s.details.toLowerCase().includes(query)
        )

        // Search loading lists
        const lists = await loadingListService.getLoadingLists(user.uid)
        const filteredLists = lists.filter(
          l =>
            l.name.toLowerCase().includes(query) ||
            l.origin.toLowerCase().includes(query) ||
            l.destination.toLowerCase().includes(query)
        )

        setShipmentResults(filteredShipments)
        setListResults(filteredLists)
      } catch (err) {
        console.error('Search error:', err)
      } finally {
        setIsLoading(false)
      }
    }

    const timer = setTimeout(performSearch, 300)
    return () => clearTimeout(timer)
  }, [searchQuery, user])

  const totalResults = shipmentResults.length + listResults.length

  return (
    <AppLayout>
      <div className="p-4 md:p-6 space-y-6 max-w-3xl mx-auto">
        <div>
          <h1 className="text-3xl font-bold text-foreground">Search</h1>
          <p className="text-muted-foreground mt-1">Find shipments and loading lists</p>
        </div>

        <input
          type="text"
          placeholder="Search by name, origin, destination..."
          value={searchQuery}
          onChange={(e) => setSearchQuery(e.target.value)}
          autoFocus
          className="w-full px-4 py-3 border border-input rounded-lg bg-background text-foreground placeholder-muted-foreground focus:outline-none focus:ring-2 focus:ring-primary text-lg"
        />

        {isLoading && (
          <div className="flex justify-center py-8">
            <div className="w-8 h-8 border-4 border-primary border-t-transparent rounded-full animate-spin"></div>
          </div>
        )}

        {searchQuery && !isLoading && (
          <div>
            <p className="text-sm text-muted-foreground mb-4">
              Found {totalResults} result{totalResults !== 1 ? 's' : ''}
            </p>

            {/* Shipments Results */}
            {shipmentResults.length > 0 && (
              <div className="space-y-4 mb-8">
                <h2 className="text-lg font-semibold text-foreground">Shipments ({shipmentResults.length})</h2>
                <div className="space-y-3">
                  {shipmentResults.map((shipment) => (
                    <div
                      key={shipment.id}
                      onClick={() => router.push(`/shipments/${shipment.id}`)}
                      className="p-4 rounded-lg border border-border bg-card hover:shadow-md transition-all cursor-pointer"
                    >
                      <div className="flex items-start justify-between gap-4">
                        <div className="flex-1">
                          <h3 className="font-semibold text-foreground">{shipment.name}</h3>
                          <p className="text-sm text-muted-foreground mt-1">
                            {shipment.origin} → {shipment.destination}
                          </p>
                          <p className="text-xs text-muted-foreground mt-2">
                            {shipment.weight}kg • {new Date(shipment.date).toLocaleDateString()}
                          </p>
                        </div>
                        <span className="px-3 py-1 rounded-full text-xs font-medium bg-blue-100 text-blue-800 whitespace-nowrap">
                          {shipment.status}
                        </span>
                      </div>
                    </div>
                  ))}
                </div>
              </div>
            )}

            {/* Loading Lists Results */}
            {listResults.length > 0 && (
              <div className="space-y-4">
                <h2 className="text-lg font-semibold text-foreground">Loading Lists ({listResults.length})</h2>
                <div className="space-y-3">
                  {listResults.map((list) => (
                    <div
                      key={list.id}
                      className="p-4 rounded-lg border border-border bg-card hover:shadow-md transition-all"
                    >
                      <div className="flex items-start justify-between gap-4">
                        <div className="flex-1">
                          <h3 className="font-semibold text-foreground">{list.name}</h3>
                          <p className="text-sm text-muted-foreground mt-1">
                            {list.origin} → {list.destination}
                          </p>
                          {list.extraDetails && (
                            <p className="text-xs text-muted-foreground mt-2">{list.extraDetails}</p>
                          )}
                        </div>
                        <span className="px-3 py-1 rounded-full text-xs font-medium bg-orange-100 text-orange-800 whitespace-nowrap">
                          {list.status}
                        </span>
                      </div>
                    </div>
                  ))}
                </div>
              </div>
            )}

            {totalResults === 0 && (
              <div className="rounded-lg border border-border bg-card p-8 text-center">
                <p className="text-muted-foreground">No results found for "{searchQuery}"</p>
              </div>
            )}
          </div>
        )}

        {!searchQuery && (
          <div className="rounded-lg border border-border bg-card p-8 text-center">
            <p className="text-muted-foreground">Enter a search query to get started</p>
          </div>
        )}
      </div>
    </AppLayout>
  )
}
