package com.cbruegg.agendafortodoist

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import com.cbruegg.agendafortodoist.shared.todoist
import kotlinx.coroutines.experimental.launch
import ru.gildor.coroutines.retrofit.await

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        scratch()
    }

    private fun scratch() = launch {
        val projects = todoist.projects().await()
        projects.forEach { println(it) }

        val tasks = todoist.tasks().await()
        val projectTasks = todoist.tasks(projects[0].id).await()

        projectTasks.forEach { println(it) }
    }
}
