package com.erraticduck.instagramcurate.domain

import androidx.recyclerview.widget.DiffUtil

data class MediaPage(
    val remoteId: Long?,  // ID can be null with paged responses
    val name: String?,  // Name can be null with paged responses
    val thumbnailUrl: String?,
    val media: List<Media>,
    val totalCount: Long?,
    val nextCursor: String?
)

data class Media(
    val remoteId: Long,
    val timestamp: Long,
    val displayUrl: String,
    val thumbnailUrl: String,
    val caption: String,
    val isVideo: Boolean
) {
    companion object {
        val DIFF_CALLBACK = object : DiffUtil.ItemCallback<Media>() {
            override fun areItemsTheSame(oldItem: Media, newItem: Media): Boolean =
                oldItem.remoteId == newItem.remoteId

            override fun areContentsTheSame(oldItem: Media, newItem: Media): Boolean =
                oldItem == newItem

        }
    }
}