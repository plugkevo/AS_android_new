import { useEffect, useState } from 'react'
import { loadingListService, LoadingListItem } from '@/lib/services/loadingListService'
import { useAuthStore } from '@/store/authStore'

export function useLoadingLists() {
  const { user } = useAuthStore()
  const [loadingLists, setLoadingLists] = useState<LoadingListItem[]>([])
  const [isLoading, setIsLoading] = useState(true)

  useEffect(() => {
    if (!user) return

    setIsLoading(true)
    const unsubscribe = loadingListService.subscribeToLoadingLists(user.uid, (data) => {
      setLoadingLists(data)
      setIsLoading(false)
    })

    return () => unsubscribe()
  }, [user])

  return { loadingLists, isLoading }
}
