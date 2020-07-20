package com.erraticduck.instagramcurate.gateway

import androidx.lifecycle.LiveData
import androidx.lifecycle.Transformations
import com.erraticduck.instagramcurate.data.dao.SearchSessionDao
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

    suspend fun deleteSession(sessionId: Long) = sessionDao.deleteById(sessionId)

}

fun SearchSessionEntity.toDomain() = Session(name, Session.Type.fromName(type) ?: throw IllegalStateException("type is null"), id)
fun Session.toEntity() = SearchSessionEntity(type.dbName, name)