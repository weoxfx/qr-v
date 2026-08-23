package com.sonicpay.app.ui.screens

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowOutward
import androidx.compose.material.icons.filled.SouthWest
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.sonicpay.app.data.SessionPrefs
import com.sonicpay.app.sonic.SonicProtocol
import com.sonicpay.app.ui.components.GlassCard
import com.sonicpay.app.ui.components.TopBar
import com.sonicpay.app.ui.theme.AccentMint
import com.sonicpay.app.ui.theme.TextMuted
import com.sonicpay.app.ui.theme.TextPrimary
import com.sonicpay.app.ui.theme.TextSecondary
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun HistoryScreen(onBack: () -> Unit) {
    val entries = SessionPrefs.history
    val fmt = SimpleDateFormat("d MMM, h:mm a", Locale.getDefault())

    Column(modifier = Modifier.fillMaxSize().padding(24.dp)) {
        TopBar(title = "History", onBack = onBack)
        Spacer(Modifier.height(18.dp))

        if (entries.isEmpty()) {
            Spacer(Modifier.weight(1f))
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                Text(
                    "Confirmed payments will show up here",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextMuted
                )
            }
            Spacer(Modifier.weight(1f))
        } else {
            LazyColumn(verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(10.dp)) {
                items(entries, key = { it.timestampMs }) { e ->
                    GlassCard(modifier = Modifier.fillMaxWidth(), cornerRadius = 20) {
                        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .background(AccentMint.copy(alpha = 0.13f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    if (e.role == "customer") Icons.Filled.SouthWest else Icons.Filled.ArrowOutward,
                                    null,
                                    tint = AccentMint,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Spacer(Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    e.vpa,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = TextPrimary
                                )
                                Text(
                                    fmt.format(Date(e.timestampMs)),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = TextMuted
                                )
                            }
                            Text(
                                "₹" + SonicProtocol.formatAmount(e.amountPaise),
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                        }
                    }
                }
            }
        }
    }
}
