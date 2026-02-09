import { Inter } from "next/font/google";
import '@/globals.css';
import { JSX } from "react";

const inter = Inter({ subsets: ['latin'] });

interface RootLayoutProps {
    children: React.ReactNode;
}

export default function RootLayout({ children }: RootLayoutProps): JSX.Element {
  return (
        <html lang="es" className="scroll-smooth">
            {/* <body className={`bg-white dark:bg-darkBackground text-black dark:text-white font-sans transition-colors duration-300 ${inter.className}`}> */}
            <body className="font-sans text-black transition-colors duration-300 bg-white dark:bg-darkBackground dark:text-white">
              {children}
          </body>
      </html>
  );
}
