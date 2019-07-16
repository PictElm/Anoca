package com.patatos.sac.anoca.cards.data

import android.arch.persistence.room.ColumnInfo
import android.arch.persistence.room.Entity
import android.arch.persistence.room.Ignore
import android.arch.persistence.room.PrimaryKey
import android.support.annotation.NonNull
import com.patatos.sac.anoca.cards.data.csv.Csvable
import java.text.DateFormat

@Entity(tableName = "Weights")
class Weight (
        @PrimaryKey(autoGenerate = true) @ColumnInfo(name = "id") @NonNull var id: Long,

        @ColumnInfo(name = "card_id") @NonNull var cardId: Long,
        @ColumnInfo(name = "last_time") @NonNull var lastTime: Long
    ) : Csvable {

    @Ignore constructor(cardId: Long) : this(0, cardId, System.currentTimeMillis())

    override fun toString(): String {
        return "(${this.cardId}) ${DateFormat.getDateInstance().format(this.lastTime)}"
    }

    override fun csv(s: String, q: String): String {
        return Csvable.dataToCsv(s, q, this.id, this.cardId, this.lastTime)
    }

    companion object {
        fun fromCsv(s: String, q: String, raw: String): Weight {
            return Csvable.csvToData(s, q, raw).let { Weight(it[0].toLong(), it[1].toLong(), it[2].toLong()) }
        }
    }

}
