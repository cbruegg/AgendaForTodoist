package com.cbruegg.agendafortodoist.task

import android.content.Context
import android.content.Intent
import android.graphics.Paint
import android.os.Bundle
import android.support.wear.ambient.AmbientMode
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import com.cbruegg.agendafortodoist.R
import com.cbruegg.agendafortodoist.WearableActivity
import com.cbruegg.agendafortodoist.app
import com.cbruegg.agendafortodoist.auth.AuthActivity
import com.cbruegg.agendafortodoist.util.UniqueRequestIdGenerator
import com.cbruegg.agendafortodoist.util.observe
import com.cbruegg.agendafortodoist.util.viewModel

const val RESULT_COMPLETED = 1000
const val RESULT_UNCOMPLETED = 1001
const val RESULT_INTENT_EXTRA_TASK_ID = "task_id"
private const val EXTRA_TASK_CONTENT = "task_content"
private const val EXTRA_TASK_ID = "task_id"
private const val EXTRA_TASK_IS_COMPLETED = "task_is_completed"

fun newTaskActivityIntent(context: Context, taskContent: String, taskId: Long, taskIsCompleted: Boolean) =
        Intent(context, TaskActivity::class.java).apply {
            putExtra(EXTRA_TASK_CONTENT, taskContent)
            putExtra(EXTRA_TASK_ID, taskId)
            putExtra(EXTRA_TASK_IS_COMPLETED, taskIsCompleted)
        }

class TaskActivity : WearableActivity() {

    init {
        ambientCallbackDelegate = object : AmbientMode.AmbientCallback() {
            override fun onExitAmbient() {
                super.onExitAmbient()
                rootView.setBackgroundResource(R.color.activity_background)
                completionButton.visibility = View.VISIBLE
            }

            override fun onEnterAmbient(ambientDetails: Bundle?) {
                super.onEnterAmbient(ambientDetails)
                rootView.setBackgroundResource(android.R.color.black)
                completionButton.visibility = View.INVISIBLE
            }
        }
    }

    private val rootView by lazy { findViewById<View>(R.id.root) }
    private val completionButton by lazy { findViewById<Button>(R.id.task_completion_button) }
    private lateinit var viewModel: TaskViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_task)
        setAmbientEnabled()

        val taskContent = intent.getStringExtra(EXTRA_TASK_CONTENT)
        val taskId = intent.getLongExtra(EXTRA_TASK_ID, -1)
        val taskIsCompleted = intent.getBooleanExtra(EXTRA_TASK_IS_COMPLETED, false)

        val contentView = findViewById<TextView>(R.id.task_content)

        val todoist = app.netComponent.todoistRepo()
        viewModel = viewModel {
            TaskViewModel(taskContent, taskId, taskIsCompleted, todoist, UniqueRequestIdGenerator)
        }
        viewModel.onAuthError = {
            startActivity(Intent(this, AuthActivity::class.java).apply { addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP) })
        }
        viewModel.completionButtonStringId.observe(this) {
            completionButton.setText(it)
        }
        viewModel.isLoading.observe(this) {
            completionButton.isEnabled = !it
        }
        viewModel.strikethrough.observe(this) {
            contentView.paintFlags = if (it) {
                contentView.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
            } else {
                contentView.paintFlags and Paint.STRIKE_THRU_TEXT_FLAG.inv()
            }
        }
        viewModel.toast = {
            Toast.makeText(this, it, Toast.LENGTH_LONG).show()
        }
        viewModel.isCompleted.observe(this) {
            setResult(if (it) RESULT_COMPLETED else RESULT_UNCOMPLETED, Intent().apply {
                putExtra(RESULT_INTENT_EXTRA_TASK_ID, taskId)
            })
        }
        contentView.text = viewModel.taskContent
        completionButton.setOnClickListener { viewModel.onCompletionButtonClick() }

        viewModel.onCreate()
    }

}