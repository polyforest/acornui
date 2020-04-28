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

package com.acornui.component

import com.acornui.Lifecycle
import com.acornui.ManagedDisposable
import com.acornui.component.layout.ElementLayoutContainer
import com.acornui.component.layout.LayoutData
import com.acornui.component.layout.algorithm.*
import com.acornui.component.style.Style
import com.acornui.component.style.StyleTag
import com.acornui.di.Context
import com.acornui.di.own
import com.acornui.function.as1
import com.acornui.input.interaction.click
import com.acornui.signal.Signal0
import com.acornui.signal.Signal1
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract
import kotlin.jvm.JvmName

interface RadioButtonRo<out T> : ButtonRo {
	val data: T
}

interface RadioButton<out T> : Button, RadioButtonRo<T>

open class RadioButtonImpl<out T>(
		owner: Context,
		override val data: T
) : ButtonImpl(owner), RadioButton<T> {

	init {
		styleTags.add(RadioButtonImpl)
		click().add {
			if (!toggled) setUserToggled(true)
		}
	}

	companion object : StyleTag
}

class RadioGroupController<T>(val owner: Context) : ManagedDisposable {

	init {
		owner.disposed.add(::dispose.as1)
	}

	private val _changed = Signal0()
	val changed = _changed.asRo()

	private val _radioButtons = ArrayList<RadioButton<T>>()
	val radioButtons: List<RadioButtonRo<T>>
		get() = _radioButtons

	@Suppress("UNCHECKED_CAST")
	private val toggledChangedHandler: (ButtonRo) -> Unit = {
		inputValue = (it as RadioButton<T>).data
		_changed.dispatch()
	}

	@Suppress("UNCHECKED_CAST")
	private val disposedHandler: (Lifecycle) -> Unit = {
		unregister(it as RadioButton<T>)
	}

	fun register(button: RadioButton<T>) {
		button.toggledChanged.add(toggledChangedHandler)
		button.disposed.add(disposedHandler)
		_radioButtons.add(button)
		if (button.data == inputValue)
			toggledButton = button
	}

	fun unregister(button: RadioButton<T>) {
		button.toggledChanged.remove(toggledChangedHandler)
		if (toggledButton == button)
			toggledButton = null
		_radioButtons.remove(button)
	}

	var toggledButton: RadioButton<T>? = null
		private set(value) {
			if (field == value) return
			field?.toggled = false
			field = value
			field?.toggled = true
		}

	var inputValue: T? = null
		set(value) {
			field = value
			toggledButton = _radioButtons.find { it.data == value }
		}

	fun radioButton(data: T, label: String = "", init: ComponentInit<RadioButton<T>> = {}): RadioButton<T> {
		val b = RadioButtonImpl(owner, data)
		b.label = label
		register(b)
		b.init()
		return b
	}

	override fun dispose() {
		owner.disposed.remove(::dispose.as1)
		_changed.dispose()
		toggledButton = null
		_radioButtons.clear()
	}
}

open class RadioGroupView<S : Style, U : LayoutData, E : UiComponent, T>(owner: Context, layoutAlgorithm: LayoutAlgorithm<S, U>) : ElementLayoutContainer<S, U, E>(owner, layoutAlgorithm), InputComponent<T?> {

	private val _changed = own(Signal1<RadioGroupView<S, U, E, T>>())
	override val changed = _changed.asRo()

	private val group = RadioGroupController<T>(this)

	fun radioButton(data: T, label: String = "", init: ComponentInit<RadioButton<T>> = {}): RadioButton<T> = group.radioButton(data, label, init)

	override var inputValue: T?
		get() = group.inputValue
		set(value) {
			group.inputValue = value
		}

	init {
		group.changed.add {
			_changed.dispatch(this)
		}
	}
}

typealias HRadioGroupView<E, T> = RadioGroupView<HorizontalLayoutStyle, HorizontalLayoutData, E, T>

@JvmName("hGroupT")
inline fun <E : UiComponent, T> Context.hRadioGroup(init: ComponentInit<HRadioGroupView<E, T>> = {}): HRadioGroupView<E, T> {
	contract { callsInPlace(init, InvocationKind.EXACTLY_ONCE) }
	return HRadioGroupView<E, T>(this, HorizontalLayout()).apply(init)
}

inline fun <T> Context.hRadioGroup(init: ComponentInit<HRadioGroupView<UiComponent, T>> = {}): HRadioGroupView<UiComponent, T> {
	contract { callsInPlace(init, InvocationKind.EXACTLY_ONCE) }
	return HRadioGroupView<UiComponent, T>(this, HorizontalLayout()).apply(init)
}

typealias VRadioGroupView<E, T> = RadioGroupView<VerticalLayoutStyle, VerticalLayoutData, E, T>

@JvmName("vGroupT")
inline fun <E : UiComponent, T> Context.vRadioGroup(init: ComponentInit<VRadioGroupView<E, T>> = {}): VRadioGroupView<E, T> {
	contract { callsInPlace(init, InvocationKind.EXACTLY_ONCE) }
	return VRadioGroupView<E, T>(this, VerticalLayout()).apply(init)
}

inline fun <T> Context.vRadioGroup(init: ComponentInit<VRadioGroupView<UiComponent, T>> = {}): VRadioGroupView<UiComponent, T> {
	contract { callsInPlace(init, InvocationKind.EXACTLY_ONCE) }
	return VRadioGroupView<UiComponent, T>(this, VerticalLayout()).apply(init)
}
