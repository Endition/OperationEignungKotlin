package com.example.operationeignung

import com.example.operationeignung.data.database.entities.QuestionType
import org.junit.Assert.assertEquals
import org.junit.Test

class QuestionTypeTest {

    @Test
    fun fromDb_acceptsCanonicalValues_caseInsensitive() {
        assertEquals(QuestionType.CHOICE, QuestionType.fromDb("choice"))
        assertEquals(QuestionType.TEXT, QuestionType.fromDb("TEXT"))
        assertEquals(QuestionType.CODE, QuestionType.fromDb("CoDe"))
    }

    @Test
    fun fromDb_nullOrUnknown_fallsBackToChoice() {
        assertEquals(QuestionType.CHOICE, QuestionType.fromDb(null))
        assertEquals(QuestionType.CHOICE, QuestionType.fromDb(""))
        assertEquals(QuestionType.CHOICE, QuestionType.fromDb("unknown"))
    }

    @Test
    fun dbValue_roundTrip_isStable() {
        for (t in QuestionType.values()) {
            val roundTripped = QuestionType.fromDb(t.dbValue)
            assertEquals(t, roundTripped)
        }
    }
}
