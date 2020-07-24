package com.erraticduck.instagramcurate.feature.detail

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.MediaController
import android.widget.Toast
import android.widget.VideoView
import androidx.appcompat.app.AppCompatActivity
import com.erraticduck.instagramcurate.R
import com.erraticduck.instagramcurate.cloud.InstagramAdapter
import com.erraticduck.instagramcurate.cloud.InstagramService
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers

class VideoActivity : AppCompatActivity() {

    private val instagramAdapter by lazy { InstagramAdapter(InstagramService.create()) }
    private val disposables = CompositeDisposable()
    private lateinit var videoView: VideoView

    companion object {
        const val EXTRA_SHORTCODE = "EXTRA_SHORTCODE"

        fun newIntent(context: Context, shortcode: String) = Intent(context, VideoActivity::class.java).apply {
            putExtra(EXTRA_SHORTCODE, shortcode)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_video)
        videoView = findViewById<VideoView>(R.id.video).apply {
            MediaController(context)
                .also {
                    setMediaController(it)
                }
                .setAnchorView(this)
        }
    }

    override fun onStart() {
        super.onStart()
        intent.getStringExtra(EXTRA_SHORTCODE)?.let { shortcode ->
            Single.fromCallable { instagramAdapter.fetchVideoUrl(shortcode) }
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe ({ response ->
                    videoView.setVideoPath(response.body())
                    videoView.requestFocus()
                    videoView.start()
                }) {
                    Toast.makeText(this, it.message, Toast.LENGTH_SHORT).show()
                }
                .also { disposables.add(it) }
        } ?: Toast.makeText(this, "Missing shortcode", Toast.LENGTH_SHORT).show()
    }

    override fun onStop() {
        super.onStop()
        disposables.clear()
    }
}