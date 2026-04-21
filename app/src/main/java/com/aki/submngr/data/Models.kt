package com.aki.submngr.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

enum class SectionType {
    CARD_STACK, PIE_CHART, CHECKLIST, LIST, TABLE, LINK;

    fun displayName(): String = when (this) {
        CARD_STACK -> "カードスタック"
        PIE_CHART  -> "円グラフ"
        CHECKLIST  -> "チェックリスト"
        LIST       -> "リスト"
        TABLE      -> "テーブル"
        LINK       -> "リンク"
    }

    fun defaultEmoji(): String = when (this) {
        CARD_STACK -> "💳"
        PIE_CHART  -> "📊"
        CHECKLIST  -> "✅"
        LIST       -> "📝"
        TABLE      -> "🗂️"
        LINK       -> "🔗"
    }
}

@Entity(tableName = "sections")
data class Section(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val type: SectionType,
    val emoji: String = "",
    val sortOrder: Int = 0
)

@Entity(
    tableName = "cards",
    foreignKeys = [ForeignKey(
        entity = Section::class,
        parentColumns = ["id"],
        childColumns = ["sectionId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("sectionId")]
)
data class Card(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sectionId: Long,
    val title: String,
    val subtitle: String = "",
    val number: String = "",
    val colorStartHex: String = "#667EEA",
    val colorEndHex: String = "#764BA2",
    val photoUri: String = "",
    val sortOrder: Int = 0
)

@Entity(
    tableName = "entries",
    foreignKeys = [ForeignKey(
        entity = Section::class,
        parentColumns = ["id"],
        childColumns = ["sectionId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("sectionId")]
)
data class Entry(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sectionId: Long,
    val label: String,
    val value: String = "",
    val amount: Double = 0.0,
    val isChecked: Boolean = false,
    val colorHex: String = "#6750A4",
    val sortOrder: Int = 0
)
