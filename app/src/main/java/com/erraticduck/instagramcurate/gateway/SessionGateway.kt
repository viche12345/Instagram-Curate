package com.erraticduck.instagramcurate.gateway

import androidx.lifecycle.LiveData
import androidx.lifecycle.Transformations
import com.erraticduck.instagramcurate.data.dao.SearchSessionDao
import com.erraticduck.instagramcurate.data.dao.SearchSessionEntityWithMediaCount
import com.erraticduck.instagramcurate.data.entity.SearchSessionEntity
import com.erraticduck.instagramcurate.domain.Session

class SessionGateway(private val sessionDao: SearchSessionDao) {

    fun getAll(): LiveData<List<Session>> = Transformations.map(sessionDao.getAll()) {
        it.map {
                entity -> entity.toDomain()
        }
    }

    fun getById(id: Long) = sessionDao.getById(id)?.toDomain()

    suspend fun addSession(session: Session): Long = sessionDao.insert(session.toEntity())

    fun updateSync(id: Long, inProgress: Boolean) = sessionDao.updateSessionSync(id, inProgress)

    fun updateRemoteCount(id: Long, remoteCount: Int) = sessionDao.updateSessionRemoteCount(id, remoteCount)

    suspend fun deleteSession(sessionId: Long) = sessionDao.deleteById(sessionId)

}

fun SearchSessionEntityWithMediaCount.toDomain() = Session(
    entity.name,
    Session.Type.fromName(entity.type) ?: throw IllegalStateException("type is null"),
    entity.id,
    mediaCount,
    entity.remoteCount,
    entity.syncing)
fun Session.toEntity() = SearchSessionEntity(type.dbName, name, remoteCount, syncing)