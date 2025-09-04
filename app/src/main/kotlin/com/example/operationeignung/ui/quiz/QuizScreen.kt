package com.example.operationeignung.ui.quiz

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.operationeignung.data.database.entities.QuestionType
import com.example.operationeignung.ui.common.LocalSnackbarHostState
import com.example.operationeignung.ui.common.SyntaxHighlighterComposable

/** Stellt die Quiz-Ansicht bereit **/
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuizScreen(
    viewModel: QuizViewModel = hiltViewModel(),
    contentPadding: PaddingValues = PaddingValues(),
    filterOpen: Boolean = false,
    onFilterClose: () -> Unit = {},
    onEditQuestion: (Int) -> Unit
) {
    val ui by viewModel.ui.collectAsState()
    val scroll = TopAppBarDefaults.pinnedScrollBehavior()
    var showFilter by remember { mutableStateOf(false) }
    val snackbar = LocalSnackbarHostState.current
    LaunchedEffect(filterOpen) { if (filterOpen) showFilter = true }

    // Sicherheitsabfrage fürs Löschen (wie gehabt)
    var confirmDelete by remember { mutableStateOf(false) }
    val q = ui.question

    // DB-Stats der aktuellen Frage beobachten
    val stats by remember(q?.id) {
        viewModel.observeQuestionStats(q?.id)
    }.collectAsState(initial = 0 to 0)

    // Fehler anzeigen
    LaunchedEffect(ui.errorMessage) {
        ui.errorMessage?.let {
            snackbar.showSnackbar(it)
            viewModel.clearError()
        }
    }

    // TopAppBar + Scaffold
    Scaffold(
        modifier = Modifier.nestedScroll(scroll.nestedScrollConnection),
        topBar = {
            TopAppBar(
                title = { Text("Quiz") },
                scrollBehavior = scroll,
                actions = {
                    IconButton(onClick = { showFilter = true }) {
                        Icon(Icons.Default.FilterList, contentDescription = "Filter")
                    }
                }
            )
        }
    ) { p ->
        QuizContentColumn(
            padding = p,
            contentPadding = contentPadding,
            ui = ui,
            q = q,
            stats = stats,
            onEditQuestion = onEditQuestion,
            onDeleteConfirm = { confirmDelete = true },
            viewModel = viewModel
        )
    }

    // Sicherheits-Dialog für Löschen
    if (confirmDelete && q != null) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("Frage löschen?") },
            text = { Text("Diese Aktion kann nicht rückgängig gemacht werden.") },
            confirmButton = {
                Button(
                    onClick = {
                        confirmDelete = false
                        viewModel.deleteQuestion(q.id)
                        viewModel.loadNext() // direkt nächste Frage anzeigen
                    },
                ) { Text("Löschen") }
            },
            dismissButton = {
                OutlinedButton(onClick = { confirmDelete = false }) { Text("Abbrechen") }
            }
        )
    }

    // Filter-Dialog
    if (showFilter) {
        QuizFilterDialog(
            state = ui,
            onApply = { mode, type, selectedCats ->
                viewModel.setMode(mode)
                viewModel.setType(type)
                viewModel.setCategories(selectedCats) // komplette Auswahl setzen
                viewModel.loadNext()
                showFilter = false; onFilterClose()
            },
            onDismiss = { showFilter = false; onFilterClose(); onFilterClose() }
        )
    }
}

/** Vertikale Spalte mit dem gesamten Quiz-Inhalt (Frage, Antworten, Aktionen, Lösungen). */
@Composable
private fun QuizContentColumn(
    padding: PaddingValues,
    contentPadding: PaddingValues,
    ui: QuizUiState,
    q: UiQuestion?,
    stats: Pair<Int, Int>,
    onEditQuestion: (Int) -> Unit,
    onDeleteConfirm: () -> Unit,
    viewModel: QuizViewModel
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(contentPadding)
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (q != null) {
            // Kopfzeile (bearbeiten/löschen)
            QuizHeaderRow(
                stats = stats,
                q = q,
                onEditQuestion = onEditQuestion,
                onDeleteConfirm = onDeleteConfirm
            )
        }

        // Fragetext + optionaler Code
        QuizQuestionBody(q = q)

        if (q != null) {
            // Antworten
            QuizAnswerArea(q = q, ui = ui, viewModel = viewModel)
            Spacer(Modifier.height(2.dp))
            // Buttons unterhalb der Antworten
            QuizActionRow(q = q, ui = ui, viewModel = viewModel)
        }

        // Lösungen
        QuizSolutionsSection(q = q, ui = ui)
    }
}

/** Kopfzeile: kleine Statistik . */
@Composable
private fun QuizHeaderRow(
    stats: Pair<Int, Int>,
    q: UiQuestion,
    onEditQuestion: (Int) -> Unit,
    onDeleteConfirm: () -> Unit
) {
    Row(
        Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            AssistChip(onClick = {}, label = { Text("✓ ${stats.first}") })
            AssistChip(onClick = {}, label = { Text("✗ ${stats.second}") })
        }
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            IconButton(onClick = { onEditQuestion(q.id) }) {
                Icon(Icons.Default.Edit, contentDescription = "Frage bearbeiten")
            }
            IconButton(
                onClick = onDeleteConfirm,
                colors = IconButtonDefaults.iconButtonColors(
                    contentColor = MaterialTheme.colorScheme.error
                )
            ) {
                Icon(Icons.Default.Delete, contentDescription = "Frage löschen")
            }
        }
    }
}

/** Fragetext */
@Composable
private fun QuizQuestionBody(q: UiQuestion?) {
    if (q == null) {
        Text("Keine passende Frage gefunden (Filter zu eng?).")
        return
    }
    Text(q.text, style = MaterialTheme.typography.titleMedium)

    if (q.code.isNotBlank()) {
        SyntaxHighlighterComposable(
            code = q.code,
            typeHint = null,
            showLineNumbers = true
        )
    }
    Spacer(Modifier.height(2.dp)) // Abstand zu den Antwortmöglichkeiten
}

/** Antwortbereich: CHOICE zeigt auswählbare Antworten, TEXT/CODE zeigt Hinweis. */
@Composable
private fun QuizAnswerArea(
    q: UiQuestion,
    ui: QuizUiState,
    viewModel: QuizViewModel
) {
    when (q.type) {
        QuestionType.CHOICE -> {
            q.mcChoices.forEachIndexed { i, c ->
                AnswerRow(
                    choice = c,
                    locked = ui.inputsLocked,
                    onToggle = { viewModel.toggleChoice(i) }
                )
            }
        }
        // Hinweis für TEXT/CODE (wie im Original)
        QuestionType.TEXT, QuestionType.CODE -> {
            Text(
                "Löse die Aufgabe außerhalb der App (z. B. auf Papier/IDE) und bewerte " +
                        "dann selbst, ob du sie richtig oder falsch beantwortest hast."
            )
        }
    }
}

/** Aktionszeile unter den Antworten */
@Composable
private fun QuizActionRow(
    q: UiQuestion,
    ui: QuizUiState,
    viewModel: QuizViewModel
) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {

        if (q.type == QuestionType.CHOICE) {
            // Button zum automatischen Prüfen
            Button(
                onClick = { viewModel.checkAndMark() },
                enabled = !ui.inputsLocked
            ) { Text("Antwort prüfen") }

        } else {
            // Manuelle Bewertung für TEXT/CODE
            IconButton(
                onClick = { viewModel.markManualResult(true) },
                enabled = !ui.inputsLocked
            ) {
                Icon(Icons.Default.Check, contentDescription = "Als richtig werten")
            }
            IconButton(
                onClick = { viewModel.markManualResult(false) },
                enabled = !ui.inputsLocked,
                colors = IconButtonDefaults.iconButtonColors(
                    contentColor = MaterialTheme.colorScheme.error
                )
            ) {
                Icon(Icons.Default.Close, contentDescription = "Als falsch werten")
            }
        }

        // Navigation zur nächsten Frage
        IconButton(
            onClick = { viewModel.loadNext() },
            enabled = ui.inputsLocked
        ) {
            Icon(Icons.Default.PlayArrow, contentDescription = "Nächste Frage")
        }

        // Frage überspringen
        IconButton(
            onClick = { viewModel.skip() }
        ) {
            Icon(Icons.Default.SkipNext, contentDescription = "überspringen")
        }
    }
}


/** Lösungen: CHOICE -> Erläuterung nach Prüfen; TEXT/CODE -> Toggle „Musterlösung“. */
@Composable
private fun QuizSolutionsSection(
    q: UiQuestion?,
    ui: QuizUiState
) {
    // Zustand pro Frage (lokal je Frage-ID)
    var showSolutions by remember(q?.id) { mutableStateOf(false) }
    if (q == null) {
        showSolutions = false
        return
    }

    // CHOICE: Nach „Antwort prüfen“ (inputsLocked == true) -> Lösungstext
    if (q.type == QuestionType.CHOICE && ui.inputsLocked && q.solutionText.isNotBlank()) {
        Spacer(Modifier.height(12.dp))
        Text("Lösung", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(4.dp))
        Text(q.solutionText)
    }

    // „Musterlösung anzeigen“
    if (q.type == QuestionType.TEXT || q.type == QuestionType.CODE) {
        Spacer(Modifier.height(12.dp))
        TextButton(onClick = { showSolutions = !showSolutions }) {
            Text(if (showSolutions) "Musterlösung ausblenden" else "Musterlösung anzeigen")
        }
        if (showSolutions) {
            Spacer(Modifier.height(8.dp))
            if (q.solutionText.isNotBlank()) {
                Text("Erläuterung", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(4.dp))
                Text(q.solutionText)
                Spacer(Modifier.height(12.dp))
            }
            // Code-Block
            if (q.solutionCode.isNotBlank()) {
                ShowCodeBlock("Code", q.solutionCode, null)
            } else {
                Text("Keine Musterlösung (Code) vorhanden.")
            }
        }
    }
}


// Methode zur Ausgabe des Codeblocks. Wird nur ausgegeben, wenn auch befüllt.
@Composable
fun ShowCodeBlock(title: String, code: String?, lang: String?) {
    if (!code.isNullOrBlank()) {
        Text(title, style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(4.dp))
        // nutzt deinen vorhandenen Highlighter:
        SyntaxHighlighterComposable(
            code = code,
            typeHint = lang,
            showLineNumbers = true
        )
        Spacer(Modifier.height(12.dp))
    }
}

// Methode für den FIlter-Dialog
@Composable
private fun QuizFilterDialog(
    state: QuizUiState,
    onApply: (Mode, QuestionType?, Set<Int>) -> Unit,
    onDismiss: () -> Unit
) {
    var localMode by remember(state.mode) { mutableStateOf(state.mode) }
    var localType by remember(state.typeFilter) { mutableStateOf(state.typeFilter?.dbValue ?: "all") }
    var localCats by remember(state.selectedCategoryIds) { mutableStateOf(state.selectedCategoryIds) }

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = { onApply(localMode, if (localType.equals("all", true)) null else QuestionType.fromDb(localType), localCats) }) { Text("Übernehmen") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Abbrechen") } },
        title = { Text("Filter") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Modus", style = MaterialTheme.typography.labelLarge)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(Mode.RANDOM to "Random", Mode.NEW to "Neu", Mode.WRONG to "Falsch").forEach { (m, lbl) ->
                        FilterChipBasic(text = lbl, selected = localMode == m) { localMode = m }
                    }
                }

                Text("Fragetyp", style = MaterialTheme.typography.labelLarge)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("all","choice","text","code").forEach { t ->
                        FilterChipBasic(text = t, selected = localType == t) { localType = t }
                    }
                }

                Text("Kategorien", style = MaterialTheme.typography.labelLarge)
                if (state.categories.isEmpty()) {
                    Text("Keine Kategorien angelegt.", style = MaterialTheme.typography.bodySmall)
                } else {
                    FlowRowWrap {
                        state.categories.forEach { c ->
                            val sel = c.id in localCats
                            FilterChipBasic(
                                text = if (sel) "✓ ${c.name}" else c.name,
                                selected = sel
                            ) {
                                localCats = if (sel) localCats - c.id else localCats + c.id
                            }
                        }
                    }
                }
            }
        }
    )
}

//Ausgabe der Chips für den FilterDialog
@Composable
private fun FilterChipBasic(text: String, selected: Boolean, onClick: () -> Unit) {
    val bg = if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
    else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
    Surface(
        modifier = Modifier.height(36.dp).padding(end = 8.dp).clickable { onClick() },
        shape = MaterialTheme.shapes.large,
        color = bg
    ) { Box(Modifier.padding(horizontal = 12.dp), contentAlignment = Alignment.Center) { Text(text) } }
}

// Verteilt die Kategorie-Chips auf dem Bildschirm
@Composable
private fun FlowRowWrap(content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) { content() }
}

// Ausgabe einer Antwortzeile bei den MC-Fragen
@Composable
private fun AnswerRow(
    choice: McChoice,
    locked: Boolean,
    onToggle: () -> Unit
) {
    val bg = answerBackground(choice.chosen, choice.isCorrect, locked)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = !locked) { onToggle() },
        colors = CardDefaults.cardColors(containerColor = bg)
    ) {
        Row(
            Modifier.padding(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = choice.chosen,
                onCheckedChange = { if (!locked) onToggle() },
                enabled = !locked
            )
            Spacer(Modifier.width(4.dp))
            Text(choice.text)
        }
    }
}


// Ändern des Hintergrundes bei MC-Fragen
fun answerBackground(chosen: Boolean, isCorrect: Boolean, locked: Boolean): Color {
    if (!locked) return Color.Unspecified  // vor dem Prüfen keine Farbe
    return when {
        chosen && isCorrect -> Color(0xFFB6FCB6)      // grün
        chosen && !isCorrect -> Color(0xFFFFCCCC)     // rot
        !chosen && isCorrect -> Color(0xFFDAF5FF)     // blau (richtige, aber nicht gewählte)
        else -> Color.Unspecified
    }
}

