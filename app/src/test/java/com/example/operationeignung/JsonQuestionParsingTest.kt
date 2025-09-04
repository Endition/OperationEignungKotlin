package com.example.operationeignung

import com.example.operationeignung.data.model.JsonQuestion
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class JsonQuestionParsingTest {

    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun jsonQuestion_parsesAllAliasedFields() {
        val input = """
            [
              {
                "question_text": "Q1?",
                "question_code": "```python\nprint(1)\n```",
                "answers": ["A","B","C","D"],
                "correct_mask": 5,
                "type": "choice",
                "solution_text": "Because…",
                "solution_php": "<?php echo 1; ?>",
                "solution_python": "print(1)",
                "solution_c": "printf(\"1\");",
                "solution_cpp": "std::cout << 1;",
                "solution_js": "console.log(1)",
                "category": "Basics"
              }
            ]
        """.trimIndent()

        val list = json.decodeFromString(ListSerializer(JsonQuestion.serializer()), input)
        assertEquals(1, list.size)

        val q = list.first()
        assertEquals("Q1?", q.questionText)
        assertEquals("```python\nprint(1)\n```", q.questionCode)
        assertEquals(listOf("A","B","C","D"), q.answers)
        assertEquals(5, q.correctMask)
        assertEquals("choice", q.type)
        assertEquals("Because…", q.solutionText)
        assertEquals("<?php echo 1; ?>", q.solutionCode)
        assertEquals("Basics", q.category)
    }

    @Test
    fun jsonQuestion_defaults_whenFieldsMissing() {
        val input = """[{}]"""
        val list = json.decodeFromString(ListSerializer(JsonQuestion.serializer()), input)
        val q = list.first()

        // Defaults aus der Datenklasse
        assertEquals(null, q.questionText)
        assertEquals(emptyList<String>(), q.answers)
        assertEquals(0, q.correctMask)
        assertTrue(q.type == null || q.type!!.isEmpty())
    }
}
