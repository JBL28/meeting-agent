import { act, renderHook } from '@testing-library/react';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { formatDuration, useAudioRecorder } from './use-audio-recorder';

type DataHandler = ((event: BlobEvent) => void) | null;

class FakeMediaRecorder {
  static instances: FakeMediaRecorder[] = [];

  state: RecordingState = 'inactive';
  ondataavailable: DataHandler = null;
  onstop: (() => void) | null = null;

  constructor() {
    FakeMediaRecorder.instances.push(this);
  }

  start() {
    this.state = 'recording';
  }

  stop() {
    this.state = 'inactive';
    this.ondataavailable?.({ data: new Blob(['audio'], { type: 'audio/webm' }) } as BlobEvent);
    this.onstop?.();
  }
}

const track = { stop: vi.fn() };
const getUserMedia = vi.fn();

async function flushPromises() {
  await act(async () => {
    await Promise.resolve();
    await Promise.resolve();
  });
}

describe('useAudioRecorder', () => {
  beforeEach(() => {
    vi.useFakeTimers();
    vi.spyOn(console, 'info').mockImplementation(() => undefined);
    FakeMediaRecorder.instances = [];
    track.stop.mockClear();
    getUserMedia.mockReset();
    getUserMedia.mockResolvedValue({ getTracks: () => [track] });
    vi.stubGlobal('MediaRecorder', FakeMediaRecorder);
    Object.defineProperty(navigator, 'mediaDevices', {
      configurable: true,
      value: { getUserMedia },
    });
  });

  afterEach(() => {
    vi.useRealTimers();
    vi.restoreAllMocks();
    vi.unstubAllGlobals();
  });

  it('formats recording duration as mm:ss', () => {
    expect(formatDuration(65)).toBe('01:05');
  });

  it('starts, stops, and uploads a webm blob', async () => {
    const upload = vi.fn().mockResolvedValue({ id: 1 });
    const { result } = renderHook(() => useAudioRecorder({ maxDurationSeconds: 60, upload }));

    await act(async () => {
      await result.current.start();
    });

    expect(result.current.status).toBe('recording');
    expect(getUserMedia).toHaveBeenCalledWith({ audio: true });

    await act(async () => {
      result.current.stop();
    });
    await flushPromises();

    expect(result.current.status).toBe('uploaded');
    expect(upload).toHaveBeenCalledWith(expect.any(Blob));
    expect(console.info).toHaveBeenCalledWith('Recording started', { maxDurationSeconds: 60 });
    expect(console.info).toHaveBeenCalledWith('Recording upload completed', expect.objectContaining({ duration: 0 }));
  });

  it('shows an error when microphone permission is denied', async () => {
    getUserMedia.mockRejectedValue(new Error('Permission denied'));
    const { result } = renderHook(() => useAudioRecorder({ maxDurationSeconds: 60 }));

    await act(async () => {
      await result.current.start();
    });

    expect(result.current.status).toBe('error');
    expect(result.current.error).toBe('Permission denied');
  });

  it('auto-stops at the max duration', async () => {
    const upload = vi.fn().mockResolvedValue({ id: 1 });
    const { result } = renderHook(() => useAudioRecorder({ maxDurationSeconds: 2, upload }));

    await act(async () => {
      await result.current.start();
    });

    await act(async () => {
      vi.advanceTimersByTime(2000);
    });
    await flushPromises();

    expect(result.current.status).toBe('uploaded');
    expect(result.current.autoStopped).toBe(true);
    expect(upload).toHaveBeenCalledTimes(1);
  });

  it('blocks upload when the recording is shorter than the minimum duration', async () => {
    const upload = vi.fn().mockResolvedValue({ id: 1 });
    const { result } = renderHook(() => useAudioRecorder({ maxDurationSeconds: 10, minDurationSeconds: 2, upload }));

    await act(async () => {
      await result.current.start();
    });
    await act(async () => {
      result.current.stop();
    });
    await flushPromises();

    expect(result.current.status).toBe('error');
    expect(result.current.error).toBe('Recording must be at least 2 seconds.');
    expect(upload).not.toHaveBeenCalled();
  });
});
