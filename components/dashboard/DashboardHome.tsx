'use client'

import { useShipments } from '@/hooks/useShipments'
import { useNotifications } from '@/hooks/useNotifications'
import { useRouter } from 'next/navigation'
import { useAuthStore } from '@/store/authStore'

export function DashboardHome() {
  const router = useRouter()
  const { user } = useAuthStore()
  const { shipments, isLoading: shipmentsLoading } = useShipments()
  const { notifications, isLoading: notificationsLoading } = useNotifications()

  const activeShipments = shipments.filter(s => s.status !== 'Delivered').length
  const deliveredShipments = shipments.filter(s => s.status === 'Delivered').length
  const unseenNotifications = notifications.filter(n => !n.seen).length

  const getStatusColor = (status: string) => {
    switch (status) {
      case 'Active':
        return 'bg-blue-100 text-blue-800'
      case 'In Transit':
        return 'bg-orange-100 text-orange-800'
      case 'Delivered':
        return 'bg-green-100 text-green-800'
      case 'Processing':
        return 'bg-yellow-100 text-yellow-800'
      default:
        return 'bg-gray-100 text-gray-800'
    }
  }

  return (
    <div className="p-4 md:p-6 space-y-6">
      {/* Welcome Section */}
      <section className="space-y-2">
        <h1 className="text-3xl font-bold text-foreground">Welcome back, {user?.displayName}</h1>
        <p className="text-muted-foreground">Here's an overview of your shipping activities</p>
      </section>

      {/* Quick Stats */}
      <section className="grid grid-cols-2 md:grid-cols-4 gap-4">
        <div className="rounded-lg border border-border bg-card p-4 space-y-2">
          <p className="text-sm text-muted-foreground">Total Shipments</p>
          <p className="text-2xl font-bold text-foreground">{shipments.length}</p>
        </div>
        <div className="rounded-lg border border-border bg-card p-4 space-y-2">
          <p className="text-sm text-muted-foreground">Active</p>
          <p className="text-2xl font-bold text-blue-600">{activeShipments}</p>
        </div>
        <div className="rounded-lg border border-border bg-card p-4 space-y-2">
          <p className="text-sm text-muted-foreground">Delivered</p>
          <p className="text-2xl font-bold text-green-600">{deliveredShipments}</p>
        </div>
        <div className="rounded-lg border border-border bg-card p-4 space-y-2">
          <p className="text-sm text-muted-foreground">Notifications</p>
          <p className="text-2xl font-bold text-primary">{unseenNotifications}</p>
        </div>
      </section>

      {/* Recent Shipments */}
      <section className="space-y-4">
        <div className="flex items-center justify-between">
          <h2 className="text-xl font-semibold text-foreground">Recent Shipments</h2>
          <button
            onClick={() => router.push('/shipments')}
            className="text-sm text-primary hover:underline"
          >
            View All
          </button>
        </div>

        {shipmentsLoading ? (
          <div className="text-center py-8">
            <div className="w-6 h-6 border-4 border-primary border-t-transparent rounded-full animate-spin mx-auto"></div>
          </div>
        ) : shipments.length === 0 ? (
          <div className="rounded-lg border border-border bg-card p-8 text-center">
            <p className="text-muted-foreground">No shipments yet</p>
            <button
              onClick={() => router.push('/shipments')}
              className="mt-4 px-4 py-2 bg-primary text-primary-foreground rounded-md hover:bg-primary/90 transition-colors"
            >
              Create Your First Shipment
            </button>
          </div>
        ) : (
          <div className="space-y-3">
            {shipments.slice(0, 5).map((shipment) => (
              <div
                key={shipment.id}
                onClick={() => router.push(`/shipments/${shipment.id}`)}
                className="p-4 rounded-lg border border-border bg-card hover:bg-secondary/50 transition-colors cursor-pointer"
              >
                <div className="flex items-start justify-between gap-4">
                  <div className="flex-1 space-y-1">
                    <h3 className="font-semibold text-foreground">{shipment.name}</h3>
                    <p className="text-sm text-muted-foreground">
                      {shipment.origin} → {shipment.destination}
                    </p>
                    <p className="text-xs text-muted-foreground">{shipment.weight}kg • {shipment.details}</p>
                  </div>
                  <span className={`px-3 py-1 rounded-full text-xs font-medium ${getStatusColor(shipment.status)}`}>
                    {shipment.status}
                  </span>
                </div>
              </div>
            ))}
          </div>
        )}
      </section>

      {/* Recent Notifications */}
      {notifications.length > 0 && (
        <section className="space-y-4">
          <div className="flex items-center justify-between">
            <h2 className="text-xl font-semibold text-foreground">Recent Notifications</h2>
            <button
              onClick={() => router.push('/notifications')}
              className="text-sm text-primary hover:underline"
            >
              View All
            </button>
          </div>
          <div className="space-y-2">
            {notifications.slice(0, 3).map((notification) => (
              <div
                key={notification.id}
                className={`p-3 rounded-lg border ${
                  notification.seen
                    ? 'border-border bg-background'
                    : 'border-primary/50 bg-primary/5'
                }`}
              >
                <p className={`text-sm ${notification.seen ? 'text-muted-foreground' : 'text-foreground font-medium'}`}>
                  {notification.message}
                </p>
                <p className="text-xs text-muted-foreground mt-1">
                  {new Date(notification.timestamp).toLocaleDateString()}
                </p>
              </div>
            ))}
          </div>
        </section>
      )}
    </div>
  )
}
