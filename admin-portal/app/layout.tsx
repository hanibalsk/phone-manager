import type { Metadata } from "next";
import { AuthProvider } from "@/contexts/auth-context";
import { Toaster } from "sonner";
import "./globals.css";

export const metadata: Metadata = {
  title: "Phone Manager - Admin Portal",
  description: "Administration portal for Phone Manager parental control system",
};

export default function RootLayout({
  children,
}: {
  children: React.ReactNode;
}) {
  return (
    <html lang="en">
      <body className="min-h-screen bg-background font-sans antialiased">
        <AuthProvider>
          {children}
        </AuthProvider>
        <Toaster richColors position="top-right" />
      </body>
    </html>
  );
}
