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

package com.acornui.validation

import com.acornui.async.launchSupervised
import com.acornui.collection.ActiveList
import com.acornui.component.*
import com.acornui.component.layout.VAlign
import com.acornui.component.style.StyleBase
import com.acornui.component.style.StyleType
import com.acornui.component.style.noSkinOptional
import com.acornui.di.Context
import com.acornui.di.own
import com.acornui.focus.showFocusHighlight
import com.acornui.function.as1
import com.acornui.math.Bounds
import com.acornui.minus
import com.acornui.signal.Signal1
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

open class FormInput<T>(owner: Context) : SingleElementContainerImpl<InputComponent<T>>(owner), InputComponent<T>, Labelable {

	private val labelTextField = addChild(formLabel())

	val style = bind(FormInputStyle())

	private val _changed = own(Signal1<InputComponentRo<T>>())
	override val changed = _changed.asRo()

	override val inputValue: T
		get() = requiredElement.inputValue

	override var label: String
		get() = labelTextField.label
		set(value) {
			labelTextField.text = value
		}

	/**
	 * Overrides using [label] as a validation name.
	 * @see name
	 */
	var nameOverride: String? = null

	/**
	 * The name to be used in the validation results. This should be a localized string that will be used in field name
	 * token replacements for validation messages.
	 *
	 * This is label by default, but may be overridden by [nameOverride].
	 */
	val name: String
		get() = nameOverride ?: label

	val requiredElement: InputComponent<T>
		get() = element ?: error("element is required")

	private var icon = addChild(stack())
	private var warningIcon: UiComponent? = null
	private var errorIcon: UiComponent? = null

	init {
		watch(style) {
			warningIcon?.dispose()
			warningIcon = icon.addOptionalElement(it.warningIcon(this))

			errorIcon?.dispose()
			errorIcon = icon.addOptionalElement(it.errorIcon(this))
		}
	}

	private val _validators = ActiveList<Validator<T>>()

	val validators: MutableList<Validator<T>> = _validators

	/**
	 * If true (default), the validation will be invoked when the component's value has been committed.
	 */
	var validateOnChanged = true

	init {
		_validators.addBinding {
			dirty()
		}
		validation.addNode(ValidationFlags.PROPERTIES, ValidationFlags.STYLES, ValidationFlags.LAYOUT, ::updateProperties)
	}

	/**
	 * Clears the current validation feedback and cancels any currently pending validation job.
	 * The next validation will not use any cached results.
	 */
	fun dirty() {
		validationJob?.cancel()
		validationJob = null
		clearValidationFeedback()
	}

	private var validationJob: Deferred<ValidationResults<T>>? = null

	suspend fun validate(showFeedback: Boolean = true): ValidationResults<T> {
		validationFeedback = null
		if (validationJob == null) {
			validationJob = async {
				val value = requiredElement.inputValue
				val results = validators.fold(emptyList<ValidationInfo>(), { acc, validator ->
					acc + validator.invoke(value).results
				})
				ValidationResults(value, results)
			}
		}
		return validationJob!!.await().also {
			if (showFeedback)
				validationFeedback = it
		}
	}

	private var validationFeedback by validationProp<ValidationResults<T>?>(null, ValidationFlags.PROPERTIES)

	/**
	 * Clears the visual feedback for the validation.
	 */
	fun clearValidationFeedback() {
		validationFeedback = null
	}

	private fun updateProperties() {
		val validationFeedback = validationFeedback
		var tooltipStr = ""
		validationFeedback?.results?.forEach {
			if (it.level != ValidationLevel.SUCCESS) {
				if (tooltipStr.isNotEmpty())
					tooltipStr += "\n"
				tooltipStr += it.message
			}
		}
		icon.tooltip(tooltipStr)

		if (validationFeedback == null || validationFeedback.success) {
			errorIcon?.visible = false
			warningIcon?.visible = false
		} else if (validationFeedback.results.any { it.level == ValidationLevel.ERROR }) {
			errorIcon?.visible = true
			warningIcon?.visible = false
		} else {
			errorIcon?.visible = false
			warningIcon?.visible = true
		}
	}

	override fun dispose() {
		validationFeedback = null
		validationJob?.cancel()
		validationJob = null
		super.dispose()
	}

	override fun onElementChanged(oldElement: InputComponent<T>?, newElement: InputComponent<T>?) {
		super.onElementChanged(oldElement, newElement)
		oldElement?.changed?.remove(::valueChangedHandler.as1)
		newElement?.changed?.add(::valueChangedHandler.as1)
	}

	private fun valueChangedHandler() {
		dirty()
		if (validateOnChanged) {
			launchSupervised {
				validate(true)
			}
		}

		_changed.dispatch(this)
	}

	override fun updateLayout(explicitWidth: Float?, explicitHeight: Float?, out: Bounds) {
		labelTextField.size(explicitWidth, null)
		requiredElement.size(explicitWidth, explicitHeight - labelTextField.height - style.verticalGap)
		requiredElement.position(0f, labelTextField.bottom + style.verticalGap)
		val iconX = requiredElement.right + style.horizontalGap
		icon.position(iconX, iconY())
		out.set(maxOf(labelTextField.width, icon.right), requiredElement.bottom, requiredElement.baselineY)
	}

	private fun iconY(): Float {
		return requiredElement.y + when (style.verticalAlign) {
			VAlign.TOP -> 0f
			VAlign.MIDDLE -> (requiredElement.height - icon.height) * 0.5f
			VAlign.BOTTOM -> requiredElement.height - icon.height
			VAlign.BASELINE -> requiredElement.baseline - icon.baseline
		}
	}
}

inline fun <T> Context.formInput(init: ComponentInit<FormInput<T>> = {}): FormInput<T> {
	contract { callsInPlace(init, InvocationKind.EXACTLY_ONCE) }
	return FormInput<T>(this).apply(init)
}

class FormInputStyle : StyleBase() {

	override val type: StyleType<FormInputStyle> = Companion

	/**
	 * The vertical gap between the label and the input.
	 */
	var verticalGap by prop(1f)

	/**
	 * The horizontal gap between the input element and the error / warning icon.
	 */
	var horizontalGap by prop(3f)

	/**
	 * The vertical alignment of the error / warning icon relative to the input element.
	 */
	var verticalAlign by prop(VAlign.MIDDLE)

	var warningIcon by prop(noSkinOptional)
	var errorIcon by prop(noSkinOptional)

	companion object : StyleType<FormInputStyle>
}

inline fun formInputStyle(init: FormInputStyle.() -> Unit = {}): FormInputStyle {
	contract { callsInPlace(init, InvocationKind.EXACTLY_ONCE) }
	return FormInputStyle().apply(init)
}


/**
 * A utility for creating and adding a validator for this input component.
 */
fun <T> FormInput<T>.addValidator(validator: suspend (data: T) -> Pair<String, ValidationLevel>) {
	validators += {
		val result = validator(it)
		ValidationResults(it, listOf(
				ValidationInfo(
						message = result.first,
						level = result.second,
						name = name,
						component = this
				)
		))
	}
}