package com.erraticduck.instagramcurate.data.entity

import androidx.room.*

@Entity(
    tableName = "labels",
    foreignKeys = [
        ForeignKey(
            entity = MediaEntity::class,
            parentColumns = ["_id"],
            childColumns = ["_media_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["_media_id"], unique = false)]
)
data class LabelEntity(
    @ColumnInfo(name = "_media_id") val _mediaId: Long,
    val name: String,
    val confidence: Float
) {

    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "_id")
    var id = 0L
}