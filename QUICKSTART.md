# Quick Start Guide

Get the African Shipping Web App running in 5 minutes!

## Prerequisites

- Node.js 18 or higher
- npm or yarn
- Firebase project (same as your Android app)
- Git

## Step 1: Clone & Install (2 min)

```bash
# Clone the repository
git clone <your-repo-url>
cd african-shipping-web

# Install dependencies
npm install
```

## Step 2: Configure Firebase (1 min)

```bash
# Create environment file
cp .env.example .env.local
```

Edit `.env.local` and add your Firebase credentials:

```
NEXT_PUBLIC_FIREBASE_API_KEY=your_key_here
NEXT_PUBLIC_FIREBASE_AUTH_DOMAIN=your_auth_domain
NEXT_PUBLIC_FIREBASE_PROJECT_ID=your_project_id
NEXT_PUBLIC_FIREBASE_STORAGE_BUCKET=your_storage_bucket
NEXT_PUBLIC_FIREBASE_MESSAGING_SENDER_ID=your_sender_id
NEXT_PUBLIC_FIREBASE_APP_ID=your_app_id
```

**How to find these values:**
1. Go to Firebase Console → Your Project → Settings → General
2. Scroll to "Your apps" section
3. Click Web app and copy the config values

## Step 3: Start Development Server (1 min)

```bash
npm run dev
```

Open [http://localhost:3000](http://localhost:3000) in your browser.

## Step 4: Test the App (1 min)

1. Click "Sign Up"
2. Create an account with any email/password
3. You should see the dashboard
4. Create a test shipment
5. View it in the list

## Default Test Account (Optional)

Create a test account:
- Email: `test@example.com`
- Password: `Test123!`

## Common Issues

### "Firebase is not initialized"
- Double-check `.env.local` values
- Restart dev server: `Ctrl+C` then `npm run dev`

### "Page not loading"
- Check browser console (F12) for errors
- Verify Firebase credentials are correct

### "Cannot create shipment"
- Make sure you're logged in
- Check browser console for error messages
- Verify Firebase Firestore is enabled

## Next Steps

1. **Explore Features**: Create shipments, payments, loading lists
2. **Customize**: Update colors in `styles/globals.css`
3. **Deploy**: Follow DEPLOYMENT.md for production
4. **Integrate**: Connect M-Pesa in `/app/api/payments/mpesa/route.ts`

## Useful Commands

```bash
# Development
npm run dev          # Start dev server

# Production
npm run build        # Build for production
npm start            # Start production server

# Code Quality
npm run lint         # Check code style

# Debugging
npm run dev -- --verbose  # Verbose logging
```

## File Structure Quick Look

```
app/              → Pages (routes)
components/       → React components
lib/              → Business logic & Firebase
store/            → State management
hooks/            → Custom React hooks
styles/           → Global styles
```

## Database Collections Created Automatically

When you first create a shipment, these collections will be created in Firestore:
- `shipments` - Your shipment data
- `loadingLists` - Your loading lists
- `payments` - Your payments
- `notifications` - Your notifications

## Mobile Testing

Open `http://localhost:3000` on your phone/tablet to test mobile view.

Or use Chrome DevTools (F12) → Toggle device toolbar (Ctrl+Shift+M).

## Stopping the Server

Press `Ctrl+C` in your terminal.

## Need Help?

1. Check **README.md** for detailed documentation
2. Check **DEVELOPMENT.md** for development patterns
3. Check **DEPLOYMENT.md** for deployment help
4. Look at existing components for examples

## What's Included

✅ Authentication (Email + Google Sign-In)
✅ Dashboard with statistics
✅ Shipment management (Create, Read, Update)
✅ Loading lists management
✅ Payment processing (M-Pesa ready)
✅ Real-time notifications
✅ User profile & settings
✅ Global search
✅ Responsive mobile design
✅ Theme support (Light/Dark/System)
✅ Multi-language ready (English/Swahili)

## Project Stats

- **51 Files** created
- **Real-time** database sync
- **TypeScript** for type safety
- **Firebase** backend
- **Tailwind CSS** styling
- **Zero config** deployment ready

## Build Time

Expected times:
- First build: 45-60 seconds
- Subsequent builds: 10-20 seconds
- Dev server startup: 5-10 seconds

---

**That's it! You're ready to start developing.** 🚀

For questions, check the documentation files or GitHub issues.
