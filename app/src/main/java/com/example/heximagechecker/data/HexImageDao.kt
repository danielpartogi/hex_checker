package com.example.heximagechecker.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface HexImageDao {

    @Query("SELECT * FROM hex_image")
    fun getAllHex() : List<HexImage>

    @Insert
    fun setHex(hex: HexImage)
}