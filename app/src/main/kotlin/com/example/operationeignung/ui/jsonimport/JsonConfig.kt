package com.example.operationeignung.ui.jsonimport

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json

/** Zentrale JSON Klasse **/
object JsonConfig {
    @OptIn(ExperimentalSerializationApi::class)
    val relaxed: Json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        allowTrailingComma = true
        explicitNulls = false
        coerceInputValues = true
    }
}