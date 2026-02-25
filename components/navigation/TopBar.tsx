'use client'

import { useRouter } from 'next/navigation'
import { useAuthStore } from '@/store/authStore'
import { useNotifications } from '@/hooks/useNotifications'

export function TopBar() {
  const router = useRouter()
  const { user, logout } = useAuthStore()
  const { unseenCount } = useNotifications()

  const handleLogout = async () => {
    await logout()
    router.push('/auth/login')
  }

  return (
    <header className="sticky top-0 z-40 border-b border-border bg-background">
      <div className="px-4 py-3 max-w-7xl mx-auto flex items-center justify-between">
        <div className="flex items-center gap-2">
          <h1 className="text-xl font-bold text-primary">AFS</h1>
          <span className="text-sm text-muted-foreground">African Shipping</span>
        </div>

        <div className="flex items-center gap-4">
          <button
            onClick={() => router.push('/notifications')}
            className="relative p-2 rounded-full hover:bg-secondary transition-colors"
            aria-label="Notifications"
          >
            <svg
              className="w-5 h-5 text-foreground"
              fill="none"
              stroke="currentColor"
              viewBox="0 0 24 24"
            >
              <path
                strokeLinecap="round"
                strokeLinejoin="round"
                strokeWidth={2}
                d="M15 17h5l-1.405-1.405A2.032 2.032 0 0118 14.158V11a6.002 6.002 0 00-4-5.659V5a2 2 0 10-4 0v.341C7.67 6.165 6 8.388 6 11v3.159c0 .538-.214 1.055-.595 1.436L4 17h5m6 0v1a3 3 0 11-6 0v-1m6 0H9"
              />
            </svg>
            {unseenCount > 0 && (
              <span className="absolute top-1 right-1 inline-flex items-center justify-center px-1.5 py-0.5 text-xs font-bold leading-none text-white transform translate-x-1/2 -translate-y-1/2 bg-destructive rounded-full">
                {unseenCount}
              </span>
            )}
          </button>

          <div className="hidden md:flex items-center gap-2">
            <div className="w-8 h-8 rounded-full bg-primary/20 flex items-center justify-center text-sm font-semibold text-primary">
              {user?.displayName?.[0]?.toUpperCase() || 'U'}
            </div>
            <span className="text-sm text-foreground">{user?.displayName}</span>
          </div>

          <button
            onClick={handleLogout}
            className="p-2 rounded-full hover:bg-secondary transition-colors text-foreground"
            aria-label="Logout"
          >
            <svg
              className="w-5 h-5"
              fill="none"
              stroke="currentColor"
              viewBox="0 0 24 24"
            >
              <path
                strokeLinecap="round"
                strokeLinejoin="round"
                strokeWidth={2}
                d="M17 16l4-4m0 0l-4-4m4 4H7m6 4v1a3 3 0 01-3 3H6a3 3 0 01-3-3V7a3 3 0 013-3h4a3 3 0 013 3v1"
              />
            </svg>
          </button>
        </div>
      </div>
    </header>
  )
}
