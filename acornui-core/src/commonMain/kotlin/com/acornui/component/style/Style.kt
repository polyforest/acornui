/*
 * Copyright 2015 Nicholas Bilyk
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

import com.acornui.recycle.Clearable
import com.acornui.collection.first2
import com.acornui.component.UiComponent
import com.acornui.core.Disposable
import com.acornui.core.di.Owned
import com.acornui.observe.*
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
	 * Dispatches a [changed] signal.
	 * This should only be invoked if the explicit values have changed.
	 */
	fun notifyChanged()

	override val modTag: ModTag
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

	override val modTag = ModTagImpl()

	override fun clear() {
		for (i in 0..allProps.lastIndex) {
			allProps[i].clear()
		}
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

class StyleValidator(
		val style: Style,
		private val calculator: StyleCalculator
) {

	fun validate(host: Styleable) {
		calculator.calculate(host, style)
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
	for (i in 0..allProps.lastIndex) {
		val p = allProps[i]
		val otherP = other.allProps.first2 { it.name == p.name }
		p.explicitValue = otherP.value
	}
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
typealias SkinPart = Owned.() -> UiComponent

/**
 * The function signature for a factory that optionally provides a component.
 */
typealias OptionalSkinPart = Owned.() -> UiComponent?

/**
 * Used as a placeholder for skin part factories that need to be declared in the skin.
 */
val noSkin: SkinPart = { throw Exception("Skin part must be created.") }
val noSkinOptional: OptionalSkinPart = { null }