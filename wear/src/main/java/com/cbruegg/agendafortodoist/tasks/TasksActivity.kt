package com.cbruegg.agendafortodoist.tasks

import android.content.Context
import android.content.Intent
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
import com.cbruegg.agendafortodoist.app
import com.cbruegg.agendafortodoist.auth.AuthActivity
import com.cbruegg.agendafortodoist.task.newTaskActivityIntent
import com.cbruegg.agendafortodoist.util.CenterScrollLayoutCallback
import com.cbruegg.agendafortodoist.util.ColorScaleListener
import com.cbruegg.agendafortodoist.util.ScaleListener
import com.cbruegg.agendafortodoist.util.UniqueRequestIdGenerator
import com.cbruegg.agendafortodoist.util.observe
import com.cbruegg.agendafortodoist.util.viewModel

private const val EXTRA_PROJECT_ID = "project_id"

fun newTasksActivityIntent(context: Context, projectId: Long) =
        Intent(context, TasksActivity::class.java).apply {
            putExtra(EXTRA_PROJECT_ID, projectId)
        }

class TasksActivity : WearableActivity() {

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
    private val viewModel by lazy {
        viewModel {
            val todoist = app.netComponent.todoist()
            TasksViewModel(intent.extras.getLong(EXTRA_PROJECT_ID), todoist, UniqueRequestIdGenerator)
        }.also {
            it.onAuthError = {
                startActivity(Intent(this, AuthActivity::class.java).apply { addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP) })
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_tasks)
        setAmbientEnabled()

        val scrollCallback = CenterScrollLayoutCallback(listOf(
                ScaleListener(),
                ColorScaleListener(Color.WHITE, Color.GRAY) {
                    (it.tag as TaskViewHolder).nameView
                }
        ))

        val adapter = TasksAdapter(emptyList(), this) {
            startActivity(newTaskActivityIntent(this, it.content, it.id, it.isCompleted))
        }
        val tasksList = findViewById<WearableRecyclerView>(R.id.tasks)
        val progressBar = findViewById<ProgressBar>(R.id.tasks_progress)
        val bigMessage = findViewById<TextView>(R.id.tasks_big_message)
        tasksList.adapter = adapter
        tasksList.isEdgeItemsCenteringEnabled = true
        tasksList.layoutManager = WearableLinearLayoutManager(this, scrollCallback)

        viewModel.taskViewModels.observe(this) {
            adapter.data = it
            adapter.notifyDataSetChanged()
        }
        viewModel.isLoading.observe(this) {
            progressBar.visibility = if (it) View.VISIBLE else View.GONE
        }
        viewModel.bigMessageId.observe(this) {
            if (it != null) bigMessage.setText(it) else bigMessage.text = ""
        }
        viewModel.showList.observe(this) {
            tasksList.visibility = if (it) View.VISIBLE else View.INVISIBLE
        }
        viewModel.onCreate()
    }

    override fun onResume() {
        super.onResume()

        viewModel.onResume()
    }
}
