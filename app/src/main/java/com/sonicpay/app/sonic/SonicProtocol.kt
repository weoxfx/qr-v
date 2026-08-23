package com.sonicpay.app.sonic

import java.io.ByteArrayOutputStream

object SonicProtocol {
    const val SAMPLE_RATE = 44100
    const val VERSION = 1

    const val SYNC_FREQ_HZ = 16400.0
    const val DATA_BASE_FREQ_HZ = 16800.0
    const val FREQ_STEP_HZ = 280.0
    const val TONES_PER_SYMBOL = 8
    const val BITS_PER_SYMBOL = 3
    const val SYMBOL_MS = 40
    const val PREAMBLE_SYMBOLS = 4

    const val SYMBOL_SAMPLES = SAMPLE_RATE * SYMBOL_MS / 1000
    const val PREAMBLE_SAMPLES = SYMBOL_SAMPLES * PREAMBLE_SYMBOLS

    const val MAX_VPA_BYTES = 24

    fun dataFreq(index: Int): Double =
        DATA_BASE_FREQ_HZ + index * FREQ_STEP_HZ

    fun encodePayload(vpa: String, amountPaise: Long): ByteArray {
        require(amountPaise in 0..Int.MAX_VALUE.toLong()) { "amount out of range" }
        val vpaBytes = vpa.toByteArray(Charsets.UTF_8)
        require(vpaBytes.size in 1..MAX_VPA_BYTES) { "vpa must be 1..$MAX_VPA_BYTES bytes" }

        val body = ByteArrayOutputStream().apply {
            write(VERSION)
            write(vpaBytes.size)
            write(vpaBytes)
            write((amountPaise ushr 24).toInt() and 0xFF)
            write((amountPaise ushr 16).toInt() and 0xFF)
            write((amountPaise ushr 8).toInt() and 0xFF)
            write(amountPaise.toInt() and 0xFF)
        }.toByteArray()

        val crc = crc16(body)
        return body + byteArrayOf(((crc ushr 8) and 0xFF).toByte(), (crc and 0xFF).toByte())
    }

    sealed class DecodeResult {
        data class Ok(val vpa: String, val amountPaise: Long) : DecodeResult()
        object BadCrc : DecodeResult()
        object Malformed : DecodeResult()
    }

    fun decodePayload(frame: ByteArray): DecodeResult {
        if (frame.size < 8 || frame[0].toInt() != VERSION) return DecodeResult.Malformed
        val crcExpected = ((frame[frame.size - 2].toInt() and 0xFF) shl 8) or
            (frame[frame.size - 1].toInt() and 0xFF)
        val crcActual = crc16(frame.copyOfRange(0, frame.size - 2))
        if (crcExpected != crcActual) return DecodeResult.BadCrc

        val vpaLen = frame[1].toInt() and 0xFF
        val payloadEnd = frame.size - 2
        if (vpaLen == 0 || 2 + vpaLen + 4 > payloadEnd) return DecodeResult.Malformed

        val vpa = String(frame, 2, vpaLen, Charsets.UTF_8)
        var p = 2 + vpaLen
        var amountPaise = 0L
        repeat(4) {
            amountPaise = (amountPaise shl 8) or (frame[p++].toInt() and 0xFF).toLong()
        }
        return DecodeResult.Ok(vpa, amountPaise)
    }

    fun formatAmount(amountPaise: Long): String =
        "%d.%02d".format(amountPaise / 100, amountPaise % 100)

    fun parseAmountToPaise(text: String): Long? {
        val cleaned = text.trim()
        if (cleaned.isEmpty()) return null
        val parts = cleaned.split('.')
        if (parts.size > 2) return null
        val rupees = parts[0].ifEmpty { "0" }
        if (rupees.any { !it.isDigit() } || rupees.length > 9) return null
        return when {
            parts.size == 1 -> rupees.toLong() * 100
            else -> {
                val frac = parts[1]
                if (frac.isEmpty() || frac.length > 2 || frac.any { !it.isDigit() }) return null
                rupees.toLong() * 100 + when (frac.length) {
                    1 -> frac.toLong() * 10
                    else -> frac.toLong()
                }
            }
        }
    }

    fun crc16(bytes: ByteArray): Int {
        var crc = 0xFFFF
        for (b in bytes) {
            crc = crc xor ((b.toInt() and 0xFF) shl 8)
            repeat(8) {
                crc = if (crc and 0x8000 != 0) ((crc shl 1) xor 0x1021) and 0xFFFF else (crc shl 1) and 0xFFFF
            }
        }
        return crc
    }
}
