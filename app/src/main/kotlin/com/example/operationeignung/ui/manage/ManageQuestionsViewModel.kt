package com.example.operationeignung.ui.manage

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.operationeignung.data.database.dao.CategoryDao
import com.example.operationeignung.data.database.dao.QuestionDao
import com.example.operationeignung.data.database.entities.Category
import com.example.operationeignung.data.database.entities.Question
import com.example.operationeignung.data.database.entities.QuestionType
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

/** Das Vier Modell für den Manage Questions Screen */
@HiltViewModel
class ManageQuestionsViewModel @Inject constructor(
    private val questionDao: QuestionDao,
    categoryDao: CategoryDao
) : ViewModel() {

    var errorMessage by mutableStateOf<String?>(null)
    val allQuestions: StateFlow<List<Question>> =
        questionDao.getAllQuestionsFlow()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val categories: StateFlow<List<Category>> =
        categoryDao.getAllCategories()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    var search by mutableStateOf("")
    var filterType by mutableStateOf<QuestionType?>(null)
    var filterCategoryId by mutableStateOf<Int?>(null)

    //Filter verwalten
    fun filtered(list: List<Question>): List<Question> =
        list.asSequence()
            .filter { q -> filterType == null || q.type == filterType }
            .filter { q -> filterCategoryId == null || q.categoryId == filterCategoryId }
            .filter { q -> search.isBlank() || q.questionText.contains(search, ignoreCase = true) }
            .toList()

    //Fragen einfügen oder updaten
    suspend fun upsert(q: Question) {
        try {
            if (q.id == 0) questionDao.insert(q) else questionDao.update(q)
        } catch (t: Throwable) {
            errorMessage = t.message ?: "Frage konnte nicht gespeichert werden."
        }
    }

    //Frage löschen
    suspend fun delete(q: Question) {
        try {
            questionDao.delete(q.id)
        } catch (t: Throwable) {
            errorMessage = t.message ?: "Frage konnte nicht gelöscht werden."
        }
    }

    //Fehler löschen
    fun clearError() { errorMessage = null }

}


