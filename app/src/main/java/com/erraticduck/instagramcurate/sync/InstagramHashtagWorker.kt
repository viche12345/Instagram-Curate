package com.erraticduck.instagramcurate.sync

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.graphics.drawable.Icon
import android.util.Log
import androidx.work.*
import com.erraticduck.instagramcurate.MainApplication
import com.erraticduck.instagramcurate.R
import com.erraticduck.instagramcurate.cloud.InstagramAdapter
import com.erraticduck.instagramcurate.cloud.InstagramService
import com.erraticduck.instagramcurate.domain.Media
import com.erraticduck.instagramcurate.domain.Session
import com.erraticduck.instagramcurate.gateway.MediaGateway
import com.erraticduck.instagramcurate.gateway.SessionGateway
import java.io.IOException

class InstagramHashtagWorker(private val context: Context, workerParams: WorkerParameters): Worker(context, workerParams) {

    private val instagramAdapter by lazy { InstagramAdapter(InstagramService.create()) }
    private var notificationManager: NotificationManager? = context.getSystemService(NotificationManager::class.java)

    override fun doWork(): Result {
        val sessionGateway = SessionGateway(MainApplication.instance.searchSessionDatabase.sessionDao())
        val mediaGateway = MediaGateway(MainApplication.instance.searchSessionDatabase.mediaDao())
        val sessionId = inputData.getLong(DATA_KEY_SESSION_ID, 0)
        val session = sessionGateway.getById(sessionId) ?: throw IllegalStateException("No such session with id $sessionId")

        mediaGateway.deleteBySessionId(sessionId)

        try {
            var nextCursor: String? = null
            var current = 0
            do {
                if (isStopped) break
                val response = when (session.type) {
                    Session.Type.HASHTAG -> instagramAdapter.fetchHashTag(session.name, nextCursor)
                    Session.Type.PERSON -> instagramAdapter.fetchUser(session, nextCursor)
                }
                if (!response.isSuccessful) {
                    Log.e(TAG, "Error ${response.code()}: ${response.message()}")
                    return Result.failure()
                }
                setForegroundAsync(createForegroundInfo(session.name))

                val hashtag = response.body()!!
                nextCursor = hashtag.nextCursor
                for (media in hashtag.media) {
                    if (isStopped) break
                    mediaGateway.insert(
                        Media(
                            media.remoteId,
                            media.timestamp,
                            media.displayUrl,
                            media.thumbnailUrl,
                            media.caption,
                            media.isVideo
                        ),
                        sessionId
                    )
                    setForegroundAsync(createForegroundInfo(session.name,
                        "${++current} / ${hashtag.totalCount}"))
                }
            } while (nextCursor != null)

            return Result.success()
        } catch (e: IOException) {
            Log.e(TAG, e.message, e)
            return Result.retry()
        } catch (e: RuntimeException) {
            Log.e(TAG, e.message, e)
            return Result.failure()
        }
    }

    private fun createForegroundInfo(hashtagName: String, progressText: String? = null): ForegroundInfo {
        val channelId = createNotificationChannel()
        val title = context.getString(R.string.notification_sync_hashtag, hashtagName)
        val cancel = context.getString(android.R.string.cancel)
        val intent = WorkManager.getInstance(context).createCancelPendingIntent(id)

        val notification = Notification.Builder(context, channelId)
            .setContentTitle(title)
            .setContentText(progressText)
            .setOngoing(true)
            .setSmallIcon(Icon.createWithResource(context, R.drawable.ic_launcher_foreground))
            .addAction(Notification.Action.Builder(null, cancel, intent).build())
            .build()

        return ForegroundInfo(R.string.notification_sync_hashtag, notification)
    }

    private fun createNotificationChannel(): String {
        val id = context.getString(R.string.notification_channel_id_sync)
        val name = context.getString(R.string.notification_channel_sync_name)
        val description = context.getString(R.string.notification_channel_sync_description)
        val importance = NotificationManager.IMPORTANCE_LOW
        val channel = NotificationChannel(id, name, importance).apply {
            this.description = description
        }
        notificationManager?.createNotificationChannel(channel)
        return id
    }

    companion object {
        val TAG = InstagramHashtagWorker::class.java.simpleName
        const val DATA_KEY_SESSION_ID = "KEY_SESSION_ID"

        fun enqueue(workManager: WorkManager, sessionId: Long) =
            OneTimeWorkRequestBuilder<InstagramHashtagWorker>().setInputData(
                Data.Builder().putLong(DATA_KEY_SESSION_ID, sessionId).build()
            )
                .addTag(TAG)
                .build().run(workManager::enqueue)
    }
}