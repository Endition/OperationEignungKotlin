package com.example.operationeignung.data.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.operationeignung.data.database.converter.Converters
import com.example.operationeignung.data.database.dao.CategoryDao
import com.example.operationeignung.data.database.dao.QuestionDao
import com.example.operationeignung.data.database.entities.Category
import com.example.operationeignung.data.database.entities.Question

/** Room-Definition für die Datenbank. Mehr wird hier nicht benötigt. Siehte di/DatabaseModule **/
@Database(entities = [Question::class, Category::class], version = 9, exportSchema = false)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun questionDao(): QuestionDao
    abstract fun categoryDao(): CategoryDao
}