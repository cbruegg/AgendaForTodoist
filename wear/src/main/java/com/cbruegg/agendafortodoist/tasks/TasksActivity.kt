package com.cbruegg.agendafortodoist.tasks

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.support.wear.widget.WearableLinearLayoutManager
import android.support.wear.widget.WearableRecyclerView
import com.cbruegg.agendafortodoist.R
import com.cbruegg.agendafortodoist.WearableActivity
import com.cbruegg.agendafortodoist.projects.ProjectViewHolder
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

        val tasksList = findViewById<WearableRecyclerView>(R.id.tasks)
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
