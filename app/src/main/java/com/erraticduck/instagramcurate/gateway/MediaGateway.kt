package com.erraticduck.instagramcurate.gateway

import androidx.lifecycle.Transformations
import com.erraticduck.instagramcurate.data.dao.MediaEntityDao
import com.erraticduck.instagramcurate.data.dao.MediaEntityWithLabels
import com.erraticduck.instagramcurate.data.entity.LabelEntity
import com.erraticduck.instagramcurate.data.entity.MediaEntity
import com.erraticduck.instagramcurate.domain.Label
import com.erraticduck.instagramcurate.domain.Media

class MediaGateway(private val mediaEntityDao: MediaEntityDao) {

    fun getAllBySessionId(sessionId: Long,
                          filterByLabels: List<String> = emptyList(),
                          showVideosOnly: Boolean = false) =
        Transformations.map(mediaEntityDao.getAllBySessionId(sessionId, filterByLabels, showVideosOnly)) {
            it.map { entity -> entity.toDomain() }.sortedByDescending { media ->
                media.labels
                    .filter { label -> filterByLabels.contains(label.name) }
                    .maxBy { label -> label.confidence }
                    ?.confidence
            }
        }

    fun getByRemoteId(id: Long) = Transformations.map(mediaEntityDao.getByRemoteId(id)) { it.toDomain() }

    fun insert(media: Media, sessionId: Long) = mediaEntityDao.insert(media.toEntity(sessionId))

    fun insert(labels: List<Label>, mediaId: Long) = mediaEntityDao.insert(labels.map { it.toEntity(mediaId) })

}

fun MediaEntityWithLabels.toDomain() = Media(
    media.id,
    media.remoteId,
    media.shortcode,
    media.createdAt,
    media.displayUrl,
    media.thumbnailUrl,
    media.caption,
    media.isVideo,
    false,
    labels.map { entity -> entity.toDomain() }
)
fun Media.toEntity(sessionId: Long) =
    MediaEntity(sessionId, remoteId, shortcode, thumbnailUrl, displayUrl, caption, timestamp, isVideo)

fun LabelEntity.toDomain() = Label(name, confidence)
fun Label.toEntity(mediaId: Long) = LabelEntity(mediaId, name, confidence)