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

import com.acornui.component.style.StyleTag
import com.acornui.Disposable
import com.acornui.Lifecycle
import com.acornui.di.Owned
import com.acornui.di.own
import com.acornui.input.interaction.click
import com.acornui.signal.Signal0

interface RadioButtonRo<out T> : ButtonRo {
	val data: T
}

interface RadioButton<out T> : Button, RadioButtonRo<T>

open class RadioButtonImpl<out T>(
		owner: Owned,
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

fun <T> Owned.radioButton(group: RadioGroup<T>, data: T, init: ComponentInit<RadioButtonImpl<T>> = {}): RadioButtonImpl<T> {
	val b = RadioButtonImpl(this, data)
	group.register(b)
	b.init()
	return b
}

fun <T> Owned.radioButton(group: RadioGroup<T>, data: T, label: String, init: ComponentInit<RadioButtonImpl<T>> = {}): RadioButtonImpl<T> {
	val b = RadioButtonImpl(this, data)
	group.register(b)
	b.label = label
	b.init()
	return b
}

class RadioGroup<T>(val owner: Owned) : Disposable {

	init {
		owner.own(this)
	}

	private val _changed = Signal0()
	val changed = _changed.asRo()

	private val _radioButtons = ArrayList<RadioButton<T>>()
	val radioButtons: List<RadioButtonRo<T>>
		get() = _radioButtons

	@Suppress("UNCHECKED_CAST")
	private val toggledChangedHandler: (ButtonRo) -> Unit = {
		selectedData = (it as RadioButton<T>).data
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
		if (button.data == selectedData)
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

	var selectedData: T? = null
		set(value) {
			field = value
			toggledButton = _radioButtons.find { it.data == value }
		}

	fun radioButton(data: T, label: String, init: ComponentInit<RadioButton<T>> = {}): RadioButton<T> {
		val b = RadioButtonImpl(owner, data)
		b.label = label
		register(b)
		b.init()
		return b
	}

	fun radioButton(data: T, init: ComponentInit<RadioButton<T>> = {}): RadioButton<T> {
		val b = RadioButtonImpl(owner, data)
		register(b)
		b.init()
		return b
	}

	override fun dispose() {
		_changed.dispose()
		toggledButton = null
		_radioButtons.clear()
	}
}

fun <T> Owned.radioGroup(init: RadioGroup<T>.() -> Unit = {}): RadioGroup<T> {
	val group = RadioGroup<T>(this)
	group.init()
	return group
}
