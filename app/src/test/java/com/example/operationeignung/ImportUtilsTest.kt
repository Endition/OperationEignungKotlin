package com.example.operationeignung

import com.example.operationeignung.data.database.dao.CategoryDao
import com.example.operationeignung.data.database.dao.QuestionDao
import com.example.operationeignung.data.database.entities.Category
import com.example.operationeignung.data.database.entities.Question
import com.example.operationeignung.ui.jsonimport.ImportError
import com.example.operationeignung.ui.jsonimport.ImportReport
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.lang.reflect.Method
import java.lang.reflect.Proxy

/**
 * Unit-Tests für den JSON-Import (ImportUtils.importFromJsonString).
 *
 * Diese Tests verwenden Java Dynamic Proxy, um CategoryDao und QuestionDao
 * als In-Memory-Fakes bereitzustellen. Implementiert werden nur die Methoden,
 * die im Import tatsächlich verwendet werden: findByName/insert/insertAll.
 */
class ImportUtilsTest {

    // --- In-Memory-Speicher ---
    private data class Memory(
        val categories: MutableList<Category> = mutableListOf(),
        val questions: MutableList<Question> = mutableListOf(),
        var nextCategoryId: Int = 1
    )

    // --- Helfer: Java-Proxy für Interface-Fakes ---
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

    private fun buildDaos(mem: Memory): Pair<CategoryDao, QuestionDao> {
        val categoryDao = proxy<CategoryDao> { method, args ->
            when (method.name) {
                // suspend fun findByName(name: String): Category?
                "findByName" -> {
                    val name = (args?.get(0) as String).trim().lowercase()
                    mem.categories.firstOrNull { it.name.trim().lowercase() == name }
                }
                // suspend fun insert(category: Category): Long
                "insert" -> {
                    val cat = args?.get(0) as Category
                    val trimmed = cat.name.trim()
                    val exists = mem.categories.any { it.name.trim().equals(trimmed, ignoreCase = true) }
                    if (exists) -1L
                    else {
                        val new = cat.copy(id = mem.nextCategoryId++)
                        mem.categories.add(new)
                        new.id.toLong()
                    }
                }
                else -> unsupported(method)
            }
        }

        val questionDao = proxy<QuestionDao> { method, args ->
            when (method.name) {
                // suspend fun insertAll(questions: List<Question>)
                "insertAll" -> {
                    @Suppress("UNCHECKED_CAST")
                    val list = args?.get(0) as List<Question>
                    mem.questions.addAll(list)
                    Unit
                }
                else -> unsupported(method)
            }
        }

        return categoryDao to questionDao
    }

    private fun unsupported(m: Method): Nothing =
        throw UnsupportedOperationException("Method not supported in test fake: ${m.declaringClass.simpleName}.${m.name}(${m.parameterTypes.joinToString()})")

    // ------------------------------------------------------------
    //                        TESTS
    // ------------------------------------------------------------

    @Test
    fun import_insertsQuestions_createsOrReusesCategories_reportsErrors() = runBlocking {
        val mem = Memory()

        // Vorab eine Kategorie, um Re-Use zu testen:
        mem.categories += Category(id = mem.nextCategoryId++, name = "Basics")  // ✅ statt: mem.categories plusAssign ...

        val json = """
            [
              {
                "question_text": "Which outputs 1 in Python?",
                "question_code": "```python\r\nprint(1)\r\n```",
                "answers": ["print(1)\r\n", "echo 1", "", ""],
                "correct_mask": 1,
                "type": "choice",
                "solution_text": "Use print in Python.",
                "solution_code": "```python\nprint(1)\n```",
                "category": "Basics"
              },
              {
                "question_text": "Show number in JS",
                "question_code": "```js\nconsole.log(1)\n```",
                "answers": [],
                "correct_mask": 31,
                "type": "code",
                "solution_code": "```js\nconsole.log(1)\n```",
                "category": "NewCat"
              },
              {
                "question_text": "Bad type",
                "type": "image"
              },
              {
                "question_text": "Empty choice answers",
                "type": "choice",
                "answers": []
              },
              {
                "question_text": "   ",
                "type": "text"
              }
            ]
        """.trimIndent()

        val (categoryDao, questionDao) = buildDaos(mem)

        val report: ImportReport = com.example.operationeignung.ui.jsonimport.importFromJsonString(
            json = json,
            questionDao = questionDao,
            categoryDao = categoryDao
        )

        // Report-Checks
        assertEquals(2, report.imported)
        assertEquals(3, report.skipped)
        assertEquals(3, report.errors.size)
        val reasons = report.errors.map(ImportError::reason).toSet()
        assertTrue("invalid type" in reasons)
        assertTrue("choice without answers" in reasons)
        assertTrue("question missing/empty" in reasons)

        // Kategorien: Basics (reuse) + NewCat (neu)
        assertTrue(mem.categories.any { it.name.equals("Basics", ignoreCase = true) })
        assertTrue(mem.categories.any { it.name.equals("NewCat", ignoreCase = true) })
        assertEquals(2, mem.categories.size)

        // Fragen: 2 valide importiert
        assertEquals(2, mem.questions.size)
        val q1 = mem.questions[0]
        val q2 = mem.questions[1]

        // Q1 Normalisierungen
        assertEquals(listOf("print(1)\n", "echo 1", "", ""), listOf(q1.answerA, q1.answerB, q1.answerC, q1.answerD))
        assertEquals("print(1)", q1.solutionCode)
        assertEquals("Which outputs 1 in Python?", q1.questionText)
        assertEquals("print(1)", q1.questionCode)
        assertTrue(q1.categoryId != null)
        assertEquals(0b0001, q1.correctMask)

        // Q2: Maskierung auf 4 Bit, Code normalisiert
        assertEquals(0b1111, q2.correctMask)
        assertEquals("console.log(1)", q2.solutionCode)
        assertEquals("console.log(1)", q2.questionCode)
        val newCatId = mem.categories.first { it.name == "NewCat" }.id
        assertEquals(newCatId, q2.categoryId)
    }

    @Test
    fun import_trimsAndDeduplicatesCategories_caseInsensitive() = runBlocking {
        val mem = Memory()
        val (categoryDao, questionDao) = buildDaos(mem)

        val json = """
            [
              { "question_text": "Q1", "type": "text", "category": "  Security " },
              { "question_text": "Q2", "type": "text", "category": "security" },
              { "question_text": "Q3", "type": "text", "category": "SECURITY" }
            ]
        """.trimIndent()

        val report = com.example.operationeignung.ui.jsonimport.importFromJsonString(
            json = json,
            questionDao = questionDao,
            categoryDao = categoryDao
        )

        assertEquals(3, report.imported)
        assertEquals(0, report.skipped)

        // Nur eine Kategorie im Speicher (Trim + case-insensitive)
        assertEquals(1, mem.categories.size)
        assertEquals("  Security ", mem.categories.first().name)
        val catId = mem.categories.first().id
        assertTrue(mem.questions.all { it.categoryId == catId })
    }
}
