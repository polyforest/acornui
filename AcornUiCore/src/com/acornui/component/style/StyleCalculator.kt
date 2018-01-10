package com.acornui.component.style

import com.acornui.collection.addSorted
import com.acornui.collection.forEach2
import com.acornui.collection.forEachReversed2
import com.acornui.collection.sortedInsertionIndex

/**
 * A style calculator is responsible for setting the calculated values
 */
interface StyleCalculator {
	fun calculate(style: Style, target: StyleableRo)
}

object CascadingStyleCalculator : StyleCalculator {

	private val entries = ArrayList<StyleRule<*>>()
	private val calculated = HashMap<String, Any?>()
	private val tmp = ArrayList<StyleRule<*>>()

	private val entrySortComparator = {
		o1: StyleRule<*>, o2: StyleRule<*> ->
		-o1.priority.compareTo(o2.priority) // Higher priority values come first.
	}

	override fun calculate(style: Style, target: StyleableRo) {
		// Collect all style rule objects for the bound style type and tags.
		// These entries will be sorted first by priority, and then by ancestry level.
		target.walkStyleableAncestry {
			ancestor ->
			style.type.walkInheritance {
				styleType ->
				ancestor.getRulesByType(styleType, tmp)
				tmp.forEachReversed2 {
					entry ->
					if (entry.filter(target) != null) {
						entries.addSorted(entry, comparator = entrySortComparator)
					}
				}
			}
		}

		// Apply style entries to the calculated values of the bound style.
		entries.forEach2 {
			for (e in it.style.explicit) {
				if (!calculated.containsKey(e.key)) {
					calculated[e.key] = e.value
				}
			}
		}
		if (calculated != style.calculated) {
			style.calculated.clear()
			style.calculated.putAll(calculated)
			style.modTag.increment()
		}
		calculated.clear()
		entries.clear()
	}

	fun getDebugInfo(style: Style, target: StyleableRo): List<StyleRuleDebugInfo> {
		// Collect all style rule objects for the bound style type and tags.
		// These entries will be sorted first by priority, and then by ancestry level.
		val appliedRules = ArrayList<StyleRuleDebugInfo>()
		target.walkStyleableAncestry {
			ancestor ->
			style.type.walkInheritance {
				styleType ->
				ancestor.getRulesByType(styleType, tmp)
				tmp.forEachReversed2 {
					entry ->
					if (entry.filter(target) != null) {
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
			for (e in entry.style.explicit) {
				if (!calculated.containsKey(e.key)) {
					calculated[e.key] = e.value
					ruleInfo.calculated[e.key] = e.value
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
		val ancestor: StyleableRo,
		val entry: StyleRule<*>) {

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