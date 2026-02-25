'use client'

import { useState, useEffect } from 'react'
import { paymentService } from '@/lib/services/paymentService'
import { useAuthStore } from '@/store/authStore'

export function PaymentManagement() {
  const { user } = useAuthStore()
  const [payments, setPayments] = useState<any[]>([])
  const [isLoading, setIsLoading] = useState(false)
  const [isFormOpen, setIsFormOpen] = useState(false)
  const [error, setError] = useState('')

  const [formData, setFormData] = useState({
    phoneNumber: '',
    amount: '',
    shipmentId: '',
  })

  useEffect(() => {
    if (user) {
      loadPayments()
    }
  }, [user])

  const loadPayments = async () => {
    if (!user) return
    try {
      const data = await paymentService.getPayments(user.uid)
      setPayments(data)
    } catch (err) {
      console.error('Failed to load payments:', err)
    }
  }

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault()
    if (!user) return

    setError('')
    setIsLoading(true)

    try {
      // Create payment record
      await paymentService.createPayment(user.uid, {
        amount: Number(formData.amount),
        phoneNumber: formData.phoneNumber,
        shipmentId: formData.shipmentId || undefined,
        status: 'pending',
        paymentMethod: 'mPesa',
      })

      // In a real implementation, you would integrate with M-Pesa API here
      // await paymentService.processMPesaPayment(...)

      setFormData({
        phoneNumber: '',
        amount: '',
        shipmentId: '',
      })
      setIsFormOpen(false)
      await loadPayments()
    } catch (err: any) {
      setError(err.message || 'Failed to process payment')
    } finally {
      setIsLoading(false)
    }
  }

  const statusColors: Record<string, string> = {
    'pending': 'bg-yellow-100 text-yellow-800',
    'completed': 'bg-green-100 text-green-800',
    'failed': 'bg-red-100 text-red-800',
  }

  return (
    <div className="p-4 md:p-6 space-y-6">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-3xl font-bold text-foreground">Payments</h1>
          <p className="text-muted-foreground mt-1">Process payments with M-Pesa</p>
        </div>
        <button
          onClick={() => setIsFormOpen(true)}
          className="px-4 py-2 bg-primary text-primary-foreground rounded-md hover:bg-primary/90 transition-colors font-medium"
        >
          New Payment
        </button>
      </div>

      {/* Form Dialog */}
      {isFormOpen && (
        <div className="fixed inset-0 bg-black/50 flex items-center justify-center p-4 z-50">
          <div className="bg-background rounded-lg border border-border max-w-md w-full">
            <div className="border-b border-border p-4 flex items-center justify-between">
              <h2 className="text-lg font-semibold text-foreground">New Payment</h2>
              <button
                onClick={() => setIsFormOpen(false)}
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
                <label className="text-sm font-medium text-foreground">Phone Number (M-Pesa)*</label>
                <input
                  type="tel"
                  value={formData.phoneNumber}
                  onChange={(e) => setFormData({ ...formData, phoneNumber: e.target.value })}
                  required
                  className="w-full px-3 py-2 border border-input rounded-md bg-background text-foreground focus:outline-none focus:ring-2 focus:ring-primary"
                  placeholder="254712345678"
                />
                <p className="text-xs text-muted-foreground">Format: country code + number</p>
              </div>

              <div className="space-y-2">
                <label className="text-sm font-medium text-foreground">Amount (KES)*</label>
                <input
                  type="number"
                  value={formData.amount}
                  onChange={(e) => setFormData({ ...formData, amount: e.target.value })}
                  required
                  min="1"
                  step="1"
                  className="w-full px-3 py-2 border border-input rounded-md bg-background text-foreground focus:outline-none focus:ring-2 focus:ring-primary"
                  placeholder="1000"
                />
              </div>

              <div className="space-y-2">
                <label className="text-sm font-medium text-foreground">Description (Optional)</label>
                <input
                  type="text"
                  value={formData.shipmentId}
                  onChange={(e) => setFormData({ ...formData, shipmentId: e.target.value })}
                  className="w-full px-3 py-2 border border-input rounded-md bg-background text-foreground focus:outline-none focus:ring-2 focus:ring-primary"
                  placeholder="Shipment reference (optional)"
                />
              </div>

              <div className="bg-secondary/50 p-3 rounded-md">
                <p className="text-sm text-foreground font-medium">Total Amount</p>
                <p className="text-2xl font-bold text-primary">{formData.amount || '0'} KES</p>
              </div>

              <div className="flex gap-3 pt-4">
                <button
                  type="button"
                  onClick={() => setIsFormOpen(false)}
                  className="flex-1 px-4 py-2 border border-input rounded-md hover:bg-secondary transition-colors font-medium"
                >
                  Cancel
                </button>
                <button
                  type="submit"
                  disabled={isLoading}
                  className="flex-1 px-4 py-2 bg-primary text-primary-foreground rounded-md hover:bg-primary/90 disabled:opacity-50 transition-colors font-medium"
                >
                  {isLoading ? 'Processing...' : 'Pay via M-Pesa'}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* Payments List */}
      <div className="space-y-4">
        <h2 className="text-xl font-semibold text-foreground">Payment History</h2>
        
        {payments.length === 0 ? (
          <div className="rounded-lg border border-border bg-card p-8 text-center">
            <p className="text-muted-foreground">No payments yet</p>
          </div>
        ) : (
          <div className="space-y-3">
            {payments.map((payment) => (
              <div
                key={payment.id}
                className="p-4 rounded-lg border border-border bg-card"
              >
                <div className="flex items-start justify-between gap-4">
                  <div className="flex-1">
                    <div className="flex items-center gap-2">
                      <h3 className="font-semibold text-foreground">M-Pesa Payment</h3>
                      <span className={`px-2 py-1 rounded text-xs font-medium ${statusColors[payment.status]}`}>
                        {payment.status.toUpperCase()}
                      </span>
                    </div>
                    <p className="text-sm text-muted-foreground mt-1">
                      {payment.phoneNumber}
                    </p>
                    <p className="text-xs text-muted-foreground mt-2">
                      {new Date(payment.createdAt).toLocaleDateString()}
                    </p>
                  </div>
                  <div className="text-right">
                    <p className="text-lg font-bold text-foreground">{payment.amount} KES</p>
                  </div>
                </div>
              </div>
            ))}
          </div>
        )}
      </div>
    </div>
  )
}
