/*
 * Copyright 2019 Poly Forest, LLC
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

package com.acornui.component

import com.acornui.assertionsEnabled
import com.acornui.collection.removeFirst
import com.acornui.math.MathUtils
import com.acornui.properties.afterChange
import com.acornui.signal.Signal
import com.acornui.string.toRadix
import kotlin.properties.ReadOnlyProperty
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

/**
 * A component that has an invalidate/validate cycle.
 *
 * @author nbilyk
 */
interface Validatable {

	/**
	 * Dispatched when this component has been invalidated.
	 */
	val invalidated: Signal<(Validatable, flags: Int) -> Unit>

	/**
	 * The currently invalid flags.
	 */
	val invalidFlags: Int

	/**
	 * True if this component is currently validating.
	 */
	val isValidating: Boolean

	/**
	 * Invalidates the given flag.
	 * Returns a bit mask representing the flags newly invalidated.
	 * Dispatches [invalidated] with the newly invalidated flags.
	 *
	 * @param flags The bit flags to invalidate. Use an `or` operation to invalidate multiple flags at once.
	 * @see [ValidationFlags]
	 */
	fun invalidate(flags: Int): Int

	/**
	 * Validates the specified flags for this component during a validation cycle.
	 *
	 * @param flags A bit mask for which flags to validate. (Use -1 to validate all)
	 * Example: validate(ValidationFlags.LAYOUT or ValidationFlags.PROPERTIES) to validate both layout an properties.
	 *
	 * @return Returns the flags actually validated.
	 */
	fun validate(flags: Int = -1): Int

	/**
	 * Returns true if the given flag is currently valid.
	 */
	fun isValid(flag: Int): Boolean {
		return invalidFlags and flag == 0
	}

	/**
	 * Returns the number of times the given flag has been validated.
	 */
	fun getValidatedCount(flag: Int): Int

}

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

	fun addNode(flag: Int, onValidate: () -> Unit) = addNode(flag, 0, 0, onValidate)

	fun addNode(flag: Int, dependencies: Int, onValidate: () -> Unit) = addNode(flag, dependencies, 0, onValidate)

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
		require(MathUtils.isPowerOfTwo(flag)) { "flag ${flag.toRadix(2)} is not a power of 2." }
		requireFlagsExist(dependencies or dependents)

		// When this node is validated, we should validate dependencies first.
		// When this node is invalidated, we should also invalidated dependents.
		val newNode = ValidationNode(flag, dependents, dependencies, onValidate)
		nodes.add(newNode)
		isNodeListValid = false
		_invalidFlags = _invalidFlags or flag
		_allFlags = _allFlags or flag
	}

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
		var str = ""
		for (i in 0..31) {
			val flag = 1 shl i
			if (flag and this > 0) {
				if (str.isNotEmpty()) str += ","
				str += flag.toFlagString()
			}
		}
		return str
	}

	private fun requireFlagsExist(flags: Int) {
		val missingFlags = _allFlags.inv()
		val missingRequiredFlags = flags and missingFlags
		require(missingRequiredFlags == 0) { "Required flags not found: ${missingRequiredFlags.toFlagsString()}" }
	}
}

/**
 * Returns true if this bit mask contains the given binary bit mask.
 * Example (for brevity consider 'b' to be a binary literal):
 * ```
 * 0101b.containsFlag(100b) // true
 * 0101b.containsFlag(10b)  // false
 * 0101b.containsFlag(101b) // true
 * 0101b.containsFlag(111b) // false
 * ```
 */
@Suppress("NOTHING_TO_INLINE")
inline infix fun Int.containsFlag(flag: Int): Boolean {
	return this and flag == flag
}

fun validationGraph(toFlagString: Int.() -> String = ValidationFlags::flagToString, init: ValidationGraph.() -> Unit): ValidationGraph {
	val v = ValidationGraph(toFlagString)
	v.init()
	return v
}

/**
 * Returns a property delegate where when set invalidates the given flag.
 */
fun <T> validationProp(initialValue: T, flags: Int): ReadWriteProperty<Validatable, T> = validationProp(initialValue, flags, { it })

/**
 * Returns a property delegate where when set, copies the set value and invalidates the given flag.
 */
fun <T> validationProp(initialValue: T, flags: Int, copy: (T) -> T): ReadWriteProperty<Validatable, T> {
	return object : ReadWriteProperty<Validatable, T> {
		private var backingValue: T = copy(initialValue)
		override fun getValue(thisRef: Validatable, property: KProperty<*>): T = backingValue
		override fun setValue(thisRef: Validatable, property: KProperty<*>, value: T) {
			if (value == backingValue) return // no-op
			backingValue = copy(value)
			thisRef.invalidate(flags)
		}
	}
}

/**
 * Returns a read-only property delegate that caches a calculated result.
 *
 * The calculated result will be recalculated when this property is read and the given flag has validated since the
 * last read.
 */
fun <T> validationProp(flag: Int, getter: () -> T): ReadOnlyProperty<Validatable, T> = ValidationProperty(flag, getter)

private class ValidationProperty<T>(private val flag: Int, private val calculator: () -> T) : ReadOnlyProperty<Validatable, T> {

	private var lastValidatedCount = -1
	private var cached: T? = null

	override fun getValue(thisRef: Validatable, property: KProperty<*>): T {
		thisRef.validate(flag)
		val c = thisRef.getValidatedCount(flag)
		if (lastValidatedCount != c) {
			cached = calculator()
			lastValidatedCount = c
		}
		@Suppress("UNCHECKED_CAST")
		return cached as T
	}
}