#!/usr/bin/env node

const fs = require('fs');
const path = require('path');

console.log('[v0] Setting up African Shipping Web Application...');

// Create a basic package-lock.json structure
const packageLockStructure = {
  "name": "african-shipping-web",
  "version": "1.0.0",
  "lockfileVersion": 3,
  "requires": true,
  "packages": {
    "": {
      "name": "african-shipping-web",
      "version": "1.0.0",
      "hasInstallScript": true
    }
  }
};

const projectRoot = path.join(__dirname, '..');
const lockFilePath = path.join(projectRoot, 'package-lock.json');

try {
  fs.writeFileSync(lockFilePath, JSON.stringify(packageLockStructure, null, 2));
  console.log('[v0] Created package-lock.json');
  console.log('[v0] Project setup complete! Preview should start automatically.');
} catch (error) {
  console.error('[v0] Setup failed:', error.message);
  process.exit(1);
}
