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
import com.acornui.reflect.observable
import com.acornui.signal.Signal
import com.acornui.string.toRadix
import kotlin.properties.ReadWriteProperty

/**
 * A component that has an invalidate/validate cycle.
 *
 * @author nbilyk
 */
interface Validatable {

	/**
	 * Dispatched when this component has been invalidated.
	 */
	val invalidated: Signal<(Validatable, Int) -> Unit>

	/**
	 * The currently invalid flags.
	 */
	val invalidFlags: Int

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
	 * Validates the specified flags for this component.
	 *
	 * @param flags A bit mask for which flags to validate. (Use -1 to validate all)
	 * Example: validate(ValidationFlags.LAYOUT or ValidationFlags.PROPERTIES) to validate both layout an properties.
	 */
	fun validate(flags: Int = -1)

}

private class ValidationNode(

		/**
		 * This node's flag.
		 */
		val flag: Int,

		/**
		 * When this node's flag is invalidated, the flags defined in invalidationMask will also be invalidated.
		 */
		var invalidationMask: Int,

		/**
		 * When flags is being validated, if any of them are in this mask, this node will also be validated.
		 */
		var validationMask: Int,

		val onValidate: () -> Unit
) {

	var isValid: Boolean = false

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
class ValidationGraph {

	private val nodes = ArrayList<ValidationNode>()

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
	 * @param dependencies If any of these dependencies become invalid, this node will also become invalid.
	 */
	fun addNode(flag: Int, dependencies: Int, dependents: Int, onValidate: () -> Unit) {
		if (assertionsEnabled && !MathUtils.isPowerOfTwo(flag)) throw IllegalArgumentException("flag ${flag.toRadix(2)} is not a power of 2.")
		val newNode = ValidationNode(flag, dependents or flag, dependencies or flag, onValidate)
		var dependenciesNotFound = dependencies
		var dependentsNotFound = dependents
		var insertIndex = nodes.size
		for (i in 0..nodes.lastIndex) {
			val previousNode = nodes[i]
			if (previousNode.flag == flag) throw Exception("flag $flag already exists.")
			val flagInv = previousNode.flag.inv()
			dependenciesNotFound = dependenciesNotFound and flagInv
			dependentsNotFound = dependentsNotFound and flagInv
			if (previousNode.validationMask and newNode.invalidationMask > 0) {
				previousNode.validationMask = newNode.validationMask or previousNode.validationMask
				newNode.invalidationMask = newNode.invalidationMask or previousNode.invalidationMask
				if (insertIndex > i)
					insertIndex = i
			}
			if (previousNode.invalidationMask and newNode.validationMask > 0) {
				newNode.validationMask = newNode.validationMask or previousNode.validationMask
				previousNode.invalidationMask = newNode.invalidationMask or previousNode.invalidationMask
				if (insertIndex <= i) {
					throw Exception("Validation node cannot be added after dependency ${previousNode.flag.toFlagString()} and before all dependents ${dependents.toFlagsString()}")
				}
			}
		}
		nodes.add(insertIndex, newNode)
		_invalidFlags = _invalidFlags or newNode.flag
		_allFlags = _allFlags or newNode.flag
		if (dependentsNotFound != 0)
			throw Exception("validation node added, but the dependent flags: ${dependentsNotFound.toFlagsString()} were not found.")
		if (dependenciesNotFound != 0)
			throw Exception("validation node added, but the dependency flags: ${dependenciesNotFound.toFlagsString()} were not found.")
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
			val badFlags = flagsToInvalidate and (currentNode.validationMask and currentNode.flag.inv())
			if (badFlags > 0) {
				throw Exception("Cannot invalidate ${flags.toFlagsString()} while validating ${currentNode.flag.toFlagString()}; The following invalidated flags are dependencies of the current node: ${badFlags.toFlagsString()}")
			}
		}
		for (i in 0..nodes.lastIndex) {
			val n = nodes[i]
			if (!n.isValid) continue
			if (flagsToInvalidate and n.flag > 0) {
				n.isValid = false
				flagsToInvalidate = flagsToInvalidate or n.invalidationMask
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
				val badFlags = (node.invalidationMask and node.flag.inv()) and flags
				if (badFlags > 0)
					throw IllegalStateException("Cannot validate ${badFlags.toFlagsString()} while validating ${node.flag.toFlagString()}")
			}
			return 0
		}
		var flagsToValidate = flags and _invalidFlags
		if (flagsToValidate == 0) return 0
		var flagsValidated = 0
		for (i in 0..nodes.lastIndex) {
			currentIndex = i
			val n = nodes[i]
			if (!n.isValid && flagsToValidate and n.invalidationMask > 0) {
				n.onValidate()
				n.isValid = true
				flagsToValidate = flagsToValidate or n.validationMask
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
}

/**
 * Using the ValidationFlags list, prints out a comma separated list of the flags this bit mask contains.
 * For non-reserved flags (flags at least 1 shl 16), the power of two will be printed.
 * @see ValidationFlags
 * @see ValidationFlags.flagToString
 */
fun Int.toFlagsString(): String {
	var str = ""
	for (i in 0..31) {
		val flag = 1 shl i
		if (flag and this > 0) {
			if (str.isNotEmpty()) str += ","
			str += ValidationFlags.flagToString(flag)
		}
	}
	return str
}

@Suppress("NOTHING_TO_INLINE")
inline fun Int.containsFlag(flag: Int): Boolean {
	return this and flag != 0
}

fun Int.toFlagString(): String = ValidationFlags.flagToString(this)

fun validationGraph(init: ValidationGraph.() -> Unit): ValidationGraph {
	val v = ValidationGraph()
	v.init()
	return v
}

fun <T> Validatable.validationProp(initialValue: T, flags: Int): ReadWriteProperty<Any, T> {
	return observable(initialValue) {
		invalidate(flags)
	}
}
