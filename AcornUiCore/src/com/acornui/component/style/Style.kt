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

import com.acornui.collection.Clearable
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
	 * The style properties, if they were explicitly set.
	 */
	val explicit: Map<String, Any?>

	/**
	 * The style properties, as calculated by a [StyleCalculator].
	 */
	val calculated: Map<String, Any?>

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

	override val explicit: MutableMap<String, Any?>

	override val calculated: MutableMap<String, Any?>

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
	override val changed: Signal<(StyleBase) -> Unit>
		get() = _changed

	override val modTag = ModTagImpl()

	override fun clear() {
		explicit.clear()
		calculated.clear()
		notifyChanged()
	}

	override val explicit = HashMap<String, Any?>()
	override val calculated = HashMap<String, Any?>()

	protected fun <P> prop(defaultValue: P) = StyleProp(defaultValue)

	override fun notifyChanged() {
		modTag.increment()
		_changed.dispatch(this)
	}

	override fun dispose() {
		_changed.dispose()
	}

	override fun toString(): String {
		var props = ""
		for (entry in calculated) {
			val explicit = if (explicit.containsKey(entry.key)) "*" else ""
			if (props.isNotEmpty()) props += ", "
			props += "${entry.key}=$explicit${entry.value}"
		}
		return "Style($props)"
	}
}

class StyleValidator(
		val style: Style,
		private val calculator: StyleCalculator
) {

	fun validate(host: Styleable) {
		calculator.calculate(style, host)
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
fun Writer.styleProperty(style: Style, id: String): Writer? {
	if (!style.explicit.containsKey(id)) return null
	return property(id)
}

/**
 * Sets this style to match that of the other style.
 */
@Suppress("unchecked_cast")
fun <T : Style> T.set(other: T) {
	explicit.clear()
	explicit.putAll(other.calculated)
	explicit.putAll(other.explicit)
	notifyChanged()
}

open class StyleProp<T>(val defaultValue: T) : ReadWriteProperty<Style, T> {

	@Suppress("unchecked_cast")
	override fun getValue(thisRef: Style, property: KProperty<*>): T {
		return if (thisRef.explicit.containsKey(property.name)) {
			thisRef.explicit[property.name]!! as T
		} else if (thisRef.calculated.containsKey(property.name)) {
			thisRef.calculated[property.name]!! as T
		} else {
			defaultValue
		}
	}

	override fun setValue(thisRef: Style, property: KProperty<*>, value: T) {
		thisRef.explicit[property.name] = value as Any?
		thisRef.notifyChanged()
	}
}

/**
 * Used as a placeholder for skin part factories that need to be declared in the skin.
 */
val noSkin: Owned.() -> UiComponent = { throw Exception("Skin part must be created.") }
val noSkinOptional: Owned.() -> UiComponent? = { null }