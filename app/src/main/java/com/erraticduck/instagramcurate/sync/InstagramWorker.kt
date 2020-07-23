package com.erraticduck.instagramcurate.sync

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.graphics.drawable.Icon
import android.util.Log
import android.widget.Toast
import androidx.work.*
import com.bumptech.glide.Glide
import com.erraticduck.instagramcurate.MainApplication
import com.erraticduck.instagramcurate.R
import com.erraticduck.instagramcurate.cloud.InstagramAdapter
import com.erraticduck.instagramcurate.cloud.InstagramService
import com.erraticduck.instagramcurate.domain.Label
import com.erraticduck.instagramcurate.domain.Media
import com.erraticduck.instagramcurate.domain.MediaPage
import com.erraticduck.instagramcurate.domain.Session
import com.erraticduck.instagramcurate.gateway.MediaGateway
import com.erraticduck.instagramcurate.gateway.SessionGateway
import com.google.mlkit.common.model.LocalModel
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.label.ImageLabeling
import com.google.mlkit.vision.label.custom.CustomImageLabelerOptions
import io.reactivex.Completable
import io.reactivex.schedulers.Schedulers
import java.io.IOException

class InstagramWorker(private val context: Context, workerParams: WorkerParameters): Worker(context, workerParams) {

    private val instagramAdapter by lazy { InstagramAdapter(InstagramService.create()) }
    private val mediaGateway by lazy { MediaGateway(MainApplication.instance.searchSessionDatabase.mediaDao()) }
    private var notificationManager: NotificationManager? = context.getSystemService(NotificationManager::class.java)

    override fun doWork(): Result {
        val sessionGateway = SessionGateway(MainApplication.instance.searchSessionDatabase.sessionDao())
        val sessionId = inputData.getLong(DATA_KEY_SESSION_ID, 0)
        val performMl = inputData.getBoolean(DATA_KEY_PERFORM_ML, false)
        val session = sessionGateway.getById(sessionId) ?: throw IllegalStateException("No such session with id $sessionId")

        sessionGateway.updateSync(sessionId, true)

        setForegroundAsync(createForegroundInfo(session.name))

        val result = try {
            var nextCursor: String? = null
            do {
                if (isStopped) break
                val response = when (session.type) {
                    Session.Type.HASHTAG -> instagramAdapter.fetchHashTag(session.name, nextCursor)
                    Session.Type.PERSON -> instagramAdapter.fetchUser(session, nextCursor)
                }
                if (!response.isSuccessful) {
                    val msg = "Error ${response.code()}: ${response.message()}"
                    Log.e(TAG, msg)
                    Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                    return Result.failure()
                }

                val mediaPage = response.body()!!
                nextCursor = mediaPage.nextCursor
                mediaPage.totalCount?.let {
                    if (it > 0) {
                        sessionGateway.updateRemoteCount(sessionId, it)
                    }
                }
                handlePage(mediaPage, sessionId, performMl)
            } while (nextCursor != null)

            Result.success()
        } catch (e: IOException) {
            Log.e(TAG, e.message, e)
            Toast.makeText(context, e.message, Toast.LENGTH_SHORT).show()
            Result.retry()
        } catch (e: RuntimeException) {
            Log.e(TAG, e.message, e)
            Toast.makeText(context, e.message, Toast.LENGTH_SHORT).show()
            Result.failure()
        }

        sessionGateway.updateSync(sessionId, false)

        return result
    }

    private fun handlePage(page: MediaPage, sessionId: Long, performMl: Boolean) {
        for (media in page.media) {
            if (isStopped) break

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
                val id = mediaGateway.insert(it, sessionId)
                if (performMl) {
                    performMl(it.copy(localId = id))
                }
            }
        }
    }

    private fun performMl(media: Media) {
        val target = Glide
            .with(context)
            .asBitmap()
            .load(media.displayUrl)
            .submit()
        val bitmap = target.get()
        val image = InputImage.fromBitmap(bitmap, 0)
        val localModel = LocalModel.Builder()
            .setAssetFilePath("mobilenet_v2_1.0_224_1_metadata_1.tflite")
            .build()
        val options = CustomImageLabelerOptions.Builder(localModel)
            .setConfidenceThreshold(.01f)
            .build()
        val labeler = ImageLabeling.getClient(options)
        labeler.process(image)
            .addOnSuccessListener { labels ->
                Completable.fromCallable {
                    mediaGateway.insert(labels.map { Label(it.text, it.confidence) }, media.localId)
                }
                    .subscribeOn(Schedulers.io())
                    .subscribe()
            }
    }

    private fun createForegroundInfo(hashtagName: String): ForegroundInfo {
        val channelId = createNotificationChannel()
        val title = context.getString(R.string.notification_sync_hashtag, hashtagName)
        val cancel = context.getString(android.R.string.cancel)
        val intent = WorkManager.getInstance(context).createCancelPendingIntent(id)

        val notification = Notification.Builder(context, channelId)
            .setContentTitle(title)
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
        val TAG = InstagramWorker::class.java.simpleName
        const val DATA_KEY_SESSION_ID = "KEY_SESSION_ID"
        const val DATA_KEY_PERFORM_ML = "KEY_PERFORM_ML"

        fun enqueue(workManager: WorkManager, sessionId: Long, performMl: Boolean) =
            OneTimeWorkRequestBuilder<InstagramWorker>().setInputData(
                Data.Builder().putLong(DATA_KEY_SESSION_ID, sessionId)
                    .putBoolean(DATA_KEY_PERFORM_ML, performMl)
                    .build()
            )
                .addTag(TAG)
                .build().run(workManager::enqueue)
    }
}