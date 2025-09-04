package com.example.operationeignung.ui.manage

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DividerDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.operationeignung.data.database.entities.QuestionType

/** Ausgabe des Formular um Fragen mnauell hinzuzufügen **/
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditQuestionScreen(
    questionId: Int?,
    onSaveClick: () -> Unit,
    viewModel: AddEditQuestionViewModel = hiltViewModel()
) {
    // Lade ggf. bestehende Frage
    LaunchedEffect(questionId) { viewModel.load(questionId) }

    val form by viewModel.form.collectAsState()
    val categories by viewModel.categories.collectAsState()

    // Einfache Validierung
    var validationError by remember { mutableStateOf<String?>(null) }
    fun validate(): Boolean {
        if (form.questionText.isBlank()) { validationError = "Fragetext darf nicht leer sein"; return false }
        if (form.type == QuestionType.CHOICE) {
            val answersFilled = listOf(form.answerA, form.answerB, form.answerC, form.answerD).any { it.isNotBlank() }
            if (!answersFilled) { validationError = "Mindestens eine Antwort angeben"; return false }
        }
        validationError = null
        return true
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        // Titel
        AddEditHeaderTitle(questionId)

        Spacer(Modifier.height(12.dp))

        // Fragetext
        QuestionTextField(
            text = form.questionText,
            onChange = { v -> viewModel.update { it.copy(questionText = v) } }
        )

        Spacer(Modifier.height(8.dp))

        // Fragentyp (Dropdown)
        QuestionTypeDropdown(
            type = form.type,
            onPick = { t ->
                // Original-Update inkl. Rücksetzen bei Nicht-CHOICE
                viewModel.update {
                    it.copy(
                        type = t,
                        answerA = if (t == QuestionType.CHOICE) it.answerA else "",
                        answerB = if (t == QuestionType.CHOICE) it.answerB else "",
                        answerC = if (t == QuestionType.CHOICE) it.answerC else "",
                        answerD = if (t == QuestionType.CHOICE) it.answerD else "",
                        correctMask = if (t == QuestionType.CHOICE) it.correctMask else 0
                    )
                }
            }
        )

        Spacer(Modifier.height(8.dp))

        // Kategorie (Dropdown)
        CategoryDropdown(
            categories = categories,
            selectedId = form.categoryId,
            onPick = { id -> viewModel.update { it.copy(categoryId = id) } }
        )

        Spacer(Modifier.height(12.dp))

        // Frage-Code (optional)
        QuestionCodeField(
            code = form.questionCode,
            onChange = { v -> viewModel.update { it.copy(questionCode = v) } }
        )

        Spacer(Modifier.height(12.dp))

        // Antworten + Maske (nur bei CHOICE)
        ChoiceAnswersSection(
            isChoice = form.type == QuestionType.CHOICE,
            a = form.answerA, b = form.answerB, c = form.answerC, d = form.answerD,
            mask = form.correctMask,
            onChangeA = { v -> viewModel.update { it.copy(answerA = v) } },
            onChangeB = { v -> viewModel.update { it.copy(answerB = v) } },
            onChangeC = { v -> viewModel.update { it.copy(answerC = v) } },
            onChangeD = { v -> viewModel.update { it.copy(answerD = v) } },
            onToggleBit = { bit -> viewModel.toggleMask(bit) }
        )

        Spacer(Modifier.height(12.dp))

        // Lösungen (Text & Code)
        SolutionsSection(
            solutionText = form.solutionText,
            onSolutionText = { v -> viewModel.update { it.copy(solutionText = v) } },
            solutionCode = form.solutionCode,
            onSolutionCode = { v -> viewModel.update { it.copy(solutionCode = v) } }
        )

        Spacer(Modifier.height(16.dp))
        HorizontalDivider(Modifier, DividerDefaults.Thickness, DividerDefaults.color)
        Spacer(Modifier.height(12.dp))

        // Aktionen (Löschen/Abbrechen/Speichern)
        SaveDeleteRow(
            isExisting = form.id != 0,
            onDelete = { viewModel.delete(onSaveClick) },
            onCancel = onSaveClick,
            onSave = { if (validate()) viewModel.save(onSaveClick) }
        )

        Spacer(Modifier.height(8.dp))
    }

    // Validation-Dialog (Original)
    validationError?.let { msg ->
        AlertDialog(
            onDismissRequest = { validationError = null },
            confirmButton = { TextButton(onClick = { validationError = null }) { Text("OK") } },
            title = { Text("Eingabe unvollständig") },
            text = { Text(msg) }
        )
    }
}

/** Überschrift (Hinzufügen/Bearbeiten). */
@Composable
private fun AddEditHeaderTitle(questionId: Int?) {
    Text(
        text = if (questionId == null) "Frage hinzufügen" else "Frage bearbeiten",
        style = MaterialTheme.typography.headlineSmall
    )
}

/** Mehrzeiliger Fragetext. */
@Composable
private fun QuestionTextField(
    text: String,
    onChange: (String) -> Unit
) {
    OutlinedTextField(
        value = text,
        onValueChange = onChange,
        label = { Text("Fragetext") },
        minLines = 3,
        modifier = Modifier.fillMaxWidth(),
        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences)
    )
}

/** Dropdown zur Auswahl des Fragentyps. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun QuestionTypeDropdown(
    type: QuestionType,
    onPick: (QuestionType) -> Unit
) {
    var typeOpen by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(expanded = typeOpen, onExpandedChange = { typeOpen = it }) {
        OutlinedTextField(
            value = type.name,
            onValueChange = {},
            readOnly = true,
            label = { Text("Fragentyp") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = typeOpen) },
            modifier = Modifier.menuAnchor().fillMaxWidth()
        )
        ExposedDropdownMenu(expanded = typeOpen, onDismissRequest = { typeOpen = false }) {
            QuestionType.entries.forEach { t ->
                DropdownMenuItem(
                    text = { Text(t.name) },
                    onClick = { onPick(t); typeOpen = false }
                )
            }
        }
    }
}

/** Dropdown zur Auswahl der Kategorie */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CategoryDropdown(
    categories: List<com.example.operationeignung.data.database.entities.Category>,
    selectedId: Int?,
    onPick: (Int?) -> Unit
) {
    var catOpen by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(expanded = catOpen, onExpandedChange = { catOpen = it }) {
        val label = categories.firstOrNull { it.id == selectedId }?.name ?: "Ohne Kategorie"
        OutlinedTextField(
            value = label,
            onValueChange = {},
            readOnly = true,
            label = { Text("Kategorie") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = catOpen) },
            modifier = Modifier.menuAnchor().fillMaxWidth()
        )
        ExposedDropdownMenu(expanded = catOpen, onDismissRequest = { catOpen = false }) {
            DropdownMenuItem(text = { Text("— Ohne Kategorie —") }, onClick = { onPick(null); catOpen = false })
            categories.forEach { c ->
                DropdownMenuItem(
                    text = { Text(c.name) },
                    onClick = { onPick(c.id); catOpen = false },
                    enabled = c.id != selectedId
                )
            }
        }
    }
}

/** Eingabefeld für optionalen Frage‑Code. */
@Composable
private fun QuestionCodeField(
    code: String,
    onChange: (String) -> Unit
) {
    OutlinedTextField(
        value = code,
        onValueChange = onChange,
        label = { Text("Frage-Code (optional)") },
        minLines = 3,
        modifier = Modifier.fillMaxWidth()
    )
}

/** Block für Antworten A–D inkl. Korrekturmaske (nur bei CHOICE sichtbar). */
@Composable
private fun ChoiceAnswersSection(
    isChoice: Boolean,
    a: String, b: String, c: String, d: String,
    mask: Int,
    onChangeA: (String) -> Unit,
    onChangeB: (String) -> Unit,
    onChangeC: (String) -> Unit,
    onChangeD: (String) -> Unit,
    onToggleBit: (Int) -> Unit
) {
    if (!isChoice) return

    Text("Antworten & Korrekt-Markierung", style = MaterialTheme.typography.titleMedium)
    Spacer(Modifier.height(8.dp))

    // A–D (identisch zu deinem ChoiceRow-Aufbau)
    ChoiceRow(label = "A", value = a, onValueChange = onChangeA, showCheck = true,
        checked = (mask and 1) != 0, onCheckedChange = { onToggleBit(1) })
    ChoiceRow(label = "B", value = b, onValueChange = onChangeB, showCheck = true,
        checked = (mask and 2) != 0, onCheckedChange = { onToggleBit(2) })
    ChoiceRow(label = "C", value = c, onValueChange = onChangeC, showCheck = true,
        checked = (mask and 4) != 0, onCheckedChange = { onToggleBit(4) })
    ChoiceRow(label = "D", value = d, onValueChange = onChangeD, showCheck = true,
        checked = (mask and 8) != 0, onCheckedChange = { onToggleBit(8) })

    Spacer(Modifier.height(6.dp))
    Text("Korrekturmaske: $mask", style = MaterialTheme.typography.bodySmall)
}

/** Block für Lösungs‑Eingaben (Text/Code). */
@Composable
private fun SolutionsSection(
    solutionText: String,
    onSolutionText: (String) -> Unit,
    solutionCode: String,
    onSolutionCode: (String) -> Unit
) {
    Text("Lösungen", style = MaterialTheme.typography.titleMedium)
    Spacer(Modifier.height(8.dp))
    OutlinedTextField(
        value = solutionText,
        onValueChange = onSolutionText,
        label = { Text("Lösung (Text)") },
        minLines = 2,
        modifier = Modifier.fillMaxWidth()
    )
    Spacer(Modifier.height(8.dp))
    OutlinedTextField(
        value = solutionCode,
        onValueChange = onSolutionCode,
        label = { Text("Lösung (Code)") },
        minLines = 2,
        modifier = Modifier.fillMaxWidth()
    )
}

/** Untere Aktionszeile */
@Composable
private fun SaveDeleteRow(
    isExisting: Boolean,
    onDelete: () -> Unit,
    onCancel: () -> Unit,
    onSave: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        if (isExisting) {
            OutlinedButton(onClick = onDelete) { Text("Löschen") }
        } else {
            Spacer(Modifier.width(8.dp))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TextButton(onClick = onCancel) { Text("Abbrechen") }
            Button(onClick = onSave) { Text("Speichern") }
        }
    }
}


/** Helfermethode zur Ausgabe der Zeilen für MC Antworten **/
@Composable
private fun ChoiceRow(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    showCheck: Boolean,
    checked: Boolean,
    onCheckedChange: () -> Unit
) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            label = { Text("Antwort $label") },
            modifier = Modifier.weight(1f)
        )
        if (showCheck) {
            Spacer(Modifier.width(8.dp))
            Checkbox(checked = checked, onCheckedChange = { onCheckedChange() })
        }
    }
    Spacer(Modifier.height(8.dp))
}
