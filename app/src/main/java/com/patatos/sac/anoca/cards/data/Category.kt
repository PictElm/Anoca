package com.patatos.sac.anoca.cards.data

import android.arch.persistence.room.ColumnInfo
import android.arch.persistence.room.Entity
import android.arch.persistence.room.Ignore
import android.arch.persistence.room.PrimaryKey
import android.support.annotation.NonNull
import com.patatos.sac.anoca.cards.data.csv.Csvable

@Entity(tableName = "Categories")
class Category(
    @PrimaryKey(autoGenerate = true) @ColumnInfo(name = "id") @NonNull var id: Long,

    @ColumnInfo(name = "name") @NonNull var name: String,
    @ColumnInfo(name = "enabled") @NonNull var enabled: Boolean
) : Csvable {

    @Ignore
    constructor(name: String) : this(0, name, true)

    override fun toString(): String {
        return this.name
    }

    override fun csv(s: String, q: String): String {
        return Csvable.dataToCsv(s, q, this.id, this.name, this.enabled)
    }

    companion object {
        fun fromCsv(s: String, q: String, raw: String): Category {
            return Csvable.csvToData(s, q, raw).let { Category(it[0].toLong(), it[1], it[2].toBoolean()) }
        }
    }

}
