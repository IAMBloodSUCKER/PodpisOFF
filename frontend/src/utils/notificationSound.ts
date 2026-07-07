let audioContext: AudioContext | null = null;
let unlocked = false;

function getAudioContext(): AudioContext | null {
  if (typeof window === 'undefined') return null;
  const AudioCtx =
    window.AudioContext ||
    (window as unknown as { webkitAudioContext?: typeof AudioContext }).webkitAudioContext;
  if (!AudioCtx) return null;
  if (!audioContext) {
    audioContext = new AudioCtx();
  }
  return audioContext;
}

export async function unlockNotificationSound(): Promise<void> {
  const ctx = getAudioContext();
  if (!ctx || unlocked) return;
  if (ctx.state === 'suspended') {
    await ctx.resume();
  }
  unlocked = ctx.state === 'running';
}

/** Soft two-tone chime — quiet but audible. */
export async function playNotificationSound(): Promise<void> {
  const ctx = getAudioContext();
  if (!ctx) return;

  if (ctx.state === 'suspended') {
    try {
      await ctx.resume();
    } catch {
      return;
    }
  }

  const start = ctx.currentTime;
  const notes = [
    { frequency: 587.33, at: 0, duration: 0.14 },
    { frequency: 739.99, at: 0.11, duration: 0.2 },
  ];

  for (const note of notes) {
    const oscillator = ctx.createOscillator();
    const gain = ctx.createGain();
    oscillator.type = 'sine';
    oscillator.frequency.setValueAtTime(note.frequency, start + note.at);

    const peak = 0.11;
    gain.gain.setValueAtTime(0.0001, start + note.at);
    gain.gain.exponentialRampToValueAtTime(peak, start + note.at + 0.018);
    gain.gain.exponentialRampToValueAtTime(0.0001, start + note.at + note.duration);

    oscillator.connect(gain);
    gain.connect(ctx.destination);
    oscillator.start(start + note.at);
    oscillator.stop(start + note.at + note.duration + 0.04);
  }
}
