package com.erraticduck.instagramcurate.sync

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.graphics.drawable.Icon
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Toast
import androidx.work.*
import com.bumptech.glide.Glide
import com.erraticduck.instagramcurate.MainApplication
import com.erraticduck.instagramcurate.R
import com.erraticduck.instagramcurate.domain.Label
import com.erraticduck.instagramcurate.domain.Media
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

    private val executor = InstagramExecutor()
    private val mediaGateway by lazy { MediaGateway(MainApplication.instance.searchSessionDatabase.mediaDao()) }
    private var notificationManager: NotificationManager? = context.getSystemService(NotificationManager::class.java)
    private val handler = Handler(Looper.getMainLooper())

    override fun doWork(): Result {
        val sessionGateway = SessionGateway(MainApplication.instance.searchSessionDatabase.sessionDao())
        val sessionId = inputData.getLong(DATA_KEY_SESSION_ID, 0)
        val performMl = inputData.getBoolean(DATA_KEY_PERFORM_ML, false)
        val session = sessionGateway.getById(sessionId) ?: throw IllegalStateException("No such session with id $sessionId")

        sessionGateway.updateSync(sessionId, true)

        setForegroundAsync(createForegroundInfo(session.name))

        val result = try {
            val executorResult = executor.execute(session, object : InstagramExecutor.Callback {
                override fun isStopped() = isStopped

                override fun onRemoteCountDetermined(count: Int) = sessionGateway.updateRemoteCount(sessionId, count)

                override fun onMediaProcessed(media: Media) {
                    val id = mediaGateway.insert(media, sessionId)
                    if (performMl) {
                        performMl(media.copy(localId = id))
                    }
                }

                override fun onError(code: Int, msg: String) {
                    val log = "Error $code: $msg"
                    Log.e(TAG, log)
                    showToastOnMainThread(msg)
                }

            })

            if (executorResult)
                Result.success()
            else
                Result.failure()
        } catch (e: IOException) {
            Log.e(TAG, e.message, e)
            showToastOnMainThread(e.message ?: e.toString())
            Result.retry()
        } catch (e: RuntimeException) {
            Log.e(TAG, e.message, e)
            showToastOnMainThread(e.message ?: e.toString())
            Result.failure()
        }

        sessionGateway.updateSync(sessionId, false)

        return result
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

        val notification = Notification.Builder(context, channelId)
            .setContentTitle(title)
            .setOngoing(true)
            .setSmallIcon(Icon.createWithResource(context, R.drawable.ic_launcher_foreground))
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

    private fun showToastOnMainThread(text: String) {
        handler.post { Toast.makeText(context, text, Toast.LENGTH_SHORT).show() }
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
                .addTag(getTagWithSessionId(sessionId))
                .build().run(workManager::enqueue)

        fun getTagWithSessionId(sessionId: Long) = TAG + "_$sessionId"
    }
}