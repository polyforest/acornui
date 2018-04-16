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

package com.acornui.component

import com.acornui.collection.ListView
import com.acornui.collection.ObservableList
import com.acornui.component.layout.DataScrollerStyle
import com.acornui.component.layout.ListItemRenderer
import com.acornui.component.layout.algorithm.LayoutDataProvider
import com.acornui.component.layout.algorithm.VerticalLayoutData
import com.acornui.component.layout.algorithm.virtual.VirtualVerticalLayoutStyle
import com.acornui.component.layout.algorithm.virtual.vDataScroller
import com.acornui.component.style.StyleBase
import com.acornui.component.style.StyleTag
import com.acornui.component.style.StyleType
import com.acornui.component.style.noSkin
import com.acornui.component.text.TextInput
import com.acornui.component.text.textInput
import com.acornui.core.di.Owned
import com.acornui.core.input.Ascii
import com.acornui.core.input.interaction.MouseInteractionRo
import com.acornui.core.input.interaction.click
import com.acornui.core.input.keyDown
import com.acornui.core.input.mouseDown
import com.acornui.core.isDescendantOf
import com.acornui.core.popup.lift
import com.acornui.core.text.StringFormatter
import com.acornui.core.text.ToStringFormatter
import com.acornui.math.Bounds
import com.acornui.math.Pad
import com.acornui.signal.Signal

// TODO: delegate focus to input

open class OptionsList<E : Any>(
		owner: Owned,
		val data: ObservableList<E>,
		rendererFactory: LayoutDataProvider<VerticalLayoutData>.() -> ListItemRenderer<E>
) : ContainerImpl(owner) {

	/**
	 * If true, search sorting and item selection will be case insensitive.
	 */
	var caseInsensitive = true

	/**
	 * Dispatched on each input character.
	 */
	val input: Signal<() -> Unit>
		get() = textInput.input

	// TODO: changed should dispatch on element clicked
	/**
	 * Dispatched on value commit.
	 */
	val changed: Signal<()->Unit>
		get() = textInput.changed

	/**
	 * The formatter to be used when converting a data element to a string.
	 * This should generally be the same formatter used for the labels in the ItemRenderer elements.
	 */
	var formatter: StringFormatter<E> = ToStringFormatter

	var selectedItem: E?
		get() = dataScroller.selection.selectedItem
		set(value) {
			dataScroller.selection.selectedItem = value
		}

	private val textInput: TextInput

	private var downArrow: UiComponent? = null

	private val dataView = ListView(data)

	private val dataScroller = vDataScroller(rendererFactory, dataView) {
		selection.changed.add {
			element, selected ->
			close()
		}
	}

	private val listLift = lift {
		focusEnabled = true
		+dataScroller layout {
			widthPercent = 1f
			heightPercent = 1f
		}

		onClosed = {
			close()
		}
	}

	/**
	 * The maximum number of full renderers that may be displayed at once.
	 */
	var maxItems: Int
		get() = dataScroller.maxItems
		set(value) {
			dataScroller.maxItems = value
		}

	private val stageMouseDownHandler = {
		event: MouseInteractionRo ->
		if (!event.target.isDescendantOf(dataScroller) && !event.target.isDescendantOf(downArrow!!)) {
			close()
		}
	}

	private var isUserInput: Boolean = false

	val style = bind(OptionsListStyle())

	val dataScrollerStyle: DataScrollerStyle
		get() = dataScroller.style

	val dataScrollerLayoutStyle: VirtualVerticalLayoutStyle
		get() = dataScroller.layoutStyle

	init {
		styleTags.add(OptionsList)
		maxItems = 10
		textInput = addChild(textInput {
			input.add(this@OptionsList::onInput)
		})

		keyDown().add {
			if (it.keyCode == Ascii.ESCAPE || it.keyCode == Ascii.RETURN || it.keyCode == Ascii.ENTER) close()
		}

		dataView.sortComparator = {
			o1, o2 ->
			var str = textInput.text
			if (caseInsensitive) str = str.toLowerCase()
			val score1 = searchScore(o1, str)
			val score2 = searchScore(o2, str)
			score1.compareTo(score2)
		}

		dataScroller.selection.changed.add {
			item, selected ->
			if (!isUserInput) {
				if (selected)
					textInput.text = formatter.format(item)
				else
					textInput.text = ""
			}
		}

		watch(style) {
			downArrow?.dispose()
			downArrow = it.downArrow(this)
			addChild(downArrow!!)
			downArrow!!.click().add {
				toggleOpen()
			}
		}
	}

	private fun onInput() {
		dataView.dirty()
		open()
		dataScroller.scrollModel.value = 0f // Scroll to the top.

		isUserInput = true
		val textLower = text.toLowerCase()
		selectedItem = data.firstOrNull { formatter.format(it).toLowerCase() == textLower }
		isUserInput = false
	}

	private fun searchScore(obj: E, str: String): Int {
		var itemStr = formatter.format(obj)
		if (caseInsensitive) itemStr = itemStr.toLowerCase()
		val i = itemStr.indexOf(str)
		if (i == -1) return 10000
		return i
	}

	private var _isOpen = false

	fun open() {
		if (_isOpen) return
		_isOpen = true
		addChild(listLift)
		stage.mouseDown(isCapture = true).add(stageMouseDownHandler)
		textInput.focus()
	}

	fun close() {
		if (!_isOpen) return
		_isOpen = false
		removeChild(listLift)
		stage.mouseDown(isCapture = true).remove(stageMouseDownHandler)
	}

	fun toggleOpen() {
		if (_isOpen) close()
		else open()
	}

	override fun onAncestorVisibleChanged(uiComponent: UiComponent, value: Boolean) {
		invalidate(ValidationFlags.LAYOUT)
	}

	var text: String
		get() = textInput.text
		set(value) {
			textInput.text = value
		}

	var listWidth: Float? by validationProp(null, ValidationFlags.LAYOUT)
	var listHeight: Float? by validationProp(null, ValidationFlags.LAYOUT)

	override fun updateLayout(explicitWidth: Float?, explicitHeight: Float?, out: Bounds) {
		val downArrow = this.downArrow!!
		textInput.boxStyle.padding = Pad(2f, downArrow.width, 2f, 2f)
		textInput.setSize(explicitWidth, explicitHeight)
		downArrow.setPosition(textInput.width - downArrow.width, (textInput.height - downArrow.height) * 0.5f)
		out.set(textInput.bounds)

		listLift.setSize(listWidth ?: textInput.width, listHeight)
		listLift.moveTo(0f, textInput.height)
	}

	companion object : StyleTag
}

class OptionsListStyle : StyleBase() {
	override val type: StyleType<OptionsListStyle> = OptionsListStyle

	var downArrow by prop(noSkin)

	companion object : StyleType<OptionsListStyle>
}

fun <E : Any> Owned.optionsList(
		data: ObservableList<E>,
		rendererFactory: LayoutDataProvider<VerticalLayoutData>.() -> ListItemRenderer<E> = { simpleItemRenderer() },
		init: ComponentInit<OptionsList<E>> = {}): OptionsList<E> {
	val t = OptionsList(this, data, rendererFactory)
	t.init()
	return t
}