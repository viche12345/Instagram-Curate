package com.erraticduck.instagramcurate.domain

import androidx.annotation.StringRes
import androidx.recyclerview.widget.DiffUtil
import com.erraticduck.instagramcurate.R

data class Session(
    val name: String,
    val type: Type,
    val id: Long = -1,
    var remoteId: Long? = null
) {

    enum class Type(val dbName: String, @StringRes val displayName: Int) {
        HASHTAG("hashtag", R.string.hashtag),
        PERSON("person", R.string.person);

        companion object {
            fun fromName(name: String) = values().find { it.dbName == name }
        }
    }

    companion object {
        val DIFF_CALLBACK = object : DiffUtil.ItemCallback<Session>() {
            override fun areItemsTheSame(oldItem: Session, newItem: Session): Boolean =
                oldItem.name == newItem.name

            override fun areContentsTheSame(oldItem: Session, newItem: Session): Boolean =
                oldItem == newItem

        }
    }
}
