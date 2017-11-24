package com.cbruegg.agendafortodoist.projects

import android.graphics.Color
import android.os.Bundle
import android.support.wear.ambient.AmbientMode
import android.support.wear.widget.WearableLinearLayoutManager
import android.support.wear.widget.WearableRecyclerView
import android.view.View
import android.widget.ProgressBar
import android.widget.TextView
import com.cbruegg.agendafortodoist.R
import com.cbruegg.agendafortodoist.WearableActivity
import com.cbruegg.agendafortodoist.Settings
import com.cbruegg.agendafortodoist.shared.todoist.todoist
import com.cbruegg.agendafortodoist.tasks.newTasksActivityIntent
import com.cbruegg.agendafortodoist.util.CenterScrollLayoutCallback
import com.cbruegg.agendafortodoist.util.ColorScaleListener
import com.cbruegg.agendafortodoist.util.ScaleListener
import com.cbruegg.agendafortodoist.util.observe
import com.cbruegg.agendafortodoist.util.viewModel

class ProjectsActivity : WearableActivity() {

    init {
        ambientCallbackDelegate = object : AmbientMode.AmbientCallback() {
            override fun onExitAmbient() {
                super.onExitAmbient()
                rootView.setBackgroundResource(R.color.activity_background)
            }

            override fun onEnterAmbient(ambientDetails: Bundle?) {
                super.onEnterAmbient(ambientDetails)
                rootView.setBackgroundResource(android.R.color.black)
            }
        }
    }

    private val rootView by lazy { findViewById<View>(R.id.root) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_projects)
        setAmbientEnabled()

        val scrollCallback = CenterScrollLayoutCallback(listOf(
                ScaleListener(),
                ColorScaleListener(Color.WHITE, Color.GRAY) {
                    (it.tag as ProjectViewHolder).nameView
                }
        ))

        val adapter = ProjectsAdapter(emptyList()) {
            startActivity(newTasksActivityIntent(this, it.id))
        }
        val projectList = findViewById<WearableRecyclerView>(R.id.projects)
        val progressBar = findViewById<ProgressBar>(R.id.projects_progress)
        val bigMessage = findViewById<TextView>(R.id.projects_big_message)
        projectList.adapter = adapter
        projectList.isEdgeItemsCenteringEnabled = true
        projectList.layoutManager = WearableLinearLayoutManager(this, scrollCallback)

        val todoist = todoist(Settings(this).retrieveAuth().accessToken)
        val viewModel = viewModel { ProjectsViewModel(todoist) }
        viewModel.projectViewModels.observe(this) {
            adapter.data = it
            adapter.notifyDataSetChanged()
        }
        viewModel.isLoading.observe(this) {
            progressBar.visibility = if (it) View.VISIBLE else View.GONE
        }
        viewModel.bigMessageId.observe(this) {
            if (it != null) bigMessage.setText(it) else bigMessage.text = ""
        }
        viewModel.onCreate()
    }
}

