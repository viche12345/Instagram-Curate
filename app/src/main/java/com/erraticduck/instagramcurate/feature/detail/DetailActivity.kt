package com.erraticduck.instagramcurate.feature.detail

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import com.bumptech.glide.Glide
import com.erraticduck.instagramcurate.MainApplication
import com.erraticduck.instagramcurate.R
import com.erraticduck.instagramcurate.gateway.MediaGateway
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
        mediaGateway.getByRemoteId(mediaId).observe(this, Observer { media ->
            val stringBuilder = StringBuilder()
            for (label in media.labels) {
                stringBuilder.append("${round(label.confidence * 100)}%")
                stringBuilder.append('\t')
                stringBuilder.append(label.name)
                stringBuilder.appendln()
            }
            findViewById<TextView>(R.id.labelText).text = stringBuilder.toString()

            val imageView = findViewById<ImageView>(R.id.imageView)
            Glide
                .with(imageView)
                .load(media.displayUrl)
                .into(imageView)

            if (media.isVideo) {
                imageView.setOnClickListener {
                    startActivity(VideoActivity.newIntent(this, media.shortcode))
                }
            }
        })
    }
}