package com.example.washyourcar.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.washyourcar.models.NeedService
import com.example.washyourcar.models.NeedWhen
import com.example.washyourcar.models.Priority
import com.example.washyourcar.models.WashMatch
import com.example.washyourcar.models.WashNeeds

/**
 * "Find my wash": the client describes what they need (AI turns it into the chips below, or they tap
 * the chips directly), then sees the best match with reasons and two alternatives with trade-offs.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FindMyWashSheet(
    needs: WashNeeds,
    onNeedsChange: (WashNeeds) -> Unit,
    busy: Boolean,
    aiNote: String?,
    matches: List<WashMatch>?,
    hasLocation: Boolean,
    onAskAi: (String) -> Unit,
    onFind: () -> Unit,
    onBackToNeeds: () -> Unit,
    onOpenWash: (WashMatch) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var text by remember { mutableStateOf("") }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(
            Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp)
                .imePadding(),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            if (matches == null) {
                // ---------- Step 1: needs ----------
                Text("What do you need?", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    placeholder = { Text("e.g. quick wash in 30 min, under 50 lei, close to me") },
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                    keyboardActions = KeyboardActions(onSend = { onAskAi(text) }),
                    trailingIcon = {
                        IconButton(onClick = { onAskAi(text) }, enabled = text.isNotBlank() && !busy) {
                            Icon(Icons.Default.AutoAwesome, contentDescription = "Ask AI", tint = MaterialTheme.colorScheme.primary)
                        }
                    }
                )
                if (busy) LinearProgressIndicator(Modifier.fillMaxWidth())
                aiNote?.let {
                    Text(it, fontSize = 13.sp, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Medium)
                }

                ChipGroup("Service") {
                    NeedService.entries.forEach { s ->
                        FilterChip(
                            selected = s in needs.services,
                            onClick = { onNeedsChange(needs.copy(services = needs.services.toggle(s).ifEmpty { setOf(NeedService.EXTERIOR) })) },
                            label = { Text(s.label) }
                        )
                    }
                }
                ChipGroup("When") {
                    NeedWhen.entries.forEach { w ->
                        FilterChip(selected = needs.time == w, onClick = { onNeedsChange(needs.copy(time = w)) }, label = { Text(w.label) })
                    }
                }
                ChipGroup("What matters most") {
                    Priority.entries.forEach { p ->
                        FilterChip(
                            selected = p in needs.priorities,
                            onClick = { onNeedsChange(needs.copy(priorities = needs.priorities.toggle(p))) },
                            label = { Text(p.label) }
                        )
                    }
                }
                ChipGroup("Budget") {
                    listOf(null, 30.0, 50.0, 100.0, 200.0).forEach { b ->
                        FilterChip(
                            selected = needs.maxBudget == b,
                            onClick = { onNeedsChange(needs.copy(maxBudget = b)) },
                            label = { Text(b?.let { "≤ ${it.toInt()} RON" } ?: "Any") }
                        )
                    }
                }
                ChipGroup("Distance") {
                    listOf(null, 2.0, 5.0, 10.0).forEach { d ->
                        FilterChip(
                            selected = needs.maxDistanceKm == d,
                            onClick = { onNeedsChange(needs.copy(maxDistanceKm = d)) },
                            label = { Text(d?.let { "≤ ${it.toInt()} km" } ?: "Any") },
                            enabled = hasLocation || d == null
                        )
                    }
                }
                if (!hasLocation) {
                    Text("Turn on location to rank by distance.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }

                Button(
                    onClick = onFind,
                    enabled = !busy,
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E5BB8), contentColor = Color.White),
                    modifier = Modifier.fillMaxWidth().height(54.dp)
                ) { Text("Show my best matches", fontSize = 16.sp, fontWeight = FontWeight.Bold) }
            } else {
                // ---------- Step 2: results ----------
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onBackToNeeds) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Change needs") }
                    Text("Best for you", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                }
                needs.summary?.let { Text(it, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                if (matches.isEmpty()) {
                    Text(
                        "No car wash fits all of that right now. Try a bigger budget or distance, or a later time.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedButton(onClick = onBackToNeeds, modifier = Modifier.fillMaxWidth()) { Text("Change what I need") }
                } else {
                    MatchCard(matches.first(), isBest = true, onOpen = { onOpenWash(matches.first()) })
                    if (matches.size > 1) Text("Also good", style = MaterialTheme.typography.titleMedium)
                    matches.drop(1).take(2).forEach { m -> MatchCard(m, isBest = false, onOpen = { onOpenWash(m) }) }
                }
            }
        }
    }
}

// Horizontally scrollable chip row. (FlowRow crashes here: the Compose foundation version at runtime
// differs from the one compiled against.)
@Composable
private fun ChipGroup(title: String, content: @Composable RowScope.() -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(title, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            content = content
        )
    }
}

@Composable
private fun MatchCard(match: WashMatch, isBest: Boolean, onOpen: () -> Unit) {
    val wash = match.candidate.item
    val container = if (isBest) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
    val content = if (isBest) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
    Card(
        colors = CardDefaults.cardColors(containerColor = container, contentColor = content),
        shape = RoundedCornerShape(18.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (isBest) {
                    Icon(Icons.Default.EmojiEvents, null, tint = Color(0xFFFFB300))
                    Spacer(Modifier.width(6.dp))
                }
                Column(Modifier.weight(1f)) {
                    if (isBest) Text("BEST MATCH", fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                    Text(wash.name, fontWeight = FontWeight.Bold, fontSize = if (isBest) 20.sp else 16.sp)
                    Text(wash.type.label, fontSize = 12.sp)
                }
                Surface(shape = RoundedCornerShape(12.dp), color = MaterialTheme.colorScheme.primary) {
                    Text(
                        "${match.score}%",
                        color = MaterialTheme.colorScheme.onPrimary,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }
            Text(match.reasons.joinToString(" · "), fontSize = 14.sp)
            match.tradeOff?.let { Text(it, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary) }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                Button(onClick = onOpen, modifier = Modifier.weight(1f)) { Text(if (isBest) "Go with this one" else "View") }
                NavigateToWashButton(wash.latitude, wash.longitude, "${wash.address}, ${wash.city}")
            }
        }
    }
}

private fun <T> Set<T>.toggle(item: T): Set<T> = if (item in this) this - item else this + item
