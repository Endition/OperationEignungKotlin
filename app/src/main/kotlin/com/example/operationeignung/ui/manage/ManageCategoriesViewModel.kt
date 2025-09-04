package com.example.operationeignung.ui.manage

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.room.withTransaction
import com.example.operationeignung.data.database.AppDatabase
import com.example.operationeignung.data.database.dao.CategoryDao
import com.example.operationeignung.data.database.dao.QuestionDao
import com.example.operationeignung.data.database.entities.Category
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Das ViewModel für den manageCategoriesScreen **/
@HiltViewModel
class ManageCategoriesViewModel @Inject constructor(
    private val db: AppDatabase,
    private val categoryDao: CategoryDao,
    private val questionDao: QuestionDao
) : ViewModel() {

    val categories: StateFlow<List<Category>> =
        categoryDao.getAllFlow()
            .map { it }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    // Neue Kategorie hinzurüfen
    fun add(name: String) = viewModelScope.launch {
        val n = name.trim()
        if (n.isNotEmpty()) {
            val existing = categoryDao.getCategoryByName(n)
            if (existing == null) categoryDao.insert(Category(name = n))
        }
    }

    //Kategorie löschen
    fun delete(cat: Category) = viewModelScope.launch {
        categoryDao.delete(cat)
    }

    //Alle unbenutzten Kategorien löschen
    fun deleteUnused() = viewModelScope.launch {
        categoryDao.deleteUnused()
    }

    //zwei Kategorien verschmelzen, dabei auch die Fragen umhängen
    fun mergeCategories(sourceId: Int, targetId: Int, onDone: (moved: Int) -> Unit = {}) =
        viewModelScope.launch {
            if (sourceId == targetId) {
                onDone(0); return@launch
            }
            val moved = db.withTransaction {
                val count = questionDao.reassignCategory(sourceId, targetId)
                categoryDao.deleteById(sourceId)
                count
            }
            onDone(moved)
        }
}