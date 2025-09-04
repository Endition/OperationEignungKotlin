package com.example.operationeignung.ui.dashboard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.DividerDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.operationeignung.data.database.dao.CategoryStatRow
import com.example.operationeignung.ui.navigation.Screen


/** Zeigt das Dashboard mit Kennzahlen, Kategorien und Schnellaktionen. */
@Composable
fun DashboardScreen(
    contentPadding: PaddingValues = PaddingValues(),
    onNavigate: (String) -> Unit,
    viewModel: DashboardViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    // Empty-State: Keine Fragen vorhanden → Hinweis & Shortcuts
    if (state.total == 0) {
        DashboardEmptyState(
            onNavigate = onNavigate,
            modifier = Modifier
                .padding(contentPadding)
                .padding(16.dp)
        )
        return
    }

    Column(
        modifier = Modifier
            .padding(contentPadding)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Kennzahlen (ausgelagert)
        DashboardStatsRow(
            total = state.total,
            correct = state.correct,
            wrong = state.wrong
        )

        // Kategorien-Tabelle: nutzt deine bestehende Composable
        // (Signatur ggf. anpassen, falls deine CategoryTable andere Parameter erwartet)
        CategoryTable(state.categoryRows)

        // Schnellaktionen (ausgelagert)
        DashboardActions(
            onNavigate = onNavigate,
        )
    }
}

/* Zeigt die drei KPI-Chips (Fragen, Richtig, Falsch) in einer Zeile. */
@Composable
fun DashboardStatsRow(
    total: Int,
    correct: Int,
    wrong: Int,
    modifier: Modifier = Modifier
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = modifier.fillMaxWidth()
    ) {
        StatChip(label = "Fragen", value = total.toString(), modifier = Modifier.weight(1f))
        StatChip(label = "Richtig", value = correct.toString(), modifier = Modifier.weight(1f))
        StatChip(label = "Falsch", value = wrong.toString(), modifier = Modifier.weight(1f))
    }
}

/* Ein einzelner KPI-Chip (kleine Karte) mit Label und Wert. */
@Composable
private fun StatChip(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    ElevatedCard(modifier) {
        Column(
            Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                value,
                style = MaterialTheme.typography.headlineSmall
            )
        }
    }
}

/* Zeigt die zwei Hauptaktionen: "Quiz starten" und "JSON importieren". */
@Composable
fun DashboardActions(
    onNavigate: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = modifier.fillMaxWidth()
    ) {
        Button(
            onClick = {onNavigate(Screen.Quiz.route)},
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Quiz starten")
        }
        OutlinedButton(
            onClick = { onNavigate(Screen.Import.route)},
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Fragen importieren")
        }
    }
}

/* Zeigt einen Willkommens-/Leerzustand, wenn noch keine Fragen vorhanden sind. */
@Composable
fun DashboardEmptyState(
    onNavigate: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(modifier) {
        Column(
            Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("Es sind noch keine Fragen vorhanden.")
            Text("Du musst zuerst Fragen anlegen oder importieren (empfohlen).")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = {onNavigate(Screen.Import.route)}) { Text("Zum Import") }
                OutlinedButton(onClick = {onNavigate(Screen.AddEditQuestion.route)}) { Text("Fragen anlegen") }
            }
            Spacer(Modifier.height(4.dp))
        }
    }
}

/** Tabelle mit Kategorien und Statistik */
@Composable
private fun CategoryTable(rows: List<CategoryStatRow>) {
    Card(Modifier.fillMaxWidth()) {
        // LazyColumn für scrollbare Tabelle
        androidx.compose.foundation.lazy.LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(0.dp)
        ) {
            // HEADER
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    HeaderCell("Kat.", Col.NAME)
                    HeaderCell("Sum", Col.CNT, alignEnd = true)
                    HeaderCell("Neu", Col.NEW, alignEnd = true)
                    HeaderCell("✓", Col.OK, alignEnd = true)
                    HeaderCell("✗", Col.BAD, alignEnd = true)
                }
                HorizontalDivider(
                    thickness = DividerDefaults.Thickness,
                    color = DividerDefaults.color
                )
            }

            // BODY
            items(items = rows, key = { it.category }) { r ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    BodyCell(r.category,                  Col.NAME)
                    BodyCell(r.cnt.toString(),            Col.CNT, alignEnd = true, numeric = true)
                    BodyCell(r.neverAnswered.toString(),  Col.NEW, alignEnd = true, numeric = true)
                    BodyCell(r.sumCorrect.toString(),     Col.OK,  alignEnd = true, numeric = true)
                    BodyCell(r.sumWrong.toString(),       Col.BAD, alignEnd = true, numeric = true)
                }
            }
        }
    }
}

// "Header" der Tabelle
@Composable
private fun RowScope.HeaderCell(
    text: String,
    weight: Float,
    alignEnd: Boolean = false
) {
    Text(
        text = text, // kein .uppercase(), spart Breite
        modifier = Modifier
            .weight(weight)
            .padding(horizontal = 8.dp, vertical = 6.dp),
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
        textAlign = if (alignEnd) TextAlign.End else TextAlign.Start
    )
}

// "Body" der Tabelle
@Composable
private fun RowScope.BodyCell(
    text: String,
    weight: Float,
    alignEnd: Boolean = false,
    numeric: Boolean = false
) {
    Text(
        text = text,
        modifier = Modifier
            .weight(weight)
            .padding(horizontal = 8.dp, vertical = 8.dp),
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        style = if (numeric)
            MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace)
        else
            MaterialTheme.typography.bodyMedium,
        textAlign = if (alignEnd) TextAlign.End else TextAlign.Start
    )
}

// Einheitliche Spaltenaufteilung (Summe = 1f)
private object Col {
    const val NAME = 0.40f
    const val CNT  = 0.15f
    const val NEW  = 0.15f
    const val OK   = 0.15f
    const val BAD  = 0.15f
}
