package com.sonicpay.app.ui.screens

import android.media.AudioManager
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.sonicpay.app.audio.TonePlayer
import com.sonicpay.app.data.SessionPrefs
import com.sonicpay.app.sonic.FskModulator
import com.sonicpay.app.sonic.SonicProtocol
import com.sonicpay.app.ui.components.GlassCard
import com.sonicpay.app.ui.components.GlassChip
import com.sonicpay.app.ui.components.PulseRings
import com.sonicpay.app.ui.components.SonicOrb
import com.sonicpay.app.ui.components.OrbState
import com.sonicpay.app.ui.components.TopBar
import com.sonicpay.app.ui.theme.AccentMint
import com.sonicpay.app.ui.theme.TextMuted
import com.sonicpay.app.ui.theme.TextPrimary
import com.sonicpay.app.ui.theme.TextSecondary
import com.sonicpay.app.ui.theme.WarnAmber
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private enum class Broadcast { IDLE, SENDING, SENT }

@Composable
fun MerchantScreen(onSettings: () -> Unit) {
    val context = LocalContext.current
    val haptics = LocalHapticFeedback.current
    var amount by remember { mutableStateOf("") }
    var state by remember { mutableStateOf(Broadcast.IDLE) }
    var showVolumeHint by remember {
        mutableStateOf(currentMediaVolumeFraction(context) < 0.6f)
    }
    val player = remember { TonePlayer() }
    val scope = rememberCoroutineScope()
    val amountPaise = SonicProtocol.parseAmountToPaise(amount)
    val vpa = SessionPrefs.merchantVpa

    DisposableEffect(Unit) {
        val window = context.findActivity()?.window
        window?.addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        onDispose {
            window?.clearFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            player.stop()
        }
    }

    fun broadcast() {
        val paise = amountPaise ?: return
        if (vpa.isEmpty() || vpa.toByteArray(Charsets.UTF_8).size > SonicProtocol.MAX_VPA_BYTES) return
        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
        showVolumeHint = currentMediaVolumeFraction(context) < 0.6f
        state = Broadcast.SENDING
        scope.launch {
            try {
                val frame = SonicProtocol.encodePayload(vpa, paise)
                player.play(FskModulator.frameToSamples(frame))
                state = Broadcast.SENT
            } catch (_: IllegalArgumentException) {
                state = Broadcast.IDLE
            }
            delay(1600)
            if (state == Broadcast.SENT) state = Broadcast.IDLE
        }
    }

    Column(modifier = Modifier.fillMaxSize().padding(24.dp)) {
        TopBar(
            title = "Merchant",
            actions = {
                IconButton(onClick = onSettings) {
                    Icon(Icons.Filled.Settings, contentDescription = "Settings", tint = TextSecondary)
                }
            }
        )

        Spacer(Modifier.height(20.dp))

        GlassCard(modifier = Modifier.fillMaxWidth(), sheen = true) {
            Column(modifier = Modifier.padding(22.dp)) {
                Text("Amount", style = MaterialTheme.typography.labelLarge, color = TextSecondary)
                OutlinedTextField(
                    value = amount,
                    onValueChange = { amount = it },
                    placeholder = { Text("₹ 0.00", color = TextMuted) },
                    textStyle = MaterialTheme.typography.displayLarge.copy(color = TextPrimary),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = AccentMint,
                        unfocusedBorderColor = TextMuted.copy(alpha = 0.4f),
                        cursorColor = AccentMint
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("10", "50", "100", "200").forEach { chip ->
                        GlassChip(
                            label = "₹$chip",
                            onClick = {
                                amount = chip
                                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            },
                            selected = amount == chip
                        )
                    }
                }
                Spacer(Modifier.height(14.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        Modifier
                            .size(30.dp)
                            .clip(CircleShape)
                            .background(AccentMint.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Filled.GraphicEq, null,
                            tint = AccentMint, modifier = Modifier.size(15.dp)
                        )
                    }
                    Spacer(Modifier.width(10.dp))
                    Column {
                        Text(
                            SessionPrefs.merchantName.ifEmpty { "Your shop" },
                            style = MaterialTheme.typography.titleMedium,
                            color = TextPrimary
                        )
                        Text(vpa.ifEmpty { "set your VPA in Settings" },
                            style = MaterialTheme.typography.labelMedium, color = TextMuted)
                    }
                }
            }
        }

        if (showVolumeHint && state == Broadcast.IDLE) {
            Spacer(Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.VolumeUp, null, tint = WarnAmber, modifier = Modifier.size(15.dp))
                Spacer(Modifier.width(6.dp))
                Text(
                    "Media volume is low — raise it for a strong tone",
                    style = MaterialTheme.typography.labelMedium, color = WarnAmber
                )
            }
        }

        Spacer(Modifier.weight(1f))

        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            PulseRings(color = AccentMint, active = state == Broadcast.SENDING, maxRadiusDp = 130) {
                SonicOrb(
                    icon = Icons.Filled.GraphicEq,
                    state = when (state) {
                        Broadcast.IDLE -> OrbState.IDLE
                        Broadcast.SENDING -> OrbState.ACTIVE
                        Broadcast.SENT -> OrbState.DONE
                    },
                    tint = AccentMint,
                    enabled = amountPaise != null && vpa.isNotEmpty() && state == Broadcast.IDLE,
                    onClick = { broadcast() }
                )
            }
        }

        Spacer(Modifier.height(24.dp))

        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            Text(
                text = when (state) {
                    Broadcast.IDLE ->
                        if (amount.isNotBlank() && amountPaise == null) "Enter a valid amount"
                        else "Tap to broadcast · ~2s ultrasonic burst"
                    Broadcast.SENDING -> "Broadcasting near-inaudible tone…"
                    Broadcast.SENT -> "Sent — waiting for the customer"
                },
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary
            )
        }
    }
}

internal fun currentMediaVolumeFraction(context: android.content.Context): Float {
    val am = context.getSystemService(AudioManager::class.java) ?: return 1f
    val max = am.getStreamMaxVolume(AudioManager.STREAM_MUSIC).coerceAtLeast(1)
    return am.getStreamVolume(AudioManager.STREAM_MUSIC).toFloat() / max
}

private tailrec fun android.content.Context.findActivity(): android.app.Activity? =
    when (this) {
        is android.app.Activity -> this
        is android.content.ContextWrapper -> baseContext.findActivity()
        else -> null
    }
