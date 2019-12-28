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
import com.acornui.math.MathUtils
import com.acornui.signal.Signal
import com.acornui.string.toRadix

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

}

private class ValidationNode(

		/**
		 * This node's flag.
		 */
		val flag: Int,

		/**
		 * The flattened dependents of this node.
		 * When this node's flag is invalidated, these flags will also be invalidated.
		 */
		var dependents: Int,

		/**
		 * The flattened dependencies of this node.
		 * When flags are being validated, if any of them are in this mask, this node will also be validated.
		 */
		var dependencies: Int,

		val onValidate: () -> Unit
) {

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

	private val nodes = ArrayList<ValidationNode>()
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
	 * Returns true if this validation graph is currently validating via [validate].
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

	fun addNode(flag: Int, dependencies: Int, dependents: Int, onValidate: () -> Unit) = addNode(flag, dependencies, dependents, true, onValidate)

	/**
	 * Appends a validation node.
	 * @param flag The target validation bit flag.  This must be a power of two.  UiComponent reserves flags
	 * `1` through `1 shl 15` so any custom flags for components should start at `1 shl 16`.
	 *
	 * @param dependencies If any of these dependencies become invalid, this node will also become invalid.
	 * This can be multiple flags by using bitwise OR.  E.g. `ValidationFlags.LAYOUT or ValidationFlags.TRANSFORM`.
	 *
	 * @param dependents If [flag] becomes invalid, all of its dependents will also become invalid.
	 * This can be multiple flags by using bitwise OR.  E.g. `ValidationFlags.LAYOUT or ValidationFlags.TRANSFORM`.
	 *
	 * @param checkAllFound If true, all dependencies and dependents provided must exist in this graph. If false,
	 * future nodes may be listed. For example, if you have a node to add that you wish to invalidate when anything
	 * else is invalidated, you can use `addNode(1 shl 16, dependencies = -1, dependents = 0, checkAllFound = false) {}`
	 */
	fun addNode(flag: Int, dependencies: Int, dependents: Int, checkAllFound: Boolean, onValidate: () -> Unit) {
		if (assertionsEnabled) require(MathUtils.isPowerOfTwo(flag)) { "flag ${flag.toRadix(2)} is not a power of 2." }
		// When this node is validated, we should validate dependencies first.
		// When this node is invalidated, we should also invalidated dependents.
		val newNode = ValidationNode(flag, dependents or flag, dependencies or flag, onValidate)
		var dependenciesNotFound = dependencies
		var dependentsNotFound = dependents
		var insertIndex = nodes.size
		for (i in 0..nodes.lastIndex) {
			val previousNode = nodes[i]
			if (previousNode.flag == flag)
				throw Exception("flag ${flag.toFlagString()} already exists.")
			if (checkAllFound) {
				val flagInv = previousNode.flag.inv()
				dependenciesNotFound = dependenciesNotFound and flagInv
				dependentsNotFound = dependentsNotFound and flagInv
			}
			if (previousNode.dependencies and newNode.dependents > 0) {
				// The existing node is a dependent of the added node.
				previousNode.addDependencies(newNode.dependencies)
				newNode.addDependents(previousNode.dependents)

				// Thew new node must come before the existing node.
				if (insertIndex > i)
					insertIndex = i
			}
			if (previousNode.dependents and newNode.dependencies > 0) {
				// The existing node is a dependency of the added node.
				newNode.addDependencies(previousNode.dependencies)
				previousNode.addDependents(newNode.dependents)

				// Do not allow cyclical dependencies:
				check(insertIndex > i) {
					"Validation node cannot be added after dependency ${previousNode.flag.toFlagString()} and before all dependents ${dependents.toFlagsString()}"
				}
			}
		}
		nodes.add(insertIndex, newNode)
		nodesMap[newNode.flag] = newNode
		_invalidFlags = _invalidFlags or newNode.flag
		_allFlags = _allFlags or newNode.flag
		if (checkAllFound) {
			check(dependentsNotFound == 0) {
				"validation node added, but the dependent flags: ${dependentsNotFound.toFlagsString()} were not found."
			}
			check(dependenciesNotFound == 0) {
				"validation node added, but the dependency flags: ${dependenciesNotFound.toFlagsString()} were not found."
			}
		}
	}

	/**
	 * Invalidates the given flags.
	 * @return Returns a bitmask of all the flags invalidated. (This will consider the dependencies for all nodes
	 * within the [flags] argument.
	 */
	fun invalidate(flags: Int = -1): Int {
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