package com.example.todolist.view.main

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.todolist.data.model.TaskModel
import com.example.todolist.databinding.TaskItemBinding

class AllTaskRecViewAdapter : RecyclerView.Adapter<AllTaskRecViewAdapter.ViewHolder>() {
    private var allTaskItems: ArrayList<TaskModel> = ArrayList()
    private var listener: ItemClickListener? = null

    class ViewHolder(mBinding: TaskItemBinding) : RecyclerView.ViewHolder(mBinding.root) {
        var binding = mBinding
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val mBinding = TaskItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(mBinding)
    }

    @SuppressLint("NotifyDataSetChanged")
    fun setAllTaskItem(allTaskItems: ArrayList<TaskModel>) {
        this.allTaskItems = allTaskItems
        notifyDataSetChanged()
    }

    fun setListener(listener: ItemClickListener?) {
        this.listener = listener
    }

    override fun getItemCount(): Int {
        return allTaskItems.size
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.binding.task = allTaskItems[position]
        holder.binding.executePendingBindings()
        setUpOnClickListener(holder, position)
    }

    private fun setUpOnClickListener(holder: ViewHolder, position: Int) {
        holder.binding.imageViewDelete.setOnClickListener {
            listener?.onItemClick(it.tag.toString(), position, holder)
        }
        holder.binding.imageViewEdit.setOnClickListener {
            listener?.onItemClick(it.tag.toString(), position, holder)
        }
        holder.binding.imageViewMark.setOnClickListener {
            listener?.onItemClick(it.tag.toString(), position, holder)
        }
    }

    interface ItemClickListener {
        fun onItemClick(tag: String, position: Int, holder: ViewHolder)
    }
}