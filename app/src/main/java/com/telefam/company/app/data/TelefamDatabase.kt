package com.telefam.company.app.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [ContactEntity::class, OwnerProfileEntity::class], version = 2, exportSchema = false)
abstract class TelefamDatabase : RoomDatabase() {
    abstract fun contactDao(): ContactDao
    abstract fun ownerProfileDao(): OwnerProfileDao

    companion object {
        @Volatile
        private var instance: TelefamDatabase? = null

        fun getInstance(context: Context): TelefamDatabase {
            return instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    TelefamDatabase::class.java,
                    "telefam_offline.db"
                )
                    .fallbackToDestructiveMigration(true)
                    .build()
                    .also { instance = it }
            }
        }
    }
}
