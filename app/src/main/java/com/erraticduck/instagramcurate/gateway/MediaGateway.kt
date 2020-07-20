package com.erraticduck.instagramcurate.gateway

import androidx.lifecycle.Transformations
import com.erraticduck.instagramcurate.data.dao.MediaEntityDao
import com.erraticduck.instagramcurate.data.entity.MediaEntity
import com.erraticduck.instagramcurate.domain.Media

class MediaGateway(private val mediaEntityDao: MediaEntityDao) {

    fun getAllBySessionId(sessionId: Long) = Transformations.map(mediaEntityDao.getAllBySessionId(sessionId)) {
        it.map { entity -> entity.toDomain() }
    }

    fun getByRemoteId(id: Long) = Transformations.map(mediaEntityDao.getByRemoteId(id)) { it.toDomain() }

    fun insert(media: Media, sessionId: Long) = mediaEntityDao.insert(media.toEntity(sessionId))

    fun deleteBySessionId(sessionId: Long) = mediaEntityDao.deleteBySessionId(sessionId)
}

fun MediaEntity.toDomain() = Media(remoteId, createdAt, displayUrl, thumbnailUrl, caption, isVideo)
fun Media.toEntity(sessionId: Long) = MediaEntity(sessionId, remoteId, thumbnailUrl, displayUrl, caption, timestamp, isVideo)