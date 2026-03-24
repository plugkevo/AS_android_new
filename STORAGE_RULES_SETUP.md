# Firebase Storage Rules Setup

## Problem
Profile images were not uploading due to Firebase Storage permission error (403 - Permission Denied).

## Solution
The issue was that Firebase Storage Security Rules were not configured to allow authenticated users to upload profile images.

## How to Deploy the Rules

### Using Firebase CLI (Recommended)

1. **Install Firebase CLI** (if not already installed):
   ```bash
   npm install -g firebase-tools
   ```

2. **Login to Firebase**:
   ```bash
   firebase login
   ```

3. **Initialize Firebase in your project** (if needed):
   ```bash
   firebase init storage
   ```

4. **Deploy the rules**:
   ```bash
   firebase deploy --only storage
   ```

### Using Firebase Console (Web UI)

1. Go to [Firebase Console](https://console.firebase.google.com)
2. Select your project: **African Shipping 25**
3. Navigate to **Storage** → **Rules** tab
4. Replace all existing rules with the content from `storage.rules` file in this repository
5. Click **Publish**

## Rules Overview

The `storage.rules` file contains:

- **Profile Images**: Authenticated users can upload/read only their own profile pictures
  ```
  /profile_images/{userId}.jpg
  ```

- **Shipments**: Authenticated users can read/write shipment-related files
  ```
  /shipments/{allPaths}
  ```

- **User Documents**: Authenticated users can read/write only their own documents
  ```
  /users/{userId}/{allPaths}
  ```

## Testing the Fix

After deploying the rules:

1. Open the app
2. Go to **Profile**
3. Click on profile picture to change it
4. Select an image from camera or gallery
5. Click **Save Profile**
6. Image should now upload successfully and display immediately

## What Changed

Before: Storage rules were either missing or too restrictive
After: Rules now allow authenticated users to manage their own profile images

## Troubleshooting

If images still aren't uploading:

1. **Check Authentication**: Ensure user is logged in
   - Verify `request.auth != null` is true
   
2. **Verify User ID**: The upload path must match the current user's UID
   - In code: `profile_images/${user.uid}.jpg`
   - In rules: `match /profile_images/{userId}.jpg`
   - These must match exactly

3. **Clear App Cache**: Sometimes old data causes issues
   - Go to Settings → Apps → African Shipping 25 → Clear Cache
   - Restart the app

4. **Check Firebase Project**: Ensure you're deploying rules to the correct Firebase project
   - Verify in `google-services.json`

## Security Notes

- Users can only upload images to their own profile folder
- Images are readable by any authenticated user (for future profile viewing features)
- All other storage access is denied by default

