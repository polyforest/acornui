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
 * A style calculator is responsible for setting the calculated values
 */
interface StyleCalculator {

	/**
	 * Sets the calculated values for the given style.
	 * 
	 * @return Returns true if the calculated values have changed.
	 */
	fun calculate(target: StylableRo, out: Style): Boolean
}

object CascadingStyleCalculator : StyleCalculator {

	private val entries = ArrayList<StyleRo>()
	private val calculated = stringMapOf<Any?>()
	private val tmp = ArrayList<StyleRo>()
	private val tmpCalculated = ArrayList<Boolean>()

	private val entrySortComparator = { o1: StyleRo, o2: StyleRo ->
		-o1.priority.compareTo(o2.priority) // Higher priority values come first.
	}

	override fun calculate(target: StylableRo, out: Style): Boolean {
		// Collect all style rule objects for the bound style type and tags.
		// These entries will be sorted first by priority, and then by ancestry level.
		target.walkStylableAncestry { ancestor ->
			out.type.walkInheritance { styleType ->
				ancestor.getRulesByType(styleType, tmp)
				tmp.forEachReversed2 { entry ->
					if (entry.filter(target)) {
						entries.addSorted(entry, comparator = entrySortComparator)
					}
				}
			}
		}

		for (i in 0..out.allProps.lastIndex) {
			tmpCalculated.add(out.allProps[i].explicitIsSet)
		}

		// Apply style entries to the calculated values of the bound style.
		var hasChanged = false
		entries.forEach2 { entry ->
			for (i in 0..entry.allProps.lastIndex) {
				val prop = entry.allProps[i]
				if (prop.explicitIsSet) {
					val foundIndex = out.allProps.indexOfFirst2 { it.name == prop.name }
					val found = out.allProps[foundIndex]
					if (!tmpCalculated[foundIndex]) {
						tmpCalculated[foundIndex] = true
						val v = prop.explicitValue
						if (!found.calculatedIsSet || found.calculatedValue != v) {
							found.calculatedValue = v
							hasChanged = true
						}
					}
				}
			}
		}
		for (i in 0..out.allProps.lastIndex) {
			if (!tmpCalculated[i]) {
				out.allProps[i].clearCalculated()
				hasChanged = true
			}
		}
		tmpCalculated.clear()
		calculated.clear()
		entries.clear()
		if (hasChanged) 
			out.modTag.increment()
		return hasChanged
	}

	fun getDebugInfo(style: Style, target: StylableRo): List<StyleRuleDebugInfo> {
		// Collect all style rule objects for the bound style type and tags.
		// These entries will be sorted first by priority, and then by ancestry level.
		val appliedRules = ArrayList<StyleRuleDebugInfo>()
		target.walkStylableAncestry { ancestor ->
			style.type.walkInheritance { styleType ->
				ancestor.getRulesByType(styleType, tmp)
				tmp.forEachReversed2 { entry ->
					if (entry.filter(target)) {
						val index = entries.sortedInsertionIndex(entry, comparator = entrySortComparator)
						entries.add(index, entry)
						appliedRules.add(index, StyleRuleDebugInfo(ancestor, entry))
					}
				}
			}
		}

		// Apply style entries to the calculated values of the bound style.
		for (i in 0..entries.lastIndex) {
			val entry = entries[i]
			val ruleInfo = appliedRules[i]
			for (j in 0..entry.allProps.lastIndex) {
				val prop = entry.allProps[j]
				if (prop.explicitIsSet) {
					val found = style.allProps.first2 { it.name == prop.name }
					if (found.calculatedIsSet) {
						found.calculatedValue = prop.explicitValue
						ruleInfo.calculated[prop.name!!] = prop.explicitValue
					}
				}
			}
		}

		calculated.clear()
		entries.clear()
		return appliedRules
	}
}

fun List<StyleRuleDebugInfo>.prettyPrint(): String {
	var str = ""
	for (styleRuleDebugInfo in this) {
		if (str.isNotEmpty()) str += ", \n"
		str += styleRuleDebugInfo.prettyPrint()
	}
	return str
}

class StyleRuleDebugInfo(
		val ancestor: StylableRo,
		val entry: StyleRo) {

	val calculated: MutableMap<String, Any?> = HashMap()

	fun prettyPrint(): String {
		var str = "$ancestor ${entry.filter} ${entry.priority} {\n"
		for ((key, value) in calculated) {
			str += "	$key = $value\n"
		}
		str += "}"
		return str
	}
}
