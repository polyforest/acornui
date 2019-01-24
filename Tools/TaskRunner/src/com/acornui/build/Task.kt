/*
 * Copyright 2019 PolyForest
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.acornui.build

import com.acornui.async.launch
import com.acornui.collection.copy
import com.acornui.core.toHyphenCase
import com.acornui.logging.Log
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.*
import kotlin.reflect.full.*
import kotlin.reflect.jvm.isAccessible

fun MutableList<String>.validate() {

}

fun runCommands(args: Array<String>, configProvider: KClass<*>, modelProvider: KClass<*>, tasksProvider: KClass<*>) = runCommands(args.toList(), configProvider, modelProvider, tasksProvider)

/**
 * @param args
 * @param configProvider
 * @param modelProvider
 * @param tasksProvider
 */
fun runCommands(args: List<String>, configProvider: KClass<*>, modelProvider: KClass<*>, tasksProvider: KClass<*>) {
	val configArgs = args.takeWhile { it.startsWith("-") }
	val freeArgs = args.subList(configArgs.size, args.size)
	if (freeArgs.isEmpty()) return // TODO: default to help task

	val configConstructor = configProvider.constructors.first()
	require(configConstructor.parameters.isEmpty()) { "${configProvider.simpleName} primary constructor must have zero arguments." }

	val config = configConstructor.call()
	config.configure(configArgs)

	val modelConstructor = modelProvider.constructors.first()
	require(modelConstructor.parameters.size <= 1) { "${modelProvider.simpleName} primary constructor must accept no arguments or the configuration as a single argument." }
	val model = if (modelConstructor.parameters.size == 1) {
		require(config::class.starProjectedType.isSubtypeOf(modelConstructor.parameters.first().type)) { "${modelProvider.simpleName}(${modelConstructor.parameters.first().type}) does not accept a super type of ${config::class.simpleName}" }
		modelConstructor.call(config)
	} else {
		modelConstructor.call()
	}

	val tasksConstructor = tasksProvider.constructors.first()
	require(tasksConstructor.parameters.size <= 1) { "${tasksProvider.simpleName} primary constructor must accept no arguments or the configuration as a single argument." }
	val tasks = if (tasksConstructor.parameters.size == 1) {
		require(config::class.starProjectedType.isSubtypeOf(tasksConstructor.parameters.first().type)) { "${tasksProvider.simpleName}(${tasksConstructor.parameters.first().type}) does not accept a super type of ${config::class.simpleName}" }
		tasksConstructor.call(config)
	} else {
		tasksConstructor.call(config)
	}

	tasks.execute(freeArgs, model)

}


private fun Any.configure(freeArgs: List<String>) {

	// Validate that the tasks are configured correctly:
	run {
		val knownAliases = HashMap<String, Boolean>()
		for (property in this::class.memberProperties) {
			val annotation = property.findAnnotation<ConfigProp>() ?: continue
			if (annotation.alias.isEmpty()) continue
			if (knownAliases.containsKey(annotation.alias))
				throw ConfigurationException("Duplicate alias in configuration: ${annotation.alias}")
			knownAliases[annotation.alias] = true
		}
	}

	val setProperties = HashMap<KProperty<*>, Boolean>()
	for (next in freeArgs) {
		if (!next.startsWith("-")) break
		val isAlias = next.argIsAlias
		val name = next.argName
		val value = next.argValue

		val matchingProperty = this::class.memberProperties.firstOrNull { memberProperty ->
			val taskSetPropertyAnnotation = memberProperty.findAnnotation<ConfigProp>()
			if (taskSetPropertyAnnotation != null) {
				if (isAlias)
					taskSetPropertyAnnotation.alias == name
				else
					memberProperty.name.toHyphenCase() == name
			} else {
				false
			}
		}
		if (matchingProperty == null) {
			AssertionLevels.unknownProperty.handle("No property with the ${if (isAlias) "alias" else "name"} \"$name\" exists.")
		} else {
			if (matchingProperty is KMutableProperty<*>) {
				matchingProperty.isAccessible = true
				@Suppress("UNCHECKED_CAST")
				val delegate = (matchingProperty as KProperty1<Any, *>).getDelegate(this)
				val v = value.toType(matchingProperty.returnType)
				matchingProperty.setter.call(this, v)
				if (delegate is Freezable<*>) {
					delegate.frozen = true
				}
				setProperties[matchingProperty] = true
			} else {
				throw Exception("Property ${matchingProperty.name} is not mutable.")
			}
		}
	}

	// Ensure required properties have been set.
	val unsetRequiredProperties = mutableListOf<String>()
	for (memberProperty in this::class.memberProperties) {
		val taskSetPropertyAnnotation = memberProperty.findAnnotation<ConfigProp>()
				?: continue
		if (taskSetPropertyAnnotation.required && !setProperties.containsKey(memberProperty)) {
			unsetRequiredProperties.add(memberProperty.name)
		}
	}
	if (unsetRequiredProperties.isNotEmpty())
		throw Exception("The required properties: $unsetRequiredProperties were not set.")
}

private val String.argIsAlias: Boolean
	get() {
		return !startsWith("--") && startsWith("-")
	}

private val String.argName: String
	get() {
		val isAlias = !startsWith("--")
		val equalsIndex = indexOf("=")
		return substring(if (isAlias) 1 else 2, if (equalsIndex == -1) length else equalsIndex)
	}

private val String.argValue: String
	get() {
		val equalsIndex = indexOf("=")
		return if (equalsIndex == -1) ""
		else substring(equalsIndex + 1)
	}

private fun String.toType(type: KType): Any? {
	return when {
		type.isSupertypeOf(Boolean::class.starProjectedType) -> toBooleanSafe()
		type.isSupertypeOf(String::class.starProjectedType) -> this
		type.isSupertypeOf(Int::class.starProjectedType) -> toInt()
		type.isSupertypeOf(Long::class.starProjectedType) -> toLong()
		type.isSupertypeOf(Float::class.starProjectedType) -> toFloat()
		type.isSupertypeOf(Double::class.starProjectedType) -> toDouble()
		type.isSupertypeOf(Byte::class.starProjectedType) -> toByte()
		type.isSupertypeOf(Char::class.starProjectedType) -> {
			require(this.length == 1) { "Char type expects a single character." }; this[0]
		}
		type.isSupertypeOf(Regex::class.starProjectedType) -> toRegex()
		type.isSubtypeOf(Enum::class.starProjectedType) -> {
			@Suppress("UNCHECKED_CAST")
			val enumClz = Class.forName((type.classifier as KClass<Enum<*>>).qualifiedName).enumConstants as Array<Enum<*>>
			enumClz.first { it.name.equals(this, ignoreCase = true) }
		}
		else -> throw Exception("Cannot deserialize to type $type")
	}
}

fun String.toBooleanSafe(): Boolean {
	return when (this.toLowerCase().trim()) {
		"0", "false" -> false
		"", "1", "true" -> true
		else -> throw NumberFormatException()
	}
}

/**
 * Executes a list of tasks using a model set.
 * @param args task-name --arg0 --arg1=Foo task2-name --arg0 --arg1 :subject1 :subject2
 */
private fun Any.execute(args: List<String>, model: Any) {
	val modelProvider = model::class
	val subjectArgs = args.takeLastWhile { it.startsWith(":") }
	val freeArgs = args.subList(0, args.size - subjectArgs.size)
	val subjectProperties: List<KProperty1<*, *>> = if (subjectArgs.isEmpty()) {
		// No specified subject models, use all.
		val list = mutableListOf<KProperty1<*, *>>()
		for (memberProperty in modelProvider.memberProperties) {
			memberProperty.findAnnotation<ModelProp>() ?: continue
			list.add(memberProperty)
		}
		list
	} else {
		// Find subject models with matching names or aliases.
		val list = mutableListOf<KProperty1<*, *>>()
		for (subjectArg in subjectArgs) {
			val name = subjectArg.substring(1)
			val matchingProperty = modelProvider.memberProperties.firstOrNull { memberProperty ->
				val taskSetPropertyAnnotation = memberProperty.findAnnotation<ModelProp>()
				if (taskSetPropertyAnnotation != null) {
					memberProperty.name.toHyphenCase() == name
				} else {
					false
				}
			}
			if (matchingProperty == null) {
				AssertionLevels.unknownSubject.handle("No model subject with the name \"$name\" exists.")
			} else {
				list.add(matchingProperty)
			}
		}
		list
	}

	// Create a list of pending work, and do no work until arguments are all verified.
	val allTasks = this::class.members.filter { member -> member.findAnnotation<Task>() != null }

	// Validate that the tasks are configured correctly:
	for (task in allTasks) {
		val knownAliases = HashMap<String, Boolean>()
		for (i in (if (task.extensionReceiverParameter == null) 1 else 2)..task.parameters.lastIndex) {
			val taskParam = task.parameters[i]
			val annotation = taskParam.findAnnotation<TaskArgument>()
			if (annotation != null && annotation.alias.isNotBlank()) {
				if (knownAliases.containsKey(annotation.alias))
					throw ConfigurationException("Duplicate argument alias in task ${task.name}: ${annotation.alias}")
				knownAliases[annotation.alias] = true
			}
			if (annotation == null && !taskParam.isOptional)
				throw ConfigurationException("Task ${task.name} has a non-optional parameter without a TaskArgument annotation.")
		}
	}

	val pendingWork = mutableListOf<() -> Unit>()

	var allTasksMatched = true
	var argIndex = 0
	while (argIndex < freeArgs.size) {
		val taskName = freeArgs[argIndex++]
		val taskArguments = mutableListOf<String>()
		while (argIndex < freeArgs.size && freeArgs[argIndex].startsWith("-")) {
			taskArguments.add(freeArgs[argIndex++])
		}

		var matchingMethodFound = false
		val tasksNameMatch = allTasks.filter { it.name.toHyphenCase() == taskName }
		for (task in tasksNameMatch) {
			// Loop over task name matches that have receivers, checking them against our model subjects list.
			val taskReceiverType = task.extensionReceiverParameter?.type ?: continue
			for (subjectProperty in subjectProperties) {
				if (taskReceiverType.isSupertypeOf(subjectProperty.returnType)) {
					val freeTaskArgs = taskArguments.copy()
					val map = mutableMapOf<KParameter, Any?>()
					if (createCallArguments(task.parameters.subList(2, task.parameters.size), freeTaskArgs, map)) {
						// We found the right overload.
						matchingMethodFound = true
						map[task.parameters[0]] = this
						map[task.parameters[1]] = subjectProperty.getter.call(model)
						pendingWork.add {
							task.callBy(map)
						}
					}
				}
			}
		}

		if (!matchingMethodFound) {
			// If we had no task matches with an extension receiver, loop over task name matches that do not have receivers.
			for (task in tasksNameMatch) {
				if (task.extensionReceiverParameter != null) continue
				val freeTaskArgs = taskArguments.copy()
				val map = mutableMapOf<KParameter, Any?>()
				if (createCallArguments(task.parameters.subList(1, task.parameters.size), freeTaskArgs, map)) {
					// We found the right overload.
					matchingMethodFound = true
					map[task.parameters[0]] = this
					pendingWork.add {
						task.callBy(map)
					}
					break
				}
			}
		}
		if (!matchingMethodFound) {
			allTasksMatched = false
			Log.warn("Could not find a matching task for $taskName $taskArguments")
		}
	}
	if (allTasksMatched) {
		for (work in pendingWork) work()
	}
}

/**
 * @param parameters The method's non-receiver or extension receiver parameters.
 * @param args The CLI arguments
 * @param out The matched argument map to be used with `KCallable.callBy`
 * @return Returns true if the parameter list matches the given cli arguments list.
 */
private fun createCallArguments(parameters: List<KParameter>, args: List<String>, out: MutableMap<KParameter, Any?>): Boolean {
	val freeArgs = args.copy()
	var allParamsFound = true
	for (taskParam in parameters) {
		val annotation = taskParam.findAnnotation<TaskArgument>()
		// TODO: handle vararg
		if (annotation != null) {
			val index = freeArgs.indexOfFirst {
				it.argName == if (it.argIsAlias) annotation.alias else taskParam.name!!.toHyphenCase()
			}
			if (index != -1 || taskParam.isOptional) {
				if (index != -1) {
					try {
						val value = freeArgs[index].argValue.toType(taskParam.type)
						out[taskParam] = value
						freeArgs.removeAt(index)
					} catch (ignore: Exception) {
					}
				}
			} else {
				allParamsFound = false
				break
			}
		}
	}
	return allParamsFound && freeArgs.isEmpty()
}


@ConfigObject("Assertion levels for various potential problems in configuration and command arguments.")
object AssertionLevels {

	@ConfigProp
	var unknownProperty = AssertionLevel.WARNING

	@ConfigProp
	var unknownSubject = AssertionLevel.WARNING

	@ConfigProp
	var unknownTask = AssertionLevel.ERROR
}

enum class AssertionLevel(val handle: (message: String) -> Unit) {
	NONE({}),
	WARNING({ Log.warn(it) }),
	ERROR({ throw Exception(it) });
}


@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
@MustBeDocumented
annotation class ConfigObject(
		val description: String
)

@Target(AnnotationTarget.PROPERTY)
@Retention(AnnotationRetention.RUNTIME)
@MustBeDocumented
annotation class ConfigProp(
		val description: String = "",
		val alias: String = "",
		val required: Boolean = false
)

@Target(AnnotationTarget.PROPERTY)
@Retention(AnnotationRetention.RUNTIME)
@MustBeDocumented
annotation class ModelProp(
		val description: String = ""
)

@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
@MustBeDocumented
annotation class Task(val description: String = "")

@Target(AnnotationTarget.VALUE_PARAMETER)
@Retention(AnnotationRetention.RUNTIME)
@MustBeDocumented
annotation class TaskArgument(
		val description: String = "",
		val alias: String = ""
)

class Freezable<T>(default: T) : ReadWriteProperty<Any, T> {

	var frozen = false
	private var value = default

	override fun getValue(thisRef: Any, property: KProperty<*>): T = value

	override fun setValue(thisRef: Any, property: KProperty<*>, value: T) {
		require(!frozen) { "Cannot change configuration properties." }
		this.value = value
	}
}

class ConfigurationException(message: String) : Exception(message)