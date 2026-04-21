package com.aki.submngr.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface SectionDao {
    @Query("SELECT * FROM sections ORDER BY sortOrder ASC, id ASC")
    fun getAllSections(): Flow<List<Section>>

    @Insert
    suspend fun insert(section: Section): Long

    @Update
    suspend fun update(section: Section)

    @Delete
    suspend fun delete(section: Section)
}

@Dao
interface CardDao {
    @Query("SELECT * FROM cards WHERE sectionId = :sectionId ORDER BY sortOrder ASC, id ASC")
    fun getCardsForSection(sectionId: Long): Flow<List<Card>>

    @Insert
    suspend fun insert(card: Card): Long

    @Update
    suspend fun update(card: Card)

    @Delete
    suspend fun delete(card: Card)
}

@Dao
interface EntryDao {
    @Query("SELECT * FROM entries WHERE sectionId = :sectionId ORDER BY sortOrder ASC, id ASC")
    fun getEntriesForSection(sectionId: Long): Flow<List<Entry>>

    @Insert
    suspend fun insert(entry: Entry): Long

    @Update
    suspend fun update(entry: Entry)

    @Delete
    suspend fun delete(entry: Entry)

    @Query("UPDATE entries SET isChecked = :isChecked WHERE id = :id")
    suspend fun updateChecked(id: Long, isChecked: Boolean)
}
