'use client'

import { useState } from 'react'
import { useLoadingLists } from '@/hooks/useLoadingLists'
import { loadingListService } from '@/lib/services/loadingListService'
import { useAuthStore } from '@/store/authStore'
import { useRouter } from 'next/navigation'

export function LoadingListManagement() {
  const router = useRouter()
  const { user } = useAuthStore()
  const { loadingLists, isLoading } = useLoadingLists()
  const [isFormOpen, setIsFormOpen] = useState(false)
  const [isCreating, setIsCreating] = useState(false)
  const [error, setError] = useState('')

  const [formData, setFormData] = useState({
    name: '',
    origin: '',
    destination: '',
    extraDetails: '',
    status: 'Active' as const,
  })

  const handleCreate = async (e: React.FormEvent) => {
    e.preventDefault()
    if (!user) return

    setError('')
    setIsCreating(true)

    try {
      await loadingListService.createLoadingList(user.uid, formData)
      setFormData({
        name: '',
        origin: '',
        destination: '',
        extraDetails: '',
        status: 'Active',
      })
      setIsFormOpen(false)
    } catch (err: any) {
      setError(err.message || 'Failed to create loading list')
    } finally {
      setIsCreating(false)
    }
  }

  const handleDelete = async (id: string) => {
    if (confirm('Are you sure you want to delete this loading list?')) {
      try {
        await loadingListService.deleteLoadingList(id)
      } catch (err) {
        console.error('Failed to delete:', err)
      }
    }
  }

  const statusColors: Record<string, string> = {
    'Active': 'bg-blue-100 text-blue-800',
    'Processing': 'bg-orange-100 text-orange-800',
    'Completed': 'bg-green-100 text-green-800',
  }

  return (
    <div className="p-4 md:p-6 space-y-6">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-3xl font-bold text-foreground">Loading Lists</h1>
          <p className="text-muted-foreground mt-1">Manage goods loading lists</p>
        </div>
        <button
          onClick={() => setIsFormOpen(true)}
          className="px-4 py-2 bg-primary text-primary-foreground rounded-md hover:bg-primary/90 transition-colors font-medium"
        >
          New List
        </button>
      </div>

      {/* Form Dialog */}
      {isFormOpen && (
        <div className="fixed inset-0 bg-black/50 flex items-center justify-center p-4 z-50">
          <div className="bg-background rounded-lg border border-border max-w-md w-full">
            <div className="border-b border-border p-4 flex items-center justify-between">
              <h2 className="text-lg font-semibold text-foreground">New Loading List</h2>
              <button
                onClick={() => setIsFormOpen(false)}
                className="text-muted-foreground hover:text-foreground"
              >
                ✕
              </button>
            </div>

            <form onSubmit={handleCreate} className="p-4 space-y-4">
              {error && (
                <div className="rounded-md bg-destructive/10 p-3 text-sm text-destructive">
                  {error}
                </div>
              )}

              <div className="space-y-2">
                <label className="text-sm font-medium text-foreground">List Name*</label>
                <input
                  type="text"
                  value={formData.name}
                  onChange={(e) => setFormData({ ...formData, name: e.target.value })}
                  required
                  className="w-full px-3 py-2 border border-input rounded-md bg-background text-foreground focus:outline-none focus:ring-2 focus:ring-primary"
                  placeholder="e.g., Electronics Load"
                />
              </div>

              <div className="grid grid-cols-2 gap-4">
                <div className="space-y-2">
                  <label className="text-sm font-medium text-foreground">Origin*</label>
                  <input
                    type="text"
                    value={formData.origin}
                    onChange={(e) => setFormData({ ...formData, origin: e.target.value })}
                    required
                    className="w-full px-3 py-2 border border-input rounded-md bg-background text-foreground focus:outline-none focus:ring-2 focus:ring-primary"
                    placeholder="Location"
                  />
                </div>
                <div className="space-y-2">
                  <label className="text-sm font-medium text-foreground">Destination*</label>
                  <input
                    type="text"
                    value={formData.destination}
                    onChange={(e) => setFormData({ ...formData, destination: e.target.value })}
                    required
                    className="w-full px-3 py-2 border border-input rounded-md bg-background text-foreground focus:outline-none focus:ring-2 focus:ring-primary"
                    placeholder="Location"
                  />
                </div>
              </div>

              <div className="space-y-2">
                <label className="text-sm font-medium text-foreground">Status*</label>
                <select
                  value={formData.status}
                  onChange={(e) => setFormData({ ...formData, status: e.target.value as any })}
                  className="w-full px-3 py-2 border border-input rounded-md bg-background text-foreground focus:outline-none focus:ring-2 focus:ring-primary"
                >
                  <option value="Active">Active</option>
                  <option value="Processing">Processing</option>
                  <option value="Completed">Completed</option>
                </select>
              </div>

              <div className="space-y-2">
                <label className="text-sm font-medium text-foreground">Details</label>
                <textarea
                  value={formData.extraDetails}
                  onChange={(e) => setFormData({ ...formData, extraDetails: e.target.value })}
                  className="w-full px-3 py-2 border border-input rounded-md bg-background text-foreground focus:outline-none focus:ring-2 focus:ring-primary resize-none"
                  placeholder="Additional details"
                  rows={3}
                />
              </div>

              <div className="flex gap-3 pt-4">
                <button
                  type="button"
                  onClick={() => setIsFormOpen(false)}
                  className="flex-1 px-4 py-2 border border-input rounded-md hover:bg-secondary transition-colors font-medium"
                >
                  Cancel
                </button>
                <button
                  type="submit"
                  disabled={isCreating}
                  className="flex-1 px-4 py-2 bg-primary text-primary-foreground rounded-md hover:bg-primary/90 disabled:opacity-50 transition-colors font-medium"
                >
                  {isCreating ? 'Creating...' : 'Create'}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* Loading State */}
      {isLoading ? (
        <div className="flex justify-center items-center py-12">
          <div className="w-8 h-8 border-4 border-primary border-t-transparent rounded-full animate-spin"></div>
        </div>
      ) : loadingLists.length === 0 ? (
        <div className="rounded-lg border border-border bg-card p-8 text-center">
          <p className="text-muted-foreground">No loading lists yet</p>
          <button
            onClick={() => setIsFormOpen(true)}
            className="mt-4 px-4 py-2 bg-primary text-primary-foreground rounded-md hover:bg-primary/90 transition-colors"
          >
            Create Your First List
          </button>
        </div>
      ) : (
        <div className="space-y-3">
          {loadingLists.map((list) => (
            <div
              key={list.id}
              className="p-4 rounded-lg border border-border bg-card hover:shadow-md transition-all"
            >
              <div className="flex items-start justify-between gap-4">
                <div className="flex-1 min-w-0">
                  <h3 className="font-semibold text-foreground">{list.name}</h3>
                  <p className="text-sm text-muted-foreground mt-1">
                    {list.origin} → {list.destination}
                  </p>
                  {list.extraDetails && (
                    <p className="text-xs text-muted-foreground mt-2">{list.extraDetails}</p>
                  )}
                </div>
                <div className="flex items-center gap-2">
                  <span className={`px-3 py-1 rounded-full text-xs font-medium ${statusColors[list.status]}`}>
                    {list.status}
                  </span>
                  <button
                    onClick={() => handleDelete(list.id)}
                    className="p-2 text-destructive hover:bg-destructive/10 rounded-md transition-colors"
                  >
                    Delete
                  </button>
                </div>
              </div>
            </div>
          ))}
        </div>
      )}
    </div>
  )
}
