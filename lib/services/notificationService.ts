import {
  collection,
  addDoc,
  updateDoc,
  doc,
  getDocs,
  query,
  where,
  onSnapshot,
  orderBy,
  Query,
} from 'firebase/firestore'
import { firestore } from '@/lib/firebase'

export interface Notification {
  id: string
  userId: string
  message: string
  timestamp: number
  seen: boolean
  type: 'shipment' | 'payment' | 'system'
  relatedId?: string
}

const NOTIFICATIONS_COLLECTION = 'notifications'

export const notificationService = {
  async createNotification(userId: string, data: Omit<Notification, 'id' | 'userId' | 'timestamp'>) {
    const docRef = await addDoc(collection(firestore, NOTIFICATIONS_COLLECTION), {
      userId,
      ...data,
      timestamp: Date.now(),
    })
    return { id: docRef.id, ...data }
  },

  async markAsRead(notificationId: string) {
    const notifRef = doc(firestore, NOTIFICATIONS_COLLECTION, notificationId)
    await updateDoc(notifRef, { seen: true })
  },

  async markAllAsRead(userId: string) {
    const q = query(
      collection(firestore, NOTIFICATIONS_COLLECTION),
      where('userId', '==', userId),
      where('seen', '==', false)
    )
    const snapshot = await getDocs(q)
    snapshot.docs.forEach(async (document) => {
      await updateDoc(document.ref, { seen: true })
    })
  },

  async getNotifications(userId: string): Promise<Notification[]> {
    const q = query(
      collection(firestore, NOTIFICATIONS_COLLECTION),
      where('userId', '==', userId),
      orderBy('timestamp', 'desc')
    )
    const snapshot = await getDocs(q)
    return snapshot.docs.map(doc => ({ id: doc.id, ...doc.data() } as Notification))
  },

  subscribeToNotifications(userId: string, callback: (notifications: Notification[]) => void) {
    const q = query(
      collection(firestore, NOTIFICATIONS_COLLECTION),
      where('userId', '==', userId),
      orderBy('timestamp', 'desc')
    )
    return onSnapshot(q, (snapshot) => {
      const notifications = snapshot.docs.map(doc => ({ id: doc.id, ...doc.data() } as Notification))
      callback(notifications)
    })
  },

  getUnseenCount(notifications: Notification[]): number {
    return notifications.filter(n => !n.seen).length
  },
}
