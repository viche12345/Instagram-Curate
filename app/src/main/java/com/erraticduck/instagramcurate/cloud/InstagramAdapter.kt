package com.erraticduck.instagramcurate.cloud

import com.erraticduck.instagramcurate.domain.MediaPage
import com.erraticduck.instagramcurate.domain.Media
import com.erraticduck.instagramcurate.domain.Session
import okhttp3.ResponseBody
import retrofit2.Response
import java.io.IOException

class InstagramAdapter(private val instagramService: InstagramService) {

    @Throws(IOException::class, RuntimeException::class)
    fun fetchHashTag(tag: String, next: String? = null): Response<MediaPage> {
        val response = next?.let { instagramService.loadMoreHashTag(InstagramService.HashTagVariables(tag, it)) }?.execute()
            ?: instagramService.fetchHashTag(tag).execute()
        return if (response.isSuccessful) {
            Response.success(response.body()?.data?.hashtag?.toDomain())
        } else {
            Response.error(response.code(), response.errorBody() ?: ResponseBody.create(null, ""))
        }
    }

    @Throws(IOException::class, RuntimeException::class)
    fun fetchUser(session: Session, next: String? = null): Response<MediaPage> {
        val response = next?.let {
            instagramService.loadMoreUser(
                InstagramService.UserVariables(
                    session.remoteId ?: throw IllegalStateException("Remote ID should've been set at this point"),
                    it
                )
            )
        }?.execute() ?: instagramService.fetchUser(session.name).execute()
        return if (response.isSuccessful) {
            if (next == null) {
                // First request
                session.remoteId = response.body()?.data?.user?.id ?: throw IllegalStateException("Expecting a valid ID here")
            }
            Response.success(response.body()?.data?.user?.toDomain())
        } else {
            Response.error(response.code(), response.errorBody() ?: ResponseBody.create(null, ""))
        }
    }
}

fun InstagramService.HashTagNode.toDomain() = MediaPage(id, name, thumbnailUrl,
    mediaEdges.edges.map { it.node.toDomain() },
    mediaEdges.count,
    mediaEdges.pageInfo?.let { if (it.hasNextPage) it.endCursor else null }
)

fun InstagramService.UserNode.toDomain() = MediaPage(id, username, thumbnailUrl,
    mediaEdges.edges.map { it.node.toDomain() },
    mediaEdges.count,
    mediaEdges.pageInfo?.let { if (it.hasNextPage) it.endCursor else null }
)

fun InstagramService.MediaNode.toDomain() = Media(
    0,
    id,
    timestamp,
    displayUrl,
    thumbnailUrl,
    if (captionEdges.edges.isNotEmpty()) captionEdges.edges[0].node.text else "",
    isVideo
)