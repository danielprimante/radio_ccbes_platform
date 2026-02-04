import type { NextConfig } from "next";

const nextConfig: NextConfig = {
  /* config options here */
  // Descomentar la siguiente línea si usas un hosting estático (cPanel, Hostinger plano, etc)
  output: 'export',
  trailingSlash: true,
  images: {
    unoptimized: true,
    remotePatterns: [
      {
        protocol: 'https',
        hostname: 'firebasestorage.googleapis.com',
      },
      {
        protocol: 'https',
        hostname: 'i.ibb.co',
      },
    ],
  },
};

export default nextConfig;
