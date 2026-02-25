import {
  collection,
  addDoc,
  updateDoc,
  deleteDoc,
  doc,
  getDocs,
  query,
  where,
  Query,
  QueryConstraint,
  onSnapshot,
} from 'firebase/firestore'
import { firestore } from '@/lib/firebase'

export interface Shipment {
  id: string
  userId: string
  name: string
  origin: string
  destination: string
  weight: number
  details: string
  status: 'Active' | 'In Transit' | 'Delivered' | 'Processing'
  date: string
  latitude?: number
  longitude?: number
  createdAt: number
  updatedAt: number
}

const SHIPMENTS_COLLECTION = 'shipments'

export const shipmentService = {
  async createShipment(userId: string, data: Omit<Shipment, 'id' | 'userId' | 'createdAt' | 'updatedAt'>) {
    const now = Date.now()
    const docRef = await addDoc(collection(firestore, SHIPMENTS_COLLECTION), {
      userId,
      ...data,
      createdAt: now,
      updatedAt: now,
    })
    return { id: docRef.id, ...data }
  },

  async updateShipment(shipmentId: string, data: Partial<Shipment>) {
    const shipmentRef = doc(firestore, SHIPMENTS_COLLECTION, shipmentId)
    await updateDoc(shipmentRef, {
      ...data,
      updatedAt: Date.now(),
    })
  },

  async deleteShipment(shipmentId: string) {
    await deleteDoc(doc(firestore, SHIPMENTS_COLLECTION, shipmentId))
  },

  async getShipments(userId: string): Promise<Shipment[]> {
    const q = query(
      collection(firestore, SHIPMENTS_COLLECTION),
      where('userId', '==', userId)
    )
    const snapshot = await getDocs(q)
    return snapshot.docs.map(doc => ({ id: doc.id, ...doc.data() } as Shipment))
  },

  subscribeToShipments(userId: string, callback: (shipments: Shipment[]) => void) {
    const q = query(
      collection(firestore, SHIPMENTS_COLLECTION),
      where('userId', '==', userId)
    )
    return onSnapshot(q, (snapshot) => {
      const shipments = snapshot.docs.map(doc => ({ id: doc.id, ...doc.data() } as Shipment))
      callback(shipments)
    })
  },

  async searchShipments(userId: string, filters: {
    status?: string
    origin?: string
    destination?: string
  }): Promise<Shipment[]> {
    const constraints: QueryConstraint[] = [where('userId', '==', userId)]

    if (filters.status) {
      constraints.push(where('status', '==', filters.status))
    }

    const q = query(collection(firestore, SHIPMENTS_COLLECTION), ...constraints)
    const snapshot = await getDocs(q)
    let results = snapshot.docs.map(doc => ({ id: doc.id, ...doc.data() } as Shipment))

    if (filters.origin) {
      results = results.filter(s => s.origin.toLowerCase().includes(filters.origin!.toLowerCase()))
    }
    if (filters.destination) {
      results = results.filter(s => s.destination.toLowerCase().includes(filters.destination!.toLowerCase()))
    }

    return results
  },
}
