package com.example.operationeignung

import com.example.operationeignung.data.database.converter.Converters
import com.example.operationeignung.data.database.entities.QuestionType
import org.junit.Assert.assertEquals
import org.junit.Test

class ConvertersTest {

    private val conv = Converters()

    @Test
    fun fromQuestionType_emitsDbValue() {
        assertEquals("choice", conv.fromQuestionType(QuestionType.CHOICE))
        assertEquals("text", conv.fromQuestionType(QuestionType.TEXT))
        assertEquals("code", conv.fromQuestionType(QuestionType.CODE))
    }

    @Test
    fun toQuestionType_parsesKnown_orFallsBack() {
        assertEquals(QuestionType.TEXT, conv.toQuestionType("text"))
        assertEquals(QuestionType.CODE, conv.toQuestionType("CODE"))
        assertEquals(QuestionType.CHOICE, conv.toQuestionType(null))
        assertEquals(QuestionType.CHOICE, conv.toQuestionType("???"))
    }
}
