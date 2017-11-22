package com.cbruegg.agendafortodoist.projects

import android.graphics.Color
import android.os.Bundle
import android.support.wear.widget.WearableLinearLayoutManager
import android.support.wear.widget.WearableRecyclerView
import com.cbruegg.agendafortodoist.R
import com.cbruegg.agendafortodoist.WearableActivity
import com.cbruegg.agendafortodoist.shared.todoist
import com.cbruegg.agendafortodoist.tasks.newTasksActivityIntent
import com.cbruegg.agendafortodoist.util.CenterScrollLayoutCallback
import com.cbruegg.agendafortodoist.util.ColorScaleListener
import com.cbruegg.agendafortodoist.util.MutableLiveData
import com.cbruegg.agendafortodoist.util.ScaleListener
import com.cbruegg.agendafortodoist.util.observe
import com.cbruegg.agendafortodoist.util.viewModel
import kotlinx.coroutines.experimental.android.UI
import kotlinx.coroutines.experimental.launch
import ru.gildor.coroutines.retrofit.await

class ProjectsActivity : WearableActivity() {

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

        val projectList = findViewById<WearableRecyclerView>(R.id.projects)
        val adapter = ProjectsAdapter(emptyList()) {
            startActivity(newTasksActivityIntent(this, it.id))
        }
        projectList.adapter = adapter
        projectList.isEdgeItemsCenteringEnabled = true
        projectList.layoutManager = WearableLinearLayoutManager(this, scrollCallback)

        val viewModel = viewModel {
            ProjectsViewModel(MutableLiveData(emptyList()))
        }
        viewModel.projectViewModels.observe(this) {
            adapter.data = it
            adapter.notifyDataSetChanged()
        }

        launch(UI) {
            val projects = todoist.projects().await()
            val projectVms = projects.map { ProjectViewModel(it) }
            viewModel.projectViewModels.data = projectVms
        }
    }
}

