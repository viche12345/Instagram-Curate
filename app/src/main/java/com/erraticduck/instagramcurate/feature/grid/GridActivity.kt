package com.erraticduck.instagramcurate.feature.grid

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.*
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.lifecycle.*
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.erraticduck.instagramcurate.MainApplication
import com.erraticduck.instagramcurate.R
import com.erraticduck.instagramcurate.domain.Media
import com.erraticduck.instagramcurate.feature.detail.DetailActivity
import com.erraticduck.instagramcurate.gateway.MediaGateway
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class GridActivity : AppCompatActivity() {

    private val mediaGateway = MediaGateway(MainApplication.instance.searchSessionDatabase.mediaDao())

    private val sessionId: Long
        get() = intent.getLongExtra(EXTRA_SESSION_ID, 0)

    private lateinit var viewModel: GridViewModel
    private lateinit var grid: RecyclerView
    private lateinit var gridLayoutManager: GridLayoutManager
    private val onDataChanged = Observer<List<Media>> { viewModel.media.value = it }
    private var currentSource: LiveData<List<Media>>? = null

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
        setUpToolbar()

        // Initialize labels
        mediaGateway.getAllBySessionId(sessionId).observe(this, Observer {
            viewModel.labelCountMap.clear()
            it.forEach { media ->
                media.labels.forEach { label ->
                    viewModel.labelCountMap[label.name] = viewModel.labelCountMap[label.name]?.inc() ?: 1
                }
            }
            viewModel.sortedLabels = viewModel.labelCountMap
                .toList()
                .sortedByDescending { ( _, value) -> value }
                .toMap()
                .keys
                .toTypedArray()
        })

        viewModel = ViewModelProvider(this).get()
        refreshData()
        val adapter = GridAdapter()
        viewModel.media.observe(this, Observer { adapter.submitList(it) })

        grid = findViewById(R.id.grid)
        grid.adapter = adapter
        gridLayoutManager = GridLayoutManager(this, 3, GridLayoutManager.VERTICAL, viewModel.ascendingOrder)
        grid.layoutManager = gridLayoutManager
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_grid, menu)
        return true
    }

    override fun onPrepareOptionsMenu(menu: Menu?): Boolean {
        menu?.findItem(R.id.filter_videos_switch)?.isChecked = viewModel.showVideosOnly
        menu?.findItem(R.id.reverse_order_switch)?.isChecked = viewModel.ascendingOrder
        return true
    }

    private fun setUpToolbar() {
        findViewById<Toolbar>(R.id.toolbar).apply {
            this@GridActivity.setSupportActionBar(this)
            this@GridActivity.supportActionBar?.title = intent.getStringExtra(EXTRA_TITLE)
            setNavigationOnClickListener { finish() }
            setOnMenuItemClickListener { item ->
                when (item.itemId) {
                    R.id.filter -> {
                        MaterialAlertDialogBuilder(this@GridActivity)
                            .setTitle(R.string.filter)
                            .setMultiChoiceItems(viewModel.sortedLabels,
                                viewModel.sortedLabels.map { viewModel.checkedLabels.contains(it) }.toBooleanArray()
                            )
                            { _, which, isChecked ->
                                if (isChecked) {
                                    viewModel.checkedLabels.add(viewModel.sortedLabels[which])
                                } else {
                                    viewModel.checkedLabels.remove(viewModel.sortedLabels[which])
                                }
                            }
                            .setNegativeButton(android.R.string.cancel) { _, _ -> }
                            .setNeutralButton(R.string.clear_filters) { _, _ ->
                                viewModel.checkedLabels.clear()
                                refreshData()
                            }
                            .setPositiveButton(R.string.apply) { _, _ ->
                                refreshData()
                            }
                            .show()
                        true
                    }
                    R.id.filter_videos_switch -> {
                        viewModel.showVideosOnly = !viewModel.showVideosOnly
                        refreshData()
                        true
                    }
                    R.id.reverse_order_switch -> {
                        viewModel.ascendingOrder = !viewModel.ascendingOrder
                        grid.scrollToPosition(if (viewModel.ascendingOrder) (viewModel.media.value?.size ?: 1) - 1 else 0)
                        gridLayoutManager.reverseLayout = viewModel.ascendingOrder
                        true
                    }
                    else -> false
                }
            }
        }
    }

    private fun refreshData() {
        currentSource?.removeObserver(onDataChanged)
        currentSource = mediaGateway.getAllBySessionId(sessionId,
            viewModel.checkedLabels.toList(),
            viewModel.showVideosOnly
        ).apply {
            observe(this@GridActivity, onDataChanged)
        }
    }

    class GridViewModel : ViewModel() {
        val media = MutableLiveData<List<Media>>()
        val labelCountMap = hashMapOf<String, Int>()
        var sortedLabels: Array<String> = emptyArray()
        var checkedLabels = mutableSetOf<String>()
        var showVideosOnly: Boolean = false
        var ascendingOrder: Boolean = false
    }

    class GridAdapter: ListAdapter<Media, GridViewHolder>(object : DiffUtil.ItemCallback<Media>() {
        override fun areItemsTheSame(oldItem: Media, newItem: Media): Boolean =
            oldItem.remoteId == newItem.remoteId

        override fun areContentsTheSame(oldItem: Media, newItem: Media): Boolean =
            oldItem == newItem

    }) {
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