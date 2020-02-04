package com.acornui.build.plugins.util

import org.gradle.api.Task
import org.gradle.api.tasks.TaskProvider
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilation

fun Task.onlyIfDidWork(taskProvider: TaskProvider<*>) {
	onlyIf {
		taskProvider.get().didWork
	}
}

fun KotlinCompilation<*>.disambiguateName(simpleName: String): String {
	return lowerCamelCaseName(
			target.disambiguationClassifier,
			compilationName.takeIf { it != org.jetbrains.kotlin.gradle.plugin.KotlinCompilation.MAIN_COMPILATION_NAME },
			simpleName
	)
}

fun lowerCamelCaseName(vararg nameParts: String?): String {
	val nonEmptyParts = nameParts.mapNotNull { it?.takeIf(String::isNotEmpty) }
	return nonEmptyParts.drop(1).joinToString(
			separator = "",
			prefix = nonEmptyParts.firstOrNull().orEmpty(),
			transform = String::capitalize
	)
}