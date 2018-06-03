package com.cbruegg.agendafortodoist.tasks

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.support.v7.app.AlertDialog
import android.support.wear.ambient.AmbientMode
import android.support.wear.widget.WearableLinearLayoutManager
import android.support.wear.widget.WearableRecyclerView
import android.view.View
import android.widget.ImageButton
import android.widget.ProgressBar
import android.widget.TextView
import com.cbruegg.agendafortodoist.R
import com.cbruegg.agendafortodoist.WearableActivity
import com.cbruegg.agendafortodoist.addtask.addTaskActivityResult
import com.cbruegg.agendafortodoist.addtask.newAddTaskActivityIntent
import com.cbruegg.agendafortodoist.app
import com.cbruegg.agendafortodoist.auth.AuthActivity
import com.cbruegg.agendafortodoist.shared.util.UniqueRequestIdGenerator
import com.cbruegg.agendafortodoist.task.RESULT_COMPLETED
import com.cbruegg.agendafortodoist.task.RESULT_INTENT_EXTRA_TASK_ID
import com.cbruegg.agendafortodoist.task.RESULT_UNCOMPLETED
import com.cbruegg.agendafortodoist.task.newTaskActivityIntent
import com.cbruegg.agendafortodoist.util.CenterScrollLayoutCallback
import com.cbruegg.agendafortodoist.util.ColorScaleListener
import com.cbruegg.agendafortodoist.util.ScaleListener
import com.cbruegg.agendafortodoist.util.observe
import com.cbruegg.agendafortodoist.util.viewModel

private const val EXTRA_PROJECT_ID = "project_id"

fun newTasksActivityIntent(context: Context, projectId: Long) =
    Intent(context, TasksActivity::class.java).apply {
        putExtra(EXTRA_PROJECT_ID, projectId)
    }

private const val REQUEST_CODE_TASK = 0
private const val REQUEST_CODE_ADD_TASK = 1

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
    private lateinit var viewModel: TasksViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_tasks)
        setAmbientEnabled()

        val projectId = intent.extras.getLong(EXTRA_PROJECT_ID)

        val scrollCallback = CenterScrollLayoutCallback(listOf(
            ScaleListener(),
            ColorScaleListener(Color.WHITE, Color.GRAY) {
                (it.tag as TaskViewHolder).nameView
            }
        ))

        val adapter = TasksAdapter(emptyList(), this) {
            startActivityForResult(newTaskActivityIntent(this, it.task.copy(isCompleted = it.isCompleted)), REQUEST_CODE_TASK)
        }
        val tasksList = findViewById<WearableRecyclerView>(R.id.tasks)
        val progressBar = findViewById<ProgressBar>(R.id.tasks_progress)
        val bigMessage = findViewById<TextView>(R.id.tasks_big_message)
        val addTasksButton = findViewById<ImageButton>(R.id.tasks_add_task)
        tasksList.adapter = adapter
        tasksList.isEdgeItemsCenteringEnabled = true
        tasksList.layoutManager = WearableLinearLayoutManager(this, scrollCallback)

        addTasksButton.setOnClickListener {
            startActivityForResult(newAddTaskActivityIntent(this, projectId), REQUEST_CODE_ADD_TASK)
        }

        viewModel = viewModel {
            val todoist = app.netComponent.todoistRepo()
            TasksViewModel(projectId, todoist, UniqueRequestIdGenerator, app.applicationComponent.settings())
        }.also {
            it.onAuthError = {
                startActivity(Intent(this, AuthActivity::class.java).apply { addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP) })
            }
        }
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
        viewModel.alert = {
            AlertDialog.Builder(this)
                .setMessage(it)
                .setPositiveButton(android.R.string.ok, null)
                .show()
        }

        viewModel.onCreate()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        when (requestCode) {
            REQUEST_CODE_TASK -> {
                val taskId = data!!.getLongExtra(RESULT_INTENT_EXTRA_TASK_ID, -1)
                val isCompleted = when (resultCode) {
                    RESULT_COMPLETED -> true
                    RESULT_UNCOMPLETED -> false
                    else -> throw IllegalArgumentException("Unexpected result code!")
                }
                viewModel.notifyCompletedStateChanged(taskId, isCompleted)
            }
            REQUEST_CODE_ADD_TASK -> {
                data!!.addTaskActivityResult?.let { viewModel.notifyTaskAdded(it) }
            }
        }
    }

}
