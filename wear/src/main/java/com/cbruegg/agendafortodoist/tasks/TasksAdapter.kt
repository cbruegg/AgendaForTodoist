package com.cbruegg.agendafortodoist.tasks

import android.annotation.SuppressLint
import android.arch.lifecycle.LifecycleOwner
import android.arch.lifecycle.Observer
import android.graphics.Paint
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import com.cbruegg.agendafortodoist.R
import com.cbruegg.agendafortodoist.util.observe

class TasksAdapter(
        var data: List<TaskViewModel>,
        lifecycleOwner: LifecycleOwner
) : RecyclerView.Adapter<TaskViewHolder>(), LifecycleOwner by lifecycleOwner {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
            TaskViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.row_task, parent, false))

    override fun onBindViewHolder(holder: TaskViewHolder, position: Int) {
        val task = data[position]

        @SuppressLint("SetTextI18n")
        holder.nameView.text = task.content
        holder.itemView.tag = holder
        holder.taskViewModel = task
        holder.strikethroughObserver = task.strikethrough.observe(this) {
            holder.nameView.paintFlags = if (it) {
                holder.nameView.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
            } else {
                holder.nameView.paintFlags and Paint.STRIKE_THRU_TEXT_FLAG.inv()
            }
        }
    }

    override fun onViewDetachedFromWindow(holder: TaskViewHolder) {
        super.onViewDetachedFromWindow(holder)

        holder.strikethroughObserver?.let { holder.taskViewModel?.strikethrough?.removeObserver(it) }
    }


    override fun getItemCount() = data.size
}

class TaskViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
    val nameView: TextView = itemView.findViewById(R.id.task_name)

    var taskViewModel: TaskViewModel? = null
    var strikethroughObserver: Observer<Boolean>? = null
}