package com.erraticduck.instagramcurate.domain

data class MediaPage(
    val remoteId: Long?,  // ID can be null with paged responses
    val name: String?,  // Name can be null with paged responses
    val thumbnailUrl: String?,
    val media: List<Media>,
    val totalCount: Int?,
    val nextCursor: String?
)