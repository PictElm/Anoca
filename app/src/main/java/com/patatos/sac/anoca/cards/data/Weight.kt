package com.patatos.sac.anoca.cards.data

import android.arch.persistence.room.ColumnInfo
import android.arch.persistence.room.Entity
import android.arch.persistence.room.Ignore
import android.arch.persistence.room.PrimaryKey
import android.support.annotation.NonNull
import java.text.DateFormat

@Entity(tableName = "Weights")
class Weight (
        @PrimaryKey(autoGenerate = true) @ColumnInfo(name = "id") @NonNull var id: Long,

        @ColumnInfo(name = "card_id") @NonNull var cardId: Long,
        @ColumnInfo(name = "last_time") @NonNull var lastTime: Long
    ) {

    @Ignore
    constructor(cardId: Long) : this(0, cardId, System.currentTimeMillis())

    override fun toString(): String {
        return "(${this.cardId}) last: ${DateFormat.getDateInstance().format(this.lastTime)}"
    }

}
