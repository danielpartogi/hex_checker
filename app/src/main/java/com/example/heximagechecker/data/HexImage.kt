package com.example.heximagechecker.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "hex_image")
data class HexImage(
    @PrimaryKey
    val programName: String,
    val primaryHex: String,
    val secondaryHex: String,
)