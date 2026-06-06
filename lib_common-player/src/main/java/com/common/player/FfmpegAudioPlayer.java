package com.common.player;

import android.media.AudioAttributes;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.PlaybackParams;
import android.media.AudioTrack;
import android.os.Build;
import android.util.Log;

public final class FfmpegAudioPlayer {
    private static final String TAG = "FfmpegAudioPlayer";

    private long nativeHandle;
    private String dataSource;
    private PlayerListener listener;
    private AudioTrack audioTrack;
    private float playbackSpeed = 1.0f;
    private boolean audioErrorNotified;

    public FfmpegAudioPlayer() {
        FfmpegPlayer.loadLibraries();
        nativeHandle = nativeCreate();
    }

    public void setDataSource(String pathOrUrl) {
        dataSource = pathOrUrl;
        audioErrorNotified = false;
        nativeSetDataSource(requireHandle(), pathOrUrl);
    }

    public void setListener(PlayerListener listener) {
        this.listener = listener;
    }

    public void start() {
        if (dataSource == null || dataSource.isEmpty()) {
            throw new IllegalStateException("Data source is empty.");
        }
        nativeStart(requireHandle());
    }

    public void pause() {
        nativePause(requireHandle());
        AudioTrack track = audioTrack;
        if (track != null) {
            track.pause();
        }
    }

    public void resume() {
        AudioTrack track = audioTrack;
        if (track != null) {
            track.play();
        }
        nativeResume(requireHandle());
    }

    public void setPlaybackSpeed(float speed) {
        if (speed <= 0f) {
            throw new IllegalArgumentException("Playback speed must be greater than 0.");
        }
        playbackSpeed = speed;
        applyPlaybackSpeed();
        nativeSetPlaybackSpeed(requireHandle(), speed);
    }

    public long getCurrentPosition() {
        return nativeGetCurrentPosition(requireHandle());
    }

    public long getDuration() {
        return nativeGetDuration(requireHandle());
    }

    public void seekTo(long positionMs) {
        nativeSeekTo(requireHandle(), Math.max(0L, positionMs));
    }

    public void stop() {
        if (nativeHandle != 0) {
            nativeStop(nativeHandle);
        }
        releaseAudioTrack();
    }

    public void release() {
        if (nativeHandle != 0) {
            nativeRelease(nativeHandle);
            nativeHandle = 0;
        }
        releaseAudioTrack();
        listener = null;
    }

    private long requireHandle() {
        if (nativeHandle == 0) {
            throw new IllegalStateException("Player has been released.");
        }
        return nativeHandle;
    }

    @SuppressWarnings("unused")
    private void onNativeAudioFormat(int sampleRate, int channels) {
        releaseAudioTrack();
        int channelMask = channels == 1
                ? AudioFormat.CHANNEL_OUT_MONO
                : AudioFormat.CHANNEL_OUT_STEREO;
        int minBufferSize = AudioTrack.getMinBufferSize(
                sampleRate,
                channelMask,
                AudioFormat.ENCODING_PCM_16BIT
        );
        if (minBufferSize <= 0) {
            notifyAudioError(-1001, "Invalid AudioTrack buffer size: " + minBufferSize);
            return;
        }
        int bufferSize = Math.max(minBufferSize, sampleRate * channels * 2 / 2);

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                audioTrack = new AudioTrack.Builder()
                        .setAudioAttributes(new AudioAttributes.Builder()
                                .setUsage(AudioAttributes.USAGE_MEDIA)
                                .setContentType(AudioAttributes.CONTENT_TYPE_MOVIE)
                                .build())
                        .setAudioFormat(new AudioFormat.Builder()
                                .setSampleRate(sampleRate)
                                .setChannelMask(channelMask)
                                .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                                .build())
                        .setTransferMode(AudioTrack.MODE_STREAM)
                        .setBufferSizeInBytes(bufferSize)
                        .build();
            } else {
                audioTrack = new AudioTrack(
                        AudioManager.STREAM_MUSIC,
                        sampleRate,
                        channelMask,
                        AudioFormat.ENCODING_PCM_16BIT,
                        bufferSize,
                        AudioTrack.MODE_STREAM
                );
            }
        } catch (RuntimeException e) {
            notifyAudioError(-1002, "Create AudioTrack failed: " + e.getMessage());
            return;
        }
        if (audioTrack.getState() != AudioTrack.STATE_INITIALIZED) {
            notifyAudioError(-1003, "AudioTrack is not initialized.");
            releaseAudioTrack();
            return;
        }
        try {
            setTrackVolume(audioTrack);
            audioTrack.play();
        } catch (RuntimeException e) {
            notifyAudioError(-1004, "Start AudioTrack failed: " + e.getMessage());
            releaseAudioTrack();
            return;
        }
        applyPlaybackSpeed();

        PlayerListener current = listener;
        if (current != null) {
            current.onPrepared();
        }
    }

    @SuppressWarnings("unused")
    private void onNativeAudioData(byte[] pcm) {
        AudioTrack track = audioTrack;
        if (track != null && pcm.length > 0) {
            int result;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                result = track.write(pcm, 0, pcm.length, AudioTrack.WRITE_BLOCKING);
            } else {
                result = track.write(pcm, 0, pcm.length);
            }
            if (result < 0) {
                notifyAudioError(result, "AudioTrack write failed.");
            }
        }
    }

    @SuppressWarnings("unused")
    private void onNativeCompletion() {
        PlayerListener current = listener;
        if (current != null) {
            current.onCompletion();
        }
    }

    @SuppressWarnings("unused")
    private void onNativeError(int code, String message) {
        notifyAudioError(code, message);
    }

    @SuppressWarnings("unused")
    private void onNativeProgress(long positionMs, long durationMs) {
        PlayerListener current = listener;
        if (current != null) {
            current.onProgress(positionMs, durationMs);
        }
    }

    private void applyPlaybackSpeed() {
        AudioTrack track = audioTrack;
        if (track == null || Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            return;
        }
        try {
            PlaybackParams params = track.getPlaybackParams();
            params.setSpeed(playbackSpeed);
            track.setPlaybackParams(params);
        } catch (IllegalArgumentException | IllegalStateException ignored) {
        }
    }

    private void setTrackVolume(AudioTrack track) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                track.setVolume(AudioTrack.getMaxVolume());
            } else {
                //noinspection deprecation
                track.setStereoVolume(AudioTrack.getMaxVolume(), AudioTrack.getMaxVolume());
            }
        } catch (RuntimeException ignored) {
        }
    }

    private void notifyAudioError(int code, String message) {
        String safeMessage = message == null ? "" : message;
        Log.e(TAG, "audio error " + code + ": " + safeMessage);
        if (audioErrorNotified) {
            return;
        }
        audioErrorNotified = true;
        PlayerListener current = listener;
        if (current != null) {
            current.onError(code, safeMessage);
        }
    }

    private void releaseAudioTrack() {
        AudioTrack track = audioTrack;
        audioTrack = null;
        if (track != null) {
            try {
                track.stop();
            } catch (IllegalStateException ignored) {
            }
            track.release();
        }
    }

    private native long nativeCreate();

    private static native void nativeSetDataSource(long handle, String pathOrUrl);

    private static native void nativeStart(long handle);

    private static native void nativePause(long handle);

    private static native void nativeResume(long handle);

    private static native void nativeSetPlaybackSpeed(long handle, float speed);

    private static native long nativeGetCurrentPosition(long handle);

    private static native long nativeGetDuration(long handle);

    private static native void nativeSeekTo(long handle, long positionMs);

    private static native void nativeStop(long handle);

    private static native void nativeRelease(long handle);
}
