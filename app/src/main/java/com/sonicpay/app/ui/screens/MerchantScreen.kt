package com.sonicpay.app.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.draw.clip
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.sonicpay.app.audio.TonePlayer
import com.sonicpay.app.sonic.FskModulator
import com.sonicpay.app.sonic.SonicProtocol
import com.sonicpay.app.ui.components.GlassCard
import com.sonicpay.app.ui.components.PulseRings
import com.sonicpay.app.ui.theme.AccentBlue
import com.sonicpay.app.ui.theme.AccentCyan
import com.sonicpay.app.ui.theme.SuccessGreen
import com.sonicpay.app.ui.theme.TextMuted
import com.sonicpay.app.ui.theme.TextPrimary
import com.sonicpay.app.ui.theme.TextSecondary
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private enum class BroadcastState { IDLE, SENDING, SENT }

@Composable
fun MerchantScreen(onBack: () -> Unit) {
    var amount by remember { mutableStateOf("") }
    var payeeVpa by remember { mutableStateOf("mans@jd") }
    var state by remember { mutableStateOf(BroadcastState.IDLE) }
    val player = remember { TonePlayer() }
    val scope = rememberCoroutineScope()
    val amountPaise = SonicProtocol.parseAmountToPaise(amount)

    DisposableEffect(Unit) {
        onDispose { player.stop() }
    }

    fun broadcast() {
        val paise = amountPaise ?: return
        val vpa = payeeVpa.trim()
        if (vpa.isEmpty() || vpa.toByteArray(Charsets.UTF_8).size > SonicProtocol.MAX_VPA_BYTES) return
        state = BroadcastState.SENDING
        scope.launch {
            try {
                val frame = SonicProtocol.encodePayload(vpa, paise)
                player.play(FskModulator.frameToSamples(frame))
                state = BroadcastState.SENT
            } catch (e: IllegalArgumentException) {
                state = BroadcastState.IDLE
            }
            delay(1800)
            if (state == BroadcastState.SENT) state = BroadcastState.IDLE
        }
    }

    Column(modifier = Modifier.fillMaxSize().padding(24.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(Icons.Filled.ArrowBack, contentDescription = "Back", tint = TextPrimary)
            }
            Text("Merchant", style = MaterialTheme.typography.headlineMedium, color = TextPrimary)
        }

        Spacer(Modifier.height(24.dp))

        GlassCard(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text("Amount to charge", style = MaterialTheme.typography.labelLarge, color = TextSecondary)
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = amount,
                    onValueChange = { amount = it },
                    placeholder = { Text("0.00", color = TextMuted) },
                    textStyle = MaterialTheme.typography.displayLarge.copy(color = TextPrimary),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = AccentBlue,
                        unfocusedBorderColor = TextMuted,
                        cursorColor = AccentBlue
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(16.dp))
                Text("Payee VPA", style = MaterialTheme.typography.labelLarge, color = TextSecondary)
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = payeeVpa,
                    onValueChange = { payeeVpa = it },
                    textStyle = MaterialTheme.typography.bodyLarge.copy(color = TextPrimary),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = AccentBlue,
                        unfocusedBorderColor = TextMuted,
                        cursorColor = AccentBlue
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        Spacer(Modifier.height(36.dp))

        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            PulseRings(
                color = AccentBlue,
                active = state == BroadcastState.SENDING,
                maxRadiusDp = 130
            ) {
                Box(
                    modifier = Modifier
                        .size(120.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.radialGradient(
                                listOf(
                                    if (state == BroadcastState.SENT) SuccessGreen else AccentBlue,
                                    (if (state == BroadcastState.SENT) SuccessGreen else AccentCyan).copy(alpha = 0.6f)
                                )
                            )
                        )
                        .clickable(
                            enabled = amountPaise != null &&
                                payeeVpa.isNotBlank() &&
                                state == BroadcastState.IDLE
                        ) {
                            broadcast()
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Filled.GraphicEq,
                        contentDescription = "Send",
                        tint = androidx.compose.ui.graphics.Color.White,
                        modifier = Modifier.size(44.dp)
                    )
                }
            }
        }

        Spacer(Modifier.height(28.dp))

        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            Text(
                text = when (state) {
                    BroadcastState.IDLE ->
                        if (amount.isNotBlank() && amountPaise == null) "Enter a valid amount (e.g. 28.00)"
                        else "Tap to broadcast payment request"
                    BroadcastState.SENDING -> "Broadcasting near-inaudible tone…"
                    BroadcastState.SENT -> "Sent — waiting for customer to confirm"
                },
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary
            )
        }
    }
}



