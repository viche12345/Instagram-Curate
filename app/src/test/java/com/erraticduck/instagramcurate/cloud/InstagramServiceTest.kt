package com.erraticduck.instagramcurate.cloud

import com.erraticduck.instagramcurate.domain.Session
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class InstagramServiceTest {

    private lateinit var instagramAdapter: InstagramAdapter

    @Before
    fun setup() {
        instagramAdapter = InstagramAdapter(InstagramService.create())
    }

    @Test
    fun testHashTag() {
        testByType(Session("kotlin", Session.Type.HASHTAG))
    }

    @Test
    fun testUser() {
        testByType(Session("someone", Session.Type.PERSON))
    }

    private fun testByType(session: Session) {
        var nextCursor: String? = null
        var current = 0
        var total: Int? = null
        do {
            val response = when (session.type) {
                Session.Type.HASHTAG -> instagramAdapter.fetchHashTag(session.name, nextCursor)
                Session.Type.PERSON -> instagramAdapter.fetchUser(session, nextCursor)
            }
            assertTrue(response.isSuccessful)

            val hashtag = response.body()!!
            nextCursor = hashtag.nextCursor
            if (total == null) total = hashtag.totalCount
            for (media in hashtag.media) {
                current++
            }

            println("Processed item $current out of $total")
        } while (nextCursor != null && current < 500)
    }
}