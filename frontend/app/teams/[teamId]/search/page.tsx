'use client';

import { FormEvent, useState } from 'react';
import Link from 'next/link';
import { useParams } from 'next/navigation';
import { Button } from '@/components/ui/button';
import type { MeetingSearchResult } from '@/lib/domain';
import { searchMeetings } from '@/lib/upload-api';

export default function TeamSearchPage() {
  const params = useParams<{ teamId: string }>();
  const teamId = params.teamId;
  const [keyword, setKeyword] = useState('');
  const [results, setResults] = useState<MeetingSearchResult[]>([]);
  const [message, setMessage] = useState<string | null>(null);
  const [error, setError] = useState<string | null>(null);

  async function onSearch(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setMessage(null);
    setError(null);
    try {
      const page = await searchMeetings(teamId, keyword);
      setResults(page.content);
      setMessage(`${page.totalElements} result(s).`);
    } catch (exception) {
      setError(exception instanceof Error ? exception.message : 'Search failed.');
    }
  }

  return (
    <main className="mx-auto min-h-screen max-w-4xl space-y-6 px-6 py-10">
      <section>
        <p className="text-sm text-slate-500">Team ID: {teamId}</p>
        <h1 className="mt-2 text-3xl font-bold">Meeting Search</h1>
      </section>
      <form className="flex gap-3 rounded-2xl border bg-white p-6 shadow-sm" onSubmit={onSearch}>
        <input
          className="flex-1 rounded-md border border-slate-300 p-2"
          placeholder="Search meeting title, summary, transcript, action items"
          value={keyword}
          onChange={(event) => setKeyword(event.target.value)}
          required
        />
        <Button>Search</Button>
      </form>
      {message ? <p className="rounded-md bg-green-50 p-3 text-sm text-green-700">{message}</p> : null}
      {error ? <p className="rounded-md bg-red-50 p-3 text-sm text-red-600">{error}</p> : null}
      <section className="space-y-3">
        {results.map((result) => (
          <article key={result.meetingId} className="rounded-2xl border bg-white p-5 shadow-sm">
            <Link className="text-lg font-semibold underline" href={`/meetings/${result.meetingId}/minutes`}>
              {result.title}
            </Link>
            <p className="mt-1 text-sm text-slate-500">Status: {result.status}</p>
            <p className="mt-3 text-sm">{result.snippet}</p>
          </article>
        ))}
      </section>
    </main>
  );
}
