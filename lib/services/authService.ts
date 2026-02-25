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
import { auth } from './firebase'

export interface AuthUser extends User {
  displayName?: string | null
  email?: string | null
  photoURL?: string | null
}

export const authService = {
  async signUp(email: string, password: string, displayName: string) {
    const userCredential = await createUserWithEmailAndPassword(auth, email, password)
    await updateProfile(userCredential.user, { displayName })
    return userCredential.user
  },

  async signIn(email: string, password: string) {
    const userCredential = await signInWithEmailAndPassword(auth, email, password)
    return userCredential.user
  },

  async signInWithGoogle() {
    const provider = new GoogleAuthProvider()
    const result = await signInWithPopup(auth, provider)
    return result.user
  },

  async logout() {
    await signOut(auth)
  },

  onAuthStateChanged(callback: (user: AuthUser | null) => void) {
    return onAuthStateChanged(auth, (user) => {
      callback(user as AuthUser | null)
    })
  },

  getCurrentUser(): AuthUser | null {
    return auth.currentUser as AuthUser | null
  },

  async updateUserProfile(displayName: string, photoURL?: string) {
    if (auth.currentUser) {
      await updateProfile(auth.currentUser, {
        displayName,
        photoURL,
      })
    }
  },
}
