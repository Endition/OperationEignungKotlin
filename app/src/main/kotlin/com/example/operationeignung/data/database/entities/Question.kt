package com.example.operationeignung.data.database.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/** Question-Datenklasse **/
@Entity(
    tableName = "questions",
    indices = [
        Index("category_id"),
        Index("type")
    ],
    foreignKeys = [ForeignKey(
        entity = Category::class,
        parentColumns = ["id"],
        childColumns = ["category_id"],
        onDelete = ForeignKey.SET_NULL
    )]
)
data class Question(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,

    @ColumnInfo(name = "question")
    val questionText: String,

    @ColumnInfo(name = "question_code", defaultValue = "")
    val questionCode: String = "",

    @ColumnInfo(name = "answer_a", defaultValue = "")
    val answerA: String? = "",

    @ColumnInfo(name = "answer_b", defaultValue = "")
    val answerB: String = "",

    @ColumnInfo(name = "answer_c", defaultValue = "")
    val answerC: String = "",

    @ColumnInfo(name = "answer_d", defaultValue = "")
    val answerD: String = "",

    @ColumnInfo(name = "correct_mask", defaultValue = "0")
    val correctMask: Int = 0,

    @ColumnInfo(name = "type", defaultValue = "choice")
    val type: QuestionType = QuestionType.CHOICE,

    @ColumnInfo(name = "solution_text", defaultValue = "")
    val solutionText: String = "",

    @ColumnInfo(name = "solution_code", defaultValue = "")
    val solutionCode: String = "",

    @ColumnInfo(name = "category_id")
    val categoryId: Int? = null,

    @ColumnInfo(name = "times_correct", defaultValue = "0")
    val timesCorrect: Int = 0,

    @ColumnInfo(name = "times_wrong", defaultValue = "0")
    val timesWrong: Int = 0,
) {
    val answers: List<String>
        get() = listOf(answerA, answerB, answerC, answerD).map { it.orEmpty() }
}