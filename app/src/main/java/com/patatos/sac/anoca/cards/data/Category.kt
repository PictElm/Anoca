package com.patatos.sac.anoca.cards.data

import android.arch.persistence.room.ColumnInfo
import android.arch.persistence.room.Entity
import android.arch.persistence.room.Ignore
import android.arch.persistence.room.PrimaryKey
import android.support.annotation.NonNull

@Entity(tableName = "Categories")
class Category(
    @PrimaryKey(autoGenerate = true) @ColumnInfo(name = "id") @NonNull var id: Long,

    @ColumnInfo(name = "name") @NonNull var name: String,
    @ColumnInfo(name = "enabled") @NonNull var enabled: Boolean
) {

    @Ignore
    constructor(name: String) : this(0, name, true)

    override fun toString(): String {
        return this.name
    }

}
