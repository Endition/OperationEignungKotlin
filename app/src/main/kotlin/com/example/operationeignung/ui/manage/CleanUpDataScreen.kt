package com.example.operationeignung.ui.manage

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DividerDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import kotlinx.coroutines.launch

/**
 * Zeigt Aufräum-Tools für die DB.
 */
@Composable
fun CleanUpDataScreen(
    viewModel: CleanUpDataViewModel = hiltViewModel(),
    contentPadding: PaddingValues = PaddingValues(0.dp)
) {
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var confirmTruncate by remember { mutableStateOf(false) }

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbar) },
        modifier = Modifier.fillMaxSize()
    ) { inner ->
        Column(
            modifier = Modifier
                .padding(inner)
                .padding(contentPadding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {

            // 1) Automatische Kategorisierung
            CleanUpSection(
                title = "Alle Fragen automatisch kategorisieren",
                description = "Ordnet Fragen per Keyword-Mapping Kategorien zu. Existierende Kategorien bleiben, wo sinnvoll.",
                buttonText = "Fragen kategorisieren"
            ) {
                scope.launch {
                    val updated = viewModel.autoCategorize()
                    snackbar.showSnackbar("$updated Fragen wurden automatisch kategorisiert.")
                }
            }

            HorizontalDivider(Modifier, DividerDefaults.Thickness, DividerDefaults.color)

            // 2) Bereinigung (Duplikate/Leere)
            CleanUpSection(
                title = "“Leere”/duplizierte Fragen entfernen",
                description = "Löscht exakte/nahe Duplikate, ungültige Choice-Fragen (alle leer) und offensichtlichen Müll.",
                buttonText = "Datenbank bereinigen"
            ) {
                scope.launch {
                    val deleted = viewModel.cleanup()
                    snackbar.showSnackbar("$deleted Fragen automatisch bereinigt.")
                }
            }

            HorizontalDivider(Modifier, DividerDefaults.Thickness, DividerDefaults.color)

            // 3) Statistik zurücksetzen
            CleanUpSection(
                title = "Statistik zurücksetzen",
                description = "Setzt alle richtig/falsch-Zähler auf 0.",
                buttonText = "Statistiken zurücksetzen"
            ) {
                scope.launch {
                    val count = viewModel.resetStats()
                    snackbar.showSnackbar("Statistiken für $count Fragen zurückgesetzt.")
                }
            }

            HorizontalDivider(Modifier, DividerDefaults.Thickness, DividerDefaults.color)

            // 4) DB leeren (gefährlich)
            DangerZone(
                onRequestTruncate = { confirmTruncate = true }
            )
        }

        if (confirmTruncate) {
            ConfirmDangerDialog(
                title = "Wirklich alle Fragen löschen?",
                message = "Diese Aktion löscht alle Fragen unwiderruflich. Willst du das wirklich tun?",
                onConfirm = {
                    scope.launch {
                        val remaining = viewModel.truncateQuestions()
                        snackbar.showSnackbar("Datenbank geleert. Verbleibend: $remaining")
                    }
                    confirmTruncate = false
                },
                onDismiss = { confirmTruncate = false }
            )
        }
    }
}

/** Wiederverwendbare Sektion mit Titel, Beschreibung und Single-Action-Button. */
@Composable
private fun CleanUpSection(
    title: String,
    description: String,
    buttonText: String,
    onClick: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(title, style = MaterialTheme.typography.titleMedium)
        Text(description, style = MaterialTheme.typography.bodyMedium)
        Button(onClick = onClick) { Text(buttonText) }
    }
}

/** Brandgefährlicher Bereich für irreversible Aktionen. */
@Composable
private fun DangerZone(
    onRequestTruncate: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Danger Zone", style = MaterialTheme.typography.titleMedium)
        Text("Löscht ALLE Fragen.")
        Button(
            onClick = onRequestTruncate,
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
        ) {
            Text("Alle Fragen löschen")
        }
    }
}

/** Bestätigungsdialog für gefährliche Dinge */
@Composable
private fun ConfirmDangerDialog(
    title: String,
    message: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text  = { Text(message) },
        confirmButton = { TextButton(onClick = onConfirm) { Text("Löschen") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Abbrechen") } }
    )
}
