package com.example.operationeignung.ui.quiz

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.operationeignung.data.database.dao.CategoryDao
import com.example.operationeignung.data.database.dao.QuestionDao
import com.example.operationeignung.data.database.entities.Category
import com.example.operationeignung.data.database.entities.Question
import com.example.operationeignung.data.database.entities.QuestionType
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

//Das sind die Quiz-Modi (Zufällig, nur neue Fragen, nur bisher falsche Fragen)
enum class Mode { RANDOM, NEW, WRONG }

data class McChoice(
    val text: String,
    val isCorrect: Boolean,
    val chosen: Boolean = false
)
/** UI-Frage Objekt **/
data class UiQuestion(
    val id: Int,
    val text: String,
    val code: String,
    val type: QuestionType,
    val mcChoices: List<McChoice> = emptyList(),
    val solutionText: String = "",
    val solutionCode: String = "",
)

/** UI-Quiz-Status Objekt **/
data class QuizUiState(
    val mode: Mode = Mode.RANDOM,
    val typeFilter: QuestionType? = QuestionType.CHOICE,
    val categories: List<Category> = emptyList(),   // verfügbar
    val selectedCategoryIds: Set<Int> = emptySet(), // Filter
    val question: UiQuestion? = null,
    val inputsLocked: Boolean = false,             // nach "prüfen" werden Inputs gesperrt
    val errorMessage: String? = null
)

@HiltViewModel
class QuizViewModel @Inject constructor(
    private val questionDao: QuestionDao,
    private val categoryDao: CategoryDao
) : ViewModel() {

    private val _ui = MutableStateFlow(QuizUiState())
    private val _questionStats = MutableStateFlow(0 to 0)
    val ui: StateFlow<QuizUiState> = _ui

    init {
        // Kategorien laden (Flow) & direkt erste Frage holen
        viewModelScope.launch {
            categoryDao.getAllFlow().collectLatest { cats ->
                _ui.update { it.copy(categories = cats) }
                if (_ui.value.question == null) loadNext()
            }
        }
    }

    //  Filtersteuerung aus dem FilterDialog für das Qiuz

    //Setzte den Mode (new, false, random) im Filter und triggert anschließend das Neuladen der nächsten Frage.
    fun setMode(mode: Mode) {
        _ui.update { it.copy(mode = mode) }
        loadNext()
    }

    //Setzt den Fragentyp im Filter und triggert anschließend das Neuladen der nächsten Frage.
    fun setType(type: QuestionType?) {
        _ui.update { it.copy(typeFilter = type) }
        loadNext()
    }

    //Setzt die ausgewählten Kategorien vollständig anhand der übergebenen Category-Objekte und triggert anschließend das Neuladen der nächsten Frage.
    fun setCategories(categoryIds: Set<Int>) {
        _ui.update { s ->
            s.copy(selectedCategoryIds = categoryIds)
        }
        loadNext()
    }

    //Überspringen der Frage
    fun skip() {
        // nichts zählen, nur nächste laden
        loadNext()
    }


    /** Lädt die nächste Frage. Erst UI sperren, dann holen, dann mappen. */
    fun loadNext() = viewModelScope.launch {
        lockInputsWhileLoading()

        try {
            val s = _ui.value
            val picked = pickNextQuestion(
                mode = s.mode,
                typeFilter = s.typeFilter,
                selectedCategoryIds = s.selectedCategoryIds.toList()
            )

            if (picked == null) {
                // Nix gefunden – freundlich sagen, UI freigeben.
                _ui.update {
                    it.copy(
                        question = null,
                        errorMessage = "Keine passende Frage gefunden.",
                        inputsLocked = false
                    )
                }
                return@launch
            }

            // Gefunden → abhängig vom Typ zu UiQuestion mappen
            val nextUi = when (picked.type) {
                QuestionType.CHOICE -> toUiChoiceQuestion(picked)
                QuestionType.TEXT,
                QuestionType.CODE   -> toUiNonChoiceQuestion(picked)
            }

            _ui.update {
                it.copy(
                    question = nextUi,
                    errorMessage = null,   // alten Fehler weg
                    inputsLocked = false   // wieder freigeben
                )
            }
        } catch (t: Throwable) {
            // Fehlermeldung, aber nicht dramatisch machen.
            _ui.update {
                it.copy(
                    errorMessage = t.message ?: "Unbekannter Fehler beim Laden der Frage.",
                    inputsLocked = false
                )
            }
        }
    }

    /**
     * Wählt die nächste Frage – erst mit Filter, wenn nötig im Fallback ohne.
     */
    private suspend fun pickNextQuestion(
        mode: Mode,
        typeFilter: QuestionType?,
        selectedCategoryIds: List<Int>
    ): Question? {
        // 1. Versuch: mit gesetztem Filter (falls vorhanden)
        pickByMode(mode, typeFilter, selectedCategoryIds)?.let { return it }

        // 2. Versuch: ohne Filter (nur wenn überhaupt einer gesetzt war)
        if (typeFilter != null) {
            pickByMode(mode, null, selectedCategoryIds)?.let { return it }
        }

        return null
    }

    /**
     * Routed die DAO-Aufrufe je nach Modus und ob Kategorie-Filter gesetzt sind.
     */
    private suspend fun pickByMode(
        mode: Mode,
        typeFilter: QuestionType?,
        selectedCategoryIds: List<Int>
    ): Question? {
        val byCats = selectedCategoryIds.isNotEmpty()
        return when (mode) {
            Mode.RANDOM -> if (byCats)
                questionDao.pickRandomWithCats(typeFilter, selectedCategoryIds)
            else
                questionDao.pickRandomAllCats(typeFilter)

            Mode.NEW -> if (byCats)
                questionDao.pickNewWithCats(typeFilter, selectedCategoryIds)
            else
                questionDao.pickNewAllCats(typeFilter)

            Mode.WRONG -> if (byCats)
                questionDao.pickWrongWithCats(typeFilter, selectedCategoryIds)
            else
                questionDao.pickWrongAllCats(typeFilter)
        }
    }

    /**
     * Sperrt die UI-Inputs und räumt alte Fehler weg.
     */
    private fun lockInputsWhileLoading() {
        _ui.update { it.copy(inputsLocked = true, errorMessage = null) }
    }

    /**
     * Baut eine UiQuestion für CHOICE-Fragen inkl. gemischter Antworten.
     */
    private fun toUiChoiceQuestion(picked: Question): UiQuestion {
        val answers = listOf(
            picked.answerA.orEmpty(),
            picked.answerB,
            picked.answerC,
            picked.answerD
        )
        val mask = (0..3).map { i -> ((picked.correctMask shr i) and 1) == 1 }
        val shuffled = answers.zip(mask).shuffled()

        return UiQuestion(
            id = picked.id,
            text = picked.questionText,
            code = picked.questionCode.orEmpty(),
            type = picked.type,
            mcChoices = shuffled.map { (t, isC) -> McChoice(text = t, isCorrect = isC) },
            solutionText = picked.solutionText,
            solutionCode = picked.solutionCode
        )
    }

    /**
     * Baut eine UiQuestion für TEXT/CODE ohne Multiple Choice.
     */
    private fun toUiNonChoiceQuestion(picked: Question): UiQuestion {
        return UiQuestion(
            id = picked.id,
            text = picked.questionText,
            code = picked.questionCode, // bei dir nullable → im UI so belassen
            type = picked.type,
            mcChoices = emptyList(),
            solutionText = picked.solutionText,
            solutionCode = picked.solutionCode
        )
    }



    //markiere die ausgewählen Antowrten bei MC
    fun toggleChoice(index: Int) {
        _ui.update { s ->
            if (s.inputsLocked || s.question == null) return@update s
            s.copy(
                question = s.question.copy(
                    mcChoices = s.question.mcChoices.mapIndexed { i, c ->
                        if (i == index) c.copy(chosen = !c.chosen) else c
                    }
                )
            )
        }
    }

    //Antworten prüfen und ggf. markieren und speichern
    fun checkAndMark() = viewModelScope.launch {
        try{
            val s = _ui.value
            val q = s.question ?: return@launch

            val isCorrect: Boolean = when (q.type) {
                QuestionType.CHOICE -> {
                    // exakt gleiche Auswahl wie korrekte Maske?
                    val chosenMask = q.mcChoices.map { it.chosen }
                    val correctMask = q.mcChoices.map { it.isCorrect }
                    chosenMask == correctMask
                }
                QuestionType.TEXT-> {
                    false
                }
                QuestionType.CODE -> {
                    false
                }
            }

            //Speichern der richtig/falsch Antwort an der Frage
            if (isCorrect) questionDao.incCorrect(q.id) else questionDao.incWrong(q.id)

            // Inputs sperren, damit die UI die „richtige aber nicht gewählte“ Lösung blau färben kann
            _ui.update {
                it.copy(inputsLocked = true,    //Inputs sperren
                errorMessage = null             //alten Fehler löschen
                )}
        }catch (t: Throwable) {
            _ui.update { it.copy(errorMessage = t.message ?: "Fehler beim Speichern der Antwort.") }
        }
    }

    // Aktuelle Frage-Statistik live aus der DB
    fun observeQuestionStats(questionId: Int?): Flow<Pair<Int, Int>> =
        if (questionId == null) flowOf(0 to 0)
        else questionDao.observeById(questionId)
            .map { q -> (q?.timesCorrect ?: 0) to (q?.timesWrong ?: 0) }

    // Manuell als richtig/falsch werten (für text/code/open)
    // => liest den aktuellen Datensatz aus der DB, erhöht Zähler, updatet, lädt nächste Frage
    fun markManualResult(correct: Boolean) {
        val q = _ui.value
        viewModelScope.launch {
            if(q.question != null){
                val fresh = questionDao.getById(q.question.id) ?: return@launch
                val updated = if (correct) {
                    fresh.copy(timesCorrect = (fresh.timesCorrect) + 1)
                } else {
                    fresh.copy(timesWrong   = (fresh.timesWrong) + 1)
                }
                questionDao.update(updated)
             }
            //inputs sperren bis zur nächsten Frage
            _ui.update { it.copy(inputsLocked = true) }
        }

    }

    // Frage löschen
    fun deleteQuestion(id: Int) = viewModelScope.launch {
        questionDao.delete(id)
    }

    //Fehler löschen
    fun clearError() {
        _ui.update { it.copy(errorMessage = null) }
    }

}
