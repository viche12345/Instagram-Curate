package com.erraticduck.instagramcurate.data.dao

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.erraticduck.instagramcurate.data.entity.MediaEntity

@Dao
interface MediaEntityDao {
    @Query("SELECT * FROM media WHERE _session_id=:id")
    fun getAllBySessionId(id: Long): LiveData<List<MediaEntity>>

    @Query("SELECT * FROM media WHERE remote_id=:id")
    fun getByRemoteId(id: Long): LiveData<MediaEntity>

    @Insert
    fun insert(entity: MediaEntity)

    @Query("DELETE FROM media WHERE _session_id=:sessionId")
    fun deleteBySessionId(sessionId: Long)

}