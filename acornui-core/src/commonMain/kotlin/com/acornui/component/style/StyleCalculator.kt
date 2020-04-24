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
	fun calculate(target: StylableRo, out: Style, debugInfo: StyleDebugInfo? = null): Boolean
}

object CascadingStyleCalculator : StyleCalculator {

	private val entries = ArrayList<StyleRo>()
	private val calculated = stringMapOf<Any?>()
	private val tmp = ArrayList<StyleRo>()
	private val tmpCalculated = ArrayList<Boolean>()

	private val entrySortComparator = { o1: StyleRo, o2: StyleRo ->
		-o1.priority.compareTo(o2.priority) // Higher priority values come first.
	}

	override fun calculate(target: StylableRo, out: Style, debugInfo: StyleDebugInfo?): Boolean {
		// Collect all style rule objects for the bound style type and tags.
		// These entries will be sorted first by priority, and then by ancestry level.
		target.walkStylableAncestry { ancestor ->
			out.type.walkInheritance { styleType ->
				ancestor.getRulesByType(styleType, tmp)
				tmp.forEachReversed { entry ->
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
		entries.forEach { entry: StyleRo ->
			for (i in 0..entry.allProps.lastIndex) {
				val prop = entry.allProps[i]
				if (prop.explicitIsSet) {
					val foundIndex = out.allProps.indexOfFirst { it.name == prop.name }
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


}

class StyleDebugInfo()