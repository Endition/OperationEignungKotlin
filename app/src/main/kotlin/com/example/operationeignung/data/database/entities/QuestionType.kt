package com.example.operationeignung.data.database.entities

/** Question-Type-Enum **/
enum class QuestionType(val dbValue: String) {
    CHOICE("choice"),
    TEXT("text"),
    CODE("code");

    companion object {
        fun fromDb(value: String?): QuestionType =
            values().firstOrNull { it.dbValue.equals(value, ignoreCase = true) }
                ?: CHOICE
    }
}
