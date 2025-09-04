package com.example.operationeignung.data.database.migrations

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val MIGRATION_7_8 = object : Migration(7, 8) {
    override fun migrate(db: SupportSQLiteDatabase) {
        // Wird ggf. zukünftig benötigt.
        // db.execSQL("CREATE INDEX IF NOT EXISTS idx_questions_times_wrong ON questions(times_wrong)")
    }
}
