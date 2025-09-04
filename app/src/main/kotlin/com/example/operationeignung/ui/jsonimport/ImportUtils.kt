package com.example.operationeignung.ui.jsonimport

import androidx.room.withTransaction
import com.example.operationeignung.data.database.AppDatabase
import com.example.operationeignung.data.database.dao.CategoryDao
import com.example.operationeignung.data.database.dao.QuestionDao
import com.example.operationeignung.data.database.entities.Category
import com.example.operationeignung.data.database.entities.Question
import com.example.operationeignung.data.database.entities.QuestionType
import com.example.operationeignung.data.model.JsonQuestion

/** Data class für Fehler beim Import **/
data class ImportError(
    val index: Int,
    val reason: String,
    val questionText: String
)

/** Data class für den Import **/
data class ImportReport(
    val imported: Int,
    val skipped: Int,
    val errors: List<ImportError>
)

/** Import Konfliktmodi **/
enum class ConflictMode(val displayName: String) {
    SKIP("Überspringen"),
    UPDATE("Aktualisieren"),
}

/** Wenn die Kategorie schon existiert, nimm die vorhanden um Dopplungen zu vermeiden **/
private suspend fun ensureCategoryId(
    rawName: String?,
    categoryDao: CategoryDao
): Int? {
    val name = rawName?.trim().orEmpty()
    if (name.isEmpty()) return null

    categoryDao.findByName(name)?.let { return it.id }

    val newId = categoryDao.insert(Category(name = name))
    if (newId > 0) return newId.toInt()

    return categoryDao.findByName(name)?.id
}

/** Code Bestandteil im JSON normalisieren*/
private fun normalizeCode(raw: String?): String {
    if (raw.isNullOrEmpty()) return ""
    val s = raw.replace("\r\n", "\n") // CRLF → LF (optional)
    val regex = Regex(
        pattern = """^\s*```[A-Za-z0-9_+-]*\n(.*?)\n```\s*$""",
        options = setOf(RegexOption.DOT_MATCHES_ALL) // . matcht auch Newlines
    )
    val m = regex.matchEntire(s)
    return m?.groupValues?.get(1) ?: s
}


/**
 * Der tatsächliche Import.
 */
suspend fun importFromJsonString(
    json: String,
    questionDao: QuestionDao,
    categoryDao: CategoryDao,
    conflictMode: ConflictMode = ConflictMode.SKIP
): ImportReport {
    val trimmed = json.trim()

    // Eingabe leer? -> Sofort abbrechen
    if (trimmed.isEmpty()) {
        return ImportReport(
            imported = 0,
            skipped = 0,
            errors = listOf(ImportError(-1, "Eingabe ist leer.", ""))
        )
    }

    // Zentrale JSON-Konfiguration
    val jsonEngine = JsonConfig.relaxed

    // 1) Root parsen
    val root = try {
        jsonEngine.parseToJsonElement(trimmed)
    } catch (e: Exception) {
        return ImportReport(
            imported = 0,
            skipped = 0,
            errors = listOf(ImportError(-1, "JSON parse error: ${e.message}", ""))
        )
    }

    // 2) Array erzwingen
    val arr = (root as? kotlinx.serialization.json.JsonArray) ?: run {
        return ImportReport(
            imported = 0,
            skipped = 0,
            errors = listOf(ImportError(-1, "Erwarte ein JSON-Array von Fragen.", ""))
        )
    }

    // 3) Elemente dekodiere
    val (decoded, decodeErrors) = decodeElements(arr, jsonEngine)

    // 4) Leeres/ungültiges Array abfangen
    if (decoded.isEmpty()) {
        return ImportReport(
            imported = 0,
            skipped = 0,
            errors = listOf(
                ImportError(
                    -1,
                    "JSON enthält keine Fragen oder Struktur passt nicht (Array erwartet).",
                    ""
                )
            )
        )
    }

    // 5) Vorvalidierung
    val (preValid, preValErrors) = validateAll(decoded)

    // Sammelcontainer
    val toInsert = mutableListOf<Question>()
    val toUpdate = mutableListOf<Question>()
    val errors = mutableListOf<ImportError>().apply {
        addAll(decodeErrors)
        addAll(preValErrors)
    }
    var skipped = decodeErrors.size + preValErrors.size

    // 6) Durchlauf über vorvalidierte Fragen
    preValid.forEachIndexed { idx, jq ->
        // Entity bauen
        val (entity, qText) = buildEntity(jq, categoryDao)

        // Duplikat anhand des Fragetexts prüfen
        val existingId = questionDao.findIdByText(qText)

        // Konfliktstrategie anwenden
        if (existingId != null) {
            when (conflictMode) {
                ConflictMode.SKIP -> {
                    errors.add(ImportError(idx, "Duplikat übersprungen", qText))
                    skipped++
                    return@forEachIndexed
                }
                ConflictMode.UPDATE -> {
                    toUpdate.add(entity.copy(id = existingId))
                    return@forEachIndexed
                }
            }
        }

        // Kein Duplikat -> einfügen
        toInsert.add(entity)
    }

    // 7) Persistieren – gesammelt wie zuvor
    if (toInsert.isNotEmpty()) questionDao.insertAll(toInsert)
    if (toUpdate.isNotEmpty()) questionDao.updateAll(toUpdate)

    // 8) Report zurückgeben
    return ImportReport(
        imported = toInsert.size,
        skipped = skipped,
        errors = errors
    )
}

/** Dekodiert jedes Array-Element sicher und sammelt Fehler pro Index. */
private fun decodeElements(
    arr: kotlinx.serialization.json.JsonArray,
    json: kotlinx.serialization.json.Json
): Pair<List<JsonQuestion>, List<ImportError>> {
    val decoded = mutableListOf<JsonQuestion>()
    val decodeErrors = mutableListOf<ImportError>()

    // Pro Element dekodieren; Fehler je Element aufnehmen
    arr.forEachIndexed { idx, el ->
        try {
            decoded += json.decodeFromJsonElement(JsonQuestion.serializer(), el)
        } catch (e: Exception) {
            decodeErrors += ImportError(
                idx,
                "Eintrag kann nicht dekodiert werden: ${e.message}",
                el.toString().take(120)
            )
        }
    }
    return decoded to decodeErrors
}

/** Erstellt Question-Entity aus einem JsonQuestion */
private suspend fun buildEntity(
    jq: JsonQuestion,
    categoryDao: CategoryDao
): Pair<Question, String> {
    val qText = (jq.questionText ?: "").trim() // Fragetext trimmen
    val typeStr = (jq.type ?: "").trim().lowercase() // Typ-Zeichenkette
    val typeEnum = QuestionType.fromDb(typeStr) // Enum aus DB-Wert

    // Kategorie-ID ermitteln
    val catId: Int? = ensureCategoryId(jq.category, categoryDao)

    // Antworten auf 4 Einträge auffüllen; CRLF -> LF; null -> ""
    val answers = (jq.answers + listOf("", "", "", "")).take(4)

    // Nur Bits 0..3 für die Korrektmaske zulassen
    val rawMask = jq.correctMask and 0b1111

    // Entity aufbauen
    val entity = Question(
        questionText = qText,
        questionCode = normalizeCode(jq.questionCode),  // Code normalisieren
        answerA = answers.getOrNull(0)?.replace("\r\n", "\n").orEmpty(),
        answerB = answers.getOrNull(1)?.replace("\r\n", "\n").orEmpty(),
        answerC = answers.getOrNull(2)?.replace("\r\n", "\n").orEmpty(),
        answerD = answers.getOrNull(3)?.replace("\r\n", "\n").orEmpty(),
        correctMask = rawMask,
        type = typeEnum,
        solutionText = jq.solutionText?.trim().orEmpty(),
        solutionCode = normalizeCode(jq.solutionCode),   // Code normalisieren
        categoryId = catId,
        timesCorrect = 0,
        timesWrong = 0
    )
    return entity to qText
}


//Kapselund des Imports, sodass alles in einer Transaktion ausgeführt wird
suspend fun importFromJsonStringTx(
    json: String,
    db: AppDatabase,
    questionDao: QuestionDao,
    categoryDao: CategoryDao,
    conflictMode: ConflictMode
): ImportReport {
    return db.withTransaction {
        importFromJsonString(json, questionDao, categoryDao,conflictMode)
    }
}