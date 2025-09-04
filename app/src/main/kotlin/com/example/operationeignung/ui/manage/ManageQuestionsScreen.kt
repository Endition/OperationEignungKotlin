package com.example.operationeignung.ui.manage

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DividerDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.operationeignung.data.database.entities.Category
import com.example.operationeignung.data.database.entities.Question
import com.example.operationeignung.data.database.entities.QuestionType
import com.example.operationeignung.ui.common.LocalSnackbarHostState
import kotlinx.coroutines.launch

/** Stellte den Fragen verwalten Screen bereit*/
@Composable
fun ManageQuestionsScreen(
    contentPadding: PaddingValues,
    viewModel: ManageQuestionsViewModel = hiltViewModel()
) {
    val scope = rememberCoroutineScope()
    val questions by viewModel.allQuestions.collectAsState()
    val categories by viewModel.categories.collectAsState()

    val snackbar = LocalSnackbarHostState.current
    val error = viewModel.errorMessage

    var showFilter by remember { mutableStateOf(false) }
    var editTarget by remember { mutableStateOf<Question?>(null) }
    var showEditor by remember { mutableStateOf(false) }

    // Snackbar bei Fehlern anzeigen
    LaunchedEffect(error) {
        if (!error.isNullOrBlank()) {
            snackbar.showSnackbar(error)
            viewModel.clearError()
        }
    }

    // lokaler Helper: Editor öffnen (neu/ändern)
    fun openEditor(q: Question?) { editTarget = q; showEditor = true }

    ManageQuestionsContent(
        contentPadding = contentPadding,
        search = viewModel.search,
        onSearchChange = { newValue -> viewModel.search = newValue },
        onOpenFilter = { showFilter = true },
        onCreateNew = { openEditor(null) }, // „Neue Frage“
        filtered = remember(questions, viewModel.search, viewModel.filterType, viewModel.filterCategoryId) {
            viewModel.filtered(questions)
        },
        categories = categories,
        onOpenEditor = { q -> openEditor(q) } // Edit-Button
    )

    // Filterdialog
    if (showFilter) {
        ManageQuestionsFilterDialog(
            categories = categories,
            currentType = viewModel.filterType,
            currentCategoryId = viewModel.filterCategoryId,
            onDismiss = { showFilter = false },
            onApply = { t, catId ->
                viewModel.filterType = t
                viewModel.filterCategoryId = catId
                showFilter = false
            }
        )
    }

    // Editor als BottomSheet
    if (showEditor) {
        QuestionEditorSheet(
            initial = editTarget,
            categories = categories.map { it.id to it.name },
            onDismiss = { showEditor = false },
            onDelete = editTarget?.let { q ->
                { scope.launch { viewModel.delete(q); showEditor = false } }
            },
            onSave = { updated ->
                scope.launch { viewModel.upsert(updated); showEditor = false }
            }
        )
    }
}

/**
 * Enthält Suche, Aktionsleiste und die Fragenliste.
 */
@Composable
private fun ManageQuestionsContent(
    contentPadding: PaddingValues,
    search: String,
    onSearchChange: (String) -> Unit,
    onOpenFilter: () -> Unit,
    onCreateNew: () -> Unit,
    filtered: List<Question>,
    categories: List<Category>,
    onOpenEditor: (Question?) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(contentPadding)
            .padding(16.dp)
    ) {
        // Suche
        ManageQuestionsSearchField(
            value = search,
            onValueChange = onSearchChange
        )

        Spacer(Modifier.height(8.dp))

        // Aktionsleiste „Filter“ + „Neue Frage“
        ManageQuestionsActionRow(
            onOpenFilter = onOpenFilter,
            onCreateNew = onCreateNew
        )

        Spacer(Modifier.height(12.dp))

        // Liste mit Items
        ManageQuestionsList(
            questions = filtered,
            categories = categories,
            onOpenEditor = onOpenEditor
        )
    }
}

/** Einfache Suchleiste für den Fragetext. */
@Composable
private fun ManageQuestionsSearchField(
    value: String,
    onValueChange: (String) -> Unit
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text("Suche im Fragetext") },
        modifier = Modifier.fillMaxWidth(),
        singleLine = true
    )
}

/** Aktionsleiste mit Filter- und Neu-Button. */
@Composable
private fun ManageQuestionsActionRow(
    onOpenFilter: () -> Unit,
    onCreateNew: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Spacer(Modifier.weight(1f)) // rechter Block ausrichten
        IconButton(onClick = onOpenFilter) {
            Icon(Icons.Default.FilterList, contentDescription = "Filter")
        }
        IconButton(onClick = onCreateNew) {
            Icon(Icons.Default.Add, contentDescription = "Neue Frage")
        }
    }
}

/**
 * Listet Fragen in einer LazyColumn auf.
 */
@Composable
private fun ManageQuestionsList(
    questions: List<Question>,
    categories: List<Category>,
    onOpenEditor: (Question?) -> Unit
) {
    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        items(
            items = questions,
            key = { q: Question -> q.id }
        ) { q: Question ->
            ElevatedCard(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(12.dp)) {

                    // Kopfzeile mit Typ + Kategorie + Bearbeiten-Button
                    ManageQuestionsItemHeader(
                        q = q,
                        categoryName = categories.firstOrNull { it.id == q.categoryId }?.name,
                        onEdit = { onOpenEditor(q) }
                    )

                    Spacer(Modifier.height(4.dp))

                    // Fragetext-Vorschau
                    Text(q.questionText, style = MaterialTheme.typography.titleMedium)

                    // „Code vorhanden“ nur bei Bedarf
                    if (q.questionCode.isNotBlank()) {
                        Spacer(Modifier.height(2.dp))
                        Text("Code vorhanden", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
    }
}

/**
 * Kopfzeile eines Fragen-Items mit Typ-Chip, optionaler Kategoriename und Bearbeiten-Icon.
 */
@Composable
private fun ManageQuestionsItemHeader(
    q: Question,
    categoryName: String?,
    onEdit: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        AssistChip(onClick = {}, label = { Text(q.type.name) })
        categoryName?.let { name ->
            Spacer(Modifier.height(2.dp))
            AssistChip(onClick = {}, label = { Text(name) })
        }
        Spacer(Modifier.weight(1f))
        IconButton(onClick = onEdit) {
            Icon(Icons.Default.Edit, contentDescription = "Bearbeiten")
        }
    }
}



//Filter-Dialog
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ManageQuestionsFilterDialog(
    categories: List<Category>,
    currentType: QuestionType?,
    currentCategoryId: Int?,
    onDismiss: () -> Unit,
    onApply: (QuestionType?, Int?) -> Unit
) {
    var type by remember { mutableStateOf(currentType) }
    var catId by remember { mutableStateOf(currentCategoryId) }

    //Filterdialog
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Filter") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Fragentyp")
                FlowRow {
                    FilterChip(
                        selected = type == null,
                        onClick = { type = null },
                        label = { Text("Alle") }
                    )
                    Spacer(Modifier.width(8.dp))
                    QuestionType.entries.forEach { t ->
                        FilterChip(
                            selected = type == t,
                            onClick = { type = t },
                            label = { Text(t.name) }
                        )
                        Spacer(Modifier.width(8.dp))
                    }
                }

                Spacer(Modifier.height(8.dp))
                Text("Kategorie")

                var open by remember { mutableStateOf(false) }
                ExposedDropdownMenuBox(expanded = open, onExpandedChange = { open = it }) {
                    val label = categories.firstOrNull { it.id == catId }?.name ?: "Alle Kategorien"
                    OutlinedTextField(
                        value = label,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Kategorie") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = open) },
                        modifier = Modifier.menuAnchor().fillMaxWidth()
                    )
                    ExposedDropdownMenu(expanded = open, onDismissRequest = { open = false }) {
                        DropdownMenuItem(text = { Text("Alle Kategorien") }, onClick = { catId = null; open = false })
                        categories.forEach { c ->
                            DropdownMenuItem(
                                text = { Text(c.name) },
                                onClick = { catId = c.id; open = false },
                                enabled = c.id != catId
                            )
                        }
                    }
                }
            }
        },
        confirmButton = { Button(onClick = { onApply(type, catId) }) { Text("Anwenden") } },
        dismissButton = { OutlinedButton(onClick = onDismiss) { Text("Abbrechen") } }
    )
}

// Editor BottomSheet für die Frage
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun QuestionEditorSheet(
    initial: Question?,
    categories: List<Pair<Int, String>>,
    onDismiss: () -> Unit,
    onDelete: (() -> Unit)? = null,
    onSave: (Question) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    // Nicht-nullable UI-States -> beim Speichern zu null mappen
    var questionText by remember { mutableStateOf(initial?.questionText.orEmpty()) }
    var questionCode by remember { mutableStateOf(initial?.questionCode.orEmpty()) }
    var type by remember { mutableStateOf(initial?.type ?: QuestionType.CHOICE) }
    var answerA by remember { mutableStateOf(initial?.answerA.orEmpty()) }
    var answerB by remember { mutableStateOf(initial?.answerB.orEmpty()) }
    var answerC by remember { mutableStateOf(initial?.answerC.orEmpty()) }
    var answerD by remember { mutableStateOf(initial?.answerD.orEmpty()) }
    var correctMask by remember { mutableStateOf(initial?.correctMask ?: 0) }
    var solutionText by remember { mutableStateOf(initial?.solutionText.orEmpty()) }
    var solutionCode by remember { mutableStateOf(initial?.solutionCode.orEmpty()) }
    var categoryId by remember { mutableStateOf(initial?.categoryId) }

    fun toggleBit(bit: Int) { correctMask = correctMask xor bit }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .heightIn(min = 240.dp, max = 740.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Text(if (initial == null) "Neue Frage" else "Frage bearbeiten", style = MaterialTheme.typography.titleLarge)

            Spacer(Modifier.height(12.dp))

            OutlinedTextField(
                value = questionText, onValueChange = { questionText = it },
                label = { Text("Fragetext") },
                minLines = 3, modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences)
            )

            Spacer(Modifier.height(8.dp))

            // Typ
            var typeOpen by remember { mutableStateOf(false) }
            ExposedDropdownMenuBox(expanded = typeOpen, onExpandedChange = { typeOpen = it }) {
                OutlinedTextField(
                    value = type.name, onValueChange = {},
                    readOnly = true, label = { Text("Fragentyp") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = typeOpen) },
                    modifier = Modifier.menuAnchor().fillMaxWidth()
                )
                ExposedDropdownMenu(expanded = typeOpen, onDismissRequest = { typeOpen = false }) {
                    QuestionType.entries.forEach { t ->
                        DropdownMenuItem(text = { Text(t.name) }, onClick = { type = t; typeOpen = false })
                    }
                }
            }

            Spacer(Modifier.height(8.dp))

            // Kategorie
            var catOpen by remember { mutableStateOf(false) }
            ExposedDropdownMenuBox(expanded = catOpen, onExpandedChange = { catOpen = it }) {
                val label = categories.firstOrNull { it.first == categoryId }?.second ?: "Ohne Kategorie"
                OutlinedTextField(
                    value = label, onValueChange = {},
                    readOnly = true, label = { Text("Kategorie") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = catOpen) },
                    modifier = Modifier.menuAnchor().fillMaxWidth()
                )
                ExposedDropdownMenu(expanded = catOpen, onDismissRequest = { catOpen = false }) {
                    DropdownMenuItem(text = { Text("— Ohne Kategorie —") }, onClick = { categoryId = null; catOpen = false })
                    categories.forEach { (id, name) ->
                        DropdownMenuItem(text = { Text(name) }, onClick = { categoryId = id; catOpen = false }, enabled = id != categoryId)
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            // Code (optional)
            OutlinedTextField(
                value = questionCode,
                onValueChange = { questionCode = it },
                label = { Text("Frage-Code (optional)") },
                minLines = 3,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(12.dp))
            //MC Fragen und Antworten nur by Typ MC
            if (type == QuestionType.CHOICE) {
                // Antworten A–D + MC-Maske
                Text(if (type == QuestionType.CHOICE) "Antworten & Korrekt-Markierung" else "Antworten (optional)", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(8.dp))

                ChoiceRow("A", answerA, { answerA = it }, type == QuestionType.CHOICE, (correctMask and 1) != 0) { toggleBit(1) }
                ChoiceRow("B", answerB, { answerB = it }, type == QuestionType.CHOICE, (correctMask and 2) != 0) { toggleBit(2) }
                ChoiceRow("C", answerC, { answerC = it }, type == QuestionType.CHOICE, (correctMask and 4) != 0) { toggleBit(4) }
                ChoiceRow("D", answerD, { answerD = it }, type == QuestionType.CHOICE, (correctMask and 8) != 0) { toggleBit(8) }
            }

            Spacer(Modifier.height(12.dp))

            // Lösungen
            Text("Lösungen", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(value = solutionText, onValueChange = { solutionText = it }, label = { Text("Lösung (Text)") }, minLines = 2, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(value = solutionCode, onValueChange = { solutionCode = it }, label = { Text("Lösung (Code)") }, minLines = 2, modifier = Modifier.fillMaxWidth())

            Spacer(Modifier.height(16.dp))
            HorizontalDivider(Modifier, DividerDefaults.Thickness, DividerDefaults.color)
            Spacer(Modifier.height(12.dp))

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                if (onDelete != null && initial != null) {
                    OutlinedButton(onClick = onDelete) { Text("Löschen") }
                } else {
                    Spacer(Modifier.width(8.dp))
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(onClick = onDismiss) { Text("Abbrechen") }
                    //Frage speichern nach Klick des Buttons
                    Button(onClick = {
                        val result = Question(
                            id = initial?.id ?: 0,
                            questionText = questionText.ifBlank { "" },
                            questionCode = questionCode.ifBlank { "" },
                            type = type,
                            answerA = answerA.ifBlank { "" },
                            answerB = answerB.ifBlank { "" },
                            answerC = answerC.ifBlank { "" },
                            answerD = answerD.ifBlank { "" },
                            correctMask = if (type == QuestionType.CHOICE) correctMask else 0,
                            solutionText = solutionText.ifBlank { "" },
                            solutionCode = solutionCode.ifBlank { "" },
                            categoryId = categoryId,
                            timesCorrect = initial?.timesCorrect ?: 0,
                            timesWrong = initial?.timesWrong ?: 0
                        )
                        onSave(result)
                    }) { Text("Speichern") }
                }
            }
            Spacer(Modifier.height(8.dp))
        }
    }
}

//Zeile für die MC-Fragen (Text + Checkbox)
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
