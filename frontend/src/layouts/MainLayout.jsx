import React from 'react';
import Header from '../components/common/Header';
import Footer from '../components/common/Footer';
import { useHealthCheck } from '../hooks/useHealthCheck';

export default function MainLayout({ children }) {
  const healthState = useHealthCheck(true, 8000);

  return (
    <div className="min-h-screen flex flex-col bg-slate-950 text-slate-100 antialiased overflow-x-hidden">
      <Header healthState={healthState} />
      <main className="flex-1 max-w-6xl w-full mx-auto px-4 sm:px-6 lg:px-8 py-6 sm:py-8">
        {React.isValidElement(children) && typeof children.type !== 'string'
          ? React.cloneElement(children, { healthState })
          : children}
      </main>
      <Footer />
    </div>
  );
}
