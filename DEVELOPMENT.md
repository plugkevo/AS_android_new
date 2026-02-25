# Development Guide

## Quick Start

### Prerequisites
- Node.js 18+
- npm, yarn, pnpm, or bun
- Git
- Firebase project account

### Local Setup

```bash
# 1. Clone repository
git clone <repository-url>
cd african-shipping-web

# 2. Install dependencies
npm install

# 3. Create .env.local (copy from .env.example)
cp .env.example .env.local

# 4. Add your Firebase credentials to .env.local

# 5. Start development server
npm run dev

# 6. Open http://localhost:3000
```

## File Structure Overview

```
african-shipping-web/
├── app/                          # Next.js App Router
│   ├── api/                      # API routes
│   │   └── payments/
│   │       └── mpesa/           # M-Pesa payment endpoint
│   ├── auth/                     # Authentication
│   │   ├── login/               # Login page
│   │   └── signup/              # Sign up page
│   ├── dashboard/               # Dashboard page
│   ├── shipments/               # Shipments
│   │   ├── page.tsx             # List view
│   │   └── [id]/page.tsx        # Detail view
│   ├── loading-lists/           # Loading lists
│   ├── payments/                # Payments
│   ├── notifications/           # Notifications
│   ├── profile/                 # User profile
│   ├── search/                  # Global search
│   ├── layout.tsx               # Root layout
│   └── page.tsx                 # Redirect to dashboard
│
├── components/                   # React components
│   ├── auth/                    # Auth components
│   ├── dashboard/               # Dashboard components
│   ├── shipments/               # Shipment components
│   ├── loading-lists/           # Loading list components
│   ├── payments/                # Payment components
│   ├── notifications/           # Notification components
│   ├── profile/                 # Profile components
│   ├── navigation/              # Navigation components
│   ├── AppLayout.tsx            # Main layout wrapper
│   ├── ProtectedRoute.tsx       # Auth guard
│   └── providers.tsx            # App providers
│
├── lib/                         # Library/utility code
│   ├── firebase.ts              # Firebase config
│   ├── utils.ts                 # Utility functions
│   └── services/                # Business logic
│       ├── authService.ts       # Auth operations
│       ├── shipmentService.ts   # Shipment CRUD
│       ├── loadingListService.ts # Loading list operations
│       ├── paymentService.ts    # Payment operations
│       └── notificationService.ts # Notification operations
│
├── store/                       # Zustand stores
│   ├── authStore.ts             # Auth state
│   ├── shipmentStore.ts         # Shipment state
│   └── uiStore.ts               # UI state (theme, language)
│
├── hooks/                       # Custom React hooks
│   ├── useShipments.ts          # Shipment data fetching
│   ├── useNotifications.ts      # Notifications data fetching
│   └── useLoadingLists.ts       # Loading lists data fetching
│
├── styles/
│   └── globals.css              # Global styles
│
├── public/                      # Static assets
│
├── package.json                 # Dependencies
├── tsconfig.json                # TypeScript config
├── tailwind.config.ts           # Tailwind CSS config
├── next.config.mjs              # Next.js config
├── postcss.config.js            # PostCSS config
├── .eslintrc.json               # ESLint config
├── .gitignore                   # Git ignore rules
├── .env.example                 # Environment template
├── README.md                    # Project documentation
├── DEPLOYMENT.md                # Deployment guide
└── DEVELOPMENT.md               # This file
```

## Development Workflow

### Starting Development
```bash
npm run dev
```
App runs at `http://localhost:3000` with HMR enabled.

### Building for Production
```bash
npm run build
npm start
```

### Linting
```bash
npm run lint
```

## Key Technologies

### Core Framework
- **Next.js 16**: React framework with App Router
- **React 19**: UI library
- **TypeScript**: Type safety

### Styling & UI
- **Tailwind CSS**: Utility-first CSS
- **shadcn/ui**: Pre-built components
- **CSS Variables**: Theme system

### State Management
- **Zustand**: Lightweight state management
- **React Context**: Provider pattern for auth

### Backend/Data
- **Firebase Firestore**: NoSQL database
- **Firebase Auth**: Authentication
- **Firebase Storage**: File storage
- **Real-time Listeners**: Live data updates

### Development Tools
- **ESLint**: Code linting
- **TypeScript**: Type checking
- **Git**: Version control

## Development Patterns

### Creating a New Feature

1. **Create Service Layer** (`lib/services/`)
```typescript
export const myService = {
  async operation(data) {
    // Firebase operations
  }
}
```

2. **Create Custom Hook** (`hooks/`)
```typescript
export function useMyData() {
  const { user } = useAuthStore()
  const [data, setData] = useState([])
  
  useEffect(() => {
    if (!user) return
    myService.onSnapshot(user.uid, setData)
  }, [user])
  
  return { data }
}
```

3. **Create Components** (`components/`)
```typescript
export function MyComponent() {
  const { data } = useMyData()
  return <div>{/* Render data */}</div>
}
```

4. **Create Page** (`app/my-feature/page.tsx`)
```typescript
import { MyComponent } from '@/components/my-feature/MyComponent'

export default function Page() {
  return (
    <AppLayout>
      <MyComponent />
    </AppLayout>
  )
}
```

### Working with Firestore

#### Real-time Listener (Preferred)
```typescript
const unsubscribe = shipmentService.subscribeToShipments(userId, (data) => {
  setShipments(data)
})

// Cleanup
return () => unsubscribe()
```

#### One-time Query
```typescript
const data = await shipmentService.getShipments(userId)
```

#### Creating Documents
```typescript
await shipmentService.createShipment(userId, {
  name: 'Test Shipment',
  // ... other fields
})
```

#### Updating Documents
```typescript
await shipmentService.updateShipment(shipmentId, {
  status: 'Delivered'
})
```

### Working with Authentication

```typescript
import { useAuthStore } from '@/store/authStore'

function MyComponent() {
  const { user, isAuthenticated, logout } = useAuthStore()
  
  return (
    <>
      {isAuthenticated && <p>Welcome, {user?.displayName}</p>}
    </>
  )
}
```

## Component Guidelines

### Naming Conventions
- Components: PascalCase (`MyComponent.tsx`)
- Functions: camelCase (`myFunction()`)
- Constants: UPPER_SNAKE_CASE (`MY_CONSTANT`)

### Component Structure
```typescript
'use client'

import { useState } from 'react'
import { MyService } from '@/lib/services/MyService'

interface MyComponentProps {
  id: string
  onUpdate?: (data: any) => void
}

export function MyComponent({ id, onUpdate }: MyComponentProps) {
  const [state, setState] = useState('')
  
  return (
    <div className="space-y-4">
      {/* JSX */}
    </div>
  )
}
```

### Props & Types
```typescript
// ✅ Good: Specific types
interface ButtonProps {
  onClick: () => void
  label: string
  variant?: 'primary' | 'secondary'
}

// ❌ Avoid: Any types
interface ButtonProps {
  onClick: any
  label: any
}
```

## Styling Guidelines

### Using Tailwind
```typescript
// ✅ Good: Use Tailwind classes
<div className="p-4 rounded-lg bg-primary text-white">

// ❌ Avoid: Inline styles
<div style={{ padding: '16px', borderRadius: '8px' }}>
```

### Responsive Design
```typescript
// Mobile first, then add responsive prefixes
<div className="p-4 md:p-6 lg:p-8">
  <h1 className="text-xl md:text-2xl lg:text-3xl">
```

### Theme Variables
```typescript
// Use CSS variables defined in globals.css
<div className="bg-background text-foreground border-border">
```

## Testing

### Manual Testing Checklist
- [ ] Login/signup flows
- [ ] Create shipment
- [ ] View shipment details
- [ ] Update shipment
- [ ] Delete loading list
- [ ] Process payment
- [ ] View notifications
- [ ] Theme switching
- [ ] Mobile responsiveness

### Browser DevTools Tips
- React DevTools: Inspect component props
- Network tab: Check API calls
- Storage: View localStorage/cookies
- Console: Check for errors

## Debugging

### Using Console Logs
```typescript
console.log('[v0] Debug message:', variable)
```

### React DevTools
- Install React DevTools browser extension
- Inspect component tree
- Check props and state

### Firebase Emulator
```bash
firebase emulators:start
```

## Performance Tips

### Code Splitting
Next.js automatically code-splits at the route level.

### Image Optimization
```typescript
import Image from 'next/image'

<Image 
  src="/image.jpg" 
  alt="Description"
  width={400}
  height={300}
/>
```

### Memoization
```typescript
import { useMemo, useCallback } from 'react'

const memoizedValue = useMemo(() => expensiveComputation(), [dep])
const memoizedCallback = useCallback(() => { /* ... */ }, [dep])
```

## Common Tasks

### Adding a New Page
1. Create file: `app/my-page/page.tsx`
2. Wrap with `AppLayout`
3. Add to navigation if needed

### Adding a Service
1. Create file: `lib/services/myService.ts`
2. Export functions/methods
3. Use in components via hooks

### Adding State
1. Create store: `store/myStore.ts` using Zustand
2. Export hooks: `export const useMyStore = create(...)`
3. Use in components: `const { data } = useMyStore()`

### Styling a Component
1. Use Tailwind classes
2. Use CSS variables for colors
3. Use design tokens from globals.css

## Troubleshooting

### Hot Reload Not Working
```bash
rm -rf .next
npm run dev
```

### Firebase Connection Issues
- Check env variables
- Verify Firebase config
- Check browser console for errors
- Verify Firebase rules allow access

### Build Errors
- Check TypeScript errors: `npx tsc --noEmit`
- Clear cache: `rm -rf .next node_modules`
- Reinstall: `npm install`

### State Not Updating
- Ensure Zustand store is imported
- Check if state path is correct
- Verify component is using the hook

## Git Workflow

### Branch Naming
- Feature: `feature/shipment-tracking`
- Bug: `fix/login-error`
- Docs: `docs/update-readme`

### Commit Messages
```
feat: Add shipment tracking
fix: Resolve payment processing error
docs: Update installation guide
```

### PR Checklist
- [ ] Code follows project style
- [ ] All tests pass
- [ ] No console errors
- [ ] Updated documentation
- [ ] Screenshot of changes (if UI)

## Resources

- [Next.js Docs](https://nextjs.org/docs)
- [React Docs](https://react.dev)
- [Tailwind CSS Docs](https://tailwindcss.com/docs)
- [Firebase Docs](https://firebase.google.com/docs)
- [Zustand Docs](https://github.com/pmndrs/zustand)
- [TypeScript Docs](https://www.typescriptlang.org/docs)

## Getting Help

1. Check existing documentation
2. Search GitHub issues
3. Check console for error messages
4. Ask in team chat/Discord
5. Create detailed GitHub issue

## Code Review Checklist

- [ ] Code is readable and well-commented
- [ ] Types are properly defined
- [ ] Error handling is implemented
- [ ] Mobile responsive
- [ ] No console errors or warnings
- [ ] Follows project conventions
- [ ] Documentation updated
- [ ] Tests pass (if applicable)
