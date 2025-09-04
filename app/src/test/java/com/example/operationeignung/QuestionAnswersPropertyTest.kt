package com.example.operationeignung

import com.example.operationeignung.data.database.entities.Question
import com.example.operationeignung.data.database.entities.QuestionType
import org.junit.Assert.assertEquals
import org.junit.Test

class QuestionAnswersPropertyTest {

    @Test
    fun answers_returnsExactlyFourStrings_inOrder_nullsBecomeEmpty() {
        val q = Question(
            id = 0,
            questionText = "Which options are valid?",
            questionCode = "",
            answerA = "A",
            answerB = "",
            answerC = "C",
            answerD = "",
            correctMask = 0b0101,
            type = QuestionType.CHOICE,
            solutionText = "",
            solutionPhp = null,
            solutionPython = null,
            solutionC = null,
            solutionCplusPlus = null,
            solutionJavaScript = null,
            categoryId = null,
            timesCorrect = 0,
            timesWrong = 0
        )

        val answers = q.answers
        assertEquals(listOf("A", "", "C", ""), answers)
    }
}
