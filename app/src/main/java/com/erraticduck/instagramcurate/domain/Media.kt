package com.erraticduck.instagramcurate.domain

import androidx.recyclerview.widget.DiffUtil

data class Media(
    val localId: Long,
    val remoteId: Long,
    val shortcode: String,
    val timestamp: Long,
    val displayUrl: String,
    val thumbnailUrl: String?,
    val caption: String,
    val isVideo: Boolean,
    val hasSidecar : Boolean = false,
    val labels: List<Label> = emptyList()
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

data class Label(
    val name: String,
    val confidence: Float
)