package com.aki.submngr.data

import kotlinx.coroutines.flow.Flow

class AppRepository(
    private val sectionDao: SectionDao,
    private val cardDao: CardDao,
    private val entryDao: EntryDao
) {
    fun getAllSections(): Flow<List<Section>> = sectionDao.getAllSections()
    fun getCardsForSection(sectionId: Long): Flow<List<Card>> = cardDao.getCardsForSection(sectionId)
    fun getEntriesForSection(sectionId: Long): Flow<List<Entry>> = entryDao.getEntriesForSection(sectionId)

    suspend fun addSection(s: Section) = sectionDao.insert(s)
    suspend fun deleteSection(s: Section) = sectionDao.delete(s)

    suspend fun addCard(c: Card) = cardDao.insert(c)
    suspend fun updateCard(c: Card) = cardDao.update(c)
    suspend fun deleteCard(c: Card) = cardDao.delete(c)

    suspend fun addEntry(e: Entry) = entryDao.insert(e)
    suspend fun updateEntry(e: Entry) = entryDao.update(e)
    suspend fun deleteEntry(e: Entry) = entryDao.delete(e)
    suspend fun toggleEntry(id: Long, checked: Boolean) = entryDao.updateChecked(id, checked)
}
