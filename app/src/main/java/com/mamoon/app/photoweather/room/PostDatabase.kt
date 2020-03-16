package com.mamoon.app.photoweather.room


import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [Post::class], version = 1, exportSchema = false)
abstract class PostDatabase : RoomDatabase() {

    abstract val postDatabaseDao: PostDatabaseDao


    companion object {
        @Volatile
        private var INSTANCE: PostDatabase? = null

        fun getInstance(context: Context): PostDatabase {
            synchronized(this) {
                var instance = INSTANCE
                if (instance == null) {
                    instance = buildDatabase(context)
                    INSTANCE = instance
                }
                return instance
            }
        }

        private fun buildDatabase(context: Context): PostDatabase {
            return Room.databaseBuilder(
                context.applicationContext,
                PostDatabase::class.java,
                "movie_database"
            )
                .fallbackToDestructiveMigration()
                .build()
        }
    }
}