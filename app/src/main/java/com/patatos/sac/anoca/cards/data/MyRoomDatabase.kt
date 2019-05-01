package com.patatos.sac.anoca.cards.data

import android.arch.persistence.room.*
import android.content.Context

import com.patatos.sac.anoca.R

@Database(entities = [DataCard::class, Category::class, Weight::class], version = 1, exportSchema = false)
abstract class MyRoomDatabase : RoomDatabase() {

    abstract fun getDao(): MyDao

    companion object {

        private var INSTANCE: MyRoomDatabase? = null

        fun getInstance(context: Context): MyRoomDatabase {
            if (INSTANCE == null)
                INSTANCE = Room.databaseBuilder(
                    context,
                    MyRoomDatabase::class.java,
                    context.getString(R.string.database_file_key)
                ).build()

            return INSTANCE as MyRoomDatabase
        }

    }

}
