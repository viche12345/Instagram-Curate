package com.erraticduck.instagramcurate.feature.detail

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.erraticduck.instagramcurate.R
import com.erraticduck.instagramcurate.cloud.InstagramAdapter
import com.erraticduck.instagramcurate.cloud.InstagramService
import com.google.android.exoplayer2.SimpleExoPlayer
import com.google.android.exoplayer2.source.ProgressiveMediaSource
import com.google.android.exoplayer2.ui.PlayerView
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers

class VideoActivity : AppCompatActivity() {

    private val instagramAdapter by lazy { InstagramAdapter(InstagramService.create()) }
    private val disposables = CompositeDisposable()
    private lateinit var videoView: PlayerView
    private var exoPlayer: SimpleExoPlayer? = null

    // ExoPlayer saved instance variables
    private var playWhenReady = true
    private var currentWindow = 0
    private var playbackPosition = 0L

    companion object {
        const val EXTRA_SHORTCODE = "EXTRA_SHORTCODE"
        const val EXTRA_ID = "EXTRA_ID"

        fun newIntent(context: Context, shortcode: String, remoteId: Long) = Intent(context, VideoActivity::class.java).apply {
            putExtra(EXTRA_SHORTCODE, shortcode)
            putExtra(EXTRA_ID, remoteId)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_video)
        videoView = findViewById(R.id.video)
    }

    override fun onResume() {
        super.onResume()
        // According to exoplayer codelab, best to init player in onResume()
        hideSystemUi()
        intent.getStringExtra(EXTRA_SHORTCODE)?.let { shortcode ->
            Single.fromCallable { instagramAdapter.fetchVideoUrl(shortcode, intent.getLongExtra(EXTRA_ID, 0)) }
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe ({ response ->
                    initializePlayer(response.body() ?: "")
                }) {
                    Toast.makeText(this, it.message, Toast.LENGTH_SHORT).show()
                }
                .also { disposables.add(it) }
        } ?: Toast.makeText(this, "Missing shortcode", Toast.LENGTH_SHORT).show()
    }

    override fun onPause() {
        super.onPause()
        releasePlayer()
        disposables.clear()
    }

    private fun initializePlayer(url: String) {
        val factory = DefaultDataSourceFactory(this, System.getProperty("http.agent"))
        val mediaSource = ProgressiveMediaSource.Factory(factory).createMediaSource(Uri.parse(url))
        exoPlayer = SimpleExoPlayer.Builder(this).build().also {
            it.playWhenReady = playWhenReady
            it.seekTo(currentWindow, playbackPosition)
            it.prepare(mediaSource)
        }
        videoView.player = exoPlayer
    }

    private fun releasePlayer() {
        exoPlayer?.let {
            playWhenReady = it.playWhenReady
            playbackPosition = it.currentPosition
            currentWindow = it.currentWindowIndex
            it.release()
            exoPlayer = null
        }
    }

    private fun hideSystemUi() {
        videoView.systemUiVisibility = (View.SYSTEM_UI_FLAG_LOW_PROFILE
                or View.SYSTEM_UI_FLAG_FULLSCREEN
                or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION)
    }
}