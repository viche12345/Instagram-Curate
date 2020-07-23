package com.erraticduck.instagramcurate.feature.grid

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.*
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.erraticduck.instagramcurate.MainApplication
import com.erraticduck.instagramcurate.R
import com.erraticduck.instagramcurate.domain.Media
import com.erraticduck.instagramcurate.feature.detail.DetailActivity
import com.erraticduck.instagramcurate.gateway.MediaGateway
import kotlinx.coroutines.launch

class GridActivity : AppCompatActivity() {

    private val mediaGateway = MediaGateway(MainApplication.instance.searchSessionDatabase.mediaDao())

    private val sessionId: Long
        get() = intent.getLongExtra(EXTRA_SESSION_ID, 0)

    companion object {
        const val EXTRA_SESSION_ID = "EXTRA_SESSION_ID"
        const val EXTRA_TITLE = "EXTRA_TITLE"

        fun newIntent(context: Context, sessionId: Long, title: String) = Intent(context, GridActivity::class.java).apply {
            putExtra(EXTRA_SESSION_ID, sessionId)
            putExtra(EXTRA_TITLE, title)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_grid)
        setSupportActionBar(findViewById(R.id.toolbar))
        intent.getStringExtra(EXTRA_TITLE)?.let { supportActionBar?.title = it }

        val model = ViewModelProvider(this).get<GridViewModel>()
        mediaGateway.getAllBySessionId(sessionId).observe(this, Observer { model.media.value = it })
        val adapter = GridAdapter()
        model.media.observe(this, Observer { adapter.submitList(it) })

        val grid = findViewById<RecyclerView>(R.id.grid)
        grid.adapter = adapter
        grid.layoutManager = GridLayoutManager(this, 3)
    }

    class GridViewModel : ViewModel() {
        val media = MutableLiveData<List<Media>>()
    }

    class GridAdapter: ListAdapter<Media, GridViewHolder>(Media.DIFF_CALLBACK) {
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): GridViewHolder =
            GridViewHolder.create(parent)

        override fun onBindViewHolder(holder: GridViewHolder, position: Int) =
            holder.bind(getItem(position))

    }

    class GridViewHolder(private val view: View) : RecyclerView.ViewHolder(view) {
        fun bind(media: Media) {
            val imageView = view.findViewById<ImageView>(R.id.imageView)
            Glide
                .with(view)
                .load(media.thumbnailUrl ?: media.displayUrl)
                .centerCrop()
                .into(imageView)
            view.setOnClickListener {
                view.context.startActivity(DetailActivity.newIntent(view.context, media.remoteId))
            }
        }

        companion object {
            fun create(parent: ViewGroup) = GridViewHolder(LayoutInflater.from(parent.context)
                .inflate(R.layout.grid_item, parent, false))
        }
    }
}