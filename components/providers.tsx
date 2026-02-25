'use client'

import { useEffect } from 'react'
import { useAuthStore } from '@/store/authStore'

export function Providers({ children }: { children: React.ReactNode }) {
  const { initializeAuth } = useAuthStore()

  useEffect(() => {
    initializeAuth()
  }, [initializeAuth])

  return <>{children}</>
}
