package com.sonicpay.app.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Hearing
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.sonicpay.app.audio.PaymentToneListener
import com.sonicpay.app.data.HistoryEntry
import com.sonicpay.app.data.SessionPrefs
import com.sonicpay.app.sonic.SonicProtocol
import com.sonicpay.app.ui.components.GlassCard
import com.sonicpay.app.ui.components.PulseRings
import com.sonicpay.app.ui.components.SonicOrb
import com.sonicpay.app.ui.components.OrbState
import com.sonicpay.app.ui.components.TopBar
import com.sonicpay.app.ui.theme.AccentMint
import com.sonicpay.app.ui.theme.TextMuted
import com.sonicpay.app.ui.theme.TextPrimary
import com.sonicpay.app.ui.theme.TextSecondary
import com.sonicpay.app.ui.theme.WarnAmber

private enum class Listen { IDLE, LISTENING, RECEIVED, CONFIRMED }

private data class Incoming(val merchantName: String, val vpa: String, val amount: String)

private fun displayNameForVpa(vpa: String): String =
    vpa.substringBefore('@').replaceFirstChar { it.uppercaseChar() }

@Composable
fun CustomerScreen(onOpenSettings: () -> Unit, onOpenHistory: () -> Unit) {
    val context = LocalContext.current
    val haptics = LocalHapticFeedback.current
    var state by remember { mutableStateOf(Listen.LISTENING) }
    var permissionDenied by remember { mutableStateOf(false) }
    var incoming by remember { mutableStateOf<Incoming?>(null) }

    val listener = remember {
        PaymentToneListener { vpa, amountPaise ->
            incoming = Incoming(
                displayNameForVpa(vpa),
                vpa,
                SonicProtocol.formatAmount(amountPaise)
            )
            state = Listen.RECEIVED
        }
    }

    fun beginListening(): Boolean {
        val started = listener.start()
        permissionDenied = !started
        return started
    }

    LaunchedEffect(Unit) {
        val granted = ContextCompat.checkSelfPermission(
            context, Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
        if (granted) {
            state = if (beginListening()) Listen.LISTENING else Listen.IDLE
        } else {
            state = Listen.IDLE
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        state = if (granted && beginListening()) Listen.LISTENING else Listen.IDLE
    }

    DisposableEffect(Unit) {
        onDispose { listener.stop() }
    }

    Column(modifier = Modifier.fillMaxSize().padding(24.dp)) {
        TopBar(
            title = "Customer",
            actions = {
                IconButton(onClick = onOpenHistory) {
                    Icon(Icons.Filled.History, contentDescription = "History", tint = TextSecondary)
                }
                IconButton(onClick = onOpenSettings) {
                    Icon(Icons.Filled.Settings, contentDescription = "Settings", tint = TextSecondary)
                }
            }
        )

        Spacer(Modifier.weight(0.7f))

        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            PulseRings(color = AccentMint, active = state == Listen.LISTENING, maxRadiusDp = 130) {
                SonicOrb(
                    icon = Icons.Filled.Hearing,
                    state = when {
                        state == Listen.LISTENING || state == Listen.RECEIVED -> OrbState.ACTIVE
                        else -> OrbState.IDLE
                    },
                    tint = AccentMint,
                    enabled = state == Listen.IDLE,
                    onClick = {
                        val granted = ContextCompat.checkSelfPermission(
                            context, Manifest.permission.RECORD_AUDIO
                        ) == PackageManager.PERMISSION_GRANTED
                        if (granted) {
                            state = if (beginListening()) Listen.LISTENING else Listen.IDLE
                        } else {
                            permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                        }
                    }
                )
            }
        }

        Spacer(Modifier.height(24.dp))

        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            Text(
                text = when {
                    permissionDenied -> "Microphone permission is needed to hear requests"
                    state == Listen.IDLE -> "Tap to start listening"
                    state == Listen.LISTENING -> "Listening… hold your phone near the merchant"
                    state == Listen.RECEIVED -> "Request received — confirm below"
                    else -> "Paid"
                },
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary
            )
        }

        Spacer(Modifier.weight(1f))

        AnimatedVisibility(
            visible = state == Listen.RECEIVED && incoming != null,
            enter = fadeIn() + expandVertically(spring(dampingRatio = Spring.DampingRatioLowBouncy)),
            exit = fadeOut() + shrinkVertically()
        ) {
            incoming?.let { req ->
                ConfirmCard(
                    request = req,
                    onConfirm = {
                        listener.stop()
                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                        SessionPrefs.addHistory(
                            HistoryEntry(req.vpa, SonicProtocol.parseAmountToPaise(req.amount) ?: 0L,
                                System.currentTimeMillis(), "customer")
                        )
                        state = Listen.CONFIRMED
                    },
                    onReject = {
                        listener.stop()
                        state = Listen.IDLE
                        incoming = null
                    }
                )
            }
        }

        AnimatedVisibility(
            visible = state == Listen.CONFIRMED,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            GlassCard(modifier = Modifier.fillMaxWidth(), sheen = true) {
                Row(
                    modifier = Modifier.padding(20.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Filled.CheckCircle, null, tint = AccentMint, modifier = Modifier.size(26.dp))
                    Spacer(Modifier.width(12.dp))
                    Text(
                        "Paid ₹${incoming?.amount} to ${incoming?.merchantName}",
                        color = TextPrimary,
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            }
        }

        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun ConfirmCard(request: Incoming, onConfirm: () -> Unit, onReject: () -> Unit) {
    GlassCard(modifier = Modifier.fillMaxWidth(), sheen = true) {
        Column(modifier = Modifier.padding(22.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(AccentMint.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Filled.Storefront, null, tint = AccentMint, modifier = Modifier.size(22.dp))
                }
                Spacer(Modifier.width(12.dp))
                Column {
                    Text(request.merchantName, style = MaterialTheme.typography.titleLarge, color = TextPrimary)
                    Text(request.vpa, style = MaterialTheme.typography.labelMedium, color = TextMuted)
                }
            }

            Spacer(Modifier.height(16.dp))
            Text("Amount", style = MaterialTheme.typography.labelLarge, color = TextSecondary)
            Text(
                "₹${request.amount}",
                style = MaterialTheme.typography.displayLarge,
                color = TextPrimary,
                fontWeight = FontWeight.Black
            )

            Spacer(Modifier.height(6.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.Hearing, null, tint = WarnAmber, modifier = Modifier.size(13.dp))
                Spacer(Modifier.width(6.dp))
                Text(
                    "Verify this is the shop you meant to pay.",
                    style = MaterialTheme.typography.labelMedium,
                    color = WarnAmber
                )
            }

            Spacer(Modifier.height(16.dp))
            Row(modifier = Modifier.fillMaxWidth()) {
                OutlinedButton(onClick = onReject, modifier = Modifier.weight(1f)) {
                    Text("Cancel")
                }
                Spacer(Modifier.width(12.dp))
                Button(
                    onClick = onConfirm,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = AccentMint,
                        contentColor = androidx.compose.ui.graphics.Color(0xFF05060A)
                    ),
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Confirm & Pay", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
