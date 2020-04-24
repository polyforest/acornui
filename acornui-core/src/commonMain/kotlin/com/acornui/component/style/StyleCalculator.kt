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

package com.acornui.component.style

import com.acornui.collection.*

/**
 * Responsible for setting the calculated values on a [Style] object.
 *
 * See [calculate] for documentation on style precedence rules.
 */
object CascadingStyleCalculator {

	private val rules = ArrayList<StyleRo>()
	private val calculated = ArrayList<Boolean>()

	private val entrySortComparator = { o1: StyleRo, o2: StyleRo ->
		-o1.priority.compareTo(o2.priority) // Higher priority values come first.
	}

	/**
	 * Populates the calculated values for a style object.
	 *
	 * The calculated values will be based on the [StyleRo] objects within the [StylableRo.styleRules] lists up the
	 * [StylableRo.styleParent] ancestry.
	 *
	 * The applied style rules will be filtered as follows:
	 *
	 * The [StyleRo.type] must match the type or a supertype of [out].
	 * The [StyleRo.filter] must return true for the given [target].
	 *
	 * The applied style rules will be ordered as follows:
	 *
	 * - First by explicit priority value (higher values first).
	 * - Second by ancestry level (child before parent).
	 * - Third by type covariance (style sub-types before super-types). See [StyleType.extends].
	 * - Finally by rule index (higher indices first).
	 *
	 * @param target The [StylableRo] object to use for collecting cascading style rules.
	 * @param out The mutable [Style] object on which to populate calculated values.
	 */
	fun calculate(target: StylableRo, out: Style, debugInfo: StyleDebugInfo? = null): Boolean {

		// Collect all style rules in the order they should be applied.
		target.walkStylableAncestry { ancestor ->
			out.type.walkInheritance { styleType ->
				ancestor.styleRules.forEachReversed { rule ->
					if (rule.type == styleType && rule.filter(target)) {
						rules.addSorted(rule, comparator = entrySortComparator)
					}
				}
			}
		}

		// The number of properties not yet calculated.
		var remaining = out.allProps.size

		// Initialize a list of booleans where true indicates that a calculated value has been found.
		for (i in 0..out.allProps.lastIndex) {
			val v = out.allProps[i].explicitIsSet
			if (v) remaining--
			calculated.add(v)
		}

		// Apply style entries to the calculated values of the bound style.
		var hasChanged = false

		rulesLoop@
		for (ruleIndex in 0..rules.lastIndex) {
			val entry = rules[ruleIndex]
			for (propIndex in 0..entry.allProps.lastIndex) {
				val prop = entry.allProps[propIndex]
				if (prop.explicitIsSet) {
					val foundIndex = out.allProps.indexOfFirst { it.name == prop.name }
					val found = out.allProps[foundIndex]
					if (!calculated[foundIndex]) {
						calculated[foundIndex] = true
						remaining--
						val v = prop.explicitValue
						if (!found.calculatedIsSet || found.calculatedValue != v) {
							found.calculatedValue = v
							hasChanged = true
						}
						if (remaining == 0) break@rulesLoop
					}
				}
			}
		}
		for (i in 0..calculated.lastIndex) {
			if (!calculated[i]) {
				out.allProps[i].clearCalculated()
				hasChanged = true
			}
		}
		calculated.clear()
		rules.clear()
		if (hasChanged) 
			out.modTag.increment()
		return hasChanged
	}


}

class StyleDebugInfo(
//		val rulesApplied: List<>
)