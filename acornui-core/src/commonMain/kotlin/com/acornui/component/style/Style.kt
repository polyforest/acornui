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

import com.acornui.Disposable
import com.acornui.collection.AlwaysFilter
import com.acornui.collection.first2
import com.acornui.component.UiComponent
import com.acornui.component.layout.spacer
import com.acornui.di.Context
import com.acornui.function.as1
import com.acornui.observe.*
import com.acornui.properties.afterChange
import com.acornui.recycle.Clearable
import com.acornui.serialization.Writer
import com.acornui.signal.Signal
import com.acornui.signal.Signal1
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

/**
 * A Style object contains maps of style properties.
 */
interface StyleRo : Observable {

	override val changed: Signal<(StyleRo) -> Unit>

	val type: StyleType<*>

	/**
	 * The style properties, as calculated by a [StyleCalculator].
	 */
	val allProps: List<StyleProp<*>>

	/**
	 * When either the explicit or calculated values change, the mod tag is incremented.
	 */
	val modTag: ModTagRo

	/**
	 * A filter responsible for determining whether or not this rule should be applied.
	 */
	val filter: StyleFilter

	/**
	 * A higher priority value will be applied before entries with a lower priority.
	 * Equivalent priorities will go to the entry deeper in the display hierarchy, and then to the order this entry
	 * was added.
	 */
	val priority: Float
}

/**
 * Readable-writable styles.
 */
interface Style : StyleRo, Clearable {

	override val changed: Signal<(Style) -> Unit>

	override val allProps: MutableList<StyleProp<Any?>>

	fun getProp(kProperty: KProperty<*>): StyleProp<Any?>? {
		return allProps.first2 { it.name == kProperty.name }
	}

	/**
	 * Dispatches a [changed] signal and increments the [modTag].
	 * Style properties created via [StyleBase.prop] will automatically invoke this on change.
	 *
	 * Style properties should be immutable, but if you have a style property that can be mutated,
	 * notifyChanged will need to be invoked explicitly.
	 */
	fun notifyChanged()

	override val modTag: ModTag

	override var priority: Float

	override var filter: StyleFilter
}

/**
 * A Style object with no style properties.
 */
class NoopStyle : StyleBase(), StyleType<NoopStyle> {
	override val type = this
}

/**
 * The base class for a typical [Style] implementation.
 */
abstract class StyleBase : Style, Disposable {

	private val _changed = Signal1<StyleBase>()
	override val changed = _changed.asRo()

	override val modTag: ModTag = modTag()

	override var priority: Float by afterChange(0f, ::notifyChanged.as1)
	override var filter: StyleFilter by afterChange(AlwaysFilter, ::notifyChanged.as1)

	override fun clear() {
		var hasChanged = false
		for (i in 0..allProps.lastIndex) {
			val prop = allProps[i]
			if (prop.explicitIsSet || prop.calculatedIsSet) {
				prop.clear()
				hasChanged = true
			}
		}
		if (hasChanged)
			notifyChanged()
	}

	override val allProps: MutableList<StyleProp<Any?>> = ArrayList()

	protected fun <P> prop(defaultValue: P) = StyleProp(defaultValue)

	override fun notifyChanged() {
		modTag.increment()
		_changed.dispatch(this)
	}

	override fun dispose() {
		_changed.dispose()
	}

	override fun toString(): String {
		val props = allProps.joinToString(", ")
		return "Style($props)"
	}
}

class StyleWatcher<out T : StyleRo>(
		val style: T,
		val priority: Float,
		private val onChanged: (T) -> Unit
) : Comparable<StyleWatcher<*>> {

	private val styleWatch = ModTagWatch()

	override fun compareTo(other: StyleWatcher<*>): Int {
		return -priority.compareTo(other.priority)
	}

	fun check() {
		if (styleWatch.set(style.modTag)) {
			onChanged(style)
		}
	}
}

/**
 * Returns a writer for the style's property if and only if that property is explicitly set.
 */
fun Writer.styleProperty(style: Style, prop: KProperty<*>): Writer? {
	if (style.getProp(prop)?.explicitIsSet != true) return null
	return property(prop.name)
}

/**
 * Sets this style's explicit values to the calculated values of the given [other] style.
 */
fun <T : Style> T.set(other: T) {
	var hasChanged = false
	for (i in 0..allProps.lastIndex) {
		val p = allProps[i]
		val otherP = other.allProps.first2 { it.name == p.name }
		if (p.explicitValue != otherP.value) {
			hasChanged = true
			p.explicitValue = otherP.value
		}
	}
	if (hasChanged)
		notifyChanged()
}

class StyleProp<T>(
		val defaultValue: T
) : ReadWriteProperty<Style, T>, Clearable {

	private var _explicitIsSet = false

	val explicitIsSet: Boolean
		get() = _explicitIsSet

	private var _explicitValue: T? = null

	/**
	 * The value of this style property.
	 */
	@Suppress("UNCHECKED_CAST")
	var explicitValue: T
		get() = _explicitValue as T
		set(v) {
			_explicitIsSet = true
			_explicitValue = v
			_calculatedIsSet = true
			_calculatedValue = v
		}

	private var _calculatedIsSet = false

	val calculatedIsSet: Boolean
		get() = _calculatedIsSet

	private var _calculatedValue: T = defaultValue

	@Suppress("UNCHECKED_CAST")
	var calculatedValue: T
		get() = _calculatedValue
		set(value) {
			_calculatedIsSet = true
			_calculatedValue = value
		}

	var name: String? = null
		private set

	operator fun provideDelegate(
			thisRef: Style,
			prop: KProperty<*>
	): StyleProp<T> {
		if (name != null) throw Exception("This style property has already been assigned.")
		name = prop.name
		@Suppress("UNCHECKED_CAST")
		thisRef.allProps.add(this as StyleProp<Any?>)
		return this
	}

	/**
	 * Returns the calculated value.
	 */
	val value: T
		get() = _calculatedValue

	@Suppress("unchecked_cast")
	override fun getValue(thisRef: Style, property: KProperty<*>): T {
		return _calculatedValue
	}

	override fun setValue(thisRef: Style, property: KProperty<*>, value: T) = setValue(thisRef, value)

	fun setValue(thisRef: Style, value: T) {
		if (_explicitIsSet && _explicitValue == value) return // No-op
		explicitValue = value
		thisRef.notifyChanged()
	}

	/**
	 * Clears the calculated value if the explicit value was not set.
	 */
	fun clearCalculated() {
		if (explicitIsSet) return
		_calculatedIsSet = false
		_calculatedValue = defaultValue
	}

	override fun clear() {
		_explicitIsSet = false
		_explicitValue = null
		_calculatedIsSet = false
		_calculatedValue = defaultValue
	}

	override fun toString(): String {
		return "${if (_explicitIsSet) "*" else ""}$name=$_explicitValue"
	}
}

/**
 * The function signature for a factory that provides a component.
 */
typealias SkinPart = Context.() -> UiComponent

/**
 * The function signature for a factory that optionally provides a component.
 */
typealias OptionalSkinPart = Context.() -> UiComponent?

/**
 * Used as a placeholder for skin part factories that need to be declared in the skin.
 */
val noSkin: SkinPart = { spacer() }
val noSkinOptional: OptionalSkinPart = { null }
