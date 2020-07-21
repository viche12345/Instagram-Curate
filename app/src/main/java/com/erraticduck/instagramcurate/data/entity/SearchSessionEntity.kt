package com.erraticduck.instagramcurate.data.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "sessions")
data class SearchSessionEntity(
    val type: String,
    val name: String,
    @ColumnInfo(name = "remote_count") val remoteCount: Int,
    val syncing: Boolean
) {

    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "_id")
    var id = 0L

}