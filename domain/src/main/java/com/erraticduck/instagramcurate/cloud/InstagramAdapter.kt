package com.erraticduck.instagramcurate.cloud

import com.erraticduck.instagramcurate.domain.MediaPage
import com.erraticduck.instagramcurate.domain.Media
import com.erraticduck.instagramcurate.domain.Session
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.Response
import java.io.IOException

class InstagramAdapter(private val instagramService: InstagramService) {

    @Throws(IOException::class, RuntimeException::class)
    fun fetchHashTag(tag: String, next: String? = null): Response<MediaPage> {
        val response = next?.let { instagramService.loadMoreHashTag(InstagramService.HashTagVariables(tag, it)) }?.executeWithTimeoutRetry()
            ?: instagramService.fetchHashTag(tag).executeWithTimeoutRetry()
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
        }?.executeWithTimeoutRetry() ?: instagramService.fetchUser(session.name).executeWithTimeoutRetry()
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

    fun fetchShortcode(code: String): Response<List<Media>> {
        val response = instagramService.fetchShortcode(code).executeWithTimeoutRetry()
        return if (response.isSuccessful) {
            val shortcode = response.body()?.data?.shortcode
            Response.success(shortcode?.sidecarEdges?.edges?.let { edges ->
                if (edges.isNotEmpty()) {
                    edges.map { it.node.toDomain(
                        shortcode.shortcode,
                        shortcode.captionEdges.toCaptionString(),
                        shortcode.timestamp) }
                } else {
                    listOf(shortcode.toDomain())
                }
            })
        } else {
            Response.error(response.code(), response.errorBody() ?: ResponseBody.create(null, ""))
        }
    }

    fun fetchVideoUrl(shortcode: String, remoteId: Long): Response<String> {
        val response = instagramService.fetchShortcode(shortcode).executeWithTimeoutRetry()
        return if (response.isSuccessful) {
            val responseShortcode = response.body()?.data?.shortcode
            val videoUrl = responseShortcode?.sidecarEdges?.edges?.find { it.node.id == remoteId } ?.node?.videoUrl
                ?: responseShortcode?.videoUrl
            Response.success(videoUrl)
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

fun InstagramService.MediaNode.toDomain(shortcodeOverride: String? = null,
                                        captionOverride: String? = null,
                                        timestampOverride: Long? = null) = Media(
    0,
    id,
    shortcodeOverride ?: shortcode,
    timestampOverride ?: timestamp,
    displayUrl,
    thumbnailUrl,
    captionOverride ?: if (captionEdges?.edges?.isNotEmpty() == true) captionEdges.edges[0].node.text else "",
    isVideo,
    typeName == "GraphSidecar"
)

fun InstagramService.Edges<InstagramService.CaptionNode>?.toCaptionString() =
    if (this?.edges?.isNotEmpty() == true) edges[0].node.text else ""

private fun <T> Call<T>.executeWithTimeoutRetry(): Response<T> {
    lateinit var exception: IOException
    for (i in 1..3) {
        try {
            return execute()
        } catch (e: IOException) {
            exception = e
        }
    }
    throw exception
}
