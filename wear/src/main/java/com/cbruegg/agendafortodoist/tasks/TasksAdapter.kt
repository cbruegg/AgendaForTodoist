package com.cbruegg.agendafortodoist.tasks

import android.annotation.SuppressLint
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import com.cbruegg.agendafortodoist.R

class TasksAdapter(
        var data: List<TaskViewModel>
) : RecyclerView.Adapter<TaskViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
            TaskViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.row_task, parent, false))


    override fun onBindViewHolder(holder: TaskViewHolder, position: Int) {
        val task = data[position]

        @SuppressLint("SetTextI18n")
        holder.nameView.text = task.content
        holder.itemView.tag = holder
    }

    override fun getItemCount() = data.size
}

class TaskViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
    val nameView: TextView = itemView.findViewById(R.id.task_name)
}