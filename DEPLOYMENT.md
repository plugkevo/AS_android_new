# Deployment Guide

## Pre-Deployment Checklist

- [ ] All Firebase environment variables configured
- [ ] Firebase Firestore security rules configured
- [ ] Firebase Auth enabled (Email/Password + Google)
- [ ] Firebase Storage configured
- [ ] M-Pesa API keys obtained (if using payments)
- [ ] Google Maps API key obtained (if using maps)
- [ ] All tests passing
- [ ] No console errors in development

## Environment Variables Required

```
# Required (Firebase)
NEXT_PUBLIC_FIREBASE_API_KEY
NEXT_PUBLIC_FIREBASE_AUTH_DOMAIN
NEXT_PUBLIC_FIREBASE_PROJECT_ID
NEXT_PUBLIC_FIREBASE_STORAGE_BUCKET
NEXT_PUBLIC_FIREBASE_MESSAGING_SENDER_ID
NEXT_PUBLIC_FIREBASE_APP_ID

# Optional (For advanced features)
NEXT_PUBLIC_GOOGLE_MAPS_API_KEY
MPESA_CONSUMER_KEY
MPESA_CONSUMER_SECRET
MPESA_PASSKEY
MPESA_BUSINESS_SHORT_CODE
MPESA_CALLBACK_URL
```

## Vercel Deployment (Recommended)

### Step 1: Connect Repository
1. Push code to GitHub repository
2. Go to [vercel.com](https://vercel.com)
3. Click "New Project"
4. Select your GitHub repository
5. Click "Import"

### Step 2: Configure Environment
1. In Vercel dashboard, go to Settings → Environment Variables
2. Add all required Firebase credentials
3. Add optional variables (M-Pesa, Maps API keys)

### Step 3: Deploy
1. Click "Deploy"
2. Wait for build to complete
3. Your app will be live at `your-project.vercel.app`

### Step 4: Configure Firebase
1. Go to Firebase Console → Settings → Authorized domains
2. Add your Vercel domain to authorized list
3. Update Google OAuth redirect URIs with Vercel domain

## Firebase Security Rules

### Firestore Rules
```javascript
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {
    // Users can only access their own data
    match /shipments/{shipmentId} {
      allow read, write: if request.auth.uid == resource.data.userId;
      allow create: if request.auth.uid == request.resource.data.userId;
    }
    
    match /loadingLists/{listId} {
      allow read, write: if request.auth.uid == resource.data.userId;
      allow create: if request.auth.uid == request.resource.data.userId;
    }
    
    match /payments/{paymentId} {
      allow read, write: if request.auth.uid == resource.data.userId;
      allow create: if request.auth.uid == request.resource.data.userId;
    }
    
    match /notifications/{notificationId} {
      allow read, write: if request.auth.uid == resource.data.userId;
      allow create: if request.auth.uid == request.resource.data.userId;
    }
  }
}
```

### Storage Rules
```javascript
rules_version = '2';
service firebase.storage {
  match /b/{bucket}/o {
    match /profile-pictures/{uid}/{allPaths=**} {
      allow read: if request.auth.uid == uid;
      allow write: if request.auth.uid == uid && request.resource.size < 5 * 1024 * 1024;
    }
  }
}
```

## Alternative Hosting Options

### Netlify
1. Connect GitHub repository
2. Build command: `npm run build`
3. Publish directory: `.next`
4. Add environment variables in Site settings
5. Deploy

### AWS Amplify
1. Connect GitHub repository
2. Configure build settings (Next.js preset available)
3. Add environment variables
4. Deploy

### Docker (Any VPS/Cloud Platform)
```dockerfile
FROM node:18-alpine
WORKDIR /app
COPY package*.json ./
RUN npm install
COPY . .
RUN npm run build
EXPOSE 3000
CMD ["npm", "start"]
```

## Performance Optimization Tips

1. **Enable Firestore Index Creation**: Let Firebase auto-create indexes
2. **Use CDN**: Vercel includes CDN automatically
3. **Monitor Bundle Size**: `npm run build` shows bundle analysis
4. **Enable Image Optimization**: Already configured in next.config.mjs

## Monitoring & Maintenance

### Firebase Console Monitoring
- Monitor Firestore read/write usage
- Check Auth sign-up success rates
- Review Storage bandwidth usage

### Vercel Analytics
- View deployment history
- Monitor build times
- Check error rates

### Logging
- Check browser console for client errors
- Monitor Firebase logs in console
- Set up error tracking (Sentry, etc.)

## Common Issues & Solutions

### "Firebase is not initialized"
- Verify all env vars are set
- Restart development server
- Check .env.local file

### "CORS errors"
- Add domain to Firebase authorized domains
- Check Google Maps API restrictions

### "Firestore permission denied"
- Review and update security rules
- Verify user is authenticated
- Check userId matches in rules

### "Build fails"
- Check Node.js version compatibility
- Clear `.next` folder and reinstall
- Verify all environment variables are set

## Rollback Procedure

### Vercel
1. Go to Deployments tab
2. Find previous stable deployment
3. Click "Promote to Production"

### GitHub
1. Revert to previous commit
2. Push to main branch
3. Vercel will auto-deploy previous version

## Monitoring Checklist

- [ ] Daily: Check error logs
- [ ] Weekly: Review analytics
- [ ] Monthly: Update dependencies
- [ ] Quarterly: Security audit
- [ ] Annually: Performance review

## Support & Contact

For deployment issues:
- Check Vercel documentation: https://vercel.com/docs
- Firebase support: https://firebase.google.com/support
- Open GitHub issue for bugs
