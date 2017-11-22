package com.cbruegg.agendafortodoist.projects

import android.annotation.SuppressLint
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import com.cbruegg.agendafortodoist.R

class ProjectsAdapter(
        var data: List<ProjectViewModel>,
        private val onClick: (ProjectViewModel) -> Unit = {}
) : RecyclerView.Adapter<ProjectViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
            ProjectViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.row_project, parent, false))


    override fun onBindViewHolder(holder: ProjectViewHolder, position: Int) {
        val project = data[position]

        holder.itemView.setOnClickListener {
            onClick(project)
        }

        @SuppressLint("SetTextI18n")
        holder.nameView.text = project.indentPrefix + project.name
        holder.itemView.tag = holder
    }

    override fun getItemCount() = data.size
}

class ProjectViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
    val nameView: TextView = itemView.findViewById(R.id.project_name)
}