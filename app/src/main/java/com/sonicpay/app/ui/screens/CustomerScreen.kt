package com.sonicpay.app.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.sonicpay.app.ui.components.GlassCard
import com.sonicpay.app.ui.components.PulseRings
import com.sonicpay.app.ui.theme.AccentBlue
import com.sonicpay.app.ui.theme.AccentViolet
import com.sonicpay.app.ui.theme.SuccessGreen
import com.sonicpay.app.ui.theme.TextMuted
import com.sonicpay.app.ui.theme.TextPrimary
import com.sonicpay.app.ui.theme.TextSecondary
import com.sonicpay.app.ui.theme.WarnAmber
import kotlinx.coroutines.delay

private enum class ListenState { IDLE, LISTENING, RECEIVED, CONFIRMED }

// Mock resolved merchant — stands in for the real audio-decoded + directory-lookup result.
private data class IncomingRequest(val merchantName: String, val vpa: String, val amount: String)

@Composable
fun CustomerScreen(onBack: () -> Unit) {
    var state by remember { mutableStateOf(ListenState.IDLE) }
    var incoming by remember { mutableStateOf<IncomingRequest?>(null) }

    LaunchedEffect(state) {
        if (state == ListenState.LISTENING) {
            delay(2200) // placeholder for real mic listen + ggwave decode latency
            incoming = IncomingRequest("Manas General Store", "mans@jd", "28.00")
            state = ListenState.RECEIVED
        }
    }

    Column(modifier = Modifier.fillMaxSize().padding(24.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(Icons.Filled.ArrowBack, contentDescription = "Back", tint = TextPrimary)
            }
            Text("Customer", style = MaterialTheme.typography.headlineMedium, color = TextPrimary)
        }

        Spacer(Modifier.height(48.dp))

        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            PulseRings(
                color = AccentViolet,
                active = state == ListenState.LISTENING,
                maxRadiusDp = 130
            ) {
                Box(
                    modifier = Modifier
                        .size(120.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.radialGradient(
                                listOf(AccentViolet, AccentBlue.copy(alpha = 0.6f))
                            )
                        )
                        .clickable(enabled = state == ListenState.IDLE) {
                            state = ListenState.LISTENING
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Filled.Sensors,
                        contentDescription = "Listen",
                        tint = Color.White,
                        modifier = Modifier.size(44.dp)
                    )
                }
            }
        }

        Spacer(Modifier.height(28.dp))

        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            Text(
                text = when (state) {
                    ListenState.IDLE -> "Tap to start listening for a payment"
                    ListenState.LISTENING -> "Listening… hold your phone near the merchant"
                    ListenState.RECEIVED -> "Payment request received"
                    ListenState.CONFIRMED -> "Payment confirmed"
                },
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary
            )
        }

        Spacer(Modifier.height(24.dp))

        AnimatedVisibility(
            visible = state == ListenState.RECEIVED && incoming != null,
            enter = fadeIn(tween(300)) + expandVertically(tween(300)),
            exit = fadeOut(tween(200)) + shrinkVertically(tween(200))
        ) {
            incoming?.let { req ->
                ConfirmCard(
                    request = req,
                    onConfirm = { state = ListenState.CONFIRMED },
                    onReject = { state = ListenState.IDLE; incoming = null }
                )
            }
        }

        AnimatedVisibility(
            visible = state == ListenState.CONFIRMED,
            enter = fadeIn(tween(300)),
            exit = fadeOut(tween(200))
        ) {
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.padding(20.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = SuccessGreen, modifier = Modifier.size(28.dp))
                    Spacer(Modifier.width(12.dp))
                    Text("Paid ₹${incoming?.amount} to ${incoming?.merchantName}", color = TextPrimary, style = MaterialTheme.typography.bodyLarge)
                }
            }
        }
    }
}

@Composable
private fun ConfirmCard(
    request: IncomingRequest,
    onConfirm: () -> Unit,
    onReject: () -> Unit
) {
    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(22.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(AccentBlue.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Filled.Storefront, contentDescription = null, tint = AccentBlue, modifier = Modifier.size(22.dp))
                }
                Spacer(Modifier.width(12.dp))
                Column {
                    Text(request.merchantName, style = MaterialTheme.typography.titleLarge, color = TextPrimary)
                    Text(request.vpa, style = MaterialTheme.typography.labelMedium, color = TextMuted)
                }
            }

            Spacer(Modifier.height(18.dp))
            Text("Amount", style = MaterialTheme.typography.labelLarge, color = TextSecondary)
            Text("₹${request.amount}", style = MaterialTheme.typography.displayLarge, color = TextPrimary, fontWeight = FontWeight.Black)

            Spacer(Modifier.height(6.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.Sensors, contentDescription = null, tint = WarnAmber, modifier = Modifier.size(14.dp))
                Spacer(Modifier.width(6.dp))
                Text(
                    "Always verify this is the shop you're paying before confirming.",
                    style = MaterialTheme.typography.labelMedium,
                    color = WarnAmber
                )
            }

            Spacer(Modifier.height(18.dp))
            Row(modifier = Modifier.fillMaxWidth()) {
                OutlinedButton(
                    onClick = onReject,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Cancel")
                }
                Spacer(Modifier.width(12.dp))
                Button(
                    onClick = onConfirm,
                    colors = ButtonDefaults.buttonColors(containerColor = SuccessGreen),
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Confirm & Pay", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
