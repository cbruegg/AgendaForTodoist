package com.cbruegg.agendafortodoist.task

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.TextView
import com.cbruegg.agendafortodoist.R
import com.cbruegg.agendafortodoist.WearableActivity

private const val EXTRA_TASK_CONTENT = "task_content"
private const val EXTRA_TASK_ID = "task_id"

fun newTaskActivityIntent(context: Context, taskContent: String, taskId: Long) =
        Intent(context, TaskActivity::class.java).apply {
            putExtra(EXTRA_TASK_CONTENT, taskContent)
            putExtra(EXTRA_TASK_ID, taskId)
        }

class TaskActivity : WearableActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_task)

        val taskContent = intent.getStringExtra(EXTRA_TASK_CONTENT)

        val contentView = findViewById<TextView>(R.id.task_content)
        contentView.text = taskContent
    }
}