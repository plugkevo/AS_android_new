/**
 * Common utility functions for the application
 */

export const formatDate = (date: string | number): string => {
  if (typeof date === 'string') {
    return new Date(date).toLocaleDateString('en-US', {
      year: 'numeric',
      month: 'short',
      day: 'numeric',
    })
  }
  return new Date(date).toLocaleDateString('en-US', {
    year: 'numeric',
    month: 'short',
    day: 'numeric',
  })
}

export const formatDateTime = (date: string | number): string => {
  if (typeof date === 'string') {
    return new Date(date).toLocaleString('en-US', {
      year: 'numeric',
      month: 'short',
      day: 'numeric',
      hour: '2-digit',
      minute: '2-digit',
    })
  }
  return new Date(date).toLocaleString('en-US', {
    year: 'numeric',
    month: 'short',
    day: 'numeric',
    hour: '2-digit',
    minute: '2-digit',
  })
}

export const formatCurrency = (amount: number, currency: string = 'KES'): string => {
  return new Intl.NumberFormat('en-US', {
    style: 'currency',
    currency,
  }).format(amount)
}

export const formatWeight = (weight: number): string => {
  if (weight < 1000) {
    return `${weight}kg`
  }
  return `${(weight / 1000).toFixed(2)}t`
}

export const truncateText = (text: string, maxLength: number): string => {
  if (text.length <= maxLength) return text
  return `${text.substring(0, maxLength)}...`
}

export const cn = (...classes: (string | undefined | boolean)[]): string => {
  return classes.filter(Boolean).join(' ')
}

export const isValidEmail = (email: string): boolean => {
  const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/
  return emailRegex.test(email)
}

export const isValidPhoneNumber = (phone: string): boolean => {
  // Basic validation for Kenyan phone numbers
  const phoneRegex = /^254[0-9]{9}$/
  return phoneRegex.test(phone.replace(/\D/g, ''))
}

export const generateId = (): string => {
  return `${Date.now()}_${Math.random().toString(36).substr(2, 9)}`
}

export const getInitials = (name: string): string => {
  return name
    .split(' ')
    .map((part) => part[0])
    .join('')
    .toUpperCase()
    .substring(0, 2)
}

export const calculateDaysSince = (date: string | number): number => {
  const targetDate = typeof date === 'string' ? new Date(date) : new Date(date)
  const now = new Date()
  const diffTime = Math.abs(now.getTime() - targetDate.getTime())
  return Math.ceil(diffTime / (1000 * 60 * 60 * 24))
}

export const sleep = (ms: number): Promise<void> => {
  return new Promise((resolve) => setTimeout(resolve, ms))
}
