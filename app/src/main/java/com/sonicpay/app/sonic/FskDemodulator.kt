package com.sonicpay.app.sonic

import kotlin.math.cos
import kotlin.math.PI

class Goertzel(private val freqHz: Double, sampleRate: Int) {
    private val coeff = 2.0 * cos(2.0 * PI * freqHz / sampleRate)

    fun power(x: FloatArray, offset: Int, length: Int): Double {
        var s1 = 0.0
        var s2 = 0.0
        val end = offset + length
        for (i in offset until end) {
            val s0 = x[i] + coeff * s1 - s2
            s2 = s1
            s1 = s0
        }
        return s1 * s1 + s2 * s2 - coeff * s1 * s2
    }
}

class FskDemodulator(
    private val onDecoded: (vpa: String, amountPaise: Long) -> Unit,
    private val timeSourceMs: () -> Long = System::currentTimeMillis
) {
    private val syncGoertzel = Goertzel(SonicProtocol.SYNC_FREQ_HZ, SonicProtocol.SAMPLE_RATE)
    private val dataGoertzels = Array(SonicProtocol.TONES_PER_SYMBOL) {
        Goertzel(SonicProtocol.dataFreq(it), SonicProtocol.SAMPLE_RATE)
    }

    private val ring = FloatArray(RING_CAPACITY)
    private var writeAbs = 0L
    private var searchAbs = 0L
    private var lockedAtAbs = -1L
    private var lockDeadlineMs = 0L
    private var lastOkKey: String? = null
    private var lastOkTimeMs = 0L

    private val scratch = FloatArray(SonicProtocol.SYMBOL_SAMPLES)

    fun push(samples: FloatArray, count: Int = samples.size) {
        for (i in 0 until count) {
            ring[(writeAbs % RING_CAPACITY).toInt()] = samples[i]
            writeAbs++
        }
        process()
    }

    private fun sampleAt(abs: Long): Float = ring[(abs % RING_CAPACITY).toInt()]

    private fun copyWindow(absStart: Long, length: Int): Boolean {
        if (absStart < writeAbs - RING_CAPACITY || absStart + length > writeAbs) return false
        for (i in 0 until length) scratch[i] = sampleAt(absStart + i)
        return true
    }

    private fun process() {
        val w = SonicProtocol.SYMBOL_SAMPLES.toLong()

        if (lockedAtAbs >= 0) {
            val outcome = attemptDecode(lockedAtAbs)
            when (outcome) {
                Outcome.NeedMore -> if (timeSourceMs() > lockDeadlineMs) unlock()
                else -> unlock()
            }
            return
        }

        while (searchAbs + w <= writeAbs) {
            if (!copyWindow(searchAbs, SonicProtocol.SYMBOL_SAMPLES)) break
            var energy = 0.0
            for (v in scratch) energy += v.toDouble() * v
            if (energy < MIN_WINDOW_ENERGY) {
                searchAbs += HOP_COARSE
                continue
            }
            val syncPower = syncGoertzel.power(scratch, 0, SonicProtocol.SYMBOL_SAMPLES)
            if (syncPower > SYNC_RATIO_THRESHOLD * energy) {
                lockedAtAbs = searchAbs
                lockDeadlineMs = timeSourceMs() + LOCK_TIMEOUT_MS
                when (attemptDecode(lockedAtAbs)) {
                    Outcome.NeedMore -> {}
                    else -> unlock()
                }
                break
            }
            searchAbs += HOP_COARSE
        }
    }

    private fun unlock() {
        searchAbs = lockedAtAbs + SonicProtocol.PREAMBLE_SAMPLES
        lockedAtAbs = -1
    }

    private enum class Outcome { NeedMore, Rejected, Decoded }

    private fun attemptDecode(coarseWindowStart: Long): Outcome {
        val w = SonicProtocol.SYMBOL_SAMPLES.toLong()
        val scanLimit = coarseWindowStart + FINE_SCAN_SPAN * w

        if (writeAbs < scanLimit + w) {
            return if (timeSourceMs() > lockDeadlineMs) Outcome.Rejected else Outcome.NeedMore
        }

        var x = coarseWindowStart
        var lastGoodX = -1L
        while (x + w <= writeAbs && x <= scanLimit) {
            if (!copyWindow(x, SonicProtocol.SYMBOL_SAMPLES)) break
            var energy = 0.0
            for (v in scratch) energy += v.toDouble() * v
            val syncPower = syncGoertzel.power(scratch, 0, SonicProtocol.SYMBOL_SAMPLES)
            if (syncPower > SYNC_RATIO_THRESHOLD * energy) {
                lastGoodX = x
            } else if (lastGoodX >= 0) {
                break
            }
            x += HOP_FINE
        }
        if (lastGoodX < 0) return Outcome.Rejected

        val t0Estimate = lastGoodX + w
        for (candidate in -T0_HYPOTHESES..T0_HYPOTHESES) {
            val outcome = attemptDecodeAt(
                t0Estimate + candidate * w / T0_STEPS
            )
            if (outcome != Outcome.Rejected) return outcome
        }
        return Outcome.Rejected
    }

    private fun attemptDecodeAt(t0: Long): Outcome {
        val w = SonicProtocol.SYMBOL_SAMPLES.toLong()
        val guard = (SonicProtocol.SYMBOL_SAMPLES * GUARD_FRACTION).toInt()
        val core = SonicProtocol.SYMBOL_SAMPLES - 2 * guard

        fun availableThrough(symbolCount: Int): Boolean {
            val end = t0 + symbolCount * w + guard + core
            return end <= writeAbs && t0 - guard >= writeAbs - RING_CAPACITY
        }

        val headerBits = decodeSymbols(t0, HEADER_SYMBOL_COUNT, guard, core)
            ?: return if (!availableThrough(HEADER_SYMBOL_COUNT)) Outcome.NeedMore else Outcome.Rejected
        val headerBytes = bitsToBytes(headerBits).copyOfRange(0, 2)
        if (headerBytes[0].toInt() != SonicProtocol.VERSION) return Outcome.Rejected
        val vpaLen = headerBytes[1].toInt() and 0xFF
        if (vpaLen == 0 || vpaLen > SonicProtocol.MAX_VPA_BYTES) return Outcome.Rejected

        val totalBytes = 8 + vpaLen
        val totalSymbols = ceilDiv(totalBytes * 8, SonicProtocol.BITS_PER_SYMBOL)
        val allBits = decodeSymbols(t0, totalSymbols, guard, core)
            ?: return if (availableThrough(totalSymbols)) Outcome.Rejected else Outcome.NeedMore
        val frame = bitsToBytes(allBits).copyOfRange(0, totalBytes)

        val decoded = SonicProtocol.decodePayload(frame)
        return if (decoded is SonicProtocol.DecodeResult.Ok) {
            val key = "${decoded.vpa}|${decoded.amountPaise}"
            val now = timeSourceMs()
            if (key != lastOkKey || now - lastOkTimeMs >= DEDUP_COOLDOWN_MS) {
                lastOkKey = key
                lastOkTimeMs = now
                onDecoded(decoded.vpa, decoded.amountPaise)
            }
            Outcome.Decoded
        } else {
            Outcome.Rejected
        }
    }

    private fun decodeSymbols(t0: Long, symbolCount: Int, guard: Int, core: Int): LongArray? {
        val w = SonicProtocol.SYMBOL_SAMPLES.toLong()
        val bits = LongArray(symbolCount)
        for (k in 0 until symbolCount) {
            val symStart = t0 + k * w + guard
            if (!copyWindow(symStart, core)) return null
            var energy = 0.0
            for (v in scratch) energy += v.toDouble() * v
            if (energy < MIN_CORE_ENERGY) return null

            var bestIdx = 0
            var bestPower = 0.0
            val powers = DoubleArray(SonicProtocol.TONES_PER_SYMBOL)
            for (t in 0 until SonicProtocol.TONES_PER_SYMBOL) {
                val p = dataGoertzels[t].power(scratch, 0, core)
                powers[t] = p
                if (p > bestPower) {
                    bestPower = p
                    bestIdx = t
                }
            }
            var sumOthers = 0.0
            for (t in 0 until SonicProtocol.TONES_PER_SYMBOL) if (t != bestIdx) sumOthers += powers[t]
            val avgOthers = sumOthers / (SonicProtocol.TONES_PER_SYMBOL - 1)
            if (bestPower < PEAK_RATIO * avgOthers) return null
            bits[k] = bestIdx.toLong()
        }
        return bits
    }

    private fun bitsToBytes(symbolBits: LongArray): ByteArray {
        val totalBits = symbolBits.size * SonicProtocol.BITS_PER_SYMBOL
        val byteCount = ceilDiv(totalBits, 8)
        val out = ByteArray(byteCount)
        var bitPos = 0
        for (sym in symbolBits) {
            for (b in SonicProtocol.BITS_PER_SYMBOL - 1 downTo 0) {
                if (bitPos >= byteCount * 8) break
                val bit = ((sym shr b) and 1L).toInt()
                out[bitPos / 8] =
                    ((out[bitPos / 8].toInt() and 0xFF) or (bit shl (7 - bitPos % 8))).toByte()
                bitPos++
            }
        }
        return out
    }

    companion object {
        private const val RING_CAPACITY = SonicProtocol.SAMPLE_RATE * 6
        private const val HOP_COARSE = SonicProtocol.SYMBOL_SAMPLES / 4L
        private const val HOP_FINE = SonicProtocol.SYMBOL_SAMPLES / 8L
        private const val FINE_SCAN_SPAN = SonicProtocol.PREAMBLE_SYMBOLS + 2L
        private const val GUARD_FRACTION = 0.15f
        private const val SYNC_RATIO_THRESHOLD = 300.0
        private const val PEAK_RATIO = 3.0
        private const val MIN_CORE_ENERGY = 1e-5 * SonicProtocol.SYMBOL_SAMPLES
        private const val MIN_WINDOW_ENERGY = 1e-5 * SonicProtocol.SYMBOL_SAMPLES
        private val HEADER_SYMBOL_COUNT = ceilDiv(16, SonicProtocol.BITS_PER_SYMBOL)
        private const val LOCK_TIMEOUT_MS = 6000L
        private const val DEDUP_COOLDOWN_MS = 2500L
        private const val T0_STEPS = 8
        private const val T0_HYPOTHESES = 2

        private fun ceilDiv(a: Int, b: Int): Int = (a + b - 1) / b
    }
}
