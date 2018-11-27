///*
// * Copyright 2015 Nicholas Bilyk
// *
// * Licensed under the Apache License, Version 2.0 (the "License");
// * you may not use this file except in compliance with the License.
// * You may obtain a copy of the License at
// *
// *     http://www.apache.org/licenses/LICENSE-2.0
// *
// * Unless required by applicable law or agreed to in writing, software
// * distributed under the License is distributed on an "AS IS" BASIS,
// * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// * See the License for the specific language governing permissions and
// * limitations under the License.
// */
//
//package com.acornui.component
//
//import com.acornui.collection.*
//import com.acornui.component.layout.DataScrollerStyle
//import com.acornui.component.layout.ListItemRenderer
//import com.acornui.component.layout.algorithm.GridLayoutData
//import com.acornui.component.layout.algorithm.GridLayoutStyle
//import com.acornui.component.layout.algorithm.LayoutDataProvider
//import com.acornui.component.layout.algorithm.virtual.ItemRendererOwner
//import com.acornui.component.layout.algorithm.virtual.VirtualVerticalLayoutStyle
//import com.acornui.component.style.*
//import com.acornui.component.text.selectable
//import com.acornui.component.text.textInput
//import com.acornui.core.di.Owned
//import com.acornui.core.di.inject
//import com.acornui.core.di.own
//import com.acornui.core.di.owns
//import com.acornui.core.focus.FocusManager
//import com.acornui.core.focus.focus
//import com.acornui.core.input.interaction.KeyInteractionRo
//import com.acornui.core.input.interaction.click
//import com.acornui.core.input.keyDown
//import com.acornui.core.popup.PopUpManager
//import com.acornui.core.popup.lift
//import com.acornui.core.text.*
//import com.acornui.core.time.DateRo
//import com.acornui.math.Bounds
//import com.acornui.math.Pad
//import com.acornui.reflect.observable
//import com.acornui.signal.Signal
//import com.acornui.signal.Signal0
//
//
//// TODO: open inline mode.
//
//open class DatePicker(
//		owner: Owned
//) : ContainerImpl(owner), Clearable {
//
//	private val _input = own(Signal0())
//
//	/**
//	 * Dispatched on each input character.
//	 * This does not dispatch when selecting a date from the picker.
//	 */
//	val input: Signal<() -> Unit>
//		get() = _input
//
//	private val _changed = own(Signal0())
//
//	/**
//	 * Dispatched on value commit.
//	 * It is dispatched when the user selects a date, or commits the value of the text input. It is not dispatched
//	 * when the selected item or text is programmatically changed.
//	 */
//	val changed: Signal<() -> Unit>
//		get() = _changed
//
//	/**
//	 * The formatter to be used when converting a date element to a string.
//	 */
//	var formatter: StringFormatter<DateRo> = dateFormatter()
//
//	/**
//	 * The parser to be used when converting a string to a date.
//	 */
//	var parser: StringParser<DateRo> = dateParser()
//
//	/**
//	 * Sets the currently selected date.
//	 * Note that this does not invoke [input] or [changed] signals.
//	 */
//	var selectedDate: DateRo?
//		get() = calendar.selection.selectedDate
//		set(value) {
//			calendar.selection.selectedDate = value
//			textInput.text = if (value == null) "" else formatter.format(value)
//		}
//
//	private val textInput = textInput {
//		input.add {
//			open()
//			setSelectedDateFromText()
//			_input.dispatch()
//		}
//	}
//
//	var editable: Boolean by observable(true) {
//		textInput.editable = it
//		textInput.selectable = it
//	}
//
//	private var background: UiComponent? = null
//	private var downArrow: UiComponent? = null
//	private val calendar = calendar()
//
//	private val listLift = lift {
//		focus = false
//		+calendar layout { fill() }
//		onClosed = {
//			close()
//		}
//	}
//
//	val style = bind(DatePickerStyle())
//
//	val calendarStyle: PanelStyle
//		get() = calendar.style
//
//	val calendarLayoutStyle: GridLayoutStyle
//		get() = calendar.layoutStyle
//
//	/**
//	 * Filters the possible dates.
//	 */
//	var filter: Filter<DateRo>?
//		get() = dataView.filter
//		set(value) {
//			dataView.filter = value
//		}
//
//	fun rendererFactory(value: ItemRendererOwner<GridLayoutData>.() -> ListItemRenderer<DateRo>) {
//		calendar.rendererFactory(value)
//	}
//
//	private fun setSelectedDateFromText() {
//		val date = parser.parse(text)
//		calendar.selection.selectedDate = item
//		if (item != null)
//			calendar.highlighted.selectedDate = item
//	}
//
//	init {
//		isFocusContainer = true
//		focusEnabled = true
//
//		styleTags.add(DatePicker)
//		addChild(textInput)
//
//		keyDown().add(this::keyDownHandler)
//
//		watch(style) {
//			background?.dispose()
//			background = addOptionalChild(0, it.background(this))
//
//			downArrow?.dispose()
//			val downArrow = addChild(it.downArrow(this))
//			downArrow.focusEnabled = true
//			downArrow.click().add {
//				// Using mouseDown instead of click because we close on blur (which is often via mouseDown).
//				if (!it.handled) {
//					it.handled = true
//					toggleOpen()
//				}
//			}
//			downArrow.interactivityMode = if (_isOpen) InteractivityMode.NONE else InteractivityMode.ALL
//			this.downArrow = downArrow
//		}
//
//		click().add {
//			if (!it.handled && !editable) {
//				it.handled = true
//				toggleOpen()
//			}
//		}
//
//		inject(FocusManager).focusedChanged.add(this::focusChangedHandler)
//	}
//
//	private fun focusChangedHandler(old: UiComponentRo?, new: UiComponentRo?) {
//		if (owns(old) && !owns(new)) {
//			close()
//			_changed.dispatch()
//		}
//	}
//
//	private fun keyDownHandler(event: KeyInteractionRo) {
////		if (event.defaultPrevented()) return
////		when (event.keyCode) {
////			Ascii.ESCAPE -> {
////				event.handled = true
////				event.preventDefault() // Prevent focus manager from setting focus back to the stage.
////				focus()
////				close()
////			}
////			Ascii.RETURN, Ascii.ENTER -> {
////				event.handled = true
////				val newSelectedDate = dataScroller.highlighted.selectedItem ?: parser.parse(text)
////				if (newSelectedDate != selectedDate) {
////					selectedDate = newSelectedDate
////					focus()
////					close()
////				}
////				_changed.dispatch()
////			}
////			Ascii.DOWN -> {
////				event.handled = true
////				if (!_isOpen)
////					open()
////				highlightNext(1)
////			}
////			Ascii.UP -> {
////				event.handled = true
////				highlightPrevious(1)
////			}
////			Ascii.PAGE_DOWN -> {
////				event.handled = true
////				highlightNext((data.size - dataScroller.scrollMax).toInt())
////			}
////			Ascii.PAGE_UP -> {
////				event.handled = true
////				highlightPrevious((data.size - dataScroller.scrollMax).toInt())
////			}
////			Ascii.HOME -> {
////				event.handled = true
////				highlightFirst()
////			}
////			Ascii.END -> {
////				event.handled = true
////				highlightLast()
////			}
////		}
//	}
//
//	private fun highlightNext(delta: Int) {
//		if (delta <= 0) return
//		if (!_isOpen) return
//		val highlighted = calendar.highlighted.selectedDate
//		val selectedIndex = if (highlighted == null) -1 else data.indexOf(highlighted)
//		val nextIndex = minOf(data.lastIndex, selectedIndex + delta)
//		val nextIndexNotNull = data.indexOfFirst2(nextIndex) { it != null }
//		if (nextIndexNotNull != -1) {
//			calendar.highlighted.selectedDate = data[nextIndexNotNull]
//			scrollTo(nextIndexNotNull.toFloat())
//			calendar.focus()
//		}
//	}
//
//	private fun highlightPrevious(delta: Int) {
//		if (delta <= 0) return
//		if (!_isOpen) return
//		val highlighted = calendar.highlighted.selectedDate
//		val selectedIndex = if (highlighted == null) data.size else data.indexOf(highlighted)
//		val previousIndex = maxOf(0, selectedIndex - delta)
//		val previousIndexNotNull = data.indexOfLast2(previousIndex) { it != null }
//		if (previousIndexNotNull != -1) {
//			calendar.highlighted.selectedDate = data[previousIndexNotNull]
//			scrollTo(previousIndexNotNull.toFloat())
//			calendar.focus()
//		}
//	}
//
//	private fun highlightLast() {
//		if (!_isOpen) return
//		val lastIndexNotNull = data.indexOfLast2 { it != null }
//		if (lastIndexNotNull != -1) {
//			calendar.highlighted.selectedDate = data[lastIndexNotNull]
//			scrollTo(lastIndexNotNull.toFloat())
//			calendar.focus()
//		}
//	}
//
//	private fun highlightFirst() {
//		if (!_isOpen) return
//		val firstIndexNotNull = data.indexOfFirst2 { it != null }
//		if (firstIndexNotNull != -1) {
//			calendar.highlighted.selectedDate = data[firstIndexNotNull]
//			scrollTo(firstIndexNotNull.toFloat())
//			calendar.focus()
//		}
//	}
//
//	private var _isOpen = false
//
//	fun open() {
//		if (_isOpen) return
//		_isOpen = true
//		calendar.highlighted.clear()
//		listLift.priority = inject(PopUpManager).currentPopUps.lastOrNull()?.priority ?: 0f
//		addChild(listLift)
//		textInput.focus()
//	}
//
//	fun close() {
//		if (!_isOpen) return
//		_isOpen = false
//		removeChild(listLift)
//	}
//
//	fun toggleOpen() {
//		if (_isOpen) close()
//		else open()
//	}
//
//	var text: String
//		get() = textInput.text
//		set(value) {
//			textInput.text = value
//		}
//
//	var listWidth: Float? by validationProp(null, ValidationFlags.LAYOUT)
//	var listHeight: Float? by validationProp(null, ValidationFlags.LAYOUT)
//
//	override fun updateLayout(explicitWidth: Float?, explicitHeight: Float?, out: Bounds) {
//		val pad = style.padding
//		val w = pad.reduceWidth(explicitWidth)
//		val h = pad.reduceHeight(explicitHeight)
//		val downArrow = this.downArrow!!
//		textInput.setSize(if (w == null) null else w - style.gap - downArrow.width, h)
//		textInput.setPosition(pad.left, pad.top)
//		downArrow.moveTo(pad.left + textInput.width + style.gap, pad.top + (textInput.height - downArrow.height) * 0.5f)
//		out.set(pad.expandWidth2(textInput.width + style.gap + downArrow.width), pad.expandHeight2(maxOf(textInput.height, downArrow.height)))
//		background?.setSize(out.width, out.height)
//
//		listLift.setSize(listWidth ?: out.width, listHeight)
//		listLift.moveTo(0f, out.height)
//	}
//
//	override fun clear() {
//		textInput.clear()
//		selectedDate = null
//	}
//
//	override fun dispose() {
//		inject(FocusManager).focusedChanged.remove(this::focusChangedHandler)
//		close()
//		super.dispose()
//	}
//
//	companion object : StyleTag
//}
//
//class DatePickerStyle : StyleBase() {
//	override val type: StyleType<DatePickerStyle> = DatePickerStyle
//
//	/**
//	 * The background of the text input / down arrow area.
//	 * Skins should ensure the text input doesn't have a background.
//	 */
//	var background by prop(noSkinOptional)
//
//	/**
//	 * The padding between the background and the text input / down arrow area.
//	 */
//	var padding by prop(Pad(0f))
//
//	var downArrow by prop(noSkin)
//
//	/**
//	 * The gap between the down arrow and the text field.
//	 */
//	var gap by prop(2f)
//
//	companion object : StyleType<DatePickerStyle>
//}
//
//fun Owned.datePicker(
//		init: ComponentInit<DatePicker> = {}): DatePicker {
//	val t = DatePicker(this)
//	t.init()
//	return t
//}
//
//fun <E : Any> Owned.datePicker(
//		rendererFactory: LayoutDataProvider<GridLayoutData>.() -> ListItemRenderer<DateRo> = { simpleItemRenderer() },
//		init: ComponentInit<DatePicker> = {}): DatePicker {
//	val t = DatePicker(this)
//	t.rendererFactory(rendererFactory)
//	t.init()
//	return t
//}