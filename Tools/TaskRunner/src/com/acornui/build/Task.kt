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

import com.acornui.collection.copy
import com.acornui.logging.Log
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.*
import kotlin.reflect.full.*
import kotlin.reflect.jvm.isAccessible

/**
 * Array arguments overload.
 */
fun <T> runCommands(args: Array<String>, configProvider: () -> T, modelProvider: (T) -> Any = {}, tasksProvider: (T) -> Any) = runCommands(args.toList(), configProvider, modelProvider, tasksProvider)

/**
 * No configuration, Array arguments, overload.
 */
fun runCommands(args: Array<String>, modelProvider: () -> Any = {}, tasksProvider: () -> Any) = runCommands(args.toList(), configProvider = {}, modelProvider = { modelProvider() }, tasksProvider = { tasksProvider() })

/**
 * No configuration overload.
 */
fun runCommands(args: List<String> = emptyList(), modelProvider: () -> Any = {}, tasksProvider: () -> Any) = runCommands(args, configProvider = {}, modelProvider = { modelProvider() }, tasksProvider = { tasksProvider() })

/**
 * @param args
 * @param configProvider
 * @param modelProvider
 * @param tasksProvider
 */
fun <T> runCommands(args: List<String> = emptyList(), configProvider: () -> T, modelProvider: (T) -> Any = {}, tasksProvider: (T) -> Any) {
	clearIdempotentCache()
	val configArgs = args.takeWhile { it.startsWith("-") }.toMutableList()
	val freeArgs = args.subList(configArgs.size, args.size)
	val config = configProvider()
	if (config is Any) config.configure(configArgs)
	val model = modelProvider(config)
	val tasks = tasksProvider(config)

	if (configArgs.isNotEmpty()) {
		AssertionLevels.unknownProperty.handle(AssertionType.CLI) {
			"The following properties were not understood: $configArgs"
		}
	}

	tasks.execute(freeArgs, model)
}

/**
 * Configures the object from the given parameter list.
 */
fun Any.configure(freeArgs: MutableList<String>) = configure(freeArgs, true, "", HashMap())

private fun Any.configure(freeArgs: MutableList<String>, recursive: Boolean, parentName: String, knownAliases: MutableMap<String, Boolean>) {
	run {
		// Validate that the properties are configured correctly:
		for (property in this::class.memberProperties) {
			property.isAccessible = true
			val annotation = property.findAnnotation<ConfigProp>() ?: continue
			if (annotation.alias.isNotEmpty()) {
				if (knownAliases.containsKey(annotation.alias))
					throw ConfigurationException("Duplicate alias in configuration: ${annotation.alias}")
				knownAliases[annotation.alias] = true
			}
			@Suppress("UNCHECKED_CAST")
			val freeze = (property as KProperty1<Any, *>).getDelegate(this) as? Freezable<*>
			if (freeze == null && property is KMutableProperty<*>) {
				AssertionLevels.delegateNotFreezable.handle { "Property ${property.name} is mutable, but not a Freezable delegate." }
			}
			if (freeze != null && property !is KMutableProperty<*>) {
				AssertionLevels.delegateNotFreezable.handle { "Property ${property.name} is not mutable, but is a Freezable delegate." }
			}
			if (freeze?.frozen == true)
				throw ConfigurationException("Object of type ${this::class.simpleName} has already been configured.")
		}
	}

	val iterator = freeArgs.listIterator()
	while (iterator.hasNext()) {
		val next = iterator.next()
		val isAlias = next.argIsAlias
		val name = next.argName
		val value = next.argValue

		val matchingProperty = this::class.memberProperties.firstOrNull { memberProperty ->
			val taskSetPropertyAnnotation = memberProperty.findAnnotation<ConfigProp>()
			if (taskSetPropertyAnnotation != null) {
				if (isAlias)
					name == taskSetPropertyAnnotation.alias
				else
					name == "$parentName${memberProperty.name}"
			} else {
				false
			}
		} as KMutableProperty<*>?
		if (matchingProperty != null) {
			matchingProperty.isAccessible = true
			@Suppress("UNCHECKED_CAST")
			val delegate = (matchingProperty as KProperty1<Any, *>).getDelegate(this) as Freezable<*>
			if (!delegate.isSet) {
				val v = value.toType(matchingProperty.returnType)
				matchingProperty.setter.call(this, v)
				iterator.remove()
			}

		}
	}
	freeze(recursive = false)

	if (recursive) {
		for (property in this::class.memberProperties) {
			property.isAccessible = true
			if (property !is KMutableProperty<*>) {
				val subObject = property.getter.call(this)
				subObject?.configure(freeArgs, recursive, "$parentName${property.name}.", knownAliases)
			}
		}
	}
}

/**
 * Freezes any Freezable property delegates.
 * @param recursive If true, the property values of this object will also be frozen.
 * @throws CliException If a required property has not been set, no properties will be frozen and an exception will
 * be thrown.
 */
fun Any.freeze(recursive: Boolean = true) {
	freeze(recursive, mutableListOf(), "")
}

private fun Any.freeze(recursive: Boolean = true, unsetRequiredProperties: MutableList<String>, parentName: String) {
	// Ensure required properties have been set.
	for (property in this::class.memberProperties) {
		property.isAccessible = true
		@Suppress("UNCHECKED_CAST")
		val delegate = (property as? KProperty1<Any, *>)?.getDelegate(this) as? Freezable<*> ?: continue
		if (delegate.required && !delegate.isSet) {
			unsetRequiredProperties.add("$parentName${property.name}")
		} else {
			delegate.frozen = true
			if (recursive) {
				val value = delegate.getValue(this, property)
				value?.freeze(true, unsetRequiredProperties, "$parentName${property.name}.")
			}
		}
	}
	if (unsetRequiredProperties.isNotEmpty()) {
		throw CliException("The required properties: $unsetRequiredProperties were not set.")
	}
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
		else -> throw CliException("Cannot deserialize to type $type")
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
					memberProperty.name == name
				} else {
					false
				}
			}
			if (matchingProperty == null) {
				AssertionLevels.unknownSubject.handle(AssertionType.CLI) { "No model subject with the name \"$name\" exists." }
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

		var matchingTaskFound = false
		val tasksNameMatch = allTasks.filter { it.name == taskName }
		for (subjectProperty in subjectProperties) {
			var bestMatchParams: Map<KParameter, Any?>? = null
			var bestMatchCallable: KCallable<*>? = null
			for (task in tasksNameMatch) {
				// Loop over task name matches that have receivers, checking them against our model subjects list.
				val taskReceiverType = task.extensionReceiverParameter?.type ?: continue

				if (taskReceiverType.isSupertypeOf(subjectProperty.returnType)) {
					val freeTaskArgs = taskArguments.copy()
					val map = mutableMapOf<KParameter, Any?>()
					if (createCallArguments(task.parameters.subList(2, task.parameters.size), freeTaskArgs, map)) {
						// We found the right overload.
						map[task.parameters[0]] = this
						map[task.parameters[1]] = subjectProperty.getter.call(model)
						if (bestMatchCallable == null || taskReceiverType.isSubtypeOf(bestMatchCallable.extensionReceiverParameter!!.type)) {
							bestMatchCallable = task
							bestMatchParams = map
						}
						matchingTaskFound = true
					}
				}
			}
			if (bestMatchCallable != null) {
				pendingWork.add {
					bestMatchCallable.callBy(bestMatchParams!!)
				}
			}
		}
		if (!matchingTaskFound) {
			// If we had no task matches with an extension receiver, loop over task name matches that do not have receivers.
			for (task in tasksNameMatch) {
				if (task.extensionReceiverParameter != null) continue
				val freeTaskArgs = taskArguments.copy()
				val map = mutableMapOf<KParameter, Any?>()
				if (createCallArguments(task.parameters.subList(1, task.parameters.size), freeTaskArgs, map)) {
					// We found the right overload.
					matchingTaskFound = true
					map[task.parameters[0]] = this
					pendingWork.add {
						task.callBy(map)
					}
					break
				}
			}
		}
		if (!matchingTaskFound) {
			allTasksMatched = false
			AssertionLevels.unknownTask.handle(AssertionType.CLI) { "Could not find a matching task for $taskName $taskArguments" }
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
				it.argName == if (it.argIsAlias) annotation.alias else taskParam.name
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


object AssertionLevels {

	/**
	 * A configuration property is not freezable.
	 */
	var delegateNotFreezable = AssertionLevel.ERROR

	/**
	 * A configuration property provided on the command line was not found.
	 */
	var unknownProperty = AssertionLevel.WARNING

	/**
	 * A model subject provided on the command line was not found.
	 */
	var unknownSubject = AssertionLevel.WARNING

	/**
	 * A task provided on the command line was not found.
	 */
	var unknownTask = AssertionLevel.ERROR
}

enum class AssertionType {
	CONFIGURATION,
	CLI;

	fun handle(message: String): Nothing {
		throw when (this) {
			CONFIGURATION -> ConfigurationException(message)
			CLI -> CliException(message)
		}
	}
}

enum class AssertionLevel {
	NONE,
	WARNING,
	ERROR;

	fun handle(type: AssertionType = AssertionType.CONFIGURATION, lazyMessage: () -> String) {
		when (this) {
			NONE -> {
			}
			WARNING -> Log.warn(lazyMessage())
			ERROR -> type.handle(lazyMessage())
		}
	}
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
		val alias: String = ""
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

class Freezable<T>(val default: T?, val required: Boolean = false) : ReadWriteProperty<Any, T> {

	constructor() : this(null, true)

	/**
	 * If this value is true, the delegate will not allow changes.
	 */
	var frozen = false
	private var value = default

	/**
	 * Returns true if the freezable property has been set.
	 */
	var isSet = false
		private set

	override fun getValue(thisRef: Any, property: KProperty<*>): T {
		if (required && !isSet) throw Exception("Required property ${property.name} was not set.")
		@Suppress("UNCHECKED_CAST")
		return value as T
	}

	override fun setValue(thisRef: Any, property: KProperty<*>, value: T) {
		isSet = true
		require(!frozen) { "Cannot change frozen configuration properties." }
		this.value = value
	}
}

class ConfigurationException(message: String) : Exception(message)
class CliException(message: String) : Exception(message)

private val idempotenceCache = HashMap<Any, Boolean>()

fun clearIdempotentCache() {
	idempotenceCache.clear()
}

fun idempotent(vararg excludes: Any, inner: () -> Unit) {
	val captured = inner::class.java.declaredFields.map { it.get(inner) } - excludes
	if (idempotenceCache.containsKey(captured)) {
		@Suppress("UNCHECKED_CAST")
		idempotenceCache[captured]
	} else {
		idempotenceCache[captured] = true
		inner()
	}
}