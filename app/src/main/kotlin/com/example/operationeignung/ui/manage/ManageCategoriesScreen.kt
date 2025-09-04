package com.example.operationeignung.ui.manage

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Clear
import androidx.compose.material.icons.outlined.CreateNewFolder
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.operationeignung.data.database.entities.Category

/** Ausgabe des Screen zur Verwaltung der Kategorien **/
@Composable
fun ManageCategoriesScreen(
    contentPadding: PaddingValues = PaddingValues(),
    viewModel: ManageCategoriesViewModel = hiltViewModel()
) {
    val cats by viewModel.categories.collectAsState()

    // lokaler UI-State
    var newName by remember { mutableStateOf("") }
    var selected by remember { mutableStateOf(setOf<Int>()) }       // max 2
    var showMergeDialog by remember { mutableStateOf(false) }
    var mergeTargetId by remember { mutableStateOf<Int?>(null) }
    var lastInfo by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(contentPadding)
            .padding(16.dp)
    ) {
        // Kopfbereich: Eingabe + Aktionen
        ManageCategoriesHeader(
            newName = newName,
            onNewNameChange = { newName = it },
            onAdd = {
                viewModel.add(newName)
                newName = ""           // Feld leeren
            },
            onDeleteUnused = { viewModel.deleteUnused() }
        )

        Spacer(Modifier.height(12.dp))
        HorizontalDivider()
        Spacer(Modifier.height(12.dp))

        // Liste der Kategorien + Checkbox-Logik (max. 2)
        ManageCategoriesList(
            categories = cats,
            selected = selected,
            onToggleChecked = { cat ->
                val checked = selected.contains(cat.id)
                // max. 2 erlauben, ansonsten entfernen/ersetzen
                selected = if (checked) {
                    selected - cat.id
                } else {
                    (selected + cat.id).take(2).toSet()
                }
                // Vorbelegung für Merge-Ziel bei genau 2 ausgewählten
                mergeTargetId = if (selected.size == 2) selected.last() else null
            },
            onDelete = { cat -> viewModel.delete(cat) } // Löschen-Button wie zuvor
        )

        // Merge-Aktion bei genau 2 Auswahlen (Dialog-Trigger)
        ManageCategoriesMergeActions(
            enabled = selected.size == 2,
            onCancel = {
                selected = emptySet()
                mergeTargetId = null
            },
            onConfirmRequest = { showMergeDialog = true }
        )

        // Ergebnis-/Statuszeile (z. B. „Verschmolzen:“)
        lastInfo?.let {
            Spacer(Modifier.height(8.dp))
            Text(it, color = MaterialTheme.colorScheme.primary)
        }
    }

    // Merge-Dialog
    if (showMergeDialog && selected.size == 2) {
        val (aId, bId) = selected.toList()
        MergeCategoriesDialog(
            aId = aId,
            bId = bId,
            categories = cats,
            initialTargetId = mergeTargetId,
            onDismiss = { showMergeDialog = false },
            onConfirm = { target ->
                val source = (selected - target).first()
                viewModel.mergeCategories(sourceId = source, targetId = target) { moved ->
                    // UI-Feedback
                    lastInfo = "Verschmolzen: $moved Frage(n) umgehängt"
                    // zurücksetzen
                    showMergeDialog = false
                    selected = emptySet()
                    mergeTargetId = null
                }
            }
        )
    }
}

/** Kopfbereich mit Eingabefeld und Hauptaktionen (Add / Unbenutzte löschen). */
@Composable
private fun ManageCategoriesHeader(
    newName: String,
    onNewNameChange: (String) -> Unit,
    onAdd: () -> Unit,
    onDeleteUnused: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        OutlinedTextField(
            value = newName,
            onValueChange = onNewNameChange,
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            placeholder = { Text("z. B. Anatomie") },
            leadingIcon = {
                Icon(
                    Icons.Outlined.CreateNewFolder,
                    contentDescription = null
                )
            },
            trailingIcon = {
                if (newName.isNotEmpty()) {
                    IconButton(onClick = { onNewNameChange("") }) {
                        Icon(Icons.Outlined.Clear, contentDescription = "Leeren")
                    }
                }
            },
            supportingText = {
                if (newName.isBlank()) Text("Gib einen Kategorienamen ein.")
            }
        )

        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Primär: Hinzufügen
            Button(onClick = onAdd, enabled = newName.isNotBlank()) { Text("Hinzufügen") }

            // Sekundär: Unbenutzte löschen
            FilledTonalButton(onClick = onDeleteUnused) { Text("Unbenutzte löschen") }
        }
    }
}

/** Kategorienliste */
@Composable
private fun ManageCategoriesList(
    categories: List<Category>,
    selected: Set<Int>,
    onToggleChecked: (Category) -> Unit,
    onDelete: (Category) -> Unit
) {
    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        items(categories, key = { it.id }) { cat ->
            val checked = selected.contains(cat.id)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Auswahlbox zum Verschmelzen
                    Checkbox(
                        checked = checked,
                        onCheckedChange = { onToggleChecked(cat) }
                    )
                    Text(cat.name)
                }
                // Löschen‑Button
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = { onDelete(cat) }) { Text("Löschen") }
                }
            }
        }
    }
}

/** Fußbereich mit „Abbrechen“ und „Verschmelzen */
@Composable
private fun ManageCategoriesMergeActions(
    enabled: Boolean,
    onCancel: () -> Unit,
    onConfirmRequest: () -> Unit
) {
    if (!enabled) return // nichts anzeigen

    Spacer(Modifier.height(12.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedButton(onClick = onCancel) { Text("Abbrechen") }
        Button(onClick = onConfirmRequest, enabled = enabled) { Text("Verschmelzen") }
    }
}


/** Ausgabe des MergerDialogs **/
@Composable
private fun MergeCategoriesDialog(
    aId: Int,
    bId: Int,
    categories: List<Category>,
    initialTargetId: Int?,
    onDismiss: () -> Unit,
    onConfirm: (targetId: Int) -> Unit
) {
    var targetId by remember { mutableStateOf(initialTargetId ?: aId) }

    val a = categories.firstOrNull { it.id == aId }
    val b = categories.firstOrNull { it.id == bId }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Kategorien verschmelzen") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Wähle die Zielkategorie (bleibt erhalten):")
                Row(verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(
                        selected = targetId == aId,
                        onClick = { targetId = aId }
                    )
                    Text(a?.name ?: "Kategorie $aId")
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(
                        selected = targetId == bId,
                        onClick = { targetId = bId }
                    )
                    Text(b?.name ?: "Kategorie $bId")
                }
            }
        },
        confirmButton = {
            Button(onClick = { onConfirm(targetId) }) { Text("Verschmelzen") }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) { Text("Abbrechen") }
        }
    )
}
