'use client';

import Link from 'next/link';
import { useRouter } from 'next/navigation';
import { FormEvent, useState } from 'react';
import { AuthCard } from '@/components/auth-card';
import { FormField } from '@/components/form-field';
import { Button } from '@/components/ui/button';
import { register } from '@/lib/auth-api';

export default function RegisterPage() {
  const router = useRouter();
  const [email, setEmail] = useState('');
  const [name, setName] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(false);

  async function onSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setLoading(true);
    setError(null);
    try {
      await register({ email, name, password });
      router.push('/login');
    } catch (exception) {
      setError(exception instanceof Error ? exception.message : 'Registration failed.');
    } finally {
      setLoading(false);
    }
  }

  return (
    <AuthCard title="Register" description="Create a member account to start team collaboration.">
      <form className="space-y-4" onSubmit={onSubmit}>
        <FormField label="Name" value={name} onChange={(event) => setName(event.target.value)} required />
        <FormField label="Email" type="email" value={email} onChange={(event) => setEmail(event.target.value)} required />
        <FormField label="Password" type="password" minLength={8} value={password} onChange={(event) => setPassword(event.target.value)} required />
        {error ? <p className="rounded-md bg-red-50 p-3 text-sm text-red-600">{error}</p> : null}
        <Button className="w-full" disabled={loading}>{loading ? 'Registering...' : 'Register'}</Button>
      </form>
      <p className="mt-4 text-center text-sm text-slate-500">
        Already have an account? <Link className="font-semibold text-primary" href="/login">Login</Link>
      </p>
    </AuthCard>
  );
}
