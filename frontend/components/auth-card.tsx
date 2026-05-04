import type { ReactNode } from 'react';

export function AuthCard({ title, description, children }: { title: string; description: string; children: ReactNode }) {
  return (
    <main className="flex min-h-screen items-center justify-center bg-slate-50 px-4">
      <section className="w-full max-w-md rounded-3xl border bg-white p-8 shadow-sm">
        <h1 className="text-3xl font-bold tracking-tight">{title}</h1>
        <p className="mt-2 text-sm text-slate-500">{description}</p>
        <div className="mt-8">{children}</div>
      </section>
    </main>
  );
}
