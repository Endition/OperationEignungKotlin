package com.example.operationeignung.ui.jsonimport

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * Zeigt den Status des Imports:
 *  - Running (Progress)
 *  - Error (mit Meldung)
 *  - Success (Report mit Importiert/Übersprungen und Fehlerliste)
 */
@Composable
fun ImportResultSection(
    report: ImportReport?,        // null = nichts anzeigen (idle)
    isRunning: Boolean,           // entspricht state.isBusy
    error: String?,               // entspricht state.error
    onRetry: () -> Unit,          // z. B. viewModel.runImport(…)
    onClose: () -> Unit,          // z. B. viewModel.reset() + input=""
    modifier: Modifier = Modifier
) {
    // 1) Anzeigezustand wählen – klar getrennt
    when {
        isRunning -> ImportRunning(modifier)
        error != null -> ImportErrorSection(
            error = error,
            onRetry = onRetry,
            onClose = onClose,
            modifier = modifier
        )
        report != null -> ImportSuccessSection(
            report = report,
            onRetry = onRetry,
            onClose = onClose,
            modifier = modifier
        )
        else -> ImportIdle(modifier) // Idle-Zustand
    }
}

@Composable
private fun ImportIdle(modifier: Modifier = Modifier) {
    Box(modifier.fillMaxWidth())
}

//Import läuft anzeigen
@Composable
private fun ImportRunning(modifier: Modifier = Modifier) {
    // Loading-State
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        CircularProgressIndicator() //  Ladeindikator
        Text("Import läuft …")     // Statusinfo
    }
}

//Anzeige der Fehlermeldung
@Composable
private fun ImportErrorSection(
    error: String,
    onRetry: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Fehlerkarte mit Aktionen
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Fehler beim Import", style = MaterialTheme.typography.titleMedium) // Überschrift
        Text(error) // Fehlermeldung
        ImportActions(onRetry = onRetry, onClose = onClose) // Buttons
    }
}

//Anzeige der Erfolgsmeldungen
@Composable
private fun ImportSuccessSection(
    report: ImportReport,
    onRetry: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    // überblick + optionale Details
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        ImportSummaryHeader(report) // Kopf mit Kennzahlen

        if (report.errors.isNotEmpty()) {
            ImportErrorsSection(
                errors = report.errors,
            )
        }

        ImportActions(onRetry = onRetry, onClose = onClose)
    }
}

// Anzeige der Zusammenfassung
@Composable
private fun ImportSummaryHeader(report: ImportReport) {
    // Anzeige der wichtigsten Zahlen
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text("Import abgeschlossen", style = MaterialTheme.typography.titleMedium) // klare Headline
        Text("Importiert: ${report.imported}")
        Text("Übersprungen: ${report.skipped}")
        Text("Fehler: ${report.errors.size}")     // dito
    }
}

// Anzeige der Fehlerliste
@Composable
private fun ImportErrorsSection(
    errors: List<ImportError>,
) {
    // Ein-/Ausklappen
    var expanded by remember { mutableStateOf(false) } // lokaler UI-State

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TextButton(onClick = { expanded = !expanded }) {
                Text(if (expanded) "Details ausblenden" else "Details anzeigen") // Toggle
            }
        }

        //Fehler anzeigen
        if (expanded) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("Fehler", style = MaterialTheme.typography.titleSmall) // Abschnittstitel
                errors.forEach { e ->
                    Text("• $e")
                }
            }
        }
    }
}

//Buttons am Ende der Statusanzeige
@Composable
private fun ImportActions(
    onRetry: () -> Unit,
    onClose: () -> Unit
) {
    //  Aktionen
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        TextButton(onClick = onRetry) { Text("Erneut versuchen") } // Aktion 1
        Button(onClick = onClose) { Text("Schließen") }            // Haupthandlung
    }
}
