package com.erraticduck.instagramcurate.domain


data class Session(
    val name: String,
    val type: Type,
    val id: Long = -1,
    val localCount: Int = 0,
    val remoteCount: Int = 0,
    val syncing: Boolean = false,
    var remoteId: Long? = null
) {

    enum class Type(val dbName: String) {
        HASHTAG("hashtag"),
        PERSON("person");

        companion object {
            fun fromName(name: String) = values().find { it.dbName == name }
        }
    }
}
