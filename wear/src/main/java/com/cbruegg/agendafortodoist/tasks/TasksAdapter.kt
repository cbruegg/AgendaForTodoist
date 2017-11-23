package com.cbruegg.agendafortodoist.tasks

import android.annotation.SuppressLint
import android.arch.lifecycle.LifecycleOwner
import android.arch.lifecycle.Observer
import android.graphics.Paint
import android.support.v4.view.GestureDetectorCompat
import android.support.v7.widget.RecyclerView
import android.view.GestureDetector
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.TextView
import com.cbruegg.agendafortodoist.R
import com.cbruegg.agendafortodoist.util.observe

class TasksAdapter(
        var data: List<TaskViewModel>,
        lifecycleOwner: LifecycleOwner,
        private val onClick: (TaskViewModel) -> Unit
) : RecyclerView.Adapter<TaskViewHolder>(), LifecycleOwner by lifecycleOwner {

    init {
        setHasStableIds(true)
    }

    override fun getItemId(position: Int) = data[position].id

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
        holder.isLoadingObserver = task.isLoading.observe(this) {
            holder.progressBar.visibility = if (it) View.VISIBLE else View.GONE
        }
        holder.onDoubleTap = task::onDoubleTab
        holder.onSwipe = task::onSwipe
        holder.onClick = { onClick(task) }
    }

    override fun onViewDetachedFromWindow(holder: TaskViewHolder) {
        super.onViewDetachedFromWindow(holder)

        holder.strikethroughObserver?.let { holder.taskViewModel?.strikethrough?.removeObserver(it) }
    }


    override fun getItemCount() = data.size
}

class TaskViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
    val nameView: TextView = itemView.findViewById(R.id.task_content)
    val progressBar: ProgressBar = itemView.findViewById(R.id.task_loading)

    var taskViewModel: TaskViewModel? = null
    var strikethroughObserver: Observer<Boolean>? = null
    var isLoadingObserver: Observer<Boolean>? = null

    var onClick: () -> Unit = {}
    var onDoubleTap: () -> Unit = {}
    var onSwipe: () -> Unit = {}

    private val detector = GestureDetectorCompat(itemView.context, object : GestureDetector.SimpleOnGestureListener() {

        override fun onSingleTapConfirmed(e: MotionEvent?): Boolean {
            onClick()
            return false
        }

        override fun onDoubleTap(e: MotionEvent?): Boolean {
            onDoubleTap()
            return false
        }

        override fun onFling(event1: MotionEvent, event2: MotionEvent, velocityX: Float, velocityY: Float): Boolean {
            val dx = Math.abs(event2.x - event1.x)
            if (itemView.width > 0 && dx >= itemView.width / 2) {
                onSwipe()
                return false
            }
            return true
        }
    })

    init {
        itemView.setOnTouchListener { _, event ->
            !detector.onTouchEvent(event)
        }
    }
}