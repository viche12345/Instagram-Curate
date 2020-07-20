package com.erraticduck.instagramcurate.cloud

import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import retrofit2.Call
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface InstagramService {

    @GET("/explore/tags/{name}/?__a=1")
    fun fetchHashTag(@Path("name") name: String): Call<HashTagFirstResponse>

    @GET("/graphql/query/?query_hash=298b92c8d7cad703f7565aa892ede943")
    fun loadMoreHashTag(@Query("variables") variables: HashTagVariables): Call<HashTagLoadMoreResponse>

    @GET("/{name}/?__a=1")
    fun fetchUser(@Path("name") name: String): Call<UserFirstResponse>

    @GET("/graphql/query/?query_hash=472f257a40c653c64c666ce877d59d2b")
    fun loadMoreUser(@Query("variables") variables: UserVariables): Call<UserLoadMoreResponse>

    interface Response<T> {
        var data: T
    }

    class HashTagFirstResponse : Response<HashTagData> {
        @SerializedName("graphql")
        override lateinit var data: HashTagData
    }

    class HashTagLoadMoreResponse : Response<HashTagData> {
        override lateinit var data: HashTagData
    }

    data class HashTagData(
        val hashtag: HashTagNode
    )

    data class HashTagNode(
        val id: Long?,  // ID can be null with paged responses
        val name: String,
        @SerializedName("profile_pic_url") val thumbnailUrl: String?,
        @SerializedName("edge_hashtag_to_media") val mediaEdges: Edges<MediaNode>
    )

    class UserFirstResponse : Response<UserData> {
        @SerializedName("graphql")
        override lateinit var data: UserData
    }

    class UserLoadMoreResponse : Response<UserData> {
        override lateinit var data: UserData
    }

    data class UserData(
        val user: UserNode
    )

    data class UserNode(
        val id: Long?,  // This field can be null with paged responses
        val username: String?,  // Same here
        @SerializedName("profile_pic_url") val thumbnailUrl: String?,
        @SerializedName("edge_owner_to_timeline_media") val mediaEdges: Edges<MediaNode>
    )

    data class MediaNode(
        val id: Long,
        @SerializedName("taken_at_timestamp") val timestamp: Long,
        @SerializedName("display_url") val displayUrl: String,
        @SerializedName("thumbnail_src") val thumbnailUrl: String,
        @SerializedName("edge_media_to_caption") val captionEdges: Edges<CaptionNode>,
        @SerializedName("is_video") val isVideo: Boolean
    )

    data class CaptionNode(
        val text: String
    )

    data class PageInfo(
        @SerializedName("has_next_page") val hasNextPage: Boolean,
        @SerializedName("end_cursor") val endCursor: String?
    )

    data class Edges<T>(
        val edges: List<Edge<T>>,
        val count: Long?,
        @SerializedName("page_info") val pageInfo: PageInfo?
    )

    data class Edge<T>(
        val node: T
    )

    data class HashTagVariables(
        @SerializedName("tag_name") val name: String,
        val after: String,
        val first: Int = 50
    ) {
        override fun toString(): String = Gson().toJson(this)
    }

    data class UserVariables(
        val id: Long,
        val after: String,
        val first: Int = 50
    ) {
        override fun toString(): String = Gson().toJson(this)
    }

    companion object Factory {
        fun create(): InstagramService = Retrofit.Builder()
            .baseUrl("https://www.instagram.com")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(InstagramService::class.java)
    }
}