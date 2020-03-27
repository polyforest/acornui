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

@file:Suppress("UNUSED_ANONYMOUS_PARAMETER")

package com.acornui.component.style

import com.acornui.Disposable
import com.acornui.DisposedException
import com.acornui.assertionsEnabled
import com.acornui.collection.*
import com.acornui.function.as1
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

interface StylableRo {

	/**
	 * The current [StyleTag] objects added. This list must be unique.
	 *
	 * The curated tags list will be passed to the style entry filter
	 */
	val styleTags: List<StyleTag>

	/**
	 * A list of style rules that will be queried in determining calculated values for bound style objects.
	 */
	val styleRules: List<StyleRule<*>>

	fun <T : StyleRo> getRulesByType(type: StyleType<T>, out: MutableList<StyleRule<T>>) {
		out.clear()
		@Suppress("UNCHECKED_CAST")
		(styleRules as Iterable<StyleRule<T>>).filterTo(out, { it.style.type == type })
	}

	/**
	 * The next ancestor of this stylable component.
	 */
	val styleParent: StylableRo?

	fun invalidateStyles()
}

interface Stylable : StylableRo {

	/**
	 * The current [StyleTag] objects added. This list must be unique.
	 *
	 * The curated tags list will be passed to the style entry filter
	 */
	override val styleTags: MutableList<StyleTag>

	/**
	 * A list of style rules that will be queried in determining calculated values for bound style objects.
	 */
	override val styleRules: MutableList<StyleRule<*>>

}

fun Stylable.addStyleRule(style: StyleRo, tag: StyleTag, priority: Float = 0f) = addStyleRule(style, tag.filter, priority)

fun Stylable.addStyleRule(style: StyleRo, filter: StyleFilter = AlwaysFilter, priority: Float = 0f) {
	styleRules.add(StyleRule(style, filter, priority))
}

class Styles(private val host: Stylable) : Disposable {

	val styleTags = ActiveList<StyleTag>()
	val styleRules = ActiveList<StyleRule<*>>()

	private val entriesByType = HashMap<StyleType<*>, MutableList<StyleRule<*>>>()

	private val styleValidators = ArrayList<StyleValidator>()
	private val styleWatchers = ArrayList<StyleWatcher<*>>()

	private var isDisposed = false

	init {
		styleTags.addBinding(host::invalidateStyles)
		styleRules.added.add {
			index, entry ->
			add(entry)
		}
		styleRules.removed.add {
			index, entry ->
			remove(entry)
		}
		styleRules.changed.add {
			index, oldEntry, newEntry ->
			remove(oldEntry)
			add(newEntry)
		}
		styleRules.reset.add {
			for (list in entriesByType.values) {
				for (entry in list) {
					entry.style.changed.remove(::invalidateStyles.as1)
				}
			}
			entriesByType.clear()
			for (entry in styleRules) add(entry)
		}
	}

	private fun add(entry: StyleRule<*>) {
		entriesByType.getOrPut(entry.style.type) { ArrayList() }.add(entry)
		invalidateStyles()
		entry.style.changed.add(::invalidateStyles.as1)
	}

	private fun remove(entry: StyleRule<*>) {
		entriesByType[entry.style.type]?.remove(entry)
		invalidateStyles()
		entry.style.changed.remove(::invalidateStyles.as1)
	}

	private fun invalidateStyles() {
		host.invalidateStyles()
	}

	fun <T : StyleRo> getRulesByType(type: StyleType<T>, out: MutableList<StyleRule<T>>) {
		@Suppress("UNCHECKED_CAST")
		val entries = entriesByType[type] as List<StyleRule<T>>?
		out.clear()
		if (entries != null)
			out.addAll(entries)
	}

	fun <T : Style> bind(style: T, calculator: StyleCalculator = CascadingStyleCalculator): T {
		if (isDisposed) throw DisposedException()
		style.changed.add(::invalidateStyles.as1)
		styleValidators.add(StyleValidator(style, calculator))
		invalidateStyles()
		return style
	}

	fun unbind(style: StyleRo) {
		style.changed.remove(::invalidateStyles.as1)
		styleValidators.removeFirst { it.style === style }
	}

	fun <T : Style> watch(style: T, priority: Float, callback: (T) -> Unit) {
		if (isDisposed) return
		if (assertionsEnabled)
			check(styleValidators.firstOrNull2 { it.style === style } != null) { "A style object is being watched without being bound. Use `val yourStyle = bind(YourStyle())`." }
		val watcher = StyleWatcher(style, priority, callback)
		styleWatchers.addSorted(watcher)
	}

	fun unwatch(style: Style) {
		styleWatchers.removeFirst { it.style === style }
	}

	fun validateStyles() {
		for (i in 0..styleValidators.lastIndex) {
			styleValidators[i].validate(host)
		}
		for (i in 0..styleWatchers.lastIndex) {
			styleWatchers[i].check()
		}
	}

	override fun dispose() {
		if (isDisposed) throw DisposedException()
		isDisposed = true
		for (i in 0..styleValidators.lastIndex) {
			styleValidators[i].style.changed.remove(::invalidateStyles.as1)
		}
		styleWatchers.clear()
		styleValidators.clear()
		styleTags.dispose()
		styleRules.dispose()
	}
}

/**
 * Holds a style object with explicit values that will be applied to the calculated values of the target style object
 * when the filter passes.
 * Entries will be applied in the order from deepest child to highest ancestor (the stage).
 */
data class StyleRule<out T : StyleRo>(

		/**
		 * The explicit values on this style object will be applied to the target if the filter passes.
		 * If this style object changes, the styles object will dispatch a changed signal.
		 */
		val style: T,

		/**
		 * A filter responsible for determining whether or not this rule should be applied.
		 */
		val filter: StyleFilter = AlwaysFilter,

		/**
		 * A higher priority value will be applied before entries with a lower priority.
		 * Equivalent priorities will go to the entry deeper in the display hierarchy, and then to the order this entry
		 * was added.
		 */
		val priority: Float = 0f
)

interface StyleType<out T : StyleRo> {

	val extends: StyleType<*>?
		get() = null
}

inline fun StyleType<*>.walkInheritance(callback: (StyleType<*>) -> Unit) {
	var p: StyleType<*>? = this
	while (p != null) {
		callback(p)
		p = p.extends
	}
}

/**
 * Style tags are markers placed on the display hierarchy that are used for filtering which styles from [StyleRule]
 * objects are applied.
 */
interface StyleTag

/**
 * Constructs a new StyleTag object.
 */
fun styleTag(): StyleTag = object : StyleTag {}

inline fun StylableRo.walkStylableAncestry(callback: (StylableRo) -> Unit) {
	findStylableAncestor {
		callback(it)
		false
	}
}

inline fun StylableRo.findStylableAncestor(filter: StyleFilter): StylableRo? {
	var p: StylableRo? = this
	while (p != null) {
		val found = filter(p)
		if (found) return p
		p = p.styleParent
	}
	return null
}

class StyleTagToggle(private val styleTag: StyleTag) : ReadWriteProperty<Stylable, Boolean> {

	override fun getValue(thisRef: Stylable, property: KProperty<*>): Boolean {
		return thisRef.styleTags.contains(styleTag)
	}

	override fun setValue(thisRef: Stylable, property: KProperty<*>, value: Boolean) {
		if (getValue(thisRef, property) != value) {
			if (value) thisRef.styleTags.add(styleTag)
			else thisRef.styleTags.remove(styleTag)
		}
	}
}
