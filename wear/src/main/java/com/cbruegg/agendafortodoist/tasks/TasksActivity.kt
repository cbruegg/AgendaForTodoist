package com.cbruegg.agendafortodoist.tasks

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.support.wear.ambient.AmbientMode
import android.support.wear.widget.WearableLinearLayoutManager
import android.support.wear.widget.WearableRecyclerView
import com.cbruegg.agendafortodoist.R
import com.cbruegg.agendafortodoist.WearableActivity
import com.cbruegg.agendafortodoist.shared.todoist
import com.cbruegg.agendafortodoist.util.CenterScrollLayoutCallback
import com.cbruegg.agendafortodoist.util.ColorScaleListener
import com.cbruegg.agendafortodoist.util.MutableLiveData
import com.cbruegg.agendafortodoist.util.ScaleListener
import com.cbruegg.agendafortodoist.util.observe
import com.cbruegg.agendafortodoist.util.viewModel
import kotlinx.coroutines.experimental.android.UI
import kotlinx.coroutines.experimental.launch
import ru.gildor.coroutines.retrofit.await

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
                tasksList.setBackgroundResource(R.color.activity_background)
            }

            override fun onEnterAmbient(ambientDetails: Bundle?) {
                super.onEnterAmbient(ambientDetails)
                tasksList.setBackgroundResource(android.R.color.black)
            }
        }
    }

    private val tasksList by lazy { findViewById<WearableRecyclerView>(R.id.tasks) }

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

        val adapter = TasksAdapter(emptyList())
        tasksList.adapter = adapter
        tasksList.isEdgeItemsCenteringEnabled = true
        tasksList.layoutManager = WearableLinearLayoutManager(this, scrollCallback)

        val viewModel = viewModel {
            TasksViewModel(MutableLiveData(emptyList()))
        }

        viewModel.taskViewModels.observe(this) {
            adapter.data = it
            adapter.notifyDataSetChanged()
        }

        val projectId = intent.extras.getLong(EXTRA_PROJECT_ID)
        launch(UI) {
            val tasks = todoist.tasks(projectId).await()
            val taskVms = tasks.map { TaskViewModel(it) }
            viewModel.taskViewModels.data = taskVms
        }
    }
}
