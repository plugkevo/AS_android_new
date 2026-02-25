'use client'

import Link from 'next/link'
import { usePathname } from 'next/navigation'

const navItems = [
  { href: '/dashboard', label: 'Home', icon: 'home' },
  { href: '/shipments', label: 'Shipments', icon: 'box' },
  { href: '/loading-lists', label: 'Loading', icon: 'list' },
  { href: '/payments', label: 'Payments', icon: 'credit' },
  { href: '/profile', label: 'Profile', icon: 'user' },
]

export function BottomNavigation() {
  const pathname = usePathname()

  const getIcon = (icon: string) => {
    switch (icon) {
      case 'home':
        return (
          <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M3 12l2-3m0 0l7-4 7 4M5 9v7a1 1 0 001 1h12a1 1 0 001-1V9m-9 4l4 4m0-11L9 9m11 0a9 9 0 11-18 0 9 9 0 0118 0z" />
          </svg>
        )
      case 'box':
        return (
          <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M20 13V6a2 2 0 00-2-2H6a2 2 0 00-2 2v7m16 0v5a2 2 0 01-2 2H6a2 2 0 01-2-2v-5m16 0h-2.586a1 1 0 00-.707.293l-2.414 2.414a1 1 0 01-.707.293h-3.172a1 1 0 01-.707-.293l-2.414-2.414A1 1 0 006.586 13H4" />
          </svg>
        )
      case 'list':
        return (
          <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 5H7a2 2 0 00-2 2v12a2 2 0 002 2h10a2 2 0 002-2V7a2 2 0 00-2-2h-2M9 5a2 2 0 002 2h2a2 2 0 002-2M9 5a2 2 0 012-2h2a2 2 0 012 2" />
          </svg>
        )
      case 'credit':
        return (
          <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M3 10h18M7 15h4m4 0h4M5 21h14a2 2 0 002-2V5a2 2 0 00-2-2H5a2 2 0 00-2 2v14a2 2 0 002 2z" />
          </svg>
        )
      case 'user':
        return (
          <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M16 7a4 4 0 11-8 0 4 4 0 018 0zM12 14a7 7 0 00-7 7h14a7 7 0 00-7-7z" />
          </svg>
        )
      default:
        return null
    }
  }

  return (
    <nav className="fixed bottom-0 left-0 right-0 md:hidden border-t border-border bg-background">
      <div className="flex items-center justify-around">
        {navItems.map((item) => {
          const isActive = pathname?.startsWith(item.href)
          return (
            <Link
              key={item.href}
              href={item.href}
              className={`flex flex-col items-center justify-center py-3 px-4 text-xs font-medium transition-colors ${
                isActive
                  ? 'text-primary border-t-2 border-primary'
                  : 'text-muted-foreground hover:text-foreground'
              }`}
            >
              {getIcon(item.icon)}
              <span className="mt-1">{item.label}</span>
            </Link>
          )
        })}
      </div>
    </nav>
  )
}
