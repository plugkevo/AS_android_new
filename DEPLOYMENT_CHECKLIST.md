# Vercel Deployment Checklist

Use this checklist to ensure a smooth deployment.

## Pre-Deployment Checklist

### Local Verification
- [ ] Run `npm install` locally
- [ ] Run `npm run build` - builds successfully with no errors
- [ ] Run `npm run dev` - starts without errors
- [ ] Test login/signup functionality locally
- [ ] Test creating a shipment
- [ ] All Firebase functions work

### GitHub Preparation
- [ ] Create new GitHub repository
- [ ] Get Firebase credentials ready
- [ ] `.env.example` is up to date with all required variables
- [ ] `.gitignore` is configured (already done)
- [ ] `vercel.json` is in place (already done)
- [ ] `.vercelignore` is in place (already done)

### Firebase Configuration
- [ ] Have Firebase Project ID ready
- [ ] Have API Key ready
- [ ] Have Auth Domain ready
- [ ] Have Storage Bucket ready
- [ ] Have Messaging Sender ID ready
- [ ] Have App ID ready

## Deployment Steps

### Step 1: Push to GitHub
- [ ] Git repository initialized
- [ ] All files added (`git add .`)
- [ ] Initial commit made (`git commit -m "..."`)
- [ ] Pushed to main branch (`git push origin main`)
- [ ] Verified files on GitHub

### Step 2: Connect to Vercel
- [ ] Logged in to https://vercel.com
- [ ] Clicked "Add New" → "Project"
- [ ] GitHub repository selected
- [ ] Project imported

### Step 3: Configure Environment Variables
- [ ] `NEXT_PUBLIC_FIREBASE_API_KEY` - added
- [ ] `NEXT_PUBLIC_FIREBASE_AUTH_DOMAIN` - added
- [ ] `NEXT_PUBLIC_FIREBASE_PROJECT_ID` - added
- [ ] `NEXT_PUBLIC_FIREBASE_STORAGE_BUCKET` - added
- [ ] `NEXT_PUBLIC_FIREBASE_MESSAGING_SENDER_ID` - added
- [ ] `NEXT_PUBLIC_FIREBASE_APP_ID` - added
- [ ] (Optional) M-Pesa variables - added
- [ ] (Optional) Google Maps API key - added

### Step 4: Deploy
- [ ] Clicked "Deploy" button
- [ ] Build started (visible in Vercel dashboard)
- [ ] Build completed successfully
- [ ] Deployment shows "Ready"
- [ ] Live URL generated

## Post-Deployment Steps

### Firebase Configuration
- [ ] Logged in to Firebase Console
- [ ] Added Vercel URL to Authorized Domains
- [ ] Updated Google Sign-In redirect URIs
- [ ] Updated Firestore Security Rules
- [ ] Published security rules

### Testing
- [ ] Opened live Vercel URL
- [ ] Page loads without errors
- [ ] Sign up functionality works
- [ ] Email verification sends correctly
- [ ] Login functionality works
- [ ] Google Sign-In works
- [ ] Dashboard displays correctly
- [ ] Can create shipment
- [ ] Can view shipments list
- [ ] Real-time updates work
- [ ] Mobile responsive layout works

### Monitoring
- [ ] Checked Vercel deployment logs for errors
- [ ] Checked Vercel Analytics dashboard
- [ ] Checked browser console for JavaScript errors
- [ ] Tested on mobile device
- [ ] Tested on tablet
- [ ] Tested on desktop

## Future Maintenance

### Regular Tasks
- [ ] Monitor Vercel Analytics monthly
- [ ] Check Firebase usage to avoid exceeding quota
- [ ] Review and update dependencies monthly
- [ ] Check deployment logs for errors weekly

### Performance Optimization (Optional)
- [ ] Enable Vercel Caching
- [ ] Optimize images (consider Next.js Image component)
- [ ] Enable Edge Function caching
- [ ] Set up monitoring alerts

### Security (Optional)
- [ ] Set up 2FA on GitHub account
- [ ] Set up 2FA on Vercel account
- [ ] Set up 2FA on Firebase account
- [ ] Review Firebase security rules quarterly
- [ ] Monitor for unauthorized API usage

## Rollback Plan

If deployment fails:
- [ ] Check Vercel build logs for errors
- [ ] Fix issues locally
- [ ] Push fix to GitHub
- [ ] Vercel automatically redeploys
- [ ] If emergency: rollback to previous deployment in Vercel dashboard

## URLs and References

### Your Deployment
- Vercel Dashboard: https://vercel.com/dashboard
- Your App URL: `https://your-app.vercel.app` (you'll get this after deployment)
- GitHub Repository: `https://github.com/YOUR_USERNAME/AS_web`

### Documentation
- VERCEL_DEPLOYMENT.md - Full deployment guide
- .env.example - Environment variables template
- vercel.json - Vercel configuration
- .vercelignore - Files to ignore during deployment

### External Resources
- Vercel Docs: https://vercel.com/docs
- Next.js Docs: https://nextjs.org/docs
- Firebase Docs: https://firebase.google.com/docs
- GitHub Push Guide: https://docs.github.com/en/get-started/using-git/pushing-commits-to-a-remote-repository

---

**Ready to deploy?** Start with Step 1: Push to GitHub, then follow the checklist above!
