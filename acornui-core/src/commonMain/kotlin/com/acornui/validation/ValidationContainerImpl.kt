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

@file:Suppress("UNUSED_ANONYMOUS_PARAMETER")

package com.acornui.validation

import com.acornui.async.awaitOrNull
import com.acornui.async.cancellingJobProp
import com.acornui.collection.Filter
import com.acornui.component.*
import com.acornui.component.layout.ElementLayoutContainer
import com.acornui.component.layout.HAlign
import com.acornui.component.layout.LayoutData
import com.acornui.component.layout.LayoutElement
import com.acornui.component.layout.algorithm.*
import com.acornui.component.style.*
import com.acornui.component.text.text
import com.acornui.di.Context
import com.acornui.focus.focus
import com.acornui.focus.focusSelf
import com.acornui.input.interaction.click
import com.acornui.math.Bounds
import com.acornui.observe.DataBindingRo
import com.acornui.observe.bind
import com.acornui.observe.dataBinding
import com.acornui.recycle.Clearable
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

@Suppress("MemberVisibilityCanBePrivate")
class ValidationContainerImpl<T, S : Style, out U : LayoutData, E : UiComponent>(
		owner: Context,
		layoutAlgorithm: LayoutAlgorithm<S, U>
) : ElementContainerImpl<E>(owner), LayoutDataProvider<StackLayoutData>, ValidationContainer<T> {

	/**
	 * The messages container is the first element in a vertical group. This layout data can set its size and alignment.
	 */
	val messagesLayoutData = VerticalLayoutData().apply {
		horizontalAlign = HAlign.CENTER
	}

	private val messagesC = addChild(vGroup<ListItemRenderer<ValidationInfo>> {
		layoutData = messagesLayoutData
	})

	/**
	 * The message item renderers are placed in a vertical layout. This style object represents those layout properties.
	 */
	val messagesContainerStyle: VerticalLayoutStyle
		get() = messagesC.style

	/**
	 * The contents stack is the second element in a vertical group. This layout data can set its size and alignment.
	 */
	val contentsLayoutData = VerticalLayoutData().apply {
		fill()
		priority = 1f
	}

	private val contents = addChild(ElementLayoutContainer<S, U, E>(this, layoutAlgorithm).apply {
		unbind(style) // Use this validation container for style inheritance.
		layoutData = contentsLayoutData
	})

	/**
	 * The layout properties for this container's elements.
	 */
	val contentsStyle: S = bind(layoutAlgorithm.style)

	private val layout = VerticalLayout()

	private val layoutElements = listOf(messagesC, contents)

	/**
	 * Layout properties for how the messages and contents are positioned.
	 */
	val style = bind(layout.style)

	/**
	 * The factory for validation message item renderers.
	 */
	var messageFactory: VerticalLayoutContainer<ListItemRenderer<ValidationInfo>>.() -> ListItemRenderer<ValidationInfo> = {
		validationMessageView {
			click().add {
				(data?.validatedData?.component as? UiComponentRo?)?.focus()
			}
		} layout { width = 300f }
	}

	private val _data = dataBinding<ValidationResults<*>?>(null)
	override val data: DataBindingRo<ValidationResults<*>?> = _data.asRo()

	private val _isBusy = dataBinding(false)
	override val isBusy: DataBindingRo<Boolean> = _isBusy.asRo()

	init {
		styleTags.add(Companion)
		messagesC.styleTags.add(MESSAGES_STYLE)
		validation.addNode(ValidationFlags.PROPERTIES, 0, ValidationFlags.LAYOUT, ::updateProperties)

		bind(_data) {
			invalidateProperties()
		}
		bind(_isBusy) {
			contents.interactivityMode = if (it) InteractivityMode.NONE else InteractivityMode.ALL
		}
	}

	private var _validator: (suspend ValidationBuilder.() -> T)? = null

	fun validator(value: suspend ValidationBuilder.() -> T) {
		_validator = value
	}

	override fun createLayoutData(): StackLayoutData = StackLayoutData()

	override fun onElementAdded(oldIndex: Int, newIndex: Int, element: E) {
		contents.addElement(newIndex, element)
	}

	override fun onElementRemoved(index: Int, element: E) {
		contents.removeElement(element)
	}

	private var validationJob by cancellingJobProp<Deferred<ValidationResults<T>>>()

	override suspend fun validateData(): ValidationResults<T>? {
		clear()
		validationJob = async {
			val validator = _validator ?: error("validator not set.")
			val validationBuilder = ValidationBuilder(this@ValidationContainerImpl)
			val data = validationBuilder.validator()
			val results = ValidationResults(data, validationBuilder.results)
			_data.value = results
			_isBusy.value = false
			results
		}
		return validationJob!!.awaitOrNull()
	}

	/**
	 * Clears any pending validation.
	 */
	override fun clear() {
		validationJob = null
		_data.value = null
		_isBusy.value = false
	}

	/**
	 * Determines what validation results to display as messages.
	 *
	 * The default is to show all results as messages except for successful results.
	 * E.g.
	 * ```
	 * messagesFilter = { it: ValidationResult ->
	 *   it.level != ValidationLevel.SUCCESS
	 * }
	 * ```
	 */
	var validationResultsFilter: Filter<ValidationInfo> by validationProp({ it: ValidationInfo ->
		it.level != ValidationLevel.SUCCESS
	}, ValidationFlags.PROPERTIES)

	private fun updateProperties() {
		val resultsList = _data.value?.results ?: emptyList()
		val m = resultsList.filter(validationResultsFilter)
		messagesC.recycleListItemRenderers(m, configure = { element, item, index ->
			element.styleTags.clear()
			element.styleTags.add(item.level.styleTag)
		}, factory = messageFactory)
		if (m.isNotEmpty())
			messagesC.focusSelf()

		messagesC.visible = m.isNotEmpty()
	}

	private val _elementsToLayout = ArrayList<LayoutElement>()

	override fun updateLayout(explicitWidth: Float?, explicitHeight: Float?, out: Bounds) {
		_elementsToLayout.clear()
		layoutElements.filterTo(_elementsToLayout, LayoutElement::shouldLayout)
		layout.layout(explicitWidth, explicitHeight, _elementsToLayout, out)
	}

	override fun dispose() {
		clear()
		super.dispose()
	}

	companion object : StyleTag {

		val MESSAGES_STYLE = styleTag()
	}
}

interface ValidationContainer<out T> : UiComponentRo, Clearable {

	/**
	 * The last completed validation results from [validateData].
	 */
	val data: DataBindingRo<ValidationResults<*>?>

	/**
	 * When [validateData] is called, isBusy will have a value of true until the asynchronous validation is complete.
	 */
	val isBusy: DataBindingRo<Boolean>

	/**
	 * Executes the validator and returns the validation results or null if the
	 * validation was cancelled.
	 *
	 * Calling this method consecutively before the previous validation finished will cancel the previous validation.
	 *
	 * @see data
	 * @return
	 */
	suspend fun validateData(): ValidationResults<T>?

	/**
	 * Clears any pending validation.
	 */
	override fun clear()
}

inline fun <T, S : Style, U : LayoutData, E : UiComponent> Context.validatedContainer(layoutAlgorithm: LayoutAlgorithm<S, U>, init: ComponentInit<ValidationContainerImpl<T, S, U, E>> = {}): ValidationContainerImpl<T, S, U, E> {
	contract { callsInPlace(init, InvocationKind.EXACTLY_ONCE) }
	return ValidationContainerImpl<T, S, U, E>(this, layoutAlgorithm).apply(init)
}

class ValidationMessageView(owner: Context) : HorizontalLayoutContainer<UiComponent>(owner), ListItemRenderer<ValidationInfo> {

	private val textField = text() layout { widthPercent = 1f }

	init {
		styleTags.add(Companion)
		+text("•")
		+textField
	}

	override var index: Int = -1

	private var _data: ValidationInfo? = null
	override var data: ValidationInfo?
		get() = _data
		set(value) {
			if (_data == value) return
			_data = value
			val text = value?.message ?: ""
			textField.text = text
		}

	override var toggled: Boolean = false

	companion object : StyleTag
}

inline fun Context.validationMessageView(init: ComponentInit<ValidationMessageView> = {}): ValidationMessageView {
	contract { callsInPlace(init, InvocationKind.EXACTLY_ONCE) }
	return ValidationMessageView(this).apply(init)
}


//abstract class ComponentValidatorBase(target: UiComponent) : ContextImpl(target), Validator {
//
//	var showFocusHighlight by afterChange(false, ::refreshFocusHighlight.as1)
//
//	private var focusTarget: UiComponentRo? = null
//	private var focusHighlighter: Highlighter? = null
//
//	init {
//
//	}
//
//	private fun refreshFocusHighlight() {
////		validate(ValidationFlags.STYLES)
////		if (focusTarget != null)
////			focusHighlighter?.unhighlight(focusTarget!!)
////		if (showFocusHighlight) {
////			focusTarget = focusHighlightDelegate ?: this
////			focusHighlighter = focusableStyle.highlighter
////			focusHighlighter?.highlight(focusTarget!!)
////		} else {
////			focusTarget = null
////			focusHighlighter = null
////		}
//	}
//
//	override suspend fun validate(): ValidationResult {
//		return ValidationResult("This is a longer invalid message that should wrap.", ValidationLevel.WARNING)
//	}
//
//	override fun clear() {
//	}
//}

class ValidationContainerStyle : StyleBase() {

	override val type = Companion

	var highlighter by prop<Highlighter?>(null)

	companion object : StyleType<ValidationContainerStyle>
}