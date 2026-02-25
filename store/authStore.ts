import { create } from 'zustand'
import { AuthUser, authService } from '@/lib/services/authService'

interface AuthState {
  user: AuthUser | null
  isLoading: boolean
  isAuthenticated: boolean
  setUser: (user: AuthUser | null) => void
  setLoading: (loading: boolean) => void
  logout: () => Promise<void>
  initializeAuth: () => void
}

export const useAuthStore = create<AuthState>((set) => ({
  user: null,
  isLoading: true,
  isAuthenticated: false,

  setUser: (user: AuthUser | null) => {
    set({
      user,
      isAuthenticated: !!user,
      isLoading: false,
    })
  },

  setLoading: (loading: boolean) => {
    set({ isLoading: loading })
  },

  logout: async () => {
    await authService.logout()
    set({
      user: null,
      isAuthenticated: false,
    })
  },

  initializeAuth: () => {
    authService.onAuthStateChanged((user) => {
      set({
        user,
        isAuthenticated: !!user,
        isLoading: false,
      })
    })
  },
}))
