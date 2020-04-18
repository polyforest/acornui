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

import com.acornui.collection.Filter
import com.acornui.component.*
import com.acornui.component.layout.HAlign
import com.acornui.component.layout.algorithm.*
import com.acornui.component.style.StyleBase
import com.acornui.component.style.StyleTag
import com.acornui.component.style.StyleType
import com.acornui.component.style.styleTag
import com.acornui.component.text.text
import com.acornui.di.Context
import com.acornui.focus.focusSelf
import com.acornui.math.Bounds
import com.acornui.observe.bind
import com.acornui.recycle.Clearable
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

@Suppress("MemberVisibilityCanBePrivate")
class ValidatedGroup(owner: Context) : ElementContainerImpl<UiComponent>(owner), LayoutDataProvider<StackLayoutData>, Clearable, ValidationContainer {

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

	private val contents = addChild(stack {
		layoutData = contentsLayoutData
	})

	/**
	 * The elements are placed in a stack layout. This style object represents those layout properties.
	 */
	val contentsStyle: StackLayoutStyle
		get() = contents.style

	/**
	 * Layout properties for how the messages and contents are positioned.
	 */
	val style = bind(VerticalLayoutStyle())

	private val layout = VerticalLayout()
	private val layoutElements = listOf(messagesC, contents)

	/**
	 * The factory for validation message item renderers.
	 */
	var messageFactory: VerticalLayoutContainer<ListItemRenderer<ValidationInfo>>.() -> ListItemRenderer<ValidationInfo> = {
		validationMessageView() layout { width = 300f }
	}

	private val validationController = ValidationController(this)

	init {
		dependencies += listOf(ValidationController to validationController, ValidationContainer to this)

		styleTags.add(Companion)
		messagesC.styleTags.add(MESSAGES_STYLE)
		validation.addNode(ValidationFlags.PROPERTIES, 0, ValidationFlags.LAYOUT, ::updateProperties)

		bind(validationController.data) {
			invalidateProperties()
		}
		bind(validationController.isBusy) {
			interactivityMode = if (it) InteractivityMode.NONE else InteractivityMode.ALL
		}
	}

	override fun createLayoutData(): StackLayoutData = StackLayoutData()

	override fun onElementAdded(oldIndex: Int, newIndex: Int, element: UiComponent) {
		contents.addElement(newIndex, element)
	}

	override fun onElementRemoved(index: Int, element: UiComponent) {
		contents.removeElement(element)
	}

	/**
	 * Clears all set validators.
	 */
	override fun clear() {
		validationController.clear()
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
		val resultsList = validationController.data.value?.results ?: emptyList()
		val m = resultsList.filter(validationResultsFilter)
		messagesC.recycleListItemRenderers(m, configure = { element, item, index ->
			element.styleTags.clear()
			element.styleTags.add(item.level.styleTag)
		}, factory = messageFactory)
		if (m.isNotEmpty())
			messagesC.focusSelf()
	}

	override fun updateLayout(explicitWidth: Float?, explicitHeight: Float?, out: Bounds) {
		layout.layout(explicitWidth, explicitHeight, layoutElements, out)
	}

	override fun dispose() {
		clear()
		super.dispose()
	}

	companion object : StyleTag {

		val MESSAGES_STYLE = styleTag()
	}
}

interface ValidationContainer : UiComponentRo {

	companion object : Context.Key<ValidationContainer>
}

inline fun Context.validatedGroup(init: ComponentInit<ValidatedGroup> = {}): ValidatedGroup {
	contract { callsInPlace(init, InvocationKind.EXACTLY_ONCE) }
	return ValidatedGroup(this).apply(init)
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

class ValidatorStyle : StyleBase() {

	override val type = Companion

	var highlighter by prop<Highlighter?>(null)

	companion object : StyleType<ValidatorStyle>
}