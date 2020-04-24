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

import com.acornui.*
import com.acornui.collection.*
import com.acornui.function.as1
import com.acornui.signal.Signal
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
	val styleRules: List<StyleRo>

	/**
	 * The stylable component parent from which style rules are inherited.
	 */
	val styleParent: StylableRo?

	val stylesInvalidated: Signal<(StylableRo)->Unit>

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
	override val styleRules: MutableList<StyleRo>

}

fun Stylable.addStyleRule(style: Style, tag: StyleTag, priority: Float = 0f) = addStyleRule(style, tag.filter, priority)

fun Stylable.addStyleRule(style: Style, filter: StyleFilter = AlwaysFilter, priority: Float = 0f) {
	style.filter = filter
	style.priority = priority
	styleRules.add(style)
}

class Styles(private val host: Stylable) : Disposable {

	val styleTags = ActiveList<StyleTag>()
	val styleRules = WatchedElementsActiveList<StyleRo>()

	/**
	 * A list of styles to validate.
	 */
	private val styles = ArrayList<Style>()
	private val styleWatchers = ArrayList<StyleWatcher<*>>()

	private var isDisposed = false

	init {
		styleTags.addBinding(host::invalidateStyles)
		styleRules.addBinding(host::invalidateStyles)
	}

	private fun invalidateStyles() {
		host.invalidateStyles()
	}

	/**
	 * Binds a style to have its calculated values set on [validateStyles].
	 */
	fun <T : Style> bind(style: T): T {
		if (isDisposed) throw DisposedException()
		style.changed.add(::invalidateStyles.as1)
		styles.add(style)
		invalidateStyles()
		return style
	}

	/**
	 * Removes a style object from calculation.
	 */
	fun <T : Style> unbind(style: T): T {
		style.changed.remove(::invalidateStyles.as1)
		styles.remove(style)
		return style
	}

	/**
	 * When [validateStyles] is called, any watched style objects whose calculated values have changed will have
	 * the given callback invoked.
	 */
	fun <T : Style> watch(style: T, callback: (T) -> Unit): ManagedDisposable {
		if (isDisposed) throw DisposedException()
		if (assertionsEnabled)
			check(styles.contains(style)) { "A style object is being watched without being bound. Use `val yourStyle = bind(YourStyle())`." }
		val styleWatcher = StyleWatcher(style, callback)
		styleWatchers.add(styleWatcher)
		return {
			styleWatchers.remove(styleWatcher)
		}.toManagedDisposable()
	}

	/**
	 * Sets calculated values on all bound style objects.
	 * Notifies each style watcher if the style's calculated values has changed.
	 */
	fun validateStyles() {
		for (i in 0..styles.lastIndex) {
			CascadingStyleCalculator.calculate(host, styles[i])
		}
		styleWatchers.forEach(action = StyleWatcher<*>::check)
	}

	override fun dispose() {
		if (isDisposed) throw DisposedException()
		isDisposed = true
		for (i in 0..styles.lastIndex) {
			styles[i].changed.remove(::invalidateStyles.as1)
		}
		styleWatchers.clear()
		styles.clear()
		styleTags.dispose()
		styleRules.dispose()
	}
}

interface StyleType<out T : StyleRo> {

	/**
	 * If set, style rules of this supertype will be applied.
	 */
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
