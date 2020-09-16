package com.erraticduck.instagramcurate.sync

import com.erraticduck.instagramcurate.cloud.InstagramAdapter
import com.erraticduck.instagramcurate.cloud.InstagramService
import com.erraticduck.instagramcurate.domain.Media
import com.erraticduck.instagramcurate.domain.MediaPage
import com.erraticduck.instagramcurate.domain.Session
import java.io.IOException

class InstagramExecutor {

    private val instagramAdapter by lazy { InstagramAdapter(InstagramService.create()) }

    @Throws(IOException::class, RuntimeException::class)
    fun execute(session: Session, callback: Callback): Boolean {
        var nextCursor: String? = null
        do {
            if (callback.isStopped()) break
            val response = when (session.type) {
                Session.Type.HASHTAG -> instagramAdapter.fetchHashTag(session.name, nextCursor)
                Session.Type.PERSON -> instagramAdapter.fetchUser(session, nextCursor)
            }
            if (!response.isSuccessful) {
                callback.onError(response.code(), response.message())
                return false
            }

            val mediaPage = response.body()!!
            nextCursor = mediaPage.nextCursor
            mediaPage.totalCount?.let {
                if (it > 0) {
                    callback.onRemoteCountDetermined(it)
                }
            }
            handlePage(mediaPage, callback)
        } while (nextCursor != null)
        return true
    }

    private fun handlePage(page: MediaPage, callback: Callback) {
        for (media in page.media) {
            if (callback.isStopped()) break

            val toInsert = if (media.hasSidecar) {
                val response = instagramAdapter.fetchShortcode(media.shortcode)
                if (response.isSuccessful) {
                    response.body() ?: listOf(media)
                } else {
                    listOf(media)
                }
            } else {
                listOf(media)
            }

            toInsert.forEach {
                callback.onMediaProcessed(it)
            }
        }
    }

    interface Callback {
        fun isStopped(): Boolean
        fun onRemoteCountDetermined(count: Int)
        fun onMediaProcessed(media: Media)
        fun onError(code: Int, msg: String)
    }
}