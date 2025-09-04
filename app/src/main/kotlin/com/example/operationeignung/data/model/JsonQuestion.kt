package com.example.operationeignung.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
/** JSON-Datenklasse für den Import **/
@Serializable
data class JsonQuestion(
    @SerialName("question_text") val questionText: String? = null,
    @SerialName("question_code") val questionCode: String? = null,
    @SerialName("answers") val answers: List<String> = emptyList(),
    @SerialName("correct_mask") val correctMask: Int = 0,
    @SerialName("type") val type: String? = null,            // "choice" | "text" | "code"
    @SerialName("solution_text") val solutionText: String? = null,
    @SerialName("solution_code") val solutionCode: String? = null,
    @SerialName("category") val category: String? = null
)
