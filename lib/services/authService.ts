import {
  createUserWithEmailAndPassword,
  signInWithEmailAndPassword,
  signOut,
  onAuthStateChanged,
  User,
  updateProfile,
  GoogleAuthProvider,
  signInWithPopup,
} from 'firebase/auth'
import { auth } from '@/lib/firebase'

export interface AuthUser extends User {
  displayName?: string | null
  email?: string | null
  photoURL?: string | null
}

export const authService = {
  async signUp(email: string, password: string, displayName: string) {
    try {
      const userCredential = await createUserWithEmailAndPassword(auth, email, password)
      await updateProfile(userCredential.user, { displayName })
      return userCredential.user
    } catch (error) {
      console.error('[Auth] Sign up failed:', error)
      throw error
    }
  },

  async signIn(email: string, password: string) {
    try {
      const userCredential = await signInWithEmailAndPassword(auth, email, password)
      return userCredential.user
    } catch (error) {
      console.error('[Auth] Sign in failed:', error)
      throw error
    }
  },

  async signInWithGoogle() {
    try {
      const provider = new GoogleAuthProvider()
      const result = await signInWithPopup(auth, provider)
      return result.user
    } catch (error) {
      console.error('[Auth] Google sign in failed:', error)
      throw error
    }
  },

  async logout() {
    try {
      await signOut(auth)
    } catch (error) {
      console.error('[Auth] Logout failed:', error)
      throw error
    }
  },

  onAuthStateChanged(callback: (user: AuthUser | null) => void) {
    try {
      return onAuthStateChanged(auth, (user) => {
        callback(user as AuthUser | null)
      })
    } catch (error) {
      console.error('[Auth] State change listener failed:', error)
      callback(null)
    }
  },

  getCurrentUser(): AuthUser | null {
    if (!auth) return null
    return auth.currentUser as AuthUser | null
  },

  async updateUserProfile(displayName: string, photoURL?: string) {
    try {
      if (auth && auth.currentUser) {
        await updateProfile(auth.currentUser, {
          displayName,
          photoURL,
        })
      }
    } catch (error) {
      console.error('[Auth] Update profile failed:', error)
      throw error
    }
  },
}
