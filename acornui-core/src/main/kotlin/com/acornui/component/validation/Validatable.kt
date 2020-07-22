/*
 * Copyright 2020 Poly Forest, LLC
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

package com.acornui.component.validation

import com.acornui.assertionsEnabled
import com.acornui.collection.removeFirst
import com.acornui.string.toRadix
import kotlin.properties.ReadOnlyProperty
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

private class ValidationNode(

	/**
	 * This node's flag.
	 */
	val flag: Int,

	/**
	 * The direct dependents (no transitive dependents).
	 */
	var dependentsSelf: Int,

	/**
	 * The direct dependencies (no transitive dependencies).
	 */
	var dependenciesSelf: Int,

	var onValidate: () -> Unit
) {

	/**
	 * The flattened dependents of this node.
	 * When this node's flag is invalidated, these flags will also be invalidated.
	 */
	var dependents: Int = -1

	/**
	 * The flattened dependencies of this node.
	 * When flags are being validated, if any of them are in this mask, this node will also be validated.
	 */
	var dependencies: Int = -1

	var isValid: Boolean = false
	var validatedCount = 0

	/**
	 * Adds the given dependency bit flags.
	 */
	fun addDependencies(value: Int) {
		dependencies = dependencies or value
	}

	/**
	 * Adds the given dependent bit flags.
	 */
	fun addDependents(value: Int) {
		dependents = dependents or value
	}

}

/**
 * A dependency graph of validation flags.
 * When a flag is invalidated, all dependents are also invalidated. When a flag is validated, all dependencies are
 * guaranteed to be valid first.
 *
 * The UI Component implementation will work in this way:
 * UI Components will validate the validation graph top down, level order. When a component validates a flag such as
 * layout, that component may require that a child component have a valid layout in order to determine its measured
 * size. On retrieving the child component size, that child component may then validate its layout, thus effectively
 * validating certain flags in a bottom-up manner.
 */
class ValidationGraph(

	/**
	 * For error messages, bit flags can be hard to read. This method will convert the Integer flag into a human
	 * readable string.
	 * The default uses the names of the [ValidationFlags] properties.
	 */
	val toFlagString: Int.() -> String = ValidationFlags::flagToString
) {

	private var isNodeListValid = false
	private var nodes = ArrayList<ValidationNode>()
	private val nodesMap = HashMap<Int, ValidationNode>()

	/**
	 * The current node being validated.
	 */
	private var currentIndex = -1

	/**
	 * Returns the current flag being validated, or -1 if there is not a current validation.
	 */
	val currentFlag: Int
		get() {
			if (currentIndex == -1) return -1
			return nodes[currentIndex].flag
		}

	/**
	 * Returns true if this validation graph is currently validating via [validateNodeList].
	 */
	val isValidating: Boolean
		get() = currentIndex != -1

	private var _invalidFlags = 0
	val invalidFlags: Int
		get() = _invalidFlags

	private var _allFlags = 0

	/**
	 * All flags the added nodes represent.
	 */
	val allFlags: Int
		get() = _allFlags

	/**
	 * Appends a validation node.
	 * @param flag The target validation bit flag.  This must be a power of two.  UiComponent reserves flags
	 * `1` through `1 shl 15` so any custom flags for components should start at `1 shl 16`.
	 *
	 * @param dependencies If any of these dependencies become invalid, this node will also become invalid.
	 * This can be multiple flags by using bitwise OR.  E.g. `ValidationFlags.LAYOUT or ValidationFlags.TRANSFORM`.
	 * An [IllegalArgumentException] will be thrown if any dependency flags couldn't be found.
	 *
	 * @param dependents If [flag] becomes invalid, all of its dependents will also become invalid.
	 * This can be multiple flags by using bitwise OR.  E.g. `ValidationFlags.LAYOUT or ValidationFlags.TRANSFORM`.
	 * An [IllegalArgumentException] will be thrown if any dependent flags couldn't be found.
	 */
	fun addNode(flag: Int, dependencies: Int, dependents: Int, onValidate: () -> Unit) {
		require(com.acornui.math.isPowerOfTwo(flag)) { "flag ${flag.toRadix(2)} is not a power of 2." }
		requireFlagsExist(dependencies or dependents)

		// When this node is validated, we should validate dependencies first.
		// When this node is invalidated, we should also invalidated dependents.
		val newNode = ValidationNode(flag, dependents, dependencies, onValidate)
		nodes.add(newNode)
		isNodeListValid = false
		_invalidFlags = _invalidFlags or flag
		_allFlags = _allFlags or flag
	}

	fun addNode(flag: Int, onValidate: () -> Unit) = addNode(flag, 0, 0, onValidate)

	fun addNode(flag: Int, dependencies: Int, onValidate: () -> Unit) = addNode(flag, dependencies, 0, onValidate)

	/**
	 * Removes the node for the given flag.
	 * @return Returns true if the node was found.
	 */
	fun removeNode(flag: Int): Boolean {
		nodes.removeFirst { it.flag == flag } ?: return false
		isNodeListValid = false
		_allFlags = _allFlags and flag.inv()
		_invalidFlags = _invalidFlags and flag.inv()
		return true
	}

	/**
	 * Adds dependencies for the given [flag].
	 * @param newDependencies Dependencies to add to the existing dependencies.
	 */
	fun addDependencies(flag: Int, newDependencies: Int = 0) {
		requireFlagsExist(newDependencies)

		val node = nodes.first { it.flag == flag }
		node.dependenciesSelf = node.dependenciesSelf or newDependencies
		isNodeListValid = false
		_invalidFlags = _invalidFlags or flag
	}

	/**
	 * Adds dependents for the given [flag].
	 * @param newDependents Dependents to add to the existing dependents.
	 */
	fun addDependents(flag: Int, newDependents: Int = 0) {
		requireFlagsExist(newDependents)
		val node = nodes.first { it.flag == flag }
		node.dependentsSelf = node.dependentsSelf or newDependents
		isNodeListValid = false
		_invalidFlags = _invalidFlags or newDependents
	}

	/**
	 * Remove dependencies for the given [flag].
	 * @param dependencies Dependencies to remove from the existing dependencies.
	 */
	fun removeDependencies(flag: Int, dependencies: Int = 0) {
		requireFlagsExist(dependencies)
		val node = nodes.first { it.flag == flag }
		node.dependenciesSelf = node.dependenciesSelf and dependencies.inv()
		isNodeListValid = false
	}

	/**
	 * Removes dependents for the given [flag].
	 * @param dependents Dependents to remove from the existing dependents.
	 */
	fun removeDependents(flag: Int, dependents: Int = 0) {
		requireFlagsExist(dependents)
		val node = nodes.first { it.flag == flag }
		node.dependentsSelf = node.dependentsSelf and dependents.inv()
		isNodeListValid = false
	}

	private fun validateNodeList() {
		isNodeListValid = true
		val pendingNodes = nodes
		nodes = ArrayList()
		for (node in pendingNodes) {
			val flag = node.flag
			node.dependencies = node.dependenciesSelf or flag
			node.dependents = node.dependentsSelf or flag
			var insertIndex = nodes.size
			for (i in 0..nodes.lastIndex) {
				val previousNode = nodes[i]
				require(previousNode.flag != flag) { "flag ${flag.toFlagString()} already exists." }
				if (previousNode.dependencies and node.dependents > 0) {
					// The existing node is a dependent of the added node.
					previousNode.addDependencies(node.dependencies)
					node.addDependents(previousNode.dependents)

					// Thew new node must come before the existing node.
					if (insertIndex > i)
						insertIndex = i
				}
				if (previousNode.dependents and node.dependencies > 0) {
					// The existing node is a dependency of the added node.
					node.addDependencies(previousNode.dependencies)
					previousNode.addDependents(node.dependents)

					// Do not allow cyclical dependencies:
					check(insertIndex > i) {
						"Validation node cannot be added after dependency ${previousNode.flag.toFlagString()} and before all dependents ${node.dependentsSelf.toFlagsString()}"
					}
				}
			}
			nodes.add(insertIndex, node)
			nodesMap[node.flag] = node
		}
	}

	/**
	 * Invalidates the given flags.
	 * @return Returns a bitmask of all the flags invalidated. (This will consider the dependencies for all nodes
	 * within the [flags] argument.
	 */
	fun invalidate(flags: Int = -1): Int {
		if (!isNodeListValid) validateNodeList()
		var flagsInvalidated = 0
		var flagsToInvalidate = flags and _invalidFlags.inv()
		if (flagsToInvalidate == 0) return 0
		if (assertionsEnabled && currentIndex >= 0) {
			// Cannot invalidate anything that is not dependent on the current node.
			val currentNode = nodes[currentIndex]
			val badFlags = flagsToInvalidate and currentNode.dependents.inv()
			check(badFlags == 0) {
				"Cannot invalidate ${flags.toFlagsString()} while validating ${currentNode.flag.toFlagString()}; The following invalidated flags are not dependents of the current node: ${badFlags.toFlagsString()}"
			}
		}
		for (i in 0..nodes.lastIndex) {
			val n = nodes[i]
			if (!n.isValid) continue
			if (flagsToInvalidate and n.flag != 0) {
				n.isValid = false
				flagsToInvalidate = flagsToInvalidate or n.dependents
				flagsInvalidated = flagsInvalidated or n.flag
			}
		}
		_invalidFlags = _invalidFlags or flagsInvalidated
		return flagsInvalidated
	}

	/**
	 * Validates the given flags.
	 *
	 * @return Returns the flags actually validated.
	 */
	fun validate(flags: Int = -1): Int {
		if (!isNodeListValid) validateNodeList()
		if (currentIndex != -1) {
			if (assertionsEnabled) {
				val node = nodes[currentIndex]
				val badFlags = (node.dependents and node.flag.inv()) and flags
				check(badFlags <= 0) { "Cannot validate ${badFlags.toFlagsString()} while validating ${node.flag.toFlagString()}" }
			}
			return 0
		}
		var flagsToValidate = flags and _invalidFlags
		if (flagsToValidate == 0) return 0
		var flagsValidated = 0
		for (i in 0..nodes.lastIndex) {
			currentIndex = i
			val n = nodes[i]
			if (!n.isValid && flagsToValidate and n.dependents > 0) {
				n.onValidate()
				n.validatedCount++
				n.isValid = true
				flagsToValidate = flagsToValidate or n.dependencies
				flagsValidated = flagsValidated or n.flag
				_invalidFlags = _invalidFlags and n.flag.inv()
			}
		}
		currentIndex = -1
		return flagsValidated
	}

	fun isValid(flag: Int): Boolean {
		return _invalidFlags and flag == 0
	}

	fun getValidatedCount(flag: Int): Int {
		return (nodesMap[flag] ?: error("Unknown flag $flag")).validatedCount
	}

	private fun Int.toFlagsString(): String {
		return toFlagsArray().joinToString { it.toFlagString() }
	}

	private fun requireFlagsExist(flags: Int) {
		val missingFlags = _allFlags.inv()
		val missingRequiredFlags = flags and missingFlags
		require(missingRequiredFlags == 0) { "Required flags not found: ${missingRequiredFlags.toFlagsString()}" }
	}
}

/**
 * Returns true if this bit mask contains the given binary bit mask.
 * The same as: `this and flags == flags`
 * ```
 * 0b0101 containsAllFlags 0b100 // true
 * 0b0101 containsAllFlags 0b101 // true
 * 0b0101 containsAllFlags 0b111 // false
 * 0b0101 containsAllFlags  0b10 // false
 * 0b0101 containsAllFlags 0b101 // true
 * 0b0101 containsAllFlags 0b111 // false
 * ```
 */
@Suppress("NOTHING_TO_INLINE")
inline infix fun Int.containsAllFlags(flags: Int): Boolean {
	return this and flags == flags
}

/**
 * Returns true if this bit mask contains any of the given flags.
 * The same as: `this and flags > 0`
 *
 * 0b0101 containsAnyFlags 0b100 // true
 * 0b0101 containsAnyFlags  0b11 // true
 * 0b0101 containsAnyFlags  0b10 // false
 * 0b0101 containsAnyFlags 0b101 // true
 * 0b0101 containsAnyFlags 0b111 // false
 */
@Suppress("NOTHING_TO_INLINE")
inline infix fun Int.containsAnyFlags(flags: Int): Boolean {
	return this and flags > 0
}

fun Int.toFlagsArray(): List<Int> {
	val list = ArrayList<Int>()
	for (i in 0..31) {
		val flag = 1 shl i
		if (this containsAllFlags flag) {
			list.add(flag)
		}
	}
	return list
}

fun validationGraph(
	toFlagString: Int.() -> String = ValidationFlags::flagToString,
	init: ValidationGraph.() -> Unit
): ValidationGraph {
	val v = ValidationGraph(toFlagString)
	v.init()
	return v
}

/**
 * Returns a property that will invalidate the given flags on set.
 * @param initialValue The initial value of this property.
 * @param flags The bit flags to invalidate when the property is set.
 */
fun <T> ValidationGraph.invalidateOnSet(initialValue: T, flags: Int): ReadWriteProperty<Any, T> {
	return object : ReadWriteProperty<Any, T> {
		private var backingValue: T = initialValue
		override fun getValue(thisRef: Any, property: KProperty<*>): T = backingValue
		override fun setValue(thisRef: Any, property: KProperty<*>, value: T) {
			if (value == backingValue) return // no-op
			backingValue = value
			this@invalidateOnSet.invalidate(flags)
		}
	}
}

/**
 * Returns a read-only property delegate that caches a calculated result.
 *
 * The calculated result will be recalculated when this property is read and any of the given flags have been validated
 * since the last read.
 *
 * @param flags The flags which, if invalidated, will invalidate the cached result.
 * @param getter The function to invoke to calculate the result. This will not be invoked when the component is
 * validated, only when this property is retrieved.
 */
fun <T> ValidationGraph.validateOnGet(flags: Int, getter: () -> T): ReadOnlyProperty<Any, T> =
	ValidationPropertyWithCalculator(this, flags, getter)

private class ValidationPropertyWithCalculator<T>(private val graph: ValidationGraph, private val flags: Int, private val calculator: () -> T) :
	ReadOnlyProperty<Any, T> {

	private var lastValidatedCount = -1
	private var cached: T? = null
	private val flagsSplit = flags.toFlagsArray()

	override fun getValue(thisRef: Any, property: KProperty<*>): T {
		graph.validate(flags)
		val c = flagsSplit.sumBy { graph.getValidatedCount(it) }
		if (lastValidatedCount != c) {
			cached = calculator()
			lastValidatedCount = c
		}
		@Suppress("UNCHECKED_CAST")
		return cached as T
	}
}

/**
 * Returns a property that will validate the given flags on get.
 * @param initialValue The initial value of the property.
 * @param flag The flag to validate on get before returning the value.
 */
fun <T> ValidationGraph.validateOnGet(initialValue: T, flag: Int): ReadWriteProperty<Any, T> =
	ValidationProperty(this, initialValue, flag)

private class ValidationProperty<T>(private val graph: ValidationGraph, initialValue: T, private val flag: Int) :
	ReadWriteProperty<Any, T> {

	private var backingValue = initialValue

	override fun getValue(thisRef: Any, property: KProperty<*>): T {
		graph.validate(flag)
		return backingValue
	}

	override fun setValue(thisRef: Any, property: KProperty<*>, value: T) {
		backingValue = value
	}
}