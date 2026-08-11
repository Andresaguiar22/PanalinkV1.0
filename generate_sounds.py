import wave
import math
import struct
import os

os.makedirs("app/src/main/res/raw", exist_ok=True)

sample_rate = 44100

def generate_wav(filename, duration, gen_sample_func):
    num_samples = int(sample_rate * duration)
    filepath = os.path.join("app/src/main/res/raw", filename)
    with wave.open(filepath, 'w') as wav_file:
        wav_file.setnchannels(1) # Mono
        wav_file.setsampwidth(2) # 16-bit
        wav_file.setframerate(sample_rate)
        
        frames = []
        for i in range(num_samples):
            t = i / sample_rate
            val = gen_sample_func(t, duration)
            val = max(-1.0, min(1.0, val))
            sample = int(val * 32767)
            frames.append(struct.pack('<h', sample))
            
        wav_file.writeframes(b''.join(frames))
    print(f"Generated {filepath} ({duration}s)")

# 1. VOICE_START: Elegant rising warm chime (0.12s)
def voice_start(t, dur):
    env = math.sin(math.pi * t / dur) ** 0.5 * math.exp(-3 * t / dur)
    f = 523.25 + (783.99 - 523.25) * (t / dur)
    return (math.sin(2 * math.pi * f * t) * 0.7 + math.sin(2 * math.pi * f * 2 * t) * 0.2) * env

# 2. VOICE_LOCK: Crystal snap (0.09s)
def voice_lock(t, dur):
    env = math.exp(-30 * t)
    f = 880.0 + 880.0 * math.exp(-20 * t)
    return (math.sin(2 * math.pi * f * t) * 0.8 + math.sin(2 * math.pi * 1760.0 * t) * 0.2) * env

# 3. VOICE_CANCEL: Soft descending swish (0.12s)
def voice_cancel(t, dur):
    env = math.exp(-12 * t / dur)
    f = 659.25 - 220.0 * (t / dur)
    return math.sin(2 * math.pi * f * t) * 0.8 * env

# 4. VOICE_SEND: Upward pop (0.11s)
def voice_send(t, dur):
    env = math.exp(-15 * t / dur)
    f = 587.33 + 293.66 * (t / dur)
    return math.sin(2 * math.pi * f * t) * 0.85 * env

# 5. MESSAGE_SEND: Warm soft tick (0.05s)
def message_send(t, dur):
    env = math.exp(-40 * t)
    return math.sin(2 * math.pi * 783.99 * t) * 0.7 * env

# 6. MESSAGE_RECEIVED: Double soft chime (0.16s)
def message_received(t, dur):
    if t < 0.07:
        env = math.exp(-25 * t)
        return math.sin(2 * math.pi * 659.25 * t) * 0.8 * env
    else:
        t2 = t - 0.07
        env = math.exp(-20 * t2)
        return math.sin(2 * math.pi * 880.0 * t2) * 0.8 * env

# 7. MESSAGE_READ: Double high tick (0.08s)
def message_read(t, dur):
    if t < 0.035:
        env = math.exp(-60 * t)
        return math.sin(2 * math.pi * 1046.5 * t) * 0.6 * env
    elif t >= 0.04:
        t2 = t - 0.04
        env = math.exp(-60 * t2)
        return math.sin(2 * math.pi * 1318.5 * t2) * 0.6 * env
    return 0.0

generate_wav("pana_voice_start.wav", 0.12, voice_start)
generate_wav("pana_voice_lock.wav", 0.09, voice_lock)
generate_wav("pana_voice_cancel.wav", 0.12, voice_cancel)
generate_wav("pana_voice_send.wav", 0.11, voice_send)
generate_wav("pana_message_send.wav", 0.05, message_send)
generate_wav("pana_message_received.wav", 0.16, message_received)
generate_wav("pana_message_read.wav", 0.08, message_read)

print("All PanaLink audio files generated successfully.")
