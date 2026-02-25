# African Shipping Web Application

A comprehensive web version of the African Shipping (AFS) logistics management platform, built with Next.js, React, and Firebase.

## Features

- **Authentication**: Email/Password and Google Sign-In via Firebase
- **Shipment Management**: Create, track, and manage shipments with real-time updates
- **Loading Lists**: Manage goods loading lists with detailed tracking
- **Payments**: M-Pesa payment integration for seamless transactions
- **Notifications**: Real-time notifications for shipment updates
- **User Profile**: Customizable user settings, theme selection, and language preferences
- **Dashboard**: Overview of shipments, statistics, and recent activities
- **Responsive Design**: Mobile-first design that works on all devices

## Tech Stack

- **Framework**: Next.js 16 with App Router
- **UI Components**: shadcn/ui
- **Styling**: Tailwind CSS
- **State Management**: Zustand
- **Database**: Firebase Firestore (shared with Android app)
- **Authentication**: Firebase Auth
- **File Storage**: Firebase Storage
- **Real-time Updates**: Firestore listeners

## Getting Started

### Prerequisites

- Node.js 18+ or newer
- Firebase project (use the same one as your Android app)

### Installation

1. **Clone the repository**
   ```bash
   git clone <repository-url>
   cd african-shipping-web
   ```

2. **Install dependencies**
   ```bash
   npm install
   # or
   yarn install
   # or
   pnpm install
   ```

3. **Set up environment variables**
   ```bash
   cp .env.example .env.local
   ```
   
   Fill in your Firebase configuration in `.env.local`:
   ```
   NEXT_PUBLIC_FIREBASE_API_KEY=...
   NEXT_PUBLIC_FIREBASE_AUTH_DOMAIN=...
   NEXT_PUBLIC_FIREBASE_PROJECT_ID=...
   NEXT_PUBLIC_FIREBASE_STORAGE_BUCKET=...
   NEXT_PUBLIC_FIREBASE_MESSAGING_SENDER_ID=...
   NEXT_PUBLIC_FIREBASE_APP_ID=...
   ```

4. **Run the development server**
   ```bash
   npm run dev
   ```

   Open [http://localhost:3000](http://localhost:3000) in your browser.

## Project Structure

```
app/
├── api/                    # API routes
│   └── payments/
│       └── mpesa/         # M-Pesa payment endpoint
├── auth/                  # Authentication pages
│   ├── login/
│   └── signup/
├── dashboard/             # Dashboard page
├── shipments/             # Shipments management
├── loading-lists/         # Loading lists management
├── payments/              # Payments page
├── notifications/         # Notifications page
├── profile/               # User profile page
└── layout.tsx             # Root layout

components/
├── auth/                  # Auth components
├── dashboard/             # Dashboard components
├── shipments/             # Shipment components
├── loading-lists/         # Loading list components
├── payments/              # Payment components
├── notifications/         # Notification components
├── profile/               # Profile components
├── navigation/            # Navigation components
├── providers.tsx          # App providers
├── AppLayout.tsx          # Main app layout
└── ProtectedRoute.tsx     # Auth protection wrapper

lib/
├── firebase.ts            # Firebase configuration
└── services/
    ├── authService.ts     # Authentication logic
    ├── shipmentService.ts # Shipment CRUD
    ├── loadingListService.ts # Loading list operations
    ├── paymentService.ts  # Payment operations
    └── notificationService.ts # Notification handling

store/
├── authStore.ts           # Auth state (Zustand)
├── shipmentStore.ts       # Shipment state
└── uiStore.ts             # UI state

hooks/
├── useShipments.ts        # Shipment data hook
├── useNotifications.ts    # Notifications hook
└── useLoadingLists.ts     # Loading lists hook
```

## Configuration

### Firebase Setup

1. Go to [Firebase Console](https://console.firebase.google.com/)
2. Create a new project or use your existing Android app's project
3. Enable these services:
   - **Authentication**: Enable Email/Password and Google Sign-In
   - **Firestore Database**: Create database in test mode (or configure rules)
   - **Storage**: Set up for profile pictures and file uploads
   - **Realtime Database** (Optional): For real-time updates

4. Get your Firebase config from Project Settings and add to `.env.local`

### M-Pesa Integration

To enable M-Pesa payments:

1. Register for Daraja API at [Safaricom Developer Portal](https://developer.safaricom.co.ke/)
2. Get your credentials (Consumer Key, Consumer Secret, etc.)
3. Add them to your environment variables
4. Implement the M-Pesa STK Push logic in `/app/api/payments/mpesa/route.ts`

## Usage

### Creating Shipments

1. Navigate to Shipments page
2. Click "Create Shipment"
3. Fill in shipment details (name, origin, destination, weight, etc.)
4. Submit to save

### Managing Loading Lists

1. Go to Loading Lists page
2. Click "New List" to create a loading list
3. Add goods details and manage the list

### Processing Payments

1. Navigate to Payments page
2. Click "New Payment"
3. Enter M-Pesa phone number and amount
4. Complete the transaction

### Viewing Notifications

1. Click the notification bell icon in the top bar
2. View all notifications or filter by type
3. Mark notifications as read

## Firestore Database Schema

### Shipments Collection
```typescript
{
  id: string
  userId: string
  name: string
  origin: string
  destination: string
  weight: number
  details: string
  status: 'Active' | 'In Transit' | 'Delivered' | 'Processing'
  date: string
  latitude?: number
  longitude?: number
  createdAt: number
  updatedAt: number
}
```

### Loading Lists Collection
```typescript
{
  id: string
  userId: string
  name: string
  origin: string
  destination: string
  extraDetails: string
  status: 'Active' | 'Processing' | 'Completed'
  createdAt: number
  updatedAt: number
}
```

### Payments Collection
```typescript
{
  id: string
  userId: string
  shipmentId?: string
  amount: number
  phoneNumber: string
  status: 'pending' | 'completed' | 'failed'
  paymentMethod: 'mPesa'
  transactionId?: string
  createdAt: number
  updatedAt: number
}
```

### Notifications Collection
```typescript
{
  id: string
  userId: string
  message: string
  timestamp: number
  seen: boolean
  type: 'shipment' | 'payment' | 'system'
  relatedId?: string
}
```

## Data Synchronization

The web app syncs data in real-time with the Android app through shared Firestore database. Any changes made on either platform are reflected across all connected clients.

## Authentication Flow

1. User signs up or logs in
2. Firebase Auth generates session token
3. User data persisted in Firestore
4. Browser localStorage maintains auth state
5. On page refresh, auth state is automatically restored

## Deployment

### Deploy to Vercel (Recommended)

```bash
npm install -g vercel
vercel
```

### Deploy to Other Platforms

The app can be deployed to any Node.js hosting service:
- Hercel
- Netlify
- AWS Amplify
- Google Cloud Run
- DigitalOcean

## Performance Optimization

- **Firestore Listeners**: Real-time updates without polling
- **Code Splitting**: Automatic with Next.js
- **Image Optimization**: Next.js Image component for profile pictures
- **Caching**: Browser caching for static assets

## Security

- **Firebase Security Rules**: Configure Firestore rules to restrict data access
- **HTTPS Only**: All communications encrypted
- **Input Validation**: Server-side validation for all inputs
- **XSS Protection**: React prevents XSS by default
- **CSRF Protection**: Next.js built-in CSRF protection

## Troubleshooting

### "Firebase is not initialized"
- Ensure all Firebase environment variables are set correctly
- Check that your `.env.local` file exists

### "Cannot read property 'uid' of null"
- User is not authenticated. Ensure they're logged in
- Check that ProtectedRoute is being used

### Real-time updates not working
- Verify Firestore rules allow read access
- Check browser console for errors
- Ensure userId matches in Firestore documents

## Support

For issues, bugs, or feature requests, please open an issue in the GitHub repository.

## License

MIT License - See LICENSE file for details

## Contributing

1. Fork the repository
2. Create a feature branch
3. Make your changes
4. Submit a pull request

## Roadmap

- [ ] Map integration for shipment tracking
- [ ] Advanced analytics dashboard
- [ ] Batch shipment creation
- [ ] Multi-language support improvements
- [ ] Offline mode support
- [ ] PWA support
- [ ] Video call integration for support

---

Built with ❤️ for African logistics
