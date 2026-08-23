package com.sonicpay.app.audio

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import com.sonicpay.app.sonic.SonicProtocol
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class TonePlayer {

    private var track: AudioTrack? = null

    suspend fun play(samples: FloatArray) = withContext(Dispatchers.IO) {
        val minBuf = AudioTrack.getMinBufferSize(
            SonicProtocol.SAMPLE_RATE,
            AudioFormat.CHANNEL_OUT_MONO,
            AudioFormat.ENCODING_PCM_FLOAT
        )
        val bufSize = maxOf(minBuf, samples.size * FLOAT_BYTES)
        val t = AudioTrack.Builder()
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
            .setTransferMode(AudioTrack.MODE_STATIC)
            .setBufferSizeInBytes(bufSize)
            .build()
        track = t
        try {
            t.play()
            var written = 0
            while (written < samples.size && t.state == AudioTrack.STATE_INITIALIZED) {
                written += t.write(
                    samples,
                    written,
                    samples.size - written,
                    AudioTrack.WRITE_BLOCKING
                )
            }
            waitForCompletion(t, samples.size)
        } finally {
            releaseQuietly(t)
            if (track === t) track = null
        }
    }

    fun stop() {
        track?.let {
            try {
                it.pause()
                it.flush()
            } catch (_: IllegalStateException) {
            }
        }
    }

    private fun waitForCompletion(t: AudioTrack, sampleCount: Int) {
        val durationMs = sampleCount * 1000L / SonicProtocol.SAMPLE_RATE
        val deadline = System.currentTimeMillis() + durationMs + COMPLETION_GRACE_MS
        while (System.currentTimeMillis() < deadline &&
            t.playState == AudioTrack.PLAYSTATE_PLAYING
        ) {
            if (t.playbackHeadPosition >= sampleCount) break
            Thread.sleep(POLL_MS)
        }
    }

    private fun releaseQuietly(t: AudioTrack) {
        try {
            t.stop()
        } catch (_: IllegalStateException) {
        }
        t.release()
    }

    companion object {
        private const val FLOAT_BYTES = 4
        private const val POLL_MS = 50L
        private const val COMPLETION_GRACE_MS = 250L
    }
}
