'use client'

import { useEffect, useState } from 'react'
import { useAuthStore } from '@/store/authStore'

export function Providers({ children }: { children: React.ReactNode }) {
  const { initializeAuth } = useAuthStore()
  const [mounted, setMounted] = useState(false)

  useEffect(() => {
    try {
      initializeAuth()
    } catch (error) {
      console.warn('[Providers] Auth initialization failed:', error)
    }
    setMounted(true)
  }, [initializeAuth])

  // Prevent hydration mismatch by waiting for client mount
  if (!mounted) {
    return null
  }

  return <>{children}</>
}
