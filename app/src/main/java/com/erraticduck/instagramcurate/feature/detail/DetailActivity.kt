package com.erraticduck.instagramcurate.feature.detail

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.os.Bundle
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import com.erraticduck.instagramcurate.MainApplication
import com.erraticduck.instagramcurate.R
import com.erraticduck.instagramcurate.gateway.MediaGateway
import com.google.mlkit.common.model.LocalModel
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.label.ImageLabeling
import com.google.mlkit.vision.label.custom.CustomImageLabelerOptions
import kotlin.math.round

class DetailActivity : AppCompatActivity() {

    private val mediaGateway by lazy { MediaGateway(MainApplication.instance.searchSessionDatabase.mediaDao()) }
    private val mediaId: Long
        get() = intent.getLongExtra(EXTRA_MEDIA_ID, 0)

    companion object {
        const val EXTRA_MEDIA_ID = "EXTRA_MEDIA_ID"

        fun newIntent(context: Context, mediaId: Long) = Intent(context, DetailActivity::class.java).apply {
            putExtra(EXTRA_MEDIA_ID, mediaId)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_detail)
        setSupportActionBar(findViewById(R.id.toolbar))
        supportActionBar?.title = getString(R.string.details)
        mediaGateway.getByRemoteId(mediaId).observe(this, Observer {
            val imageView = findViewById<ImageView>(R.id.imageView)
            Glide
                .with(imageView)
                .asBitmap()
                .load(it.displayUrl)
                .listener(object : RequestListener<Bitmap> {
                    override fun onLoadFailed(e: GlideException?, model: Any?, target: Target<Bitmap>?, isFirstResource: Boolean): Boolean {
                        return false
                    }

                    override fun onResourceReady(
                        resource: Bitmap?,
                        model: Any?,
                        target: Target<Bitmap>?,
                        dataSource: DataSource?,
                        isFirstResource: Boolean
                    ): Boolean {
                        if (resource != null) {
                            val image = InputImage.fromBitmap(resource, 0)
                            val localModel = LocalModel.Builder()
                                .setAssetFilePath("mobilenet_v2_1.0_224_1_metadata_1.tflite")
                                .build()
                            val options = CustomImageLabelerOptions.Builder(localModel)
                                .setConfidenceThreshold(.01f)
                                .build()
                            val labeler = ImageLabeling.getClient(options)
                            labeler.process(image)
                                .addOnSuccessListener { labels ->
                                    val stringBuilder = StringBuilder()
                                    for (label in labels) {
                                        stringBuilder.append("${round(label.confidence * 100)}%")
                                        stringBuilder.append('\t')
                                        stringBuilder.append(label.text)
                                        stringBuilder.appendln()
                                    }
                                    findViewById<TextView>(R.id.labelText).text = stringBuilder.toString()
                                }
                        }
                        return false
                    }

                })
                .into(imageView)
        })
    }
}