package com.erraticduck.instagramcurate.data.entity

import androidx.room.*
import androidx.room.ForeignKey.CASCADE

@Entity(
    tableName = "media",
    foreignKeys = [
        ForeignKey(
            entity = SearchSessionEntity::class,
            parentColumns = ["_id"],
            childColumns = ["_session_id"],
            onDelete = CASCADE
        )
    ],
    indices = [
        Index(value = ["_session_id"], unique = false),
        Index(value = ["remote_id"], unique = true)
    ]
)
data class MediaEntity(
    @ColumnInfo(name = "_session_id") val _sessionId: Long,
    @ColumnInfo(name = "remote_id") val remoteId: Long,
    val shortcode: String,
    @ColumnInfo(name = "thumbnail_url") val thumbnailUrl: String?,
    @ColumnInfo(name = "display_url") val displayUrl: String,
    val caption: String,
    @ColumnInfo(name = "created_at") val createdAt: Long,
    @ColumnInfo(name = "is_video") val isVideo: Boolean
) {

    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "_id")
    var id = 0L
}