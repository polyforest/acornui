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

@file:Suppress("UNUSED_PARAMETER")

package com.acornui.js.dom.component

import com.acornui.collection.firstOrNull2
import com.acornui.component.*
import com.acornui.component.layout.algorithm.FlowHAlign
import com.acornui.component.layout.setSize
import com.acornui.component.scroll.ClampedScrollModel
import com.acornui.component.scroll.ScrollPolicy
import com.acornui.component.text.*
import com.acornui.core.di.Owned
import com.acornui.core.di.inject
import com.acornui.core.di.own
import com.acornui.core.focus.focused
import com.acornui.core.input.keyDown
import com.acornui.core.input.keyUp
import com.acornui.core.selection.SelectionManager
import com.acornui.core.selection.SelectionRange
import com.acornui.math.Bounds
import com.acornui.signal.Signal
import com.acornui.signal.Signal0
import org.w3c.dom.HTMLDivElement
import org.w3c.dom.HTMLElement
import org.w3c.dom.HTMLInputElement
import org.w3c.dom.HTMLTextAreaElement
import kotlin.browser.document

open class DomTextField(
		owner: Owned,
		protected val element: HTMLElement = document.createElement("div") as HTMLDivElement,
		domContainer: DomContainer = DomContainer(element)
) : ContainerImpl(owner, domContainer), TextField {

	final override val charStyle = bind(CharStyle())
	final override val flowStyle = bind(TextFlowStyle())

	// TODO: DomTextSelection

	init {
		styleTags.add(TextField)
		nativeAutoSize = false
		element.style.apply {
			overflowX = "hidden"
			overflowY = "hidden"
		}
		watch(charStyle) {
			it.applyCss(element)
		}
		watch(flowStyle) {
			it.applyCss(element)
		}
	}

	override fun onAncestorVisibleChanged(uiComponent: UiComponent, value: Boolean) {
		invalidate(ValidationFlags.LAYOUT)
	}

	override var text: String
		get() {
			return element.textContent ?: ""
		}
		set(value) {
			if (element.textContent == value) return
			element.textContent = value
			invalidate(ValidationFlags.LAYOUT)
		}

	override var htmlText: String?
		get() = element.innerHTML
		set(value) {
			element.innerHTML = value ?: ""
			invalidate(ValidationFlags.LAYOUT)
		}

	override fun updateLayout(explicitWidth: Float?, explicitHeight: Float?, out: Bounds) {
		if (!flowStyle.multiline || explicitWidth == null) {
			element.style.whiteSpace = "nowrap"
		} else {
			element.style.whiteSpace = "normal"
		}
		native.setSize(explicitWidth, explicitHeight)
		out.set(native.bounds)
	}

}

class DomTextInput(
		owner: Owned,
		val inputElement: HTMLInputElement = document.createElement("input") as HTMLInputElement
) : ContainerImpl(owner, DomContainer(inputElement)), TextInput {

	override val charStyle = bind(CharStyle())
	override val flowStyle = bind(TextFlowStyle())
	override val boxStyle = bind(BoxStyle())
	override val textInputStyle = bind(TextInputStyle())

	override var focusEnabled: Boolean = true
	override var focusOrder: Float = 0f
	override var highlight: UiComponent? by createSlot()

	private val _input = own(Signal0())
	override val input: Signal<()->Unit>
			get() = _input

	private val _changed = own(Signal0())
	override val changed: Signal<()->Unit>
		get() = _changed

	private var _editable: Boolean = true

	private val selectionManager = inject(SelectionManager)

	private fun selectionChangedHandler(old: List<SelectionRange>, new: List<SelectionRange>) {
		refreshSelection()
	}

	private fun refreshSelection() {
		if (!isActive) return // IE has a problem setting the selection range on elements not yet active.
		val new = selectionManager.selection.firstOrNull2 { it: SelectionRange -> it.target == this }
		try {
			inputElement.setSelectionRange(new?.startIndex ?: 0, new?.endIndex ?: 0)
		} catch (e: Throwable) {
			// IE can puke on setSelectionRange...
		}
	}

	override fun onAncestorVisibleChanged(uiComponent: UiComponent, value: Boolean) {
		invalidate(ValidationFlags.LAYOUT)
	}

	override var editable: Boolean
		get() = _editable
		set(value) {
			if (_editable == value) return
			_editable = value
			inputElement.readOnly = !value
		}

	private var _maxLength: Int? = null
	override var maxLength: Int?
		get() = _maxLength
		set(value) {
			if (_maxLength == value) return
			_maxLength = value
			if (value != null) {
				inputElement.maxLength = value
			} else {
				inputElement.removeAttribute("maxlength")
			}
		}

	init {
		styleTags.add(TextField)
		styleTags.add(TextInput)
		nativeAutoSize = false
		keyDown().add({ it.handled = true })
		keyUp().add({ it.handled = true })

		selectionManager.selectionChanged.add(this::selectionChangedHandler)

		inputElement.autofocus = false
		inputElement.tabIndex = 0
		inputElement.onchange = {
			restrict()
			_changed.dispatch()
		}
		inputElement.oninput = {
			restrict()
			_input.dispatch()
		}

		watch(charStyle) {
			it.applyCss(inputElement)
		}
		watch(flowStyle) {
			it.applyCss(inputElement)
		}
		watch(boxStyle) {
			it.applyCss(inputElement)
			it.applyBox(native as DomComponent)
		}
		focused().add(this::refreshSelection)
	}

	override fun onActivated() {
		super.onActivated()
		refreshSelection()
	}

	private fun restrict() {
		if (_restrict == null) return
		inputElement.value = inputElement.value.replace(_restrict!!, "")
	}

	override var text: String
		get() = inputElement.value
		set(value) {
			inputElement.value = value
			refreshSelection()
		}

	override var placeholder: String
		get() = inputElement.placeholder
		set(value) {
			inputElement.placeholder
		}

	private var _restrict: Regex? = null
	override var restrictPattern: String?
		get() = _restrict?.pattern
		set(value) {
			_restrict = if (value == null) null else Regex(value)
		}

	override var password: Boolean
		get() = inputElement.type == "password"
		set(value) {
			inputElement.type = if (value) "password" else "text"
		}

	@Suppress("RedundantSetter")
	override var allowTab: Boolean
		get() = false
		set(value) {
		}

	override fun updateLayout(explicitWidth: Float?, explicitHeight: Float?, out: Bounds) {
		native.setSize(explicitWidth ?: textInputStyle.defaultWidth, explicitHeight)
		out.set(native.bounds)
		highlight?.setSize(out)
	}

	override fun clear() {
		text = ""
	}

	override fun dispose() {
		super.dispose()
		inputElement.oninput = null
	}
}

class DomTextArea(
		owner: Owned,
		private val areaElement: HTMLTextAreaElement = document.createElement("textarea") as HTMLTextAreaElement
) : ContainerImpl(owner, DomContainer(areaElement)), TextArea {

	override val charStyle = bind(CharStyle())
	override val flowStyle = bind(TextFlowStyle())
	override val boxStyle = bind(BoxStyle())
	override val textInputStyle = bind(TextInputStyle())

	override var focusEnabled: Boolean = true
	override var focusOrder: Float = 0f
	override var highlight: UiComponent? by createSlot()

	private val _input = own(Signal0())
	override val input: Signal<()->Unit>
		get() = _input

	private val _changed = own(Signal0())
	override val changed: Signal<()->Unit>
		get() = _changed


	private var _editable: Boolean = true

	override var editable: Boolean
		get() = _editable
		set(value) {
			if (_editable == value) return
			_editable = value
			areaElement.readOnly = !value
		}

	private val _hScrollModel = own(DomScrollLeftModel(areaElement))
	override val hScrollModel: ClampedScrollModel
		get() = _hScrollModel

	private val _vScrollModel = own(DomScrollTopModel(areaElement))
	override val vScrollModel: ClampedScrollModel
		get() = _vScrollModel

	private var _hScrollPolicy = ScrollPolicy.AUTO
	override var hScrollPolicy: ScrollPolicy
		get() = _hScrollPolicy
		set(value) {
			areaElement.style.overflowX = when(value) {
				ScrollPolicy.AUTO -> "auto"
				ScrollPolicy.OFF -> "hidden"
				ScrollPolicy.ON -> "scroll"
			}
			invalidateLayout()
		}

	private var _vScrollPolicy = ScrollPolicy.AUTO
	override var vScrollPolicy: ScrollPolicy
		get() = _vScrollPolicy
		set(value) {
			areaElement.style.overflowY = when(value) {
				ScrollPolicy.AUTO -> "auto"
				ScrollPolicy.OFF -> "hidden"
				ScrollPolicy.ON -> "scroll"
			}
			invalidateLayout()
		}

	private val contentBounds = Bounds()
	override val contentsWidth: Float
		get() {
			validate(ValidationFlags.LAYOUT)
			return contentBounds.width
		}

	override val contentsHeight: Float
		get() {
			validate(ValidationFlags.LAYOUT)
			return contentBounds.height
		}

	init {
		styleTags.add(TextField)
		styleTags.add(TextArea)
		nativeAutoSize = false
		keyDown().add({ it.handled = true })

		areaElement.autofocus = false
		areaElement.tabIndex = 0
		areaElement.onchange = {
			restrict()
			_changed.dispatch()
		}
		areaElement.oninput = {
			restrict()
			_input.dispatch()
		}
		areaElement.style.resize = "none"

		watch(charStyle) {
			it.applyCss(areaElement)
		}
		watch(flowStyle) {
			it.applyCss(areaElement)
		}
		watch(boxStyle) {
			it.applyCss(areaElement)
			it.applyBox(native as DomComponent)
		}
		watch(textInputStyle) {
			invalidateLayout()
		}
	}

	private var _maxLength: Int? = null
	override var maxLength: Int?
		get() = _maxLength
		set(value) {
			if (_maxLength == value) return
			_maxLength = value
			if (value != null) {
				areaElement.maxLength = value
			} else {
				areaElement.removeAttribute("maxlength")
			}
		}

	override var placeholder: String
		get() = areaElement.placeholder
		set(value) {
			areaElement.placeholder
		}

	private var _restrict: Regex? = null
	override var restrictPattern: String?
		get() = _restrict?.pattern
		set(value) {
			_restrict = if (value == null) null else Regex(value)
		}

	@Suppress("RedundantSetter")
	override var password: Boolean
		get() = false
		set(value) {
		}

	@Suppress("RedundantSetter")
	override var allowTab: Boolean
		get() = false
		set(value) {
		}

	override fun onAncestorVisibleChanged(uiComponent: UiComponent, value: Boolean) {
		invalidate(ValidationFlags.LAYOUT)
	}

	override var text: String
		get() = areaElement.textContent ?: ""
		set(value) {
			areaElement.textContent = value
			invalidateLayout()
		}

	private fun restrict() {
		if (_restrict == null) return
		areaElement.value = areaElement.value.replace(_restrict!!, "")
	}

	override fun updateLayout(explicitWidth: Float?, explicitHeight: Float?, out: Bounds) {
		native.setSize(explicitWidth ?: textInputStyle.defaultWidth, explicitHeight)
		out.set(native.bounds)
		highlight?.setSize(out)
		contentBounds.set(boxStyle.margin.expandWidth2(areaElement.offsetWidth.toFloat()), boxStyle.margin.expandHeight2(areaElement.offsetHeight.toFloat()))
	}

	override fun clear() {
		text = ""
	}

	override fun dispose() {
		super.dispose()
		areaElement.oninput = null
	}

}

fun TextFlowStyle.applyCss(element: HTMLElement) {
//	element.style.verticalAlign = when (verticalAlign) {
//
//	}
	element.style.textAlign = when (horizontalAlign) {
		FlowHAlign.LEFT -> "left"
		FlowHAlign.CENTER -> "center"
		FlowHAlign.RIGHT -> "right"
		FlowHAlign.JUSTIFY -> "justify"
	}
}

fun CharStyle.applyCss(element: HTMLElement) {
	element.style.apply {
		fontFamily = face
		fontSize = "${size}px"
		fontWeight = if (bold) "bold" else "normal"
		fontStyle = if (italic) "italic" else "normal"
		textDecoration = if (underlined) "underline" else "none"
		color = colorTint.toCssString()

		val selectable = selectable
		userSelect(selectable)
		cursor = if (selectable) "text" else "default"
	}
}
