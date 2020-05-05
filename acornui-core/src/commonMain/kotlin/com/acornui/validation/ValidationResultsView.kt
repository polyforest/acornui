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

@file:Suppress("MemberVisibilityCanBePrivate")

package com.acornui.validation

import com.acornui.collection.Filter
import com.acornui.component.*
import com.acornui.component.layout.algorithm.HorizontalLayoutContainer
import com.acornui.component.layout.algorithm.VerticalLayoutContainer
import com.acornui.component.layout.algorithm.vGroup
import com.acornui.component.style.StyleBase
import com.acornui.component.style.StyleTag
import com.acornui.component.style.StyleType
import com.acornui.component.text.TextField
import com.acornui.component.text.selectable
import com.acornui.component.text.text
import com.acornui.cursor.StandardCursor
import com.acornui.cursor.cursor
import com.acornui.di.Context
import com.acornui.focus.FocusOptions
import com.acornui.focus.focus
import com.acornui.focus.mousePressOnKey
import com.acornui.input.interaction.click
import com.acornui.math.Bounds
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

class ValidationResultsView(owner: Context) : ContainerImpl(owner), ItemRenderer<ValidationResults<*>> {

	private val contents = addChild(vGroup<ListItemRenderer<ValidationInfo>>())

	val style = bind(ValidationResultsStyle())

	init {
		styleTags.add(Companion)
		watch(style) {
			contents.clearElements(dispose = true)
			refresh()
		}
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
	var validationResultsFilter: Filter<ValidationInfo> = { it: ValidationInfo ->
		it.level != ValidationLevel.SUCCESS
	}
		set(value) {
			field = value
			refresh()
		}

	override var data: ValidationResults<*>? = null
		set(value) {
			if (field == value) return
			field = value
			refresh()
		}

	private fun refresh() {
		val resultsList = data?.results ?: emptyList()
		val filteredResults = resultsList.filter(validationResultsFilter)
		contents.recycleListItemRenderers(
				filteredResults,
				factory = style.messageFactory
		)
		visible = filteredResults.isNotEmpty()
	}

	override fun updateLayout(explicitWidth: Float?, explicitHeight: Float?, out: Bounds) {
		contents.size(explicitWidth, explicitHeight)
		out.set(contents.bounds)
	}

	companion object : StyleTag
}

inline fun Context.validationResultsView(init: ComponentInit<ValidationResultsView> = {}): ValidationResultsView {
	contract { callsInPlace(init, InvocationKind.EXACTLY_ONCE) }
	return ValidationResultsView(this).apply(init)
}

class ValidationInfoItemRenderer(owner: Context) : HorizontalLayoutContainer<UiComponent>(owner), ListItemRenderer<ValidationInfo> {

	private val textField: TextField

	init {
		focusEnabled = true
		styleTags.add(Companion)
		+text("•")
		textField = +text {
			selectable = false
			flowStyle.sizeToContents = true
		} layout { widthPercent = 1f }

		cursor(StandardCursor.HAND)

		click().add {
			it.handled = true
			data?.component?.focus(FocusOptions(highlight = true))
		}
		mousePressOnKey()
	}

	override var index: Int = -1

	override var data: ValidationInfo? = null
		set(value) {
			val previous = field
			if (previous == value) return
			field = value
			if (previous?.level != value?.level) {
				if (previous != null)
					styleTags.remove(previous.level.styleTag)
				if (value != null)
					styleTags.add(value.level.styleTag)
			}
			val text = value?.message ?: ""
			textField.text = text
		}

	override var toggled: Boolean = false

	companion object : StyleTag
}

class ValidationResultsStyle : StyleBase() {

	override val type: StyleType<ValidationResultsStyle> = Companion

	/**
	 * The factory for validation message item renderers.
	 */
	var messageFactory by prop<VerticalLayoutContainer<ListItemRenderer<ValidationInfo>>.() -> ListItemRenderer<ValidationInfo>> {
		validationInfoItemRenderer() layout { widthPercent = 1f }
	}

	companion object : StyleType<ValidationResultsStyle>
}


inline fun validationResultsStyle(init: ValidationResultsStyle.() -> Unit = {}): ValidationResultsStyle {
	contract { callsInPlace(init, InvocationKind.EXACTLY_ONCE) }
	return ValidationResultsStyle().apply(init)
}

inline fun Context.validationInfoItemRenderer(init: ComponentInit<ValidationInfoItemRenderer> = {}): ValidationInfoItemRenderer {
	contract { callsInPlace(init, InvocationKind.EXACTLY_ONCE) }
	return ValidationInfoItemRenderer(this).apply(init)
}