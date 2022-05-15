package com.example.heximagechecker.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(entities = [HexImage::class], version = 8)
abstract class AppDatabase : RoomDatabase() {

    companion object {
        private var instance: AppDatabase? = null

        @Synchronized
        fun getInstance(ctx: Context): AppDatabase {
            if (instance == null)
                instance = Room.databaseBuilder(ctx.applicationContext, AppDatabase::class.java,
                    "hex_database")
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
                    HexImage("Photoshop",
                        "4A 46 49 46",
                        "41 64 6F 62 65 20 50 68 6F 74 6F 73 68 6F 70 20"),
                    HexImage("Photoshop CS3",
                        "4A 46 49 46",
                        "41 64 6F 62 65 20 50 68 6F 74 6F 73 68 6F 70 20"),
                    HexImage("Photoshop CS4",
                        "4A 46 49 46",
                        "41 64 6F 62 65 20 50 68 6F 74 6F 73 68 6F 70 20"),
                    HexImage("Photoshop CS5",
                        "4A 46 49 46",
                        "41 64 6F 62 65 20 50 68 6F 74 6F 73 68 6F 70 20"),
                    HexImage("Photoshop CS6",
                        "4A 46 49 46",
                        "41 64 6F 62 65 20 50 68 6F 74 6F 73 68 6F 70 20"),
                    HexImage("Photoshop CC",
                        "4A 46 49 46",
                        "41 64 6F 62 65 20 50 68 6F 74 6F 73 68 6F 70 20"),
                    HexImage("Photoshop CC 2020",
                        "4A 46 49 46",
                        "41 64 6F 62 65 20 50 68 6F 74 6F 73 68 6F 70 20"),
                    HexImage(
                        "Adobe Photoshop 7.0 ME",
                        "4A 46 49 46",
                        "41 64 6F 62 65 20 50 68 6F 74 6F 73 68 6F 70 20 37 2E 30 20 4D 45"
                    ),
                    HexImage("paint", "", "4A 46 49 46")
                )
                listHex.forEach {
                    hexImageDao.setHex(it)
                }
            }
        }
    }

    abstract fun userDao(): HexImageDao


}