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

import com.acornui.collection.copy
import com.acornui.collection.fill
import com.acornui.collection.forEachReversed
import com.acornui.collection.sortedInsertionIndex
import com.acornui.component.UiComponentRo
import com.acornui.component.style.CascadingStyleCalculator.calculate

/**
 * Responsible for setting the calculated values on a [Style] object.
 *
 * See [calculate] for documentation on style precedence rules.
 */
object CascadingStyleCalculator {

	private val rules = ArrayList<StyleRo>()
	private val calculated = ArrayList<Int>()
	private val ruleHosts = ArrayList<StylableRo>()

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
	 * The [StyleRo.filter] must return true for the given [host].
	 *
	 * The applied style rules will be ordered as follows:
	 *
	 * - First by explicit priority value (higher values first).
	 * - Second by ancestry level (child before parent).
	 * - Third by type covariance (style sub-types before super-types). See [StyleType.extends].
	 * - Finally by rule index (higher indices first).
	 *
	 * @param host The [StylableRo] object to use for collecting cascading style rules.
	 * @param out The mutable [Style] object on which to populate calculated values.
	 * @param debugInfo If provided, an object to be populated with debug information for determining where each style
	 * property came from.
	 */
	fun calculate(host: StylableRo, out: Style, debugInfo: StyleDebugInfo? = null): Boolean {

		// Collect all style rules in the order they should be applied.
		rules.add(out)
		if (debugInfo != null) ruleHosts.add(host)
		host.walkStylableAncestry { ancestor ->
			out.type.walkInheritance { styleType ->
				ancestor.styleRules.forEachReversed { rule ->
					if (rule.type == styleType && rule.filter(host)) {
						val index = rules.sortedInsertionIndex(rule, comparator = entrySortComparator)
						rules.add(index, rule)
						if (debugInfo != null)
							ruleHosts.add(index, ancestor)
					}
				}
			}
		}

		// The number of properties not yet calculated.
		var remaining = out.allProps.size
		calculated.fill(out.allProps.size) { -1 }

		// Apply style entries to the calculated values of the bound style.
		var hasChanged = false

		rulesLoop@
		for (ruleIndex in 0..rules.lastIndex) {
			val rule = rules[ruleIndex]
			for (propIndex in 0..rule.allProps.lastIndex) {
				val prop = rule.allProps[propIndex]
				if (prop.explicitIsSet) {
					val foundIndex = out.allProps.indexOfFirst { it.name == prop.name }
					val found = out.allProps[foundIndex]
					if (calculated[foundIndex] == -1) {
						calculated[foundIndex] = ruleIndex
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
			if (calculated[i] == -1) {
				out.allProps[i].clearCalculated()
				hasChanged = true
			}
		}

		if (debugInfo != null) {
			debugInfo.host = host
			debugInfo.style = out
			debugInfo.rulesApplied = rules.copy()
			debugInfo.ruleHosts = ruleHosts.copy()
			debugInfo.propertyToRule = calculated.copy()
			ruleHosts.clear()
		}

		calculated.clear()
		rules.clear()
		if (hasChanged) 
			out.modTag.increment()
		return hasChanged
	}


}


class StyleDebugInfo(

		/**
		 * The host Stylable object used for collecting cascading style rules.
		 */
		var host: StylableRo? = null,

		/**
		 * The style for which rules were calculated.
		 */
		var style: StyleRo? = null,

		/**
		 * The list of rules that matched, in descending priority. This will include the target style itself.
		 */
		var rulesApplied: List<StyleRo> = emptyList(),

		/**
		 * For each rule in [rulesApplied], this is the host from which the rule came.
		 */
		var ruleHosts: List<StylableRo> = emptyList(),

		/**
		 * For each property in `style.allProps`, the index of the rule that supplied the explicit value.
		 * This will be of size: `style.allProps.size`
		 * Each value will correspond to an index in [rulesApplied].
		 */
		var propertyToRule: List<Int> = emptyList()
) {

	fun toDebugString(): String {
		val style = style ?: return "Empty Debug Info"
		val host = host!!

		var str = "-------------- Style ${style::class.simpleName} on $host ---------------- \n\n"
		for ((ruleIndex, rule) in rulesApplied.withIndex()) {
			val ruleHost = ruleHosts[ruleIndex]
			val ruleHostName = if (ruleHost == host) "self" else if (ruleHost is UiComponentRo) ruleHost.componentId else ruleHost.toString()
			str += "${rule.name ?: rule.toString()} from $ruleHostName priority(${rule.priority}) {\n"
			for (i in 0..propertyToRule.lastIndex) {
				if (propertyToRule[i] == ruleIndex) {
					val prop = style.allProps[i]
					val pStr = if (prop.calculatedValue is String) "\"${prop.calculatedValue}\"" else "${prop.calculatedValue}"
					str += "\t${prop.name} = $pStr\n"
				}
			}
			str += "}\n"
		}
		return str
	}
}