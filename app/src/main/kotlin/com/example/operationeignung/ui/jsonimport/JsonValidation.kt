package com.example.operationeignung.ui.jsonimport

import com.example.operationeignung.data.database.entities.QuestionType
import com.example.operationeignung.data.model.JsonQuestion

// in ImportUtils.kt oder eigener Validator-Datei
data class ValidationIssue(val index: Int, val field: String, val message: String)

data class ValidationResult(
    val valid: List<JsonQuestion>,
    val errors: List<ImportError>
)

fun validateAll(items: List<JsonQuestion>): ValidationResult {
    val valid = mutableListOf<JsonQuestion>()
    val errors = mutableListOf<ImportError>()

    items.forEachIndexed { idx, q ->
        val issues = mutableListOf<ValidationIssue>()

        //text auf Enum matchen
        val typeEnum = QuestionType.fromDb(q.type)

        val questionText = q.questionText?.trim().orEmpty()
        val solutionText = q.solutionText?.trim().orEmpty()
        val solutionCode = q.solutionCode?.trim().orEmpty()


        val type = q.type?.trim()?.lowercase().orEmpty().ifEmpty { "text" }
        val answers = q.answers.map { it.trim() }.filter { it.isNotEmpty() }

        //schneidet alle Bits oberhalb von 0..3 ab (max. 4 Antworten).
        val rawMask = q.correctMask and 0b1111

        //Frage darf nie leer sein.
        if (questionText.isEmpty()) issues += ValidationIssue(idx, "questionText", "Frage darf nicht leer sein")

        //Nur die drei Typen erlauben
        if (typeEnum != QuestionType.CHOICE && typeEnum != QuestionType.TEXT && typeEnum != QuestionType.CODE) {
            issues += ValidationIssue(idx, "type", "darf nur 'choice', 'text' oder 'code' sein. Eingabe ist: ${q.type}")
        }

        //Bei Code Fragen muss es eine Musterlösung mit Code geben
        if (typeEnum == QuestionType.CODE) {
            if (solutionCode.isEmpty()) issues += ValidationIssue(idx, "solutionCode", "Musterlösung (code) darf nicht leer sein")
        }

        //Bei den beiden anderen Typen brauchen wir eine Text lösung
        if (typeEnum == QuestionType.TEXT || typeEnum == QuestionType.CHOICE) {
            if (solutionText.isEmpty()) issues += ValidationIssue(idx, "solutionText", "Musterlösung darf nicht leer sein")
        }


        //Nur bei MC Frgaen
        if (typeEnum == QuestionType.CHOICE) {
            //Prüfen ob 4 Antworten vorliegen
            if (answers.size != 4){
                issues += ValidationIssue(idx, "answers", "MC Frage benötigt immer 4 Antwortmöglichkeiten")
            }

            //Sicherstellen, das Antworten auch Text haben
            if (answers.all { it.isBlank() }) {
                issues += ValidationIssue(idx, "answers", "MC-Frage hat nur leere Antworten")
            }

            //Keine richtige Antwort ist auch nicht erlaubt
            if (rawMask == 0) {
                issues += ValidationIssue(idx, "answers", "MC-Frage hat 0 richtige Antworten")
            }
        }

        if (issues.isEmpty()) {
            valid += q.copy(
                questionText = questionText,
                answers = answers,
                type = type
            )
        } else {
            issues.forEach {
                errors += ImportError(it.index, it.message, questionText.take(120))            }
        }
    }
    return ValidationResult(valid, errors)
}
