/** @type {import('next').NextConfig} */
const nextConfig = {
  images: {
    // domains: ['img.freepik.com', 'ui-avatars.com', 'api.dicebear.com'],
    dangerouslyAllowSVG: true,
  },
  async rewrites() {
    return [
      {
        source: '/api/:path*',
        destination: 'http://localhost:8080/:path*', // Proxy to Backend
      },
    ];
  },
};

export default nextConfig;