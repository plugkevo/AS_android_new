import {
  collection,
  addDoc,
  getDocs,
  query,
  where,
  orderBy,
} from 'firebase/firestore'
import { firestore } from '@/lib/firebase'

export interface Payment {
  id: string
  userId: string
  shipmentId?: string
  amount: number
  phoneNumber: string
  status: 'pending' | 'completed' | 'failed'
  paymentMethod: 'mPesa'
  transactionId?: string
  createdAt: number
  updatedAt: number
}

const PAYMENTS_COLLECTION = 'payments'

export const paymentService = {
  async createPayment(userId: string, data: Omit<Payment, 'id' | 'userId' | 'createdAt' | 'updatedAt'>) {
    const now = Date.now()
    const docRef = await addDoc(collection(firestore, PAYMENTS_COLLECTION), {
      userId,
      ...data,
      createdAt: now,
      updatedAt: now,
    })
    return { id: docRef.id, ...data }
  },

  async getPayments(userId: string): Promise<Payment[]> {
    const q = query(
      collection(firestore, PAYMENTS_COLLECTION),
      where('userId', '==', userId),
      orderBy('createdAt', 'desc')
    )
    const snapshot = await getDocs(q)
    return snapshot.docs.map(doc => ({ id: doc.id, ...doc.data() } as Payment))
  },

  async processMPesaPayment(phoneNumber: string, amount: number, userId: string, shipmentId?: string) {
    try {
      const response = await fetch('/api/payments/mpesa', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
        },
        body: JSON.stringify({
          phoneNumber,
          amount,
          userId,
          shipmentId,
        }),
      })

      if (!response.ok) {
        throw new Error('Payment processing failed')
      }

      const data = await response.json()
      return data
    } catch (error) {
      console.error('M-Pesa payment error:', error)
      throw error
    }
  },
}
