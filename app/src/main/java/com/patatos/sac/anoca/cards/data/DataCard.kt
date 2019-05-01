package com.patatos.sac.anoca.cards.data

import android.arch.persistence.room.ColumnInfo
import android.arch.persistence.room.Entity
import android.arch.persistence.room.Ignore
import android.arch.persistence.room.PrimaryKey
import android.os.Parcel
import android.os.Parcelable
import android.support.annotation.NonNull

import com.patatos.sac.anoca.cards.Content
import com.patatos.sac.anoca.cards.CustomCardParse

import kotlin.math.min

    @Entity(tableName = "Cards")
class DataCard (
        @PrimaryKey(autoGenerate = true) @ColumnInfo(name = "id") @NonNull var id: Long,

        @ColumnInfo(name = "data_f") @NonNull var dataFRaw: String,
        @ColumnInfo(name = "data_b") @NonNull var dataBRaw: String,

        @ColumnInfo(name = "answered_right") @NonNull var answeredRight: Int,
        @ColumnInfo(name = "answered_wrong") @NonNull var answeredWrong: Int,

        @ColumnInfo(name = "can_included") @NonNull var canIncluded: Boolean,
        @ColumnInfo(name = "weight") @NonNull var weight: Int,

        @ColumnInfo(name = "category_id") var categoryId: Long?,
        @Ignore var categoryName: String?
    ) : Parcelable {

    @Ignore
    constructor(parcel: Parcel) : this(
        parcel.readLong(),
        parcel.readString()!!,
        parcel.readString()!!,
        parcel.readInt(),
        parcel.readInt(),
        parcel.readInt() != 0,
        parcel.readInt(),
        parcel.readLong(),
        parcel.readString()!!
    )

    @Ignore
    constructor(dataFRaw: String, dataBRaw: String, weight: Int = 0, category: Category? = null) : this(
        0, dataFRaw, dataBRaw,
        0, 0,
        true, weight,
        category?.id, category?.name
    )

    constructor() : this("", "")

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeLong(this.id)
        parcel.writeString(this.dataFRaw)
        parcel.writeString(this.dataBRaw)
        parcel.writeInt(this.answeredRight)
        parcel.writeInt(this.answeredWrong)
        parcel.writeInt(if (this.canIncluded) 1 else 0)
        parcel.writeInt(this.weight)
        parcel.writeLong(this.categoryId!!)
        parcel.writeString(this.categoryName)
    }

    override fun describeContents(): Int {
        return 0
    }

    private fun getSimplifiedData(c: String): String {
        return CustomCardParse(c, true).toString().let {
            Content.IMG_SUF.fold(
                Content.WEB_PRF.fold(
                    it.substring(0, min(it.length, 12)) + if (12 < it.length) "..." else ""
                ) { acc, prf -> acc.removePrefix(prf) }
            ) { acc, suf -> acc.removeSuffix(suf) }
        }
    }

    fun getSimplifiedDataF(): String {
        return this.getSimplifiedData(this.dataFRaw)
    }

    fun getSimplifiedDataB(): String {
        return this.getSimplifiedData(this.dataBRaw)
    }

    companion object CREATOR : Parcelable.Creator<DataCard> {

        override fun createFromParcel(parcel: Parcel): DataCard {
            return DataCard(parcel)
        }

        override fun newArray(size: Int): Array<DataCard?> {
            return arrayOfNulls(size)
        }

    }

}
