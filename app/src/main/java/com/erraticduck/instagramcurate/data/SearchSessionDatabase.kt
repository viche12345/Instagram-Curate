package com.erraticduck.instagramcurate.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.erraticduck.instagramcurate.data.dao.MediaEntityDao
import com.erraticduck.instagramcurate.data.dao.SearchSessionDao
import com.erraticduck.instagramcurate.data.entity.LabelEntity
import com.erraticduck.instagramcurate.data.entity.MediaEntity
import com.erraticduck.instagramcurate.data.entity.SearchSessionEntity

@Database(
    entities = [
        SearchSessionEntity::class,
        MediaEntity::class,
        LabelEntity::class
    ],
    version = 1
)
abstract class SearchSessionDatabase : RoomDatabase() {

    abstract fun sessionDao(): SearchSessionDao
    abstract fun mediaDao(): MediaEntityDao

    companion object {

        private const val DB_NAME = "session_db"

        @Volatile
        private var INSTANCE: SearchSessionDatabase? = null

        fun getInstance(context: Context) = INSTANCE ?: synchronized(this) {
            INSTANCE ?: buildDatabase(context)
        }

        private fun buildDatabase(context: Context) = Room.databaseBuilder(context, SearchSessionDatabase::class.java, DB_NAME)
            .build()
    }
}