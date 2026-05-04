import Link from 'next/link';
import { Button } from '@/components/ui/button';

export default function HomePage() {
  return (
    <main className="mx-auto flex min-h-screen max-w-5xl flex-col gap-8 px-6 py-16">
      <section className="rounded-3xl border bg-white p-8 shadow-sm">
        <p className="text-sm font-semibold uppercase tracking-wide text-primary">Phase 1</p>
        <h1 className="mt-3 text-4xl font-bold tracking-tight">?? STT & Agentic ?? ??</h1>
        <p className="mt-4 max-w-2xl text-lg text-slate-600">
          ????, ???, ? ??? ?? ?? ??? ??? ? ?? MVP ?? ?????.
        </p>
        <div className="mt-8 flex gap-3">
          <Button asChild>
            <Link href="/login">???</Link>
          </Button>
          <Button asChild variant="outline">
            <Link href="/register">????</Link>
          </Button>
        </div>
      </section>
    </main>
  );
}
