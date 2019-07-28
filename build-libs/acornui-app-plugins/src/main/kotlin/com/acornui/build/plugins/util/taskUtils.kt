package com.acornui.build.plugins.util

import org.gradle.api.Task
import org.gradle.api.tasks.TaskProvider

fun Task.onlyIfDidWork(taskProvider: TaskProvider<*>) {
    onlyIf {
        taskProvider.get().didWork
    }
}