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
import com.acornui.core.Disposable
import com.acornui.core.Lifecycle
import com.acornui.core.di.Owned
import com.acornui.core.di.own
import com.acornui.core.input.interaction.click
import com.acornui.signal.Signal0


open class RadioButton<out T>(
		owner: Owned,
		val data: T
) : Button(owner) {

	init {
		styleTags.add(RadioButton)
		click().add {
			if (!toggled) setUserToggled(true)
		}
	}

	companion object : StyleTag
}

fun <T> Owned.radioButton(group: RadioGroup<T>, data: T, init: ComponentInit<RadioButton<T>> = {}): RadioButton<T> {
	val b = RadioButton(this, data)
	group.register(b)
	b.init()
	return b
}

fun <T> Owned.radioButton(group: RadioGroup<T>, data: T, label: String, init: ComponentInit<RadioButton<T>> = {}): RadioButton<T> {
	val b = RadioButton(this, data)
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
	val radioButtons: List<RadioButton<T>>
		get() = _radioButtons

	@Suppress("UNCHECKED_CAST")
	private val selectedChangedHandler: (Button) -> Unit = {
		selectedButton = it as RadioButton<T>
		_changed.dispatch()
	}

	@Suppress("UNCHECKED_CAST")
	private val disposedHandler: (Lifecycle) -> Unit = {
		unregister(it as RadioButton<T>)
	}

	fun register(button: RadioButton<T>) {
		button.toggledChanged.add(selectedChangedHandler)
		button.disposed.add(disposedHandler)
		_radioButtons.add(button)
	}

	fun unregister(button: RadioButton<T>) {
		button.toggledChanged.remove(selectedChangedHandler)
		if (_selectedButton == button)
			selectedButton = null
		_radioButtons.remove(button)
	}

	private var _selectedButton: RadioButton<T>? = null
	var selectedButton: RadioButton<T>?
		get() = _selectedButton
		set(value) {
			if (_selectedButton == value) return
			_selectedButton?.toggled = false
			_selectedButton = value
			_selectedButton?.toggled = true
		}

	var selectedData: T?
		get() = _selectedButton?.data
		set(value) {
			for (i in 0.._radioButtons.lastIndex) {
				if (_radioButtons[i].data == value) {
					selectedButton = _radioButtons[i]
					break
				}
			}
		}

	fun radioButton(data: T, label: String, init: ComponentInit<RadioButton<T>> = {}): RadioButton<T> {
		val b = RadioButton(owner, data)
		b.label = label
		register(b)
		b.init()
		return b
	}

	fun radioButton(data: T, init: ComponentInit<RadioButton<T>> = {}): RadioButton<T> {
		val b = RadioButton(owner, data)
		register(b)
		b.init()
		return b
	}

	override fun dispose() {
		_changed.dispose()
		_selectedButton = null
		_radioButtons.clear()
	}
}

fun <T> Owned.radioGroup(init: RadioGroup<T>.() -> Unit): RadioGroup<T> {
	val group = RadioGroup<T>(this)
	group.init()
	return group
}
