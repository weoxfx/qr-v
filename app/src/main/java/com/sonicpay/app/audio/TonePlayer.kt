package com.sonicpay.app.audio

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import com.sonicpay.app.sonic.SonicProtocol
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Plays one-shot PCM clips on a fire-and-forget basis. Never throws:
 * every failure mode (unsupported format, init failure, device weirdness)
 * comes back as [play] == false so callers can degrade gracefully.
 */
class TonePlayer {

    private var track: AudioTrack? = null

    suspend fun play(samples: FloatArray): Boolean =
        withContext(Dispatchers.IO) { playBlocking(samples) }

    fun stop() {
        track?.let {
            try {
                it.pause()
                it.flush()
            } catch (_: Throwable) {
            }
        }
    }

    private fun playBlocking(samples: FloatArray): Boolean {
        stop()
        if (samples.isEmpty()) return false

        val minBuf = try {
            AudioTrack.getMinBufferSize(
                SonicProtocol.SAMPLE_RATE,
                AudioFormat.CHANNEL_OUT_MONO,
                AudioFormat.ENCODING_PCM_FLOAT
            )
        } catch (_: Throwable) {
            0
        }
        if (minBuf <= 0) return false

        val t = try {
            AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_FLOAT)
                        .setSampleRate(SonicProtocol.SAMPLE_RATE)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                        .build()
                )
                .setTransferMode(AudioTrack.MODE_STREAM)
                .setBufferSizeInBytes(maxOf(minBuf, SonicProtocol.SYMBOL_SAMPLES * FLOAT_BYTES))
                .build()
        } catch (_: Throwable) {
            null
        } ?: return false

        if (t.state != AudioTrack.STATE_INITIALIZED) {
            try {
                t.release()
            } catch (_: Throwable) {
            }
            return false
        }

        track = t
        try {
            t.play()
            var written = 0
            while (written < samples.size) {
                if (t.playState != AudioTrack.PLAYSTATE_PLAYING) break
                val n = t.write(
                    samples,
                    written,
                    minOf(WRITE_CHUNK_SAMPLES, samples.size - written),
                    AudioTrack.WRITE_BLOCKING
                )
                if (n < 0) return false
                written += n
            }
            drain(t, samples.size)
            return true
        } catch (_: Throwable) {
            return false
        } finally {
            releaseQuietly(t)
            if (track === t) track = null
        }
    }

    private fun drain(t: AudioTrack, sampleCount: Int) {
        val durationMs = sampleCount * 1000L / SonicProtocol.SAMPLE_RATE
        val deadline = System.currentTimeMillis() + durationMs + COMPLETION_GRACE_MS
        while (System.currentTimeMillis() < deadline &&
            t.playState == AudioTrack.PLAYSTATE_PLAYING &&
            t.playbackHeadPosition < sampleCount
        ) {
            try {
                Thread.sleep(POLL_MS)
            } catch (_: InterruptedException) {
                break
            }
        }
    }

    private fun releaseQuietly(t: AudioTrack) {
        try {
            t.stop()
        } catch (_: Throwable) {
        }
        try {
            t.release()
        } catch (_: Throwable) {
        }
    }

    companion object {
        private const val FLOAT_BYTES = 4
        private const val POLL_MS = 20L
        private const val COMPLETION_GRACE_MS = 250L
        private const val WRITE_CHUNK_SAMPLES = 4410
    }
}
