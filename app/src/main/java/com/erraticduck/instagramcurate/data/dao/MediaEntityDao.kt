package com.erraticduck.instagramcurate.data.dao

import androidx.lifecycle.LiveData
import androidx.room.*
import com.erraticduck.instagramcurate.data.entity.LabelEntity
import com.erraticduck.instagramcurate.data.entity.MediaEntity

@Dao
interface MediaEntityDao {

    @Transaction
    @Query("""
        SELECT media.* FROM media
        LEFT JOIN labels label ON media._id = label._media_id
        WHERE 
          CASE WHEN :labelFilterEnabled THEN label.name IN (:labelFilterBy) ELSE 1 END
          AND _session_id=:id
          AND is_video <> CASE WHEN :videosOnly THEN 0 ELSE 2 END
        GROUP BY media._id
        ORDER BY
          CASE WHEN :ascendingOrder THEN created_at END ASC,
          CASE WHEN NOT :ascendingOrder THEN created_at END DESC
        """)
    fun getAllBySessionId(id: Long,
                          labelFilterBy: List<String>,
                          videosOnly: Boolean,
                          ascendingOrder: Boolean,
                          labelFilterEnabled: Boolean = labelFilterBy.isNotEmpty()
    ): LiveData<List<MediaEntityWithLabels>>

    @Transaction
    @Query("SELECT * FROM media WHERE remote_id=:id")
    fun getByRemoteId(id: Long): LiveData<MediaEntityWithLabels>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
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