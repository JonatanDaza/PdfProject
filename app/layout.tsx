import type { Metadata } from "next";

import "@/styles/globals.css";

export const metadata: Metadata = {
  title: "PDF Front",
  description: "Sistema de lectura de historias clínicas",
};

interface RootLayoutProps {
  children: React.ReactNode;
}

export default function RootLayout({ children }: RootLayoutProps) {
  return (
    <html lang="es">
      <body>{children}</body>
    </html>
  );
}
