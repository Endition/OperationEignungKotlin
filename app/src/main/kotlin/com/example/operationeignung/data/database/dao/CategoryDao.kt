package com.example.operationeignung.data.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.operationeignung.data.database.entities.Category
import kotlinx.coroutines.flow.Flow

/** Kategorie-Datenbank-Zugriffsklasse **/
@Dao
interface CategoryDao {
    /** Kategorien einfügen **/
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(category: Category): Long

    /** Einzelne Kategorie laden nach Name **/
    @Query("SELECT * FROM categories WHERE name = :name LIMIT 1")
    suspend fun getCategoryByName(name: String): Category?

    /** KategorieListe als Flow laden **/
    @Query("SELECT * FROM categories ORDER BY name ASC")
    fun getAllCategories(): Flow<List<Category>>

    /** nicht benutzte Kategorien löschen **/
    @Query("DELETE FROM categories WHERE id NOT IN (SELECT DISTINCT category_id FROM questions WHERE category_id IS NOT NULL)")
    suspend fun deleteUnused(): Int

    /** Kategorieliste laden als Flow **/
    @Query("SELECT * FROM categories ORDER BY name")
    fun getAllFlow(): Flow<List<Category>>

    /** Kategorieliste laden als statische liste **/
    @Query("SELECT * FROM categories ORDER BY name")
    suspend fun getAll(): List<Category>

    /** Kategorie updaten **/
    @Update
    suspend fun update(category: Category)

    /** Kategorie löschen **/
    @Delete
    suspend fun delete(category: Category)

    /** Kategorie löschen by Id **/
    @Query("DELETE FROM categories WHERE id = :id")
    suspend fun deleteById(id: Int)

    /** Kategorie suchen bei Name **/
    @Query("""
        SELECT * FROM categories
        WHERE LOWER(TRIM(name)) = LOWER(TRIM(:name))
        LIMIT 1
    """)
    suspend fun findByName(name: String): Category?

}