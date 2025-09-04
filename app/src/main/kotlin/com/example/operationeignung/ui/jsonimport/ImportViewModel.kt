package com.example.operationeignung.ui.jsonimport

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.operationeignung.data.database.AppDatabase
import com.example.operationeignung.data.database.dao.CategoryDao
import com.example.operationeignung.data.database.dao.QuestionDao
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/** ViewModell für den ImportScreen **/
@HiltViewModel
class ImportViewModel @Inject constructor(
    private val questionDao: QuestionDao,
    private val categoryDao: CategoryDao,
    private val db: AppDatabase
) : ViewModel() {

    data class UiState(
        val isBusy: Boolean = false,
        val report: ImportReport? = null,
        val error: String? = null,
        val conflictMode: ConflictMode = ConflictMode.SKIP
    )

    private val _state = MutableStateFlow(UiState())
    val state: StateFlow<UiState> = _state

    //Speichern des Konfliktmodus
    fun setConflictMode(m: ConflictMode) {
        _state.value = _state.value.copy(conflictMode = m)
    }

    //Resetten des Eingabefeldes
    fun reset() {
        _state.value = UiState()
    }

    //Fehler vom Screen annehmen und speichern
    fun setError(msg: String) {
        _state.value = _state.value.copy(error = msg)
    }

    // Importiert Fragen aus einem JSON-String
    fun runImport(json: String) {
        viewModelScope.launch {
            _state.value = _state.value.copy(isBusy = true, error = null)
            try {
                 val rep = importFromJsonStringTx(
                     json,
                     db,
                     questionDao,
                     categoryDao,
                     conflictMode = _state.value.conflictMode
                 )
                _state.value = _state.value.copy(isBusy = false, report = rep)
            } catch (t: Throwable) {
                _state.value = _state.value.copy(
                    isBusy = false,
                    error = t.localizedMessage ?: "Unbekannter Fehler beim Import"
                )
            }
        }
    }
}
