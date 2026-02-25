import {
  collection,
  addDoc,
  updateDoc,
  deleteDoc,
  doc,
  getDocs,
  query,
  where,
  onSnapshot,
} from 'firebase/firestore'
import { firestore } from '@/lib/firebase'

export interface LoadingListItem {
  id: string
  userId: string
  name: string
  origin: string
  destination: string
  extraDetails: string
  status: 'Active' | 'Processing' | 'Completed'
  createdAt: number
  updatedAt: number
}

const LOADING_LISTS_COLLECTION = 'loadingLists'

export const loadingListService = {
  async createLoadingList(userId: string, data: Omit<LoadingListItem, 'id' | 'userId' | 'createdAt' | 'updatedAt'>) {
    const now = Date.now()
    const docRef = await addDoc(collection(firestore, LOADING_LISTS_COLLECTION), {
      userId,
      ...data,
      createdAt: now,
      updatedAt: now,
    })
    return { id: docRef.id, ...data }
  },

  async updateLoadingList(listId: string, data: Partial<LoadingListItem>) {
    const listRef = doc(firestore, LOADING_LISTS_COLLECTION, listId)
    await updateDoc(listRef, {
      ...data,
      updatedAt: Date.now(),
    })
  },

  async deleteLoadingList(listId: string) {
    await deleteDoc(doc(firestore, LOADING_LISTS_COLLECTION, listId))
  },

  async getLoadingLists(userId: string): Promise<LoadingListItem[]> {
    const q = query(
      collection(firestore, LOADING_LISTS_COLLECTION),
      where('userId', '==', userId)
    )
    const snapshot = await getDocs(q)
    return snapshot.docs.map(doc => ({ id: doc.id, ...doc.data() } as LoadingListItem))
  },

  subscribeToLoadingLists(userId: string, callback: (lists: LoadingListItem[]) => void) {
    const q = query(
      collection(firestore, LOADING_LISTS_COLLECTION),
      where('userId', '==', userId)
    )
    return onSnapshot(q, (snapshot) => {
      const lists = snapshot.docs.map(doc => ({ id: doc.id, ...doc.data() } as LoadingListItem))
      callback(lists)
    })
  },
}
