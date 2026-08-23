package com.sonicpay.app.sonic

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import kotlin.random.Random

class StressTest {

    private fun pushAll(demod: FskDemodulator, source: FloatArray) {
        val chunk = FloatArray(276)
        var i = 0
        while (i < source.size) {
            val n = minOf(chunk.size, source.size - i)
            source.copyInto(chunk, 0, i, i + n)
            demod.push(chunk, n)
            i += n
        }
    }

    @Test
    fun randomPayloadsUnderNoise() {
        val rng = Random(7)
        val vpas = listOf("a@b", "mans@jd", "shop123@upi", "x-y_z@bank", "q")
        repeat(20) { iter ->
            val vpa = vpas[iter % vpas.size]
            val paise = rng.nextLong(1, 5_000_000)
            var sig = FskModulator.frameToSamples(SonicProtocol.encodePayload(vpa, paise))
            val amp = 0.2f + rng.nextFloat() * 0.6f
            for (i in sig.indices) {
                sig[i] = sig[i] * amp + (rng.nextFloat() - 0.5f) * 0.03f
            }
            var decoded: Pair<String, Long>? = null
            val demod = FskDemodulator({ v, a -> decoded = v to a })
            pushAll(demod, FloatArray(4410) { (rng.nextFloat() - 0.5f) * 0.03f } + sig)
            assertNotNull("iter=$iter vpa=$vpa", decoded)
            assertEquals("iter=$iter", paise, decoded?.second)
        }
    }

    @Test
    fun joinMidPreambleStillDecodes() {
        val vpa = "late@join"
        val paise = 999L
        val full = FskModulator.frameToSamples(SonicProtocol.encodePayload(vpa, paise))
        val droppedHalf = SonicProtocol.PREAMBLE_SAMPLES / 2
        val tail = full.copyOfRange(droppedHalf, full.size)

        var decoded: Pair<String, Long>? = null
        val demod = FskDemodulator({ v, a -> decoded = v to a })
        pushAll(demod, tail)
        assertNotNull("mid-preamble join should decode", decoded)
        assertEquals(vpa, decoded?.first)
        assertEquals(paise, decoded?.second)
    }

    @Test
    fun wrongFrequenciesDoNotDecode() {
        val rng = Random(3)
        var fired = false
        val demod = FskDemodulator({ _, _ -> fired = true })
        val noiseTone = FloatArray(SonicProtocol.SAMPLE_RATE * 4) {
            kotlin.math.sin(2.0 * Math.PI * 1000.0 * it / SonicProtocol.SAMPLE_RATE).toFloat() * 0.5f +
                (rng.nextFloat() - 0.5f) * 0.05f
        }
        pushAll(demod, noiseTone)
        assertEquals(false, fired)
    }
}
