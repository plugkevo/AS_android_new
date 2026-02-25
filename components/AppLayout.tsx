'use client'

import { TopBar } from '@/components/navigation/TopBar'
import { BottomNavigation } from '@/components/navigation/BottomNavigation'
import { ProtectedRoute } from '@/components/ProtectedRoute'

export function AppLayout({ children }: { children: React.ReactNode }) {
  return (
    <ProtectedRoute>
      <div className="flex flex-col min-h-screen bg-background">
        <TopBar />
        <main className="flex-1 overflow-y-auto pb-20 md:pb-0">
          <div className="max-w-7xl mx-auto">
            {children}
          </div>
        </main>
        <BottomNavigation />
      </div>
    </ProtectedRoute>
  )
}
