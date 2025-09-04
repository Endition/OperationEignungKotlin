package com.example.operationeignung.ui.manage

import androidx.lifecycle.ViewModel
import com.example.operationeignung.data.database.dao.CategoryDao
import com.example.operationeignung.data.database.dao.QuestionDao
import com.example.operationeignung.data.database.entities.Category
import com.example.operationeignung.data.database.entities.Question
import com.example.operationeignung.data.database.entities.QuestionType
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

/** ViewModel für CleanUpDataScreen */
@HiltViewModel
class CleanUpDataViewModel @Inject constructor(
    private val questionDao: QuestionDao,
    private val categoryDao: CategoryDao
) : ViewModel() {

    /** Automatische Kategorisierung – gibt Anzahl aktualisierter Fragen zurück. */
    suspend fun autoCategorize(): Int =
        autoCategorizeQuestions(questionDao, categoryDao)

    /** Bereinigt „leere“/duplizierte Fragen – liefert Anzahl gelöschter Datensätze. */
    suspend fun cleanup(): Int =
        cleanupDatabase(questionDao)

    /** Setzt die Statistik-Felder aller Fragen zurück – liefert verbleibende Fragenanzahl. */
    suspend fun resetStats(): Int =
        resetStatistics(questionDao)

    /** Leert die Fragen-Tabelle komplett – liefert verbleibende Fragenanzahl (nach dem Truncate/Reset). */
    suspend fun truncateQuestions(): Int =
        trunicateQuestionsTable(questionDao)

    /** Leert die Fragendatenbank */
    suspend fun trunicateQuestionsTable(questionDao: QuestionDao): Int = withContext(Dispatchers.IO)
    {
        questionDao.trunicateQuestionsTable()
        return@withContext questionDao.getAllQuestionsList().size
    }

    /** Setzt Statistik-Zähler für alle Fragen auf 0 */
    suspend fun resetStatistics(questionDao: QuestionDao): Int = withContext(Dispatchers.IO)
    {
        questionDao.resetStatistics()
        return@withContext questionDao.getAllQuestionsList().size
    }

    /** Bereinigt die Fragen-Datenbank: */
    suspend fun cleanupDatabase(questionDao: QuestionDao): Int = withContext(Dispatchers.IO) {
        val all: List<Question> = questionDao.getAllQuestionsList()
        var deletedTotal = 0

        // 1) Exakte Duplikate
        deletedTotal += deleteExactDuplicates(all, questionDao)

        // 2) Sehr ähnliche Fragen (O(n²) – wie bisher, aber gekapselt)
        deletedTotal += deleteNearDuplicates(all, questionDao)

        // 3) Ungültige Choice-Fragen (alle Antworten leer)
        deletedTotal += deleteEmptyChoices(all, questionDao)

        // 4) Platzhalter-Inhalte ("code","choice","text") entfernen
        deletedTotal += deleteForbiddenPlaceholderRows(all, questionDao)

        return@withContext deletedTotal
    }

    /** Sucht Kategorien mit Namen **/
    private suspend fun ensureCategory(
        categoryDao: CategoryDao,
        name: String
    ): Category {
        val existing = categoryDao.findByName(name)
        if (existing != null) return existing
        val newId = categoryDao.insert(Category(name = name))
        return Category(id = newId.toInt(), name = name)
    }

    /** 1) Exakte Duplikate – nur kleinste ID bleibt. */
    private suspend fun deleteExactDuplicates(
        all: List<Question>,
        questionDao: QuestionDao
    ): Int {
        val seen = mutableSetOf<String>()
        val dupes = mutableListOf<Question>()
        for (q in all.sortedBy { it.id }) {
            val key = q.questionText.orEmpty().trim().lowercase()
            if (key.isEmpty()) continue
            if (!seen.add(key)) dupes += q
        }
        dupes.forEach { questionDao.delete(it.id) }
        return dupes.size
    }

    /** 2) Sehr ähnliche Fragen entfernen. */
    private suspend fun deleteNearDuplicates(
        all: List<Question>,
        questionDao: QuestionDao
    ): Int {
        val toDelete = mutableSetOf<Question>()
        val rows = all.sortedBy { it.id }
        for (i in rows.indices) {
            val q1 = rows[i]
            for (j in i + 1 until rows.size) {
                val q2 = rows[j]
                if (q2 in toDelete) continue
                val ratio = similarity(q1.questionText.orEmpty(), q2.questionText.orEmpty())
                if (ratio > 0.90) toDelete += q2
            }
        }
        toDelete.forEach { questionDao.delete(it.id) }
        return toDelete.size
    }

    /** 3) MC-Fragen entfernen, wenn alle vier Antworten leer sind. */
    private suspend fun deleteEmptyChoices(
        all: List<Question>,
        questionDao: QuestionDao
    ): Int {
        val victims = all.filter { q ->
            q.type == QuestionType.CHOICE &&
                    q.answerA.isNullOrBlank() &&
                    q.answerB.isBlank() &&
                    q.answerC.isBlank() &&
                    q.answerD.isBlank()
        }
        victims.forEach { questionDao.delete(it.id) }
        return victims.size
    }

    /** 4) Zeilen löschen, wenn irgendeine relevante Spalte nur verbotene Platzhalter enthält. */
    private suspend fun deleteForbiddenPlaceholderRows(
        all: List<Question>,
        questionDao: QuestionDao
    ): Int {
        val forbidden = setOf("code", "choice", "text")
        val cols: List<(Question) -> String?> = listOf(
            { it.questionText }, { it.questionCode },
            { it.answerA }, { it.answerB }, { it.answerC }, { it.answerD },
            { it.solutionText }, { it.solutionCode }
        )
        val bad = all.filter { q -> cols.any { get -> forbidden.contains(get(q)?.trim()?.lowercase()) } }
        bad.forEach { questionDao.delete(it.id) }
        return bad.size
    }

    /**
     * Ähnlichkeit zweier Strings zwischen 0.0 und 1.0
     */
    private fun similarity(s1: String, s2: String): Double {
        if (s1.isEmpty() && s2.isEmpty()) return 1.0
        val longer = if (s1.length > s2.length) s1 else s2
        val shorter = if (s1.length > s2.length) s2 else s1
        val matchCount = longer.commonPrefixWith(shorter).length
        return matchCount.toDouble() / longer.length
    }

    //#########################################
    /**
     * Automatische Kategorisierung nach Keyword-Mapping.
     * - Überspringt Fragen mit bereits gesetzter Kategorie.
     * - Wenn nichts passt -> "Sonstiges".
     */
    suspend fun autoCategorizeQuestions(
        questionDao: QuestionDao,
        categoryDao: CategoryDao
    ): Int {
        val mapping = buildKeywordMapping()                      // 1) deine Liste wie gehabt
        val all: List<Question> = questionDao.getAllQuestionsList()
        var updated = 0

        for (q in all) {
            if (q.categoryId != null) continue                   // bereits zugeordnet -> skip

            val text = buildSearchText(q)                        // 2) Textquellen bündig zusammenführen (lowercased)
            val matchedName = detectCategoryName(text, mapping)  // 3) Matchen gegen Keywords (früh exit)

            if (matchedName != null) {
                val cat = ensureCategory(categoryDao, matchedName)
                questionDao.update(q.copy(categoryId = cat.id))
                updated += 1
            } else {
                val misc = ensureCategory(categoryDao, "Sonstiges")
                questionDao.update(q.copy(categoryId = misc.id))
                updated += 1
            }
        }
        return updated
    }

    /** Keyword-Mapping  */
    private fun buildKeywordMapping(): Map<String, List<String>> = mapOf(
        "Netzwerktechnik" to listOf(
            "tcp","udp","osi","ip-adresse","ip adresse","subnet","vlan","switch","router","firewall",
            "dns","http","https","port","protokoll","mac","gateway","netzwerk","broadcast","arp","icmp",
            "snmp","dhcp","nat","lan","wan","wlan","ethernet","paket","routing","netzmaske","netzwerkadresse",
            "subnetting","layer","osi-schicht","vpn","ssid"
        ),
        "MS Windows" to listOf(
            "windows","powershell","active directory","cmd","registry","dienst","domäne","benutzerkonto",
            "gruppenrichtlinie","netzlaufwerk","explorer","taskmanager","systemsteuerung","windows server",
            "ntfs","cmdlet","laufwerk","windows update","windows defender","remotedesktop","windows firewall",
            "dateifreigabe","windows 10","windows 11","windows 7","windows 8"
        ),
        // ... (Rest deiner Kategorien aus der Datei) ...
    )

    /** Fügt alle relevanten Textfelder zusammen und normalisiert auf lowercase. */
    private fun buildSearchText(q: Question): String = buildString {
        appendLine(q.questionText.orEmpty())
        appendLine(q.questionCode.orEmpty())
        appendLine(q.answerA.orEmpty()); appendLine(q.answerB.orEmpty())
        appendLine(q.answerC.orEmpty()); appendLine(q.answerD.orEmpty())
        appendLine(q.solutionText.orEmpty())
        appendLine(q.solutionCode.orEmpty())
    }.lowercase()

    /** Findet den ersten Kategorienamen, dessen Keyword im Text vorkommt (frühes Beenden). */
    private fun detectCategoryName(text: String, mapping: Map<String, List<String>>): String? {
        for ((name, keywords) in mapping) {
            if (keywords.any { kw -> kw in text }) return name
        }
        return null
    }


}
