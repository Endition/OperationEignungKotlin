package com.example.operationeignung.ui.manage

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.operationeignung.data.database.dao.CategoryDao
import com.example.operationeignung.data.database.dao.QuestionDao
import com.example.operationeignung.data.database.entities.Question
import com.example.operationeignung.data.database.entities.QuestionType
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class QuestionFormState(
    val id: Int = 0,
    val questionText: String = "",
    val questionCode: String = "",
    val type: QuestionType = QuestionType.CHOICE,
    val answerA: String = "",
    val answerB: String = "",
    val answerC: String = "",
    val answerD: String = "",
    val correctMask: Int = 0,
    val solutionText: String = "",
    val solutionCode: String = "",
    val categoryId: Int? = null,
    val timesCorrect: Int = 0,
    val timesWrong: Int = 0
)

@HiltViewModel
class AddEditQuestionViewModel @Inject constructor(
    private val questionDao: QuestionDao,
    categoryDao: CategoryDao
) : ViewModel() {

    private val _form = MutableStateFlow(QuestionFormState())
    val form: StateFlow<QuestionFormState> = _form.asStateFlow()

    val categories = categoryDao.getAllCategories()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    //Fragen laden und ins Form bringen
    fun load(questionId: Int?) {
        if (questionId == null) return
        viewModelScope.launch {
            val q = questionDao.getById(questionId) ?: return@launch
            _form.value = QuestionFormState(
                id = q.id,
                questionText = q.questionText,
                questionCode = q.questionCode,
                type = q.type,
                answerA = q.answerA.orEmpty(),
                answerB = q.answerB,
                answerC = q.answerC,
                answerD = q.answerD,
                correctMask = q.correctMask,
                solutionText = q.solutionText,
                solutionCode = q.solutionCode.orEmpty(),
                categoryId = q.categoryId,
                timesCorrect = q.timesCorrect,
                timesWrong = q.timesWrong
            )
        }
    }

    // Form updaten
    fun update(transform: (QuestionFormState) -> QuestionFormState) {
        _form.value = transform(_form.value)
    }

    //
    fun toggleMask(bit: Int) {
        _form.value = _form.value.copy(correctMask = _form.value.correctMask xor bit)
    }

    //Frage speichern
    fun save(onDone: () -> Unit) {
        viewModelScope.launch {
            val f = _form.value
            val isChoice = f.type == QuestionType.CHOICE
            val entity = Question(
                id = f.id,
                questionText = f.questionText.ifBlank { "" },
                questionCode = f.questionCode.ifBlank { "" },
                type = f.type,
                answerA = if (isChoice) f.answerA.ifBlank { "" } else "",
                answerB = if (isChoice) f.answerB.ifBlank { "" } else "",
                answerC = if (isChoice) f.answerC.ifBlank { "" } else "",
                answerD = if (isChoice) f.answerD.ifBlank { "" } else "",
                correctMask = if (f.type == QuestionType.CHOICE) f.correctMask else 0,
                solutionText = f.solutionText.ifBlank { "" },
                solutionCode = f.solutionCode.ifBlank { "" },
                categoryId = f.categoryId,
                timesCorrect = f.timesCorrect,
                timesWrong = f.timesWrong
            )
            if (entity.id == 0) questionDao.insert(entity) else questionDao.update(entity)
            onDone()
        }
    }

    //Frage löschen
    fun delete(onDone: () -> Unit) {
        val id = _form.value.id
        if (id == 0) { onDone(); return }
        viewModelScope.launch {
            questionDao.delete(id)
            onDone()
        }
    }
}
