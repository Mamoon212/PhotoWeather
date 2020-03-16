package com.mamoon.app.photoweather.room

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface PostDatabaseDao {

    @Insert
    fun insert(post: Post)

    @Query("SELECT * FROM post_table ORDER BY postId DESC")
    fun getAllPosts(): LiveData<List<Post>>
}