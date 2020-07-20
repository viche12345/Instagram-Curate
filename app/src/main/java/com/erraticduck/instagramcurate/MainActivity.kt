package com.erraticduck.instagramcurate

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.LifecycleCoroutineScope
import androidx.lifecycle.Observer
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import androidx.work.WorkManager
import com.erraticduck.instagramcurate.domain.Session
import com.erraticduck.instagramcurate.feature.grid.GridActivity
import com.erraticduck.instagramcurate.gateway.SessionGateway
import com.erraticduck.instagramcurate.sync.InstagramHashtagWorker
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private val gateway by lazy { SessionGateway(MainApplication.instance.searchSessionDatabase.sessionDao()) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setSupportActionBar(findViewById(R.id.toolbar))
        val adapter = SessionAdapter(lifecycleScope)
        gateway.getAll().observe(this, Observer { adapter.submitList(it) })
        val list = findViewById<RecyclerView>(R.id.list)
        list.adapter = adapter
        list.layoutManager = LinearLayoutManager(this)
    }

    fun addSearch(view: View) {
        AddSessionFragment.show(supportFragmentManager)
    }

    class SessionAdapter(private val lifecycleScope: LifecycleCoroutineScope) : ListAdapter<Session, SessionViewHolder>(Session.DIFF_CALLBACK) {
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SessionViewHolder =
            SessionViewHolder.create(parent, lifecycleScope)

        override fun onBindViewHolder(holder: SessionViewHolder, position: Int) {
            holder.bind(getItem(position))
        }

    }

    class SessionViewHolder(private val view: View, private val lifecycleScope: LifecycleCoroutineScope) : RecyclerView.ViewHolder(view) {
        private val gateway by lazy { SessionGateway(MainApplication.instance.searchSessionDatabase.sessionDao()) }

        fun bind(session: Session) {
            val title = view.findViewById<TextView>(R.id.title)
            val subtitle = view.findViewById<TextView>(R.id.subtitle)
            title.text = session.name
            subtitle.text = view.context.getString(session.type.displayName)
            view.setOnLongClickListener {
                MaterialAlertDialogBuilder(view.context)
                    .setMessage(view.context.getString(R.string.delete_question, session.name))
                    .setPositiveButton(R.string.delete) { _, _ ->
                        WorkManager.getInstance(view.context).cancelAllWorkByTag(InstagramHashtagWorker.TAG)
                        lifecycleScope.launch {
                            gateway.deleteSession(session.id)
                        }
                    }
                    .setNegativeButton(android.R.string.cancel) { _, _ -> }
                    .show()
                true
            }
            view.setOnClickListener { view.context.startActivity(GridActivity.newIntent(view.context, session.id, session.name)) }
        }

        companion object {
            fun create(parent: ViewGroup, lifecycleScope: LifecycleCoroutineScope) = SessionViewHolder(
                LayoutInflater.from(parent.context)
                    .inflate(R.layout.list_item_session, parent, false), lifecycleScope
            )
        }
    }
}