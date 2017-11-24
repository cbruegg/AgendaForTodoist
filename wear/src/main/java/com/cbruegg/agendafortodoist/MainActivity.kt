package com.cbruegg.agendafortodoist

import android.content.Intent
import android.os.Bundle
import com.cbruegg.agendafortodoist.auth.AuthActivity
import com.cbruegg.agendafortodoist.projects.ProjectsActivity

class MainActivity : WearableActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val settings = app.applicationComponent.settings()
        val act = if (settings.containsAuth()) {
            ProjectsActivity::class.java
        } else {
            AuthActivity::class.java
        }
        Intent(this, act).let { startActivity(it) }
        finish()
    }
}
