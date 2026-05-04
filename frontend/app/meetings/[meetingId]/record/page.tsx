'use client';

import Link from 'next/link';
import { useParams } from 'next/navigation';
import { Button } from '@/components/ui/button';
import { markMeetingRecording, uploadMeetingAudio } from '@/lib/upload-api';
import { formatDuration, useAudioRecorder } from '@/lib/use-audio-recorder';

const MAX_MEETING_RECORDING_SECONDS = 60 * 60;

export default function MeetingRecordPage() {
  const params = useParams<{ meetingId: string }>();
  const meetingId = params.meetingId;

  const recorder = useAudioRecorder({
    maxDurationSeconds: MAX_MEETING_RECORDING_SECONDS,
    onBeforeStart: async () => {
      await markMeetingRecording(meetingId);
    },
    upload: (blob) => {
      const file = new File([blob], `meeting-${meetingId}-${Date.now()}.webm`, {
        type: 'audio/webm;codecs=opus',
      });
      return uploadMeetingAudio(meetingId, file);
    },
  });

  return (
    <main className="mx-auto min-h-screen max-w-3xl space-y-6 px-6 py-10">
      <section>
        <p className="text-sm text-slate-500">Meeting ID: {meetingId}</p>
        <h1 className="mt-2 text-3xl font-bold">Browser Recording</h1>
        <p className="mt-2 text-slate-500">
          Record meeting audio in this browser. The recording is uploaded through the existing meeting audio API.
        </p>
      </section>

      <section className="rounded-2xl border bg-white p-6 shadow-sm">
        <div className="text-center">
          <p className="text-sm uppercase tracking-wide text-slate-500">Elapsed</p>
          <p className="mt-2 font-mono text-6xl font-bold">{formatDuration(recorder.elapsedSeconds)}</p>
          <p className="mt-3 text-sm text-slate-500">Status: {recorder.status}</p>
        </div>

        {recorder.error ? <p className="mt-6 rounded-md bg-red-50 p-3 text-sm text-red-600">{recorder.error}</p> : null}
        {recorder.autoStopped ? (
          <p className="mt-6 rounded-md bg-amber-50 p-3 text-sm text-amber-700">
            The 60 minute limit was reached, so recording stopped and upload started automatically.
          </p>
        ) : null}
        {recorder.status === 'uploaded' ? (
          <p className="mt-6 rounded-md bg-green-50 p-3 text-sm text-green-700">
            Recording uploaded. Meeting status should now be RECORDED.
          </p>
        ) : null}

        <div className="mt-8 flex flex-wrap justify-center gap-3">
          <Button type="button" onClick={recorder.start} disabled={recorder.isRecording || recorder.status === 'requesting' || recorder.status === 'uploading'}>
            Start Recording
          </Button>
          <Button type="button" variant="outline" onClick={() => recorder.stop()} disabled={!recorder.isRecording}>
            Stop and Upload
          </Button>
          <Button type="button" variant="outline" onClick={recorder.reset} disabled={recorder.isRecording || recorder.status === 'uploading'}>
            Reset
          </Button>
        </div>
      </section>

      <Link className="text-sm font-medium text-slate-700 underline" href={`/meetings/${meetingId}`}>
        Back to meeting detail
      </Link>
    </main>
  );
}
