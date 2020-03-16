package com.mamoon.app.photoweather.room

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "post_table")
data class Post(
    @PrimaryKey(autoGenerate = true)
    val postId: Long =0L,
    @ColumnInfo(name = "photo")
    val photoUri: String,
    @ColumnInfo(name = "city")
    val cityName: String
)