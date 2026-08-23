package com.sonicpay.app.audio

import android.annotation.SuppressLint
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import com.sonicpay.app.sonic.FskDemodulator
import com.sonicpay.app.sonic.SonicProtocol
import kotlin.concurrent.thread

class PaymentToneListener(
    onDecoded: (vpa: String, amountPaise: Long) -> Unit
) {
    private val demodulator = FskDemodulator(onDecoded)
    private var record: AudioRecord? = null
    private var worker: Thread? = null

    @Volatile
    private var running = false

    @SuppressLint("MissingPermission")
    fun start(): Boolean {
        if (running) return true
        val minBuf = AudioRecord.getMinBufferSize(
            SonicProtocol.SAMPLE_RATE,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_FLOAT
        )
        if (minBuf <= 0) return false
        val bufSize = maxOf(minBuf, SonicProtocol.SAMPLE_RATE / 2)
        val rec = try {
            AudioRecord(
                MediaRecorder.AudioSource.MIC,
                SonicProtocol.SAMPLE_RATE,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_FLOAT,
                bufSize
            )
        } catch (_: RuntimeException) {
            return false
        }
        if (rec.state != AudioRecord.STATE_INITIALIZED) {
            rec.release()
            return false
        }
        record = rec
        running = true
        rec.startRecording()
        worker = thread(name = "sonic-listen") {
            val chunk = FloatArray(READ_CHUNK_SAMPLES)
            while (running) {
                val n = rec.read(chunk, 0, chunk.size, AudioRecord.READ_BLOCKING)
                if (n > 0) demodulator.push(chunk, n)
                else if (n < 0) break
            }
        }
        return true
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
    }

    companion object {
        private const val READ_CHUNK_SAMPLES = SonicProtocol.SYMBOL_SAMPLES / 8
        private const val JOIN_TIMEOUT_MS = 1000L
    }
}
