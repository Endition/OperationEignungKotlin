package com.example.operationeignung.data.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.operationeignung.data.database.entities.Question
import com.example.operationeignung.data.database.entities.QuestionType
import kotlinx.coroutines.flow.Flow

data class CategoryStatRow(
    val category: String,
    val cnt: Int,
    val neverAnswered: Int,
    val sumCorrect: Int,
    val sumWrong: Int
)

/** Frage-Datenbank-Zugriffsklasse **/
@Dao
interface QuestionDao {

    /** Frage einfügen **/
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(question: Question)

    /** Frageliste einfügen **/
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(questions: List<Question>)

    /** Frage update **/
    @Update
    suspend fun update(question: Question)

    /** Alle Fragen updaten **/
    @Update
    suspend fun updateAll(items: List<Question>)

    /** Frage löschen **/
    @Query("DELETE FROM questions WHERE id = :id")
    suspend fun delete(id: Int)

    /** Frage laden bei id **/
    @Query("SELECT * FROM questions WHERE id = :id LIMIT 1")
    suspend fun getById(id: Int): Question?

    /** Frage laden anhand Fragetext **/
    @Query("SELECT id FROM questions WHERE question = :text LIMIT 1")
    suspend fun findIdByText(text: String): Int?

    /** QuestionListe als Flow laden **/
    @Query("SELECT * FROM questions ORDER BY id DESC")
    fun getAllQuestionsFlow(): Flow<List<Question>>

    /** QuestionListe als statische Liste laden  **/
    @Query("SELECT * FROM questions ORDER BY id DESC")
    suspend fun getAllQuestionsList(): List<Question>

    /** Einzelne Frage als Flow laden **/
    @Query("SELECT * FROM questions WHERE id = :id LIMIT 1")
    fun observeById(id: Int): Flow<Question?>

    // Statistik
    /** times_correct + 1 speichern **/
    @Query("UPDATE questions SET times_correct = times_correct + 1 WHERE id = :id")
    suspend fun incCorrect(id: Int)

    /** times_wrong + 1 speichern **/
    @Query("UPDATE questions SET times_wrong = times_wrong + 1 WHERE id = :id")
    suspend fun incWrong(id: Int)

    /** Zufällige Frage laden **/
    @Query("SELECT * FROM questions ORDER BY RANDOM() LIMIT 1")
    suspend fun getRandom(): Question?

    /** Kategorien neu zuordnen **/
    @Query("UPDATE questions SET category_id = :targetId WHERE category_id = :sourceId")
    suspend fun reassignCategory(sourceId: Int, targetId: Int): Int

    /** Statitisk resetten **/
    @Query("UPDATE questions SET times_wrong = 0, times_correct = 0")
    suspend fun resetStatistics()

    /** Datenbank leeren **/
    @Query("DELETE FROM questions")
    suspend fun trunicateQuestionsTable()

    /** Anzahl Fragen laden **/
    @Query("SELECT COUNT(*) FROM questions")
    fun totalQuestionsFlow(): Flow<Int>

    /** Anzahl korrekter Fragen laden **/
    @Query("SELECT COUNT(*)  FROM questions WHERE times_correct > times_wrong")
    fun totalCorrectFlow(): Flow<Int>

    /** Anzahl falscher Fragen laden **/
    @Query("SELECT COUNT(*)  FROM questions WHERE times_correct <= times_wrong AND times_wrong != 0")
    fun totalWrongFlow(): Flow<Int>

    /** Anzahl falsch/richtig/nie pro Kategory laden **/
    @Query("""
        SELECT c.name AS category,
               COUNT(q.id) AS cnt,
               SUM(CASE WHEN q.times_correct+q.times_wrong=0 THEN 1 ELSE 0 END) AS neverAnswered,
               SUM(CASE WHEN q.times_correct > q.times_wrong AND q.times_correct != 0 THEN 1 ELSE 0 END) AS sumCorrect,
               SUM(CASE WHEN q.times_correct <= q.times_wrong AND q.times_wrong != 0 THEN 1 ELSE 0 END) AS sumWrong
        FROM categories c
        LEFT JOIN questions q ON q.category_id = c.id
        GROUP BY c.name
        ORDER BY c.name
    """)
    fun categoryStatsFlow(): Flow<List<CategoryStatRow>>

    /** Lade zufällige Fragen mit Type-Filter**/
    @Query("""
        SELECT * FROM questions
        WHERE (:type IS NULL OR type = :type)
        ORDER BY RANDOM() LIMIT 1
    """)
    suspend fun pickRandomAllCats(type: QuestionType?): Question?

    /** Lade zufällige Fragen mit Type und Kategorie-Filter**/
    @Query("""
        SELECT * FROM questions
        WHERE (:type IS NULL OR type = :type)
          AND category_id IN (:categoryIds)
        ORDER BY RANDOM() LIMIT 1
    """)
    suspend fun pickRandomWithCats(type: QuestionType?, categoryIds: List<Int>): Question?

    /** Lade neue Fragen mit Type-Filter**/
    @Query("""
        SELECT * FROM questions
        WHERE (times_correct=0 AND times_wrong=0)
          AND (:type IS NULL OR type = :type)
        ORDER BY RANDOM() LIMIT 1
    """)
    suspend fun pickNewAllCats(type: QuestionType?): Question?

    /** Lade neue Fragen mit Type und Kategorie-Filter**/
    @Query("""
        SELECT * FROM questions
        WHERE (times_correct=0 AND times_wrong=0)
          AND (:type IS NULL OR type = :type)
          AND category_id IN (:categoryIds)
        ORDER BY RANDOM() LIMIT 1
    """)
    suspend fun pickNewWithCats(type: QuestionType?, categoryIds: List<Int>): Question?

    /** Lade falsche Fragen mit Type und Kategorie-Filter**/
    @Query("""
        SELECT * FROM questions
        WHERE times_wrong > times_correct
          AND (:type IS NULL OR type = :type)
        ORDER BY RANDOM() LIMIT 1
    """)
    suspend fun pickWrongAllCats(type: QuestionType?): Question?

    /** Lade falsche Fragen mit Type und Kategorie-Filter**/
    @Query("""
        SELECT * FROM questions
        WHERE times_wrong > times_correct
          AND (:type IS NULL OR type = :type)
          AND category_id IN (:categoryIds)
        ORDER BY RANDOM() LIMIT 1
    """)
    suspend fun pickWrongWithCats(type: QuestionType?, categoryIds: List<Int>): Question?
}
