'use client';

import Link from 'next/link';
import { useRouter } from 'next/navigation';
import { FormEvent, useState } from 'react';
import { AuthCard } from '@/components/auth-card';
import { FormField } from '@/components/form-field';
import { Button } from '@/components/ui/button';
import { login } from '@/lib/auth-api';
import { useAuthStore } from '@/lib/auth-store';

export default function LoginPage() {
  const router = useRouter();
  const setSession = useAuthStore((state) => state.setSession);
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(false);

  async function onSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setLoading(true);
    setError(null);
    try {
      const response = await login({ email, password });
      setSession(response.member, response.accessToken);
      router.push('/teams');
    } catch (exception) {
      setError(exception instanceof Error ? exception.message : '???? ??????.');
    } finally {
      setLoading(false);
    }
  }

  return (
    <AuthCard title="???" description="JWT ?? ? ? ?? ???? ?????.">
      <form className="space-y-4" onSubmit={onSubmit}>
        <FormField label="???" type="email" value={email} onChange={(event) => setEmail(event.target.value)} required />
        <FormField label="????" type="password" value={password} onChange={(event) => setPassword(event.target.value)} required />
        {error ? <p className="rounded-md bg-red-50 p-3 text-sm text-red-600">{error}</p> : null}
        <Button className="w-full" disabled={loading}>{loading ? '??? ?...' : '???'}</Button>
      </form>
      <p className="mt-4 text-center text-sm text-slate-500">
        ??? ???? <Link className="font-semibold text-primary" href="/register">????</Link>
      </p>
    </AuthCard>
  );
}
