# Vercel Deployment Guide for African Shipping Web

This guide will help you deploy the African Shipping web application to Vercel in minutes.

## Prerequisites

- GitHub account
- Vercel account (free at vercel.com)
- Firebase project credentials
- Node.js 18+ (for local testing)

## Step 1: Create New GitHub Repository

1. Go to https://github.com/new
2. Repository name: `AS_web` (or your preferred name)
3. Description: `African Shipping Web Application`
4. Choose visibility (Public or Private)
5. **Do NOT** initialize with README, .gitignore, or license
6. Click "Create repository"

## Step 2: Push Web Code to GitHub

In your terminal, run:

```bash
cd /path/to/web-project

# Initialize git
git init

# Add remote (replace with your repo URL)
git remote add origin https://github.com/YOUR_USERNAME/AS_web.git

# Create main branch and commit
git branch -M main
git add .
git commit -m "Initial commit: African Shipping web application"

# Push to GitHub
git push -u origin main
```

Verify all files are pushed by visiting your GitHub repository.

## Step 3: Connect to Vercel

### Option A: Vercel Dashboard (Recommended)

1. Go to https://vercel.com/dashboard
2. Click "Add New" → "Project"
3. Select "Import Git Repository"
4. Authenticate with GitHub if needed
5. Find and click on your `AS_web` repository
6. Click "Import"

### Option B: Vercel CLI

```bash
# Install Vercel CLI
npm i -g vercel

# Deploy from project directory
vercel

# Follow the prompts to connect your GitHub repo
```

## Step 4: Configure Environment Variables

After clicking "Import" in Vercel dashboard:

1. You'll see "Configure Project" screen
2. Scroll down to "Environment Variables"
3. Add all variables from `.env.example`:

### Required Firebase Variables

| Name | Value |
|------|-------|
| `NEXT_PUBLIC_FIREBASE_API_KEY` | From Firebase Console |
| `NEXT_PUBLIC_FIREBASE_AUTH_DOMAIN` | `your-project.firebaseapp.com` |
| `NEXT_PUBLIC_FIREBASE_PROJECT_ID` | From Firebase Console |
| `NEXT_PUBLIC_FIREBASE_STORAGE_BUCKET` | `your-project.appspot.com` |
| `NEXT_PUBLIC_FIREBASE_MESSAGING_SENDER_ID` | From Firebase Console |
| `NEXT_PUBLIC_FIREBASE_APP_ID` | From Firebase Console |

### Optional: M-Pesa Variables

| Name | Value |
|------|-------|
| `MPESA_CONSUMER_KEY` | From Safaricom Developer |
| `MPESA_CONSUMER_SECRET` | From Safaricom Developer |
| `MPESA_PASSKEY` | Your M-Pesa passkey |
| `MPESA_BUSINESS_SHORT_CODE` | Your business short code |
| `MPESA_CALLBACK_URL` | Your callback URL |

### Optional: Google Maps API Key

| Name | Value |
|------|-------|
| `NEXT_PUBLIC_GOOGLE_MAPS_API_KEY` | From Google Cloud Console |

## Step 5: Deploy

1. After adding environment variables, click "Deploy"
2. Vercel will start building your project
3. Wait 2-5 minutes for build to complete
4. You'll see a success page with your live URL

**Your app is now live!** 🎉

## Step 6: Configure Firebase Security

Complete these final setup steps:

### Add Vercel Domain to Firebase

1. Go to Firebase Console → Your Project
2. Click on Authentication
3. Go to "Settings" tab
4. Scroll down to "Authorized Domains"
5. Click "Add Domain"
6. Add your Vercel URL: `your-app.vercel.app`
7. Click "Save"

### Enable Google Sign-In (if using)

1. In Firebase Console → Authentication
2. Click on "Google" provider
3. Make sure it's "Enabled"
4. Add your Vercel URL to authorized redirect URIs

### Update Firestore Security Rules

1. Go to Firestore Database → Rules
2. Replace default rules with these:

```javascript
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {
    
    // Shipments collection
    match /shipments/{doc=**} {
      allow read: if request.auth != null;
      allow create: if request.auth != null && 
                       request.resource.data.userId == request.auth.uid;
      allow update, delete: if request.auth != null &&
                              resource.data.userId == request.auth.uid;
    }

    // User profiles
    match /users/{userId} {
      allow read: if request.auth != null;
      allow write: if request.auth != null && 
                      request.auth.uid == userId;
    }

    // Notifications
    match /notifications/{doc=**} {
      allow read: if request.auth != null;
      allow write: if request.auth != null;
    }

    // Loading lists
    match /loadingLists/{doc=**} {
      allow read: if request.auth != null;
      allow create: if request.auth != null &&
                       request.resource.data.userId == request.auth.uid;
      allow update, delete: if request.auth != null &&
                              resource.data.userId == request.auth.uid;
    }

    // Payments
    match /payments/{doc=**} {
      allow read: if request.auth != null;
      allow create: if request.auth != null &&
                       request.resource.data.userId == request.auth.uid;
      allow update: if request.auth != null &&
                       resource.data.userId == request.auth.uid;
    }
  }
}
```

3. Click "Publish"

## Step 7: Test Your Deployment

After everything is live, test:

1. Go to your Vercel URL
2. Sign up with email
3. Create a shipment
4. Check dashboard stats
5. Try Google Sign-In
6. Create loading lists
7. Check notifications

## Troubleshooting

### Build Failures

**Error: "Firebase config is invalid"**
- Check all Firebase environment variables are set correctly
- Make sure keys don't have extra spaces
- Verify they match your Firebase project

**Error: "Cannot find module 'firebase'"**
- Check `package.json` includes firebase dependencies
- Run `npm install` locally to verify

### Runtime Issues

**Blank page or "Firebase Error"**
- Check browser console (F12) for errors
- Verify Firebase domain is authorized
- Check network tab for API calls

**Authentication not working**
- Verify Firebase credentials
- Check authorized domains in Firebase
- Clear browser cache and try again

### Performance

**Slow loading**
- Images may need optimization
- Check Firestore query performance
- Consider adding caching headers in `vercel.json`

## Continuous Deployment

Every time you push to GitHub:

```bash
git add .
git commit -m "Your message"
git push origin main
```

Vercel automatically detects changes and redeploys. You'll see the deployment in your Vercel dashboard.

## Custom Domain (Optional)

1. In Vercel dashboard → Project Settings
2. Go to "Domains"
3. Add your custom domain
4. Follow DNS configuration instructions

## Rollback to Previous Deployment

If something goes wrong:

1. Go to Vercel dashboard
2. Click your project
3. Go to "Deployments"
4. Find the previous successful deployment
5. Click the three dots → "Promote to Production"

## Getting Help

- **Vercel Support**: https://vercel.com/help
- **Firebase Support**: https://firebase.google.com/support
- **Next.js Docs**: https://nextjs.org/docs

## Next Steps

After successful deployment:

1. Share the URL with team members
2. Set up monitoring (Analytics in Vercel dashboard)
3. Enable automatic deployments on GitHub push
4. Consider adding a custom domain
5. Monitor performance and optimize as needed

---

**Deployment complete!** Your African Shipping web app is now live and accessible to users worldwide. 🚀
