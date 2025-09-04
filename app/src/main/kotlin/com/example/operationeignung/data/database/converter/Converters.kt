package com.example.operationeignung.data.database.converter

import androidx.room.TypeConverter
import com.example.operationeignung.data.database.entities.QuestionType

/** Konverter für das QuestionType-Enum in die DB **/
class Converters {
    @TypeConverter
    fun fromQuestionType(type: QuestionType): String = type.dbValue

    @TypeConverter
    fun toQuestionType(value: String?): QuestionType =
        QuestionType.fromDb(value)
}
