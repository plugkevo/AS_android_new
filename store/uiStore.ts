import { create } from 'zustand'

interface UIState {
  theme: 'light' | 'dark' | 'system'
  language: 'en' | 'sw'
  sidebarOpen: boolean
  setTheme: (theme: 'light' | 'dark' | 'system') => void
  setLanguage: (language: 'en' | 'sw') => void
  toggleSidebar: () => void
  setSidebarOpen: (open: boolean) => void
}

export const useUIStore = create<UIState>((set) => ({
  theme: typeof window !== 'undefined' ? (localStorage.getItem('theme') as any) || 'system' : 'system',
  language: typeof window !== 'undefined' ? (localStorage.getItem('language') as any) || 'en' : 'en',
  sidebarOpen: true,

  setTheme: (theme) => {
    localStorage.setItem('theme', theme)
    set({ theme })
  },

  setLanguage: (language) => {
    localStorage.setItem('language', language)
    set({ language })
  },

  toggleSidebar: () => {
    set((state) => ({ sidebarOpen: !state.sidebarOpen }))
  },

  setSidebarOpen: (open) => {
    set({ sidebarOpen: open })
  },
}))
