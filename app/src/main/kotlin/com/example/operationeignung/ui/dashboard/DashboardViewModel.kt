package com.example.operationeignung.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.operationeignung.data.database.dao.CategoryStatRow
import com.example.operationeignung.data.database.dao.QuestionDao
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val questionDao: QuestionDao
) : ViewModel() {

    data class UiState(
        val total: Int = 0,
        val correct: Int = 0,
        val wrong: Int = 0,
        val categoryRows: List<CategoryStatRow> = emptyList()
    )

    // Kombiniert alle benötigten DB-Infos in einen einzigen State fürs UI
    val uiState: StateFlow<UiState> = combine(
        questionDao.totalQuestionsFlow(),
        questionDao.totalCorrectFlow(),
        questionDao.totalWrongFlow(),
        questionDao.categoryStatsFlow()
    ) { total, correct, wrong, rows ->
        UiState(
            total = total,
            correct = correct,
            wrong = wrong,
            categoryRows = rows
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = UiState()
    )
}
