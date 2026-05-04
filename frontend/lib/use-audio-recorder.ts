import { useCallback, useEffect, useRef, useState } from 'react';

export type RecordingStatus = 'idle' | 'requesting' | 'recording' | 'stopped' | 'uploading' | 'uploaded' | 'error';

type UseAudioRecorderOptions<TUploadResult = unknown> = {
  maxDurationSeconds: number;
  minDurationSeconds?: number;
  mimeType?: string;
  upload?: (blob: Blob) => Promise<TUploadResult>;
  onBeforeStart?: () => Promise<void> | void;
};

type StopReason = 'manual' | 'maxDuration';

export function useAudioRecorder<TUploadResult = unknown>({
  maxDurationSeconds,
  minDurationSeconds = 0,
  mimeType = 'audio/webm;codecs=opus',
  upload,
  onBeforeStart,
}: UseAudioRecorderOptions<TUploadResult>) {
  const [status, setStatus] = useState<RecordingStatus>('idle');
  const [elapsedSeconds, setElapsedSeconds] = useState(0);
  const [error, setError] = useState<string | null>(null);
  const [autoStopped, setAutoStopped] = useState(false);
  const [lastBlob, setLastBlob] = useState<Blob | null>(null);

  const recorderRef = useRef<MediaRecorder | null>(null);
  const streamRef = useRef<MediaStream | null>(null);
  const chunksRef = useRef<Blob[]>([]);
  const timerRef = useRef<ReturnType<typeof setInterval> | null>(null);
  const elapsedRef = useRef(0);
  const stopReasonRef = useRef<StopReason>('manual');

  const clearTimer = useCallback(() => {
    if (timerRef.current) {
      clearInterval(timerRef.current);
      timerRef.current = null;
    }
  }, []);

  const stopTracks = useCallback(() => {
    streamRef.current?.getTracks().forEach((track) => track.stop());
    streamRef.current = null;
  }, []);

  const stop = useCallback((reason: StopReason = 'manual') => {
    const recorder = recorderRef.current;
    if (!recorder || recorder.state === 'inactive') {
      return;
    }
    stopReasonRef.current = reason;
    setAutoStopped(reason === 'maxDuration');
    console.info('Recording stopped', { reason });
    recorder.stop();
  }, []);

  const finishRecording = useCallback(async () => {
    clearTimer();
    stopTracks();
    const blob = new Blob(chunksRef.current, { type: mimeType });
    const duration = elapsedRef.current;
    setLastBlob(blob);
    setStatus('stopped');

    if (duration < minDurationSeconds) {
      setError(`Recording must be at least ${minDurationSeconds} seconds.`);
      setStatus('error');
      return;
    }

    if (!upload) {
      return;
    }

    try {
      setStatus('uploading');
      await upload(blob);
      setStatus('uploaded');
      console.info('Recording upload completed', { size: blob.size, duration });
    } catch (exception) {
      setError(exception instanceof Error ? exception.message : 'Recording upload failed.');
      setStatus('error');
    }
  }, [clearTimer, mimeType, minDurationSeconds, stopTracks, upload]);

  const start = useCallback(async () => {
    setError(null);
    setAutoStopped(false);
    setLastBlob(null);
    setElapsedSeconds(0);
    elapsedRef.current = 0;
    chunksRef.current = [];

    if (typeof window === 'undefined' || typeof MediaRecorder === 'undefined' || !navigator.mediaDevices?.getUserMedia) {
      setStatus('error');
      setError('This browser does not support audio recording.');
      return;
    }

    try {
      setStatus('requesting');
      const stream = await navigator.mediaDevices.getUserMedia({ audio: true });
      const recorder = new MediaRecorder(stream, { mimeType });
      streamRef.current = stream;
      recorderRef.current = recorder;
      recorder.ondataavailable = (event) => {
        if (event.data.size > 0) {
          chunksRef.current.push(event.data);
        }
      };
      recorder.onstop = () => {
        void finishRecording();
      };

      await onBeforeStart?.();
      recorder.start();
      setStatus('recording');
      console.info('Recording started', { maxDurationSeconds });
      timerRef.current = setInterval(() => {
        elapsedRef.current += 1;
        setElapsedSeconds(elapsedRef.current);
        if (elapsedRef.current >= maxDurationSeconds) {
          stop('maxDuration');
        }
      }, 1000);
    } catch (exception) {
      clearTimer();
      stopTracks();
      setStatus('error');
      setError(exception instanceof Error ? exception.message : 'Microphone access was denied or unavailable.');
    }
  }, [clearTimer, finishRecording, maxDurationSeconds, mimeType, onBeforeStart, stop, stopTracks]);

  const reset = useCallback(() => {
    clearTimer();
    stopTracks();
    recorderRef.current = null;
    chunksRef.current = [];
    elapsedRef.current = 0;
    setElapsedSeconds(0);
    setError(null);
    setAutoStopped(false);
    setLastBlob(null);
    setStatus('idle');
  }, [clearTimer, stopTracks]);

  useEffect(() => {
    return () => {
      clearTimer();
      stopTracks();
    };
  }, [clearTimer, stopTracks]);

  return {
    status,
    elapsedSeconds,
    error,
    autoStopped,
    lastBlob,
    isRecording: status === 'recording',
    start,
    stop,
    reset,
  };
}

export function formatDuration(totalSeconds: number) {
  const minutes = Math.floor(totalSeconds / 60).toString().padStart(2, '0');
  const seconds = (totalSeconds % 60).toString().padStart(2, '0');
  return `${minutes}:${seconds}`;
}
