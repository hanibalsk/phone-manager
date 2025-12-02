import type { Metadata } from "next";
import { Sidebar } from "@/components/layout";
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
        <div className="flex h-screen">
          <Sidebar />
          <main className="flex-1 overflow-auto">
            {children}
          </main>
        </div>
      </body>
    </html>
  );
}
