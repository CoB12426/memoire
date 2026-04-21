package com.aki.submngr.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.aki.submngr.data.*
import com.aki.submngr.ui.theme.cardGradients
import com.aki.submngr.ui.theme.pieColors
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val db = AppDatabase.getDatabase(application)
    private val repo = AppRepository(db.sectionDao(), db.cardDao(), db.entryDao())

    val sections: StateFlow<List<Section>> = repo.getAllSections()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun getCardsForSection(sectionId: Long): Flow<List<Card>> = repo.getCardsForSection(sectionId)
    fun getEntriesForSection(sectionId: Long): Flow<List<Entry>> = repo.getEntriesForSection(sectionId)

    fun addSection(name: String, type: SectionType, emoji: String) {
        viewModelScope.launch { repo.addSection(Section(name = name, type = type, emoji = emoji)) }
    }

    fun deleteSection(section: Section) {
        viewModelScope.launch { repo.deleteSection(section) }
    }

    fun addCard(sectionId: Long, title: String, subtitle: String, number: String,
                colorStart: String, colorEnd: String, photoUri: String) {
        viewModelScope.launch {
            repo.addCard(Card(sectionId = sectionId, title = title, subtitle = subtitle,
                number = number, colorStartHex = colorStart, colorEndHex = colorEnd,
                photoUri = photoUri))
        }
    }

    fun updateCard(card: Card) {
        viewModelScope.launch { repo.updateCard(card) }
    }

    fun deleteCard(card: Card) {
        viewModelScope.launch { repo.deleteCard(card) }
    }

    fun addEntry(sectionId: Long, label: String, value: String, amount: Double, colorHex: String) {
        viewModelScope.launch {
            repo.addEntry(Entry(sectionId = sectionId, label = label, value = value,
                amount = amount, colorHex = colorHex))
        }
    }

    fun updateEntry(entry: Entry) {
        viewModelScope.launch { repo.updateEntry(entry) }
    }

    fun toggleEntry(id: Long, isChecked: Boolean) {
        viewModelScope.launch { repo.toggleEntry(id, isChecked) }
    }

    fun deleteEntry(entry: Entry) {
        viewModelScope.launch { repo.deleteEntry(entry) }
    }

    companion object {
        fun nextGradient(existingCards: List<Card>): Pair<String, String> {
            val used = existingCards.map { Pair(it.colorStartHex, it.colorEndHex) }
            return cardGradients.firstOrNull { it !in used }
                ?: cardGradients[existingCards.size % cardGradients.size]
        }

        fun nextPieColor(existingEntries: List<Entry>): String {
            val used = existingEntries.map { it.colorHex }
            return pieColors.firstOrNull { it !in used }
                ?: pieColors[existingEntries.size % pieColors.size]
        }
    }
}
