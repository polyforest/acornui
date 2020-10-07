/*
 * Copyright 2020 Poly Forest, LLC
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

package com.acornui.component.input

import com.acornui.component.Div
import com.acornui.component.UiComponent
import com.acornui.component.UiComponentImpl
import com.acornui.component.connected
import com.acornui.di.Context
import com.acornui.dom.createElement
import com.acornui.input.ChangeSignal
import com.acornui.observe.Observable
import com.acornui.own
import com.acornui.recycle.Clearable
import com.acornui.signal.Signal
import com.acornui.signal.SignalImpl
import com.acornui.signal.SignalSubscription
import com.acornui.signal.event
import org.w3c.dom.HTMLFormElement
import org.w3c.dom.HTMLInputElement
import org.w3c.dom.ValidityState
import org.w3c.dom.events.Event
import org.w3c.dom.events.InputEvent

/**
 * The common interface to all [HTMLInputElement] types.
 */
interface Input : UiComponent, Observable, Clearable {

	/**
	 * Dispatched on each input character.
	 * Note - this does not invoke when the text is programmatically changed.
	 */
	val input: Signal<InputEvent>

	/**
	 * Dispatched when the parent form has been reset.
	 */
	val formReset: Signal<Event>

	/**
	 * Indicates that the user must fill in a value before submitting a form.
	 */
	var required: Boolean

	/**
	 * Returns the element's current validity state.
	 */
	val validity: ValidityState

	/**
	 * Returns a localized message that describes the validation constraints that the control does not satisfy (if any).
	 * This is the empty string if the control is not a candidate for constraint validation (willvalidate is false), or
	 * it satisfies its constraints. This value can be set by the setCustomValidity method.
	 */
	val validationMessage: String

	/**
	 * Returns whether the element is a candidate for constraint validation.
	 */
	val willValidate: Boolean

	/**
	 * Sets the element's disabled attribute, indicating that the control is not available for interaction.
	 * The input values will not be submitted with the form.
	 */
	var disabled: Boolean

	/**
	 * The [HTMLInputElement]'s name.
	 */
	var name: String

	fun checkValidity(): Boolean

	fun reportValidity(): Boolean

	fun setCustomValidity(error: String)
}

open class InputImpl(owner: Context, type: String) :
	UiComponentImpl<HTMLInputElement>(owner, createElement("input") { this.type = type }), Input {

	/**
	 * Dispatched on value commit.
	 * This is only dispatched on a user interaction, such as pressing ENTER or TAB. It is not dispatched when
	 * the text is programmatically changed.
	 */
	final override val changed = ChangeSignal(this)

	final override val input: Signal<InputEvent>
		get() = event("input")

	final override val formReset: Signal<Event> by lazy {
		own(object : SignalImpl<Event>() {

			private val connectedSub: SignalSubscription
			private var resetSub: SignalSubscription? = null

			init {
				connectedSub = connected.listen {
					form = dom.form
				}
			}

			private var form: HTMLFormElement? = null
				set(value) {
					if (field == value) return
					resetSub?.dispose()
					field = value
					resetSub = value?.event<Event>("reset")?.listen {
						dispatch(it)
					}
				}

			override fun dispose() {
				super.dispose()
				connectedSub.dispose()
				form = null
			}
		})
	}

	/**
	 * Indicates that the user must fill in a value before submitting a form.
	 */
	final override var required: Boolean
		get() = dom.required
		set(value) {
			dom.required = value
		}

	/**
	 * Returns the element's current validity state.
	 */
	final override val validity: ValidityState
		get() = dom.validity

	/**
	 * Returns a localized message that describes the validation constraints that the control does not satisfy (if any).
	 * This is the empty string if the control is not a candidate for constraint validation (willvalidate is false), or
	 * it satisfies its constraints. This value can be set by the setCustomValidity method.
	 */
	final override val validationMessage: String
		get() = dom.validationMessage

	/**
	 * Returns whether the element is a candidate for constraint validation.
	 */
	final override val willValidate: Boolean
		get() = dom.willValidate

	/**
	 * Sets the element's disabled attribute, indicating that the control is not available for interaction.
	 * The input values will not be submitted with the form.
	 */
	final override var disabled: Boolean
		get() = dom.disabled
		set(value) {
			dom.disabled = value
		}

	/**
	 * The [HTMLInputElement]'s name.
	 */
	final override var name: String
		get() = dom.name
		set(value) {
			dom.name = value
		}

	final override fun checkValidity() = dom.checkValidity()

	final override fun reportValidity() = dom.reportValidity()

	final override fun setCustomValidity(error: String) = dom.setCustomValidity(error)

	val form: HTMLFormElement?
		get() = dom.form

	override fun clear() {
		dom.value = dom.defaultValue
	}
}

/**
 * If an input component is encapsulated, it may be convenient to delegate its [Input] methods.
 */
abstract class DivWithInputComponent(owner: Context) : Div(owner), Input {

	protected abstract val inputComponent: Input

	final override val changed: Signal<Observable>
		get() = inputComponent.changed

	final override val input: Signal<InputEvent>
		get() = inputComponent.input

	final override val formReset: Signal<Event>
		get() = inputComponent.formReset

	override var required: Boolean
		get() = inputComponent.required
		set(value) {
			inputComponent.required = value
		}

	override val validity: ValidityState
		get() = inputComponent.validity

	override val validationMessage: String
		get() = inputComponent.validationMessage

	override val willValidate: Boolean
		get() = inputComponent.willValidate

	final override var name: String
		get() = inputComponent.name
		set(value) {
			inputComponent.name = value
		}

	override var disabled: Boolean
		get() = inputComponent.disabled
		set(value) {
			inputComponent.disabled = value
		}

	override fun checkValidity(): Boolean = inputComponent.checkValidity()

	override fun reportValidity(): Boolean = inputComponent.reportValidity()

	override fun setCustomValidity(error: String) = inputComponent.setCustomValidity(error)

	override fun clear() = inputComponent.clear()
}