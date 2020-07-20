package com.erraticduck.instagramcurate.data.dao

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.erraticduck.instagramcurate.data.entity.SearchSessionEntity

@Dao
interface SearchSessionDao {
    @Query("SELECT * FROM sessions")
    fun getAll(): LiveData<List<SearchSessionEntity>>

    @Query("SELECT * FROM sessions WHERE _id=:id")
    fun getById(id: Long): SearchSessionEntity?

    @Insert
    suspend fun insert(entity: SearchSessionEntity): Long

    @Query("DELETE FROM sessions WHERE _id=:id")
    suspend fun deleteById(id: Long)
}