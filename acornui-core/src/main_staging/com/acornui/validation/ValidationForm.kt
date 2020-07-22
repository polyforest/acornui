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

import com.acornui.component.*
import com.acornui.component.layout.ElementLayoutContainer
import com.acornui.component.layout.LayoutData
import com.acornui.component.layout.LayoutElement
import com.acornui.component.layout.algorithm.*
import com.acornui.component.style.Style
import com.acornui.component.style.StyleType
import com.acornui.di.Context
import com.acornui.function.as1
import com.acornui.math.Bounds
import com.acornui.time.CallbackWrapper
import com.acornui.time.delayedCallback
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

class ValidationForm<T, S : Style, out U : LayoutData>(
		owner: Context,
		layoutAlgorithm: LayoutAlgorithm<S, U>
) : ElementContainerImpl<UiComponent>(owner), LayoutDataProvider<U> {

	val style = bind(ValidationFormStyle())

	/**
	 * The layout data applied to the validation messages area.
	 */
	val messagesLayoutData = VerticalLayoutData().apply {
		widthPercent = 1.0
		priority = -1.0
	}

	private val messages = addChild(validationResultsView {
		layoutData = messagesLayoutData
	})

	/**
	 * The layout data applied to the contents area.
	 */
	val contentsLayoutData = VerticalLayoutData().apply {
		fill()
	}

	private val _contents = addChild(ElementLayoutContainer<S, U, UiComponent>(this, layoutAlgorithm).apply {
		layoutData = contentsLayoutData
	})

	val contents: UiComponent
		get() = _contents

	/**
	 * The layout style for the contents area.
	 */
	val contentsStyle: S = bind(_contents.unbind(_contents.style))

	private val vLayout = VerticalLayout()

	override fun createLayoutData(): U = _contents.createLayoutData()

	override fun onElementAdded(oldIndex: Int, newIndex: Int, element: UiComponent) {
		_contents.addElement(newIndex, element)
	}

	override fun onElementRemoved(index: Int, element: UiComponent) {
		_contents.removeElement(element)
	}

	private val _childrenToLayout = ArrayList<LayoutElement>()

	private var formOutputBuilder: (suspend ValidationResultsBuilder.() -> T)? = null

	fun formOutput(builder: suspend ValidationResultsBuilder.() -> T) {
		formOutputBuilder = builder
	}

	private val refreshResultsCallback: CallbackWrapper = delayedCallback(0.1) {
		launch {
			try {
				validateForm(showResults = true)
			} catch (e: CancellationException) {}
		}
	}

	private var resultsFeedback: ValidationResults<T>? = null
		set(value) {
			field?.results?.forEach {
				it.component.changed.remove(refreshResultsCallback::invoke.as1)
			}
			field = value
			field?.results?.forEach {
				it.component.changed.add(refreshResultsCallback::invoke.as1)
			}
			messages.data = value
		}

	suspend fun validateForm(showResults: Boolean = true): ValidationResults<T> {
		resultsFeedback = null
		_contents.interactivityMode = InteractivityMode.NONE
		val formOutputBuilder = formOutputBuilder ?: error("formOutput must be set")
		val builder = ValidationResultsBuilder(showResults)
		val formOutput = builder.formOutputBuilder()
		_contents.interactivityMode = InteractivityMode.ALL
		val results = ValidationResults(formOutput, builder.results)
		if (showResults)
			resultsFeedback = results
		return results
	}

	override fun updateLayout(explicitBounds: ExplicitBounds): Bounds {
		_childrenToLayout.clear()
		_children.filterTo(_childrenToLayout, LayoutElement::shouldLayout)
		vLayout.layout(explicitWidth, explicitHeight, _childrenToLayout, out)
	}
}

class ValidationResultsBuilder(private val showFeedback: Boolean) {

	var results = emptyList<ValidationInfo>()

	suspend fun <T> validate(input: FormInput<T>): T {
		val inputResults = input.validate(showFeedback)
		results = results + inputResults.results
		return inputResults.data
	}
}

inline fun <T> Context.validationForm(init: ComponentInit<ValidationForm<T, VerticalLayoutStyle, VerticalLayoutData>> = {}): ValidationForm<T, VerticalLayoutStyle, VerticalLayoutData> {
	contract { callsInPlace(init, InvocationKind.EXACTLY_ONCE) }
	return ValidationForm<T, VerticalLayoutStyle, VerticalLayoutData>(this, VerticalLayout()).apply(init)
}

inline fun <S : Style, U : LayoutData, T> Context.validationForm(layout: LayoutAlgorithm<S, U>, init: ComponentInit<ValidationForm<T, S, U>> = {}): ValidationForm<T, S, U> {
	contract { callsInPlace(init, InvocationKind.EXACTLY_ONCE) }
	return ValidationForm<T, S, U>(this, layout).apply(init)
}

class ValidationFormStyle : VerticalLayoutStyle() {

	override val type: StyleType<ValidationFormStyle> = Companion

	companion object : StyleType<ValidationFormStyle>
}


inline fun validationFormStyle(init: ValidationFormStyle.() -> Unit = {}): ValidationFormStyle {
	contract { callsInPlace(init, InvocationKind.EXACTLY_ONCE) }
	return ValidationFormStyle().apply(init)
}