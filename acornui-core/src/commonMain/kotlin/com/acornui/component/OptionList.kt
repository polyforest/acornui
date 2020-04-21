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

@file:Suppress("unused")

package com.acornui.component

import com.acornui.Disposable
import com.acornui.collection.ListView
import com.acornui.collection.ObservableList
import com.acornui.collection.indexOfFirst
import com.acornui.collection.indexOfLast
import com.acornui.component.layout.DataScrollerStyle
import com.acornui.component.layout.algorithm.VerticalLayoutData
import com.acornui.component.layout.algorithm.virtual.ItemRendererContext
import com.acornui.component.layout.algorithm.virtual.VirtualVerticalLayoutStyle
import com.acornui.component.layout.algorithm.virtual.vDataScroller
import com.acornui.component.scroll.ScrollModel
import com.acornui.component.style.*
import com.acornui.component.text.TextInput
import com.acornui.component.text.selectable
import com.acornui.component.text.textInput
import com.acornui.cursor.StandardCursor
import com.acornui.cursor.cursor
import com.acornui.di.Context
import com.acornui.di.own
import com.acornui.focus.blurred
import com.acornui.focus.focus
import com.acornui.focus.focusSelf
import com.acornui.input.Ascii
import com.acornui.input.interaction.KeyInteractionRo
import com.acornui.input.interaction.click
import com.acornui.input.keyDown
import com.acornui.math.Bounds
import com.acornui.math.Pad
import com.acornui.observe.bind
import com.acornui.popup.PopUpManager
import com.acornui.popup.lift
import com.acornui.properties.afterChange
import com.acornui.recycle.Clearable
import com.acornui.signal.Signal0
import com.acornui.text.StringFormatter
import com.acornui.text.ToStringFormatter
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

open class OptionList<E : Any>(
		owner: Context
) : ContainerImpl(owner), Clearable {

	constructor(owner: Context, data: List<E?>) : this(owner) {
		data(data)
	}

	constructor(owner: Context, data: ObservableList<E?>) : this(owner) {
		data(data)
	}

	/**
	 * If true, when this option list is clicked, this list will open. Clicking the down arrow will still result in a
	 * toggle.
	 * If false (default), this list is only opened when either the down arrow is clicked, or the down key is pressed.
	 */
	var autoOpen: Boolean = false

	private val _input = own(Signal0())

	/**
	 * Dispatched on each input character.
	 * This does not dispatch when selecting an item from the drop down list.
	 */
	val input = _input.asRo()

	private val _changed = own(Signal0())

	/**
	 * Dispatched on value commit.
	 * It is dispatched when the user selects an item, or commits the value of the text input. It is not dispatched
	 * when the selected item or text is programmatically changed.
	 */
	val changed = _changed.asRo()

	/**
	 * The formatter to be used when converting a data element to a string.
	 * This should generally be the same formatter used for the labels in the ItemRenderer elements.
	 */
	var formatter: StringFormatter<E> = ToStringFormatter

	/**
	 * Given the text input's text, returns the matching item in the data list, or null if there are no matches.
	 * By default this will search for a case insensitive match to the item's string result from the [formatter].
	 */
	var textToItem = { text: String ->
		data.firstOrNull {
			if (it != null) {
				val itStr = formatter.format(it)
				itStr.equals(text, ignoreCase = true)
			} else false
		}
	}

	/**
	 * Sets the currently selected item.
	 * Note that this does not invoke [input] or [changed] signals.
	 */
	var selectedItem: E?
		get() = dataScroller.selection.selectedItem
		set(value) {
			dataScroller.selection.selectedItem = value
			textInput.text = if (value == null) "" else formatter.format(value)
		}

	/**
	 * If true, the list will open when typing into the input field.
	 */
	var openOnInput = true

	private val _isOpenChanged = own(Signal0())

	/**
	 * Dispatched when the [isOpen] property has changed.
	 */
	val isOpenChanged = _isOpenChanged.asRo()

	private val textInput: TextInput = textInput {
		input.add {
			if (openOnInput) open()
			scrollModel.value = 0f // Scroll to the top.
			dataScroller.highlighted.selectedItem = null
			setSelectedItemFromText()
			_input.dispatch()
		}
	}

	private var handCursor: Disposable? = null

	/**
	 * If false, the text input will not accept type input, and items may only be selected via the dropdown.
	 */
	var editable: Boolean by afterChange(true) {
		textInput.editable = it
		textInput.selectable = it
		handCursor?.dispose()
		if (!it) handCursor = cursor(StandardCursor.HAND)
	}

	/**
	 * If true, this option list will use the CommonStyleTags.disabled style tag and have interactivity disabled.
	 */
	var disabled: Boolean by afterChange(false) {
		interactivityMode = if (it) InteractivityMode.NONE else InteractivityMode.ALL
		disabledTag = it
	}

	/**
	 * The background, as created by [OptionListStyle.background].
	 */
	private var background: UiComponent? = null

	/**
	 * The down arrow, as created by [OptionListStyle.downArrow].
	 */
	private var downArrow: UiComponent? = null

	private val dataScroller = vDataScroller<E> {
		focusEnabled = true
		keyDown().add(::keyDownHandler)
		selection.changed.add { _, newSelection ->
			val value = newSelection.firstOrNull()
			textInput.text = if (value == null) "" else formatter.format(value)
			focus()
			_changed.dispatch()
			close()
		}
	}

	private val listLift = lift {
		focus = false
		+dataScroller layout { fill() }
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

	val style = bind(OptionListStyle())

	val dataScrollerStyle: DataScrollerStyle
		get() = dataScroller.style

	val dataScrollerLayoutStyle: VirtualVerticalLayoutStyle
		get() = dataScroller.layoutStyle

	/**
	 * The scroll model for the dropdown list.
	 */
	val scrollModel: ScrollModel
		get() = dataScroller.scrollModel

	fun rendererFactory(value: ItemRendererContext<VerticalLayoutData>.() -> ListItemRenderer<E>) {
		dataScroller.rendererFactory(value)
	}

	/**
	 * Sets the nullRenderer factory for this list. The nullRenderer factory is responsible for creating nullRenderers
	 * to be used in this list.
	 */
	fun nullRendererFactory(value: ItemRendererContext<VerticalLayoutData>.() -> ListRenderer) {
		dataScroller.nullRendererFactory(value)
	}

	private var dataBinding: Disposable? = null
		set(value) {
			if (field == value) return
			field?.dispose()
			field = value
		}

	val data: List<E?>
		get() = dataScroller.data

	fun data(value: List<E?>?) {
		dataBinding = null
		dataScroller.data(value)
		setSelectedItemFromText()
	}

	fun data(value: ObservableList<E?>?) {
		dataScroller.data(value)
		dataBinding = bind(value) {
			setSelectedItemFromText()
		}
	}

	private fun setSelectedItemFromText() {
		val item = textToItem(text)
		dataScroller.selection.selectedItem = item
		if (item != null) {
			dataScroller.highlighted.selectedItem = item
			scrollTo(item)
		}
	}

	fun emptyListRenderer(value: ItemRendererContext<VerticalLayoutData>.() -> UiComponent) {
		dataScroller.emptyListRenderer(value)
	}

	init {
		isFocusContainer = true
		focusEnabled = true
		focusDelegate = textInput
		textInput.focusHighlightDelegate = this

		styleTags.add(OptionList)
		maxItems = 10
		addChild(textInput)

		keyDown().add(::keyDownHandler)

		click().add {
			if (autoOpen) {
				if (it.target == downArrow) toggleOpen() else open()
			}
		}

		watch(style) {
			background?.dispose()
			background = addOptionalChild(0, it.background(this))

			downArrow?.dispose()
			val downArrow = addChild(it.downArrow(this))
			downArrow.focusEnabled = false
			downArrow.interactivityMode = if (editable) InteractivityMode.ALL else InteractivityMode.NONE
			downArrow.click().add { e ->
				if (!e.handled) {
					e.handled = true
					if (!autoOpen)
						toggleOpen()
				}
			}
			this.downArrow = downArrow
		}

		click().add {
			if (!it.handled && !editable) {
				it.handled = true
				toggleOpen()
			}
		}

		blurred().add {
			close()
			_changed.dispatch()
		}
	}

	private fun keyDownHandler(event: KeyInteractionRo) {
		if (event.defaultPrevented()) return
		when (event.keyCode) {
			Ascii.ESCAPE -> {
				if (isOpen) {
					event.handled = true
					event.preventDefault() // Prevent focus manager from setting focus back to the stage.
					focus()
					close()
				}
			}
			Ascii.RETURN, Ascii.ENTER -> {
				event.handled = true
				val highlighted = dataScroller.highlighted.selectedItem
				if (highlighted != null) {
					// An item was highlighted.
					selectedItem = highlighted
					focus()
				} else {
					// Text was typed. Try to match an item from the data.
					val typedItem = textToItem(text)
					dataScroller.selection.selectedItem = typedItem
					if (typedItem != null) textInput.text = formatter.format(typedItem)
				}
				close()
				focusManager.highlightFocused()
				_changed.dispatch()
			}
			Ascii.DOWN -> {
				event.handled = true
				open()
				highlightNext(1)
			}
			Ascii.UP -> {
				event.handled = true
				highlightPrevious(1)
			}
			Ascii.PAGE_DOWN -> {
				event.handled = true
				highlightNext((data.size - dataScroller.scrollMax).toInt())
			}
			Ascii.PAGE_UP -> {
				event.handled = true
				highlightPrevious((data.size - dataScroller.scrollMax).toInt())
			}
			Ascii.HOME -> {
				event.handled = true
				highlightFirst()
			}
			Ascii.END -> {
				event.handled = true
				highlightLast()
			}
		}
	}

	private fun highlightNext(delta: Int) {
		if (delta <= 0) return
		if (!isOpen) return
		val highlighted = dataScroller.highlighted.selectedItem
		val selectedIndex = if (highlighted == null) -1 else data.indexOf(highlighted)
		val nextIndex = minOf(data.lastIndex, selectedIndex + delta)
		val nextIndexNotNull = data.indexOfFirst(nextIndex) { it != null }
		if (nextIndexNotNull != -1) {
			dataScroller.highlighted.selectedItem = data[nextIndexNotNull]
			scrollTo(nextIndexNotNull.toFloat())
			dataScroller.focusSelf()
		}
	}

	private fun highlightPrevious(delta: Int) {
		if (delta <= 0) return
		if (!isOpen) return
		val highlighted = dataScroller.highlighted.selectedItem
		val selectedIndex = if (highlighted == null) data.size else data.indexOf(highlighted)
		val previousIndex = maxOf(0, selectedIndex - delta)
		val previousIndexNotNull = data.indexOfLast(previousIndex) { it != null }
		if (previousIndexNotNull != -1) {
			dataScroller.highlighted.selectedItem = data[previousIndexNotNull]
			scrollTo(previousIndexNotNull.toFloat())
			dataScroller.focusSelf()
		}
	}

	private fun highlightLast() {
		if (!isOpen) return
		val lastIndexNotNull = data.indexOfLast { it != null }
		if (lastIndexNotNull != -1) {
			dataScroller.highlighted.selectedItem = data[lastIndexNotNull]
			scrollTo(lastIndexNotNull.toFloat())
			dataScroller.focusSelf()
		}
	}

	private fun highlightFirst() {
		if (!isOpen) return
		val firstIndexNotNull = data.indexOfFirst { it != null }
		if (firstIndexNotNull != -1) {
			dataScroller.highlighted.selectedItem = data[firstIndexNotNull]
			scrollTo(firstIndexNotNull.toFloat())
			dataScroller.focusSelf()
		}
	}

	private fun scrollTo(item: E) {
		scrollTo(data.indexOf(item).toFloat())
	}

	/**
	 * Scrolls the minimum distance to show the given bounding rectangle.
	 */
	private fun scrollTo(index: Float) {
		if (index < scrollModel.value)
			scrollModel.value = index
		val pageSize = data.size - dataScroller.scrollMax
		if (index + 1f > scrollModel.value + pageSize)
			scrollModel.value = index + 1f - pageSize
	}

	var isOpen: Boolean = false
		set(value) {
			if (field != value) {
				field = value
				if (value) {
					dataScroller.highlighted.clear()
					listLift.priority = inject(PopUpManager).currentPopUps.lastOrNull()?.priority ?: 0f
					addChild(listLift)
					textInput.focusSelf()
				} else {
					removeChild(listLift)
				}
				_isOpenChanged.dispatch()
			}
		}

	fun open() {
		isOpen = true
	}

	fun close() {
		isOpen = false
	}

	fun toggleOpen() {
		isOpen = !isOpen
	}

	var text: String
		get() = textInput.text
		set(value) {
			textInput.text = value
		}

	var listWidth: Float? by validationProp(null, ValidationFlags.LAYOUT)
	var listHeight: Float? by validationProp(null, ValidationFlags.LAYOUT)

	override fun updateLayout(explicitWidth: Float?, explicitHeight: Float?, out: Bounds) {
		val pad = style.padding
		val w = pad.reduceWidth(explicitWidth)
		val h = pad.reduceHeight(explicitHeight)
		val downArrow = downArrow!!
		downArrow.cursor(StandardCursor.HAND)
		textInput.size(if (w == null) null else w - style.hGap - downArrow.width, h)
		textInput.position(pad.left, pad.top)
		downArrow.position(pad.left + textInput.width + style.hGap, pad.top + (textInput.height - downArrow.height) * 0.5f)
		out.set(pad.expandWidth(textInput.width + style.hGap + downArrow.width), pad.expandHeight(maxOf(textInput.height, downArrow.height)), textInput.baselineY)
		background?.size(out.width, out.height)
		listLift.size(listWidth ?: out.width, listHeight)
		listLift.position(0f, out.height + style.vGap)
	}

	override fun clear() {
		selectedItem = null
	}

	override fun dispose() {
		dataBinding = null
		close()
		super.dispose()
	}

	companion object : StyleTag
}

class OptionListStyle : StyleBase() {
	override val type: StyleType<OptionListStyle> = OptionListStyle

	/**
	 * The background of the text input / down arrow area.
	 * Skins should ensure the text input doesn't have a background.
	 */
	var background by prop(noSkinOptional)

	/**
	 * The padding between the background and the text input / down arrow area.
	 */
	var padding by prop(Pad(0f))

	/**
	 * The button that opens this list.
	 */
	var downArrow by prop(noSkin)

	/**
	 * The gap between the down arrow and the text field.
	 */
	var hGap by prop(2f)

	/**
	 * The gap between the option list background and the data scroller.
	 */
	var vGap by prop(0f)

	companion object : StyleType<OptionListStyle>
}

fun <E : Any> Context.optionList(
		init: ComponentInit<OptionList<E>> = {}): OptionList<E> {
	val t = OptionList<E>(this)
	t.init()
	return t
}

inline fun <E : Any> Context.optionList(
		data: ObservableList<E?>,
		noinline rendererFactory: OptionListRendererFactory<E> = { simpleItemRenderer() },
		init: ComponentInit<OptionList<E>> = {}): OptionList<E> {
	contract { callsInPlace(init, InvocationKind.EXACTLY_ONCE) }
	val t = OptionList<E>(this)
	t.data(data)
	t.rendererFactory(rendererFactory)
	t.init()
	return t
}

inline fun <E : Any> Context.optionList(
		data: List<E?>,
		noinline rendererFactory: OptionListRendererFactory<E> = { simpleItemRenderer() },
		init: ComponentInit<OptionList<E>> = {}): OptionList<E> {
	contract { callsInPlace(init, InvocationKind.EXACTLY_ONCE) }
	val t = OptionList<E>(this)
	t.data(data)
	t.rendererFactory(rendererFactory)
	t.init()
	return t
}

typealias OptionListRendererFactory<E> = ItemRendererContext<VerticalLayoutData>.() -> ListItemRenderer<E>

fun <E : Any> OptionList<E>.sortedByInput(data: List<E?>, ignoreCase: Boolean = true): ListView<E?> {
	val listView = ListView(data)
	listView.sortComparator = compareBy<E?>({ it != null }, {
		formatter.format(it!!).indexOf(text, ignoreCase = ignoreCase)
	})::compare
	input.add(listView::dirty)
	return listView
}