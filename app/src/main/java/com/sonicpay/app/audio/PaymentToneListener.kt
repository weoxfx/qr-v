package com.sonicpay.app.audio

import android.annotation.SuppressLint
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import com.sonicpay.app.sonic.FskDemodulator
import com.sonicpay.app.sonic.SonicProtocol
import kotlin.concurrent.thread
import kotlin.math.sqrt

class PaymentToneListener(
    private val onDecoded: (vpa: String, amountPaise: Long) -> Unit
) {
    private val demodulator = FskDemodulator(onDecoded)
    private var record: AudioRecord? = null
    private var worker: Thread? = null

    @Volatile
    private var running = false

    @Volatile
    var inputLevel: Float = 0f
        private set

    fun start(): Boolean {
        if (running) return true
        val asFloat = tryOpen(AudioFormat.ENCODING_PCM_FLOAT)
        val as16Bit = if (asFloat == null) tryOpen(AudioFormat.ENCODING_PCM_16BIT) else null
        val rec = asFloat ?: as16Bit ?: return false
        val pcmFloat = asFloat != null
        record = rec
        running = true
        inputLevel = 0f
        rec.startRecording()
        worker = thread(name = "sonic-listen") {
            if (pcmFloat) readLoopFloat(rec) else readLoop16Bit(rec)
        }
        return true
    }

    @SuppressLint("MissingPermission")
    private fun tryOpen(encoding: Int): AudioRecord? {
        val minBuf = try {
            AudioRecord.getMinBufferSize(
                SonicProtocol.SAMPLE_RATE,
                AudioFormat.CHANNEL_IN_MONO,
                encoding
            )
        } catch (_: Throwable) {
            0
        }
        if (minBuf <= 0) return null
        val bufSize = maxOf(minBuf, SonicProtocol.SAMPLE_RATE / 2)
        val rec = try {
            AudioRecord(
                MediaRecorder.AudioSource.MIC,
                SonicProtocol.SAMPLE_RATE,
                AudioFormat.CHANNEL_IN_MONO,
                encoding,
                bufSize
            )
        } catch (_: Throwable) {
            return null
        }
        if (rec.state != AudioRecord.STATE_INITIALIZED) {
            rec.release()
            return null
        }
        return rec
    }

    private fun readLoopFloat(rec: AudioRecord) {
        val chunk = FloatArray(READ_CHUNK_SAMPLES)
        while (running) {
            val n = rec.read(chunk, 0, chunk.size, AudioRecord.READ_BLOCKING)
            if (n < 0) break
            if (n == 0) continue
            updateLevel(chunk, n)
            demodulator.push(chunk, n)
        }
    }

    private fun readLoop16Bit(rec: AudioRecord) {
        val raw = ShortArray(READ_CHUNK_SAMPLES)
        val chunk = FloatArray(READ_CHUNK_SAMPLES)
        while (running) {
            val n = rec.read(raw, 0, raw.size, AudioRecord.READ_BLOCKING)
            if (n < 0) break
            if (n == 0) continue
            for (i in 0 until n) chunk[i] = raw[i] / 32768f
            updateLevel(chunk, n)
            demodulator.push(chunk, n)
        }
    }

    private fun updateLevel(chunk: FloatArray, n: Int) {
        var sum = 0.0
        for (i in 0 until n) sum += chunk[i].toDouble() * chunk[i]
        val rms = sqrt(sum / n)
        inputLevel = (rms.toFloat() * LEVEL_GAIN).coerceIn(0f, 1f)
    }

    fun stop() {
        running = false
        worker?.let {
            try {
                it.join(JOIN_TIMEOUT_MS)
            } catch (_: InterruptedException) {
            }
        }
        worker = null
        record?.let {
            try {
                it.stop()
            } catch (_: IllegalStateException) {
            }
            it.release()
        }
        record = null
        inputLevel = 0f
    }

    companion object {
        private const val READ_CHUNK_SAMPLES = SonicProtocol.SYMBOL_SAMPLES / 8
        private const val JOIN_TIMEOUT_MS = 1000L
        private const val LEVEL_GAIN = 6f
    }
}
