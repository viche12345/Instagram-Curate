package com.erraticduck.instagramcurate.domain

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
)

data class Label(
    val name: String,
    val confidence: Float
)