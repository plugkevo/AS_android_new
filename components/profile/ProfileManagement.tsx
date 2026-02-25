'use client'

import { useState } from 'react'
import { useAuthStore } from '@/store/authStore'
import { useUIStore } from '@/store/uiStore'
import { authService } from '@/lib/services/authService'
import { useRouter } from 'next/navigation'
import { useShipments } from '@/hooks/useShipments'

export function ProfileManagement() {
  const router = useRouter()
  const { user, logout } = useAuthStore()
  const { theme, setTheme, language, setLanguage } = useUIStore()
  const { shipments } = useShipments()

  const [isEditingName, setIsEditingName] = useState(false)
  const [displayName, setDisplayName] = useState(user?.displayName || '')
  const [error, setError] = useState('')
  const [success, setSuccess] = useState('')

  const handleSaveName = async () => {
    setError('')
    setSuccess('')

    try {
      await authService.updateUserProfile(displayName)
      setSuccess('Profile updated successfully')
      setIsEditingName(false)
    } catch (err: any) {
      setError(err.message || 'Failed to update profile')
    }
  }

  const handleLogout = async () => {
    await logout()
    router.push('/auth/login')
  }

  const deliveredCount = shipments.filter(s => s.status === 'Delivered').length
  const activeCount = shipments.filter(s => s.status !== 'Delivered').length

  return (
    <div className="p-4 md:p-6 space-y-6 max-w-2xl mx-auto">
      <h1 className="text-3xl font-bold text-foreground">Profile Settings</h1>

      {/* Profile Card */}
      <div className="rounded-lg border border-border bg-card p-6 space-y-6">
        <div className="flex items-start gap-4">
          <div className="w-16 h-16 rounded-full bg-primary/20 flex items-center justify-center text-2xl font-bold text-primary">
            {user?.displayName?.[0]?.toUpperCase() || 'U'}
          </div>
          <div className="flex-1">
            <h2 className="text-2xl font-bold text-foreground">{user?.displayName}</h2>
            <p className="text-muted-foreground">{user?.email}</p>
            <p className="text-sm text-muted-foreground mt-2">
              Member since {user?.metadata?.creationTime ? new Date(user.metadata.creationTime).toLocaleDateString() : 'N/A'}
            </p>
          </div>
        </div>

        {/* Stats */}
        <div className="grid grid-cols-2 gap-4">
          <div className="p-4 rounded-lg bg-secondary/50">
            <p className="text-sm text-muted-foreground">Total Shipments</p>
            <p className="text-2xl font-bold text-foreground">{shipments.length}</p>
          </div>
          <div className="p-4 rounded-lg bg-secondary/50">
            <p className="text-sm text-muted-foreground">Delivered</p>
            <p className="text-2xl font-bold text-green-600">{deliveredCount}</p>
          </div>
        </div>
      </div>

      {/* Edit Profile */}
      <div className="rounded-lg border border-border bg-card p-6 space-y-4">
        <h3 className="text-lg font-semibold text-foreground">Edit Profile</h3>

        {error && (
          <div className="rounded-md bg-destructive/10 p-3 text-sm text-destructive">
            {error}
          </div>
        )}

        {success && (
          <div className="rounded-md bg-green-100 p-3 text-sm text-green-800">
            {success}
          </div>
        )}

        <div className="space-y-3">
          <label className="text-sm font-medium text-foreground">Display Name</label>
          <div className="flex gap-2">
            <input
              type="text"
              value={displayName}
              onChange={(e) => setDisplayName(e.target.value)}
              disabled={!isEditingName}
              className="flex-1 px-3 py-2 border border-input rounded-md bg-background text-foreground disabled:opacity-50 focus:outline-none focus:ring-2 focus:ring-primary"
            />
            {isEditingName ? (
              <>
                <button
                  onClick={handleSaveName}
                  className="px-4 py-2 bg-primary text-primary-foreground rounded-md hover:bg-primary/90 transition-colors"
                >
                  Save
                </button>
                <button
                  onClick={() => {
                    setIsEditingName(false)
                    setDisplayName(user?.displayName || '')
                  }}
                  className="px-4 py-2 border border-input rounded-md hover:bg-secondary transition-colors"
                >
                  Cancel
                </button>
              </>
            ) : (
              <button
                onClick={() => setIsEditingName(true)}
                className="px-4 py-2 border border-input rounded-md hover:bg-secondary transition-colors"
              >
                Edit
              </button>
            )}
          </div>
        </div>
      </div>

      {/* Settings */}
      <div className="rounded-lg border border-border bg-card p-6 space-y-4">
        <h3 className="text-lg font-semibold text-foreground">Settings</h3>

        <div className="space-y-4">
          <div>
            <label className="text-sm font-medium text-foreground">Theme</label>
            <select
              value={theme}
              onChange={(e) => setTheme(e.target.value as any)}
              className="w-full mt-2 px-3 py-2 border border-input rounded-md bg-background text-foreground focus:outline-none focus:ring-2 focus:ring-primary"
            >
              <option value="light">Light</option>
              <option value="dark">Dark</option>
              <option value="system">System</option>
            </select>
          </div>

          <div>
            <label className="text-sm font-medium text-foreground">Language</label>
            <select
              value={language}
              onChange={(e) => setLanguage(e.target.value as any)}
              className="w-full mt-2 px-3 py-2 border border-input rounded-md bg-background text-foreground focus:outline-none focus:ring-2 focus:ring-primary"
            >
              <option value="en">English</option>
              <option value="sw">Swahili</option>
            </select>
          </div>
        </div>
      </div>

      {/* Danger Zone */}
      <div className="rounded-lg border border-destructive/50 bg-destructive/5 p-6 space-y-4">
        <h3 className="text-lg font-semibold text-foreground">Danger Zone</h3>
        <button
          onClick={handleLogout}
          className="w-full px-4 py-2 bg-destructive text-destructive-foreground rounded-md hover:bg-destructive/90 transition-colors font-medium"
        >
          Logout
        </button>
      </div>
    </div>
  )
}
