package com.erraticduck.instagramcurate.data.dao

import androidx.lifecycle.LiveData
import androidx.room.*
import com.erraticduck.instagramcurate.data.entity.LabelEntity
import com.erraticduck.instagramcurate.data.entity.MediaEntity

@Dao
interface MediaEntityDao {

    @Transaction
    @Query("""SELECT * FROM media 
        WHERE _session_id=:id
        ORDER BY created_at DESC""")
    fun getAllBySessionId(id: Long): LiveData<List<MediaEntityWithLabels>>

    @Transaction
    @Query("SELECT * FROM media WHERE remote_id=:id")
    fun getByRemoteId(id: Long): LiveData<MediaEntityWithLabels>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(entity: MediaEntity): Long

    @Insert
    fun insert(labels: List<LabelEntity>)

    @Query("DELETE FROM media WHERE _session_id=:sessionId")
    fun deleteBySessionId(sessionId: Long)

}

data class MediaEntityWithLabels(
    @Embedded val media: MediaEntity,
    @Relation(parentColumn = "_id", entityColumn = "_media_id") val labels: List<LabelEntity>
)