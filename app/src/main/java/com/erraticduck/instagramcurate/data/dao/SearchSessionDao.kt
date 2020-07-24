package com.erraticduck.instagramcurate.data.dao

import androidx.lifecycle.LiveData
import androidx.room.*
import com.erraticduck.instagramcurate.data.entity.SearchSessionEntity

@Dao
interface SearchSessionDao {
    @Query("""SELECT s.*, count(m.remote_id) AS media_count
        FROM sessions s
        LEFT JOIN media m ON s._id = m._session_id
        GROUP BY s._id
        """)
    fun getAll(): LiveData<List<SearchSessionEntityWithMediaCount>>

    @Query("""SELECT s.*, count(m.remote_id) AS media_count
        FROM sessions s
        LEFT JOIN media m ON s._id = m._session_id
        WHERE s._id=:id
        GROUP BY s._id
        """)
    fun getById(id: Long): SearchSessionEntityWithMediaCount?

    @Insert
    suspend fun insert(entity: SearchSessionEntity): Long

    @Query("UPDATE sessions SET syncing=:syncInProgress WHERE _id=:id")
    fun updateSessionSync(id: Long, syncInProgress: Boolean)

    @Query("UPDATE sessions SET remote_count=:remoteCount WHERE _id=:id")
    fun updateSessionRemoteCount(id: Long, remoteCount: Int)

    @Query("DELETE FROM sessions WHERE _id=:id")
    suspend fun deleteById(id: Long)
}

data class SearchSessionEntityWithMediaCount(
    @Embedded val entity: SearchSessionEntity,
    @ColumnInfo(name = "media_count") val mediaCount: Int
)