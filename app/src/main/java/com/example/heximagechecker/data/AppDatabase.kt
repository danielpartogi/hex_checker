package com.example.heximagechecker.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(entities = [HexImage::class], version = 7)
abstract class AppDatabase : RoomDatabase() {

    companion object {
        private var instance: AppDatabase? = null

        @Synchronized
        fun getInstance(ctx: Context): AppDatabase {
            if(instance == null)
                instance = Room.databaseBuilder(ctx.applicationContext, AppDatabase::class.java,
                    "note_database")
                    .fallbackToDestructiveMigration()
                    .addCallback(roomCallback)
                    .build()

            return instance!!

        }

        private val roomCallback = object : Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                populateDatabase(instance!!)
            }
        }

        private fun populateDatabase(db: AppDatabase) {
            CoroutineScope(Dispatchers.IO).launch {
                val hexImageDao = db.userDao()
                val listHex = listOf(
                    HexImage("Photoshop", "41 64 6F 62 65 20 50 68 6F 74 6F 73 68 6F 70 20"),
                    HexImage("Photoshop CS3", "41 64 6F 62 65 20 50 68 6F 74 6F 73 68 6F 70 20"),
                    HexImage("Photoshop CS4", "41 64 6F 62 65 20 50 68 6F 74 6F 73 68 6F 70 20"),
                    HexImage("Photoshop CS5", "41 64 6F 62 65 20 50 68 6F 74 6F 73 68 6F 70 20"),
                    HexImage("Photoshop CS6", "41 64 6F 62 65 20 50 68 6F 74 6F 73 68 6F 70 20"),
                    HexImage("Photoshop CC", "41 64 6F 62 65 20 50 68 6F 74 6F 73 68 6F 70 20"),
                    HexImage("Photoshop CC 2020", "41 64 6F 62 65 20 50 68 6F 74 6F 73 68 6F 70 20"),
                    HexImage("paint", "4A 46 49 46"),
                    HexImage("paint2", "89 50 4E 47 0D 0A 1A 0A")
                )
                listHex.forEach {
                    hexImageDao.setHex(it)
                }
            }
        }
    }

    abstract fun userDao(): HexImageDao


}