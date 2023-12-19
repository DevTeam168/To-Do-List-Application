package com.example.todolist.data.model

data class TaskModel(
    var task: String = "",
    var dateTime: String = "",
    var isComplete: Boolean = false
)