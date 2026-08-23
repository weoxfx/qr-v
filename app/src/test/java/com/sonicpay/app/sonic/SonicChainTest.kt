package com.sonicpay.app.sonic

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class SonicChainTest {

    @Test
    fun payloadRoundTrip() {
        val frame = SonicProtocol.encodePayload("mans@jd", 2800L)
        val result = SonicProtocol.decodePayload(frame)
        assertTrue(result is SonicProtocol.DecodeResult.Ok)
        result as SonicProtocol.DecodeResult.Ok
        assertEquals("mans@jd", result.vpa)
        assertEquals(2800L, result.amountPaise)
    }

    @Test
    fun payloadRejectsCorruption() {
        val frame = SonicProtocol.encodePayload("mans@jd", 2800L)
        frame[3] = (frame[3] + 1).toByte()
        assertTrue(SonicProtocol.decodePayload(frame) is SonicProtocol.DecodeResult.BadCrc)
    }

    @Test
    fun amountParsing() {
        assertEquals(2800L, SonicProtocol.parseAmountToPaise("28.00"))
        assertEquals(2850L, SonicProtocol.parseAmountToPaise("28.5"))
        assertEquals(2800L, SonicProtocol.parseAmountToPaise("28"))
        assertEquals(7L, SonicProtocol.parseAmountToPaise(".07"))
        assertNull(SonicProtocol.parseAmountToPaise("28.123"))
        assertNull(SonicProtocol.parseAmountToPaise("-5"))
        assertNull(SonicProtocol.parseAmountToPaise(""))
        assertNull(SonicProtocol.parseAmountToPaise("abc"))
        assertEquals("28.00", SonicProtocol.formatAmount(2800L))
        assertEquals("0.07", SonicProtocol.formatAmount(7L))
    }

    @Test
    fun fullAudioChannelRoundTrip() {
        val vpa = "mans@jd"
        val paise = 2850L
        val samples = FskModulator.frameToSamples(SonicProtocol.encodePayload(vpa, paise))

        var decoded: Pair<String, Long>? = null
        val demod = FskDemodulator({ v, a -> decoded = v to a })

        val leadIn = FloatArray(SonicProtocol.SAMPLE_RATE / 4)
        val chunk = FloatArray(276)
        pushAll(demod, leadIn + samples, chunk)

        assertNotNull("frame should decode over clean channel", decoded)
        assertEquals(vpa, decoded?.first)
        assertEquals(paise, decoded?.second)
    }

    @Test
    fun decodesWithNoiseAndGain() {
        val vpa = "shop@upi"
        val paise = 10L
        var raw = FskModulator.frameToSamples(SonicProtocol.encodePayload(vpa, paise))
        val rng = Random(42)
        val noisy = FloatArray(raw.size) { i -> raw[i] * 0.35f + (rng.nextFloat() - 0.5f) * 0.02f }
        raw = FloatArray(0)

        var decoded: Pair<String, Long>? = null
        val demod = FskDemodulator({ v, a -> decoded = v to a })
        pushAll(
            demod,
            FloatArray(SonicProtocol.SAMPLE_RATE / 4) { (rng.nextFloat() - 0.5f) * 0.02f } + noisy,
            FloatArray(276)
        )

        assertNotNull("frame should survive mild noise + gain change", decoded)
        assertEquals(vpa, decoded?.first)
        assertEquals(paise, decoded?.second)
    }

    @Test
    fun silenceDoesNotDecode() {
        var fired = false
        val demod = FskDemodulator({ _, _ -> fired = true })
        pushAll(demod, FloatArray(SonicProtocol.SAMPLE_RATE * 2), FloatArray(276))
        assertTrue(!fired)
    }

    private fun pushAll(demod: FskDemodulator, source: FloatArray, chunk: FloatArray) {
        var i = 0
        while (i < source.size) {
            val n = minOf(chunk.size, source.size - i)
            source.copyInto(chunk, 0, i, i + n)
            demod.push(chunk, n)
            i += n
        }
    }
}
