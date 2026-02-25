/** @type {import('next').NextConfig} */
const nextConfig = {
  typescript: {
    ignoreBuildErrors: false,
  },
  images: {
    domains: ['firebasestorage.googleapis.com', 'lh3.googleusercontent.com'],
  },
}

module.exports = nextConfig
