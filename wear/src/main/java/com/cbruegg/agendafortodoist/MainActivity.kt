package com.cbruegg.agendafortodoist

import android.content.Intent
import android.os.Bundle
import com.cbruegg.agendafortodoist.projects.ProjectsActivity

class MainActivity : WearableActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Intent(this, ProjectsActivity::class.java).let { startActivity(it) }
        finish()
    }
}
