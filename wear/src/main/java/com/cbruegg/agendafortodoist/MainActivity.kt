package com.cbruegg.agendafortodoist

import android.content.Intent
import android.os.Bundle
import android.support.wearable.activity.WearableActivity
import com.cbruegg.agendafortodoist.projects.ProjectsActivity

class MainActivity : WearableActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Enables Always-on
        setAmbientEnabled()

        Intent(this, ProjectsActivity::class.java).let { startActivity(it) }
    }
}
