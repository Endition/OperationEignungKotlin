package com.example.operationeignung

import com.example.operationeignung.data.database.dao.CategoryDao
import com.example.operationeignung.data.database.dao.QuestionDao
import com.example.operationeignung.data.database.entities.Category
import com.example.operationeignung.data.database.entities.Question
import com.example.operationeignung.data.database.entities.QuestionType
import com.example.operationeignung.ui.manage.ManageQuestionsViewModel
import kotlinx.coroutines.flow.flowOf
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.lang.reflect.Method
import java.lang.reflect.Proxy

/**
 * Unit-Tests für die reine Filter-Logik von ManageQuestionsViewModel.filtered(...).
 *
 * WICHTIG:
 * - Es wird NUR die Filterfunktion (kein Room, keine Android APIs) getestet.
 * - Die ViewModel-Konstruktion verlangt DAOs; diese werden hier via Java-Proxy gefaked.
 * - Die StateFlows im ViewModel (getAllQuestionsFlow/getAllCategories) werden nicht
 *   gesammelt; dadurch muss nicht auf Dispatchers.Main o.ä. zurpck gegriffen werden.
 */
class ManageQuestionsViewModelFilteredTest {

    // ------------------------------------------------------------
    // Hilfsfunktion: Java-Dynamic-Proxy für Interface-Fakes
    // ------------------------------------------------------------
    private inline fun <reified T : Any> proxy(
        crossinline impl: (method: Method, args: Array<out Any?>?) -> Any?
    ): T {
        val iface = T::class.java
        val handler = java.lang.reflect.InvocationHandler { _, method, args ->
            when (method.name) {
                "toString" -> "${iface.simpleName}Proxy"
                "hashCode" -> System.identityHashCode(this)
                "equals" -> (args?.getOrNull(0) === this)
                else -> impl(method, args)
            }
        }
        return Proxy.newProxyInstance(iface.classLoader, arrayOf(iface), handler) as T
    }

    // ------------------------------------------------------------
    // Fakes für QuestionDao / CategoryDao – nur benötigte Methoden
    // ------------------------------------------------------------
    private fun questionDaoFake(): QuestionDao = proxy { method, args ->
        when (method.name) {
            // Flow wird im Test nicht konsumiert; reicht leere Liste.
            "getAllQuestionsFlow" -> flowOf(emptyList<Question>())
            // Einfache No-ops für Upsert/Delete – werden hier nicht aufgerufen.
            "insert", "insertAll", "update", "delete" -> Unit
            // Zähler/Statistiken/Picker etc. – werden in diesen Tests nicht genutzt.
            else -> unsupported(method)
        }
    }

    private fun categoryDaoFake(): CategoryDao = proxy { method, _ ->
        when (method.name) {
            "getAllCategories", "getAllFlow" -> flowOf(emptyList<Category>())
            // Nicht benötigt in diesen Tests:
            else -> unsupported(method)
        }
    }

    private fun unsupported(m: Method): Nothing =
        throw UnsupportedOperationException("Not supported in this test: ${m.declaringClass.simpleName}.${m.name}")

    // ------------------------------------------------------------
    // Testdaten – kleine, repräsentative Menge
    // ------------------------------------------------------------
    private val q1 = Question( // CHOICE, cat=1, passt auf "Linux"
        id = 1,
        questionText = "Linux permissions: which command sets 755?",
        questionCode = "",
        answerA = "chmod 755 file",
        answerB = "chmod 644 file",
        answerC = "",
        answerD = "",
        correctMask = 0b0001,
        type = QuestionType.CHOICE,
        solutionText = "Use chmod 755",
        solutionPhp = null,
        solutionPython = null,
        solutionC = null,
        solutionCplusPlus = null,
        solutionJavaScript = null,
        categoryId = 1,
        timesCorrect = 0,
        timesWrong = 0
    )

    private val q2 = Question( // TEXT, cat=2, passt auf "Docker"
        id = 2,
        questionText = "Docker: explain layers",
        questionCode = "",
        answerA = "",
        answerB = "",
        answerC = "",
        answerD = "",
        correctMask = 0,
        type = QuestionType.TEXT,
        solutionText = "Union FS ...",
        solutionPhp = null,
        solutionPython = null,
        solutionC = null,
        solutionCplusPlus = null,
        solutionJavaScript = null,
        categoryId = 2,
        timesCorrect = 0,
        timesWrong = 0
    )

    private val q3 = Question( // CODE, cat=1, passt auf "Python"
        id = 3,
        questionText = "Python: print numbers 0..2",
        questionCode = "for i in range(3): print(i)",
        answerA = "",
        answerB = "",
        answerC = "",
        answerD = "",
        correctMask = 0,
        type = QuestionType.CODE,
        solutionText = "",
        solutionPhp = null,
        solutionPython = "for i in range(3): print(i)",
        solutionC = null,
        solutionCplusPlus = null,
        solutionJavaScript = null,
        categoryId = 1,
        timesCorrect = 0,
        timesWrong = 0
    )

    private val all = listOf(q1, q2, q3)

    // ------------------------------------------------------------
    // Tests
    // ------------------------------------------------------------

    @Test
    fun filtered_withoutFilters_returnsAll() {
        val vm = ManageQuestionsViewModel(questionDaoFake(), categoryDaoFake())
        vm.search = ""
        vm.filterType = null
        vm.filterCategoryId = null

        val out = vm.filtered(all)
        assertEquals(all, out)
    }

    @Test
    fun filtered_byType_onlyMatchesThatType() {
        val vm = ManageQuestionsViewModel(questionDaoFake(), categoryDaoFake())
        vm.search = ""
        vm.filterCategoryId = null

        // TYPE = CHOICE -> nur q1
        vm.filterType = QuestionType.CHOICE
        assertEquals(listOf(q1), vm.filtered(all))

        // TYPE = TEXT -> nur q2
        vm.filterType = QuestionType.TEXT
        assertEquals(listOf(q2), vm.filtered(all))

        // TYPE = CODE -> nur q3
        vm.filterType = QuestionType.CODE
        assertEquals(listOf(q3), vm.filtered(all))
    }

    @Test
    fun filtered_byCategory_onlyMatchesThatCategory() {
        val vm = ManageQuestionsViewModel(questionDaoFake(), categoryDaoFake())
        vm.search = ""
        vm.filterType = null

        // catId = 1 -> q1, q3
        vm.filterCategoryId = 1
        assertEquals(listOf(q1, q3), vm.filtered(all))

        // catId = 2 -> q2
        vm.filterCategoryId = 2
        assertEquals(listOf(q2), vm.filtered(all))

        // catId = 999 -> leer
        vm.filterCategoryId = 999
        assertTrue(vm.filtered(all).isEmpty())
    }

    @Test
    fun filtered_bySearch_isCaseInsensitive_andSubstring() {
        val vm = ManageQuestionsViewModel(questionDaoFake(), categoryDaoFake())
        vm.filterType = null
        vm.filterCategoryId = null

        vm.search = "linux"
        assertEquals(listOf(q1), vm.filtered(all))

        vm.search = "DOCKER"
        assertEquals(listOf(q2), vm.filtered(all))

        vm.search = "print"
        assertEquals(listOf(q3), vm.filtered(all))

        vm.search = "   " // blank -> keine Einschränkung
        assertEquals(all, vm.filtered(all))
    }

    @Test
    fun filtered_combinesAllFilters_withLogicalAnd() {
        val vm = ManageQuestionsViewModel(questionDaoFake(), categoryDaoFake())
        // Suche: "print", Typ: CODE, Kategorie: 1  -> passt nur q3
        vm.search = "print"
        vm.filterType = QuestionType.CODE
        vm.filterCategoryId = 1

        val out = vm.filtered(all)
        assertEquals(listOf(q3), out)
    }
}
