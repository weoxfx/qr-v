package com.sonicpay.app.sonic

import kotlin.math.PI
import kotlin.math.ceil
import kotlin.math.sin

object FskModulator {

    fun frameToSamples(frame: ByteArray): FloatArray {
        val bitsPerByte = 8
        val totalBits = frame.size * bitsPerByte
        val symbolCount = ceil(totalBits.toDouble() / SonicProtocol.BITS_PER_SYMBOL).toInt()
        val totalSamples =
            SonicProtocol.PREAMBLE_SAMPLES + symbolCount * SonicProtocol.SYMBOL_SAMPLES +
                SonicProtocol.SYMBOL_SAMPLES
        val out = FloatArray(totalSamples)

        var cursor = 0
        repeat(SonicProtocol.PREAMBLE_SYMBOLS) {
            writeTone(out, cursor, SonicProtocol.SYNC_FREQ_HZ)
            cursor += SonicProtocol.SYMBOL_SAMPLES
        }

        for (symbolIndex in 0 until symbolCount) {
            var bits = 0
            repeat(SonicProtocol.BITS_PER_SYMBOL) { b ->
                val bitIndex = symbolIndex * SonicProtocol.BITS_PER_SYMBOL + b
                val byteIndex = bitIndex / bitsPerByte
                val bitInByte = 7 - (bitIndex % bitsPerByte)
                val bit = if (byteIndex < frame.size) {
                    (frame[byteIndex].toInt() shr bitInByte) and 1
                } else 0
                bits = (bits shl 1) or bit
            }
            writeTone(out, cursor, SonicProtocol.dataFreq(bits))
            cursor += SonicProtocol.SYMBOL_SAMPLES
        }

        applyFade(out, SonicProtocol.SAMPLE_RATE / 100)
        return out
    }

    private fun writeTone(out: FloatArray, start: Int, freq: Double) {
        val phaseIncrement = 2.0 * PI * freq / SonicProtocol.SAMPLE_RATE
        var phase = 0.0
        for (i in 0 until SonicProtocol.SYMBOL_SAMPLES) {
            val idx = start + i
            if (idx >= out.size) break
            out[idx] = sin(phase).toFloat() * 0.9f
            phase += phaseIncrement
        }
    }

    private fun applyFade(samples: FloatArray, fadeLength: Int) {
        val len = minOf(fadeLength, samples.size / 2)
        for (i in 0 until len) {
            val gain = i.toFloat() / len
            samples[i] *= gain
            samples[samples.size - 1 - i] *= gain
        }
    }
}
