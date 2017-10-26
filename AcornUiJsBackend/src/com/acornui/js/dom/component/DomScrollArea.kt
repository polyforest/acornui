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

package com.acornui.js.dom.component

import com.acornui.component.*
import com.acornui.component.scroll.*
import com.acornui.core.Disposable
import com.acornui.core.di.Owned
import com.acornui.math.Bounds
import com.acornui.math.Matrix4Ro
import com.acornui.signal.Signal
import com.acornui.signal.Signal1
import org.w3c.dom.HTMLElement
import org.w3c.dom.events.Event
import kotlin.properties.Delegates
import kotlin.properties.ReadWriteProperty

class DomScrollArea(
		owner: Owned,
		native: DomContainer = DomContainer()
) : ElementContainerImpl<UiComponent>(owner, native), ScrollArea {

	private val element = native.element
	override val style = bind(ScrollAreaStyle())

	private val _hScrollModel = DomScrollLeftModel(element)
	override val hScrollModel: ClampedScrollModel
		get() = _hScrollModel
	private val _vScrollModel = DomScrollTopModel(element)
	override val vScrollModel: ClampedScrollModel
		get() = _vScrollModel

	private val contents = addChild(StackLayoutContainer(owner, DomInlineContainer()))

	override var hScrollPolicy: ScrollPolicy by validationProp(ScrollPolicy.AUTO, ValidationFlags.LAYOUT)
	override var vScrollPolicy: ScrollPolicy by validationProp(ScrollPolicy.AUTO, ValidationFlags.LAYOUT)

	private val scrollChangedHandler: (scrollModel: ScrollModelRo) -> Unit = {
		invalidate(ScrollArea.SCROLLING)
		Unit
	}

	init {
		styleTags.add(ScrollArea)
		validation.addNode(ScrollArea.SCROLLING, ValidationFlags.LAYOUT, { validateScroll() })

		hScrollModel.changed.add(scrollChangedHandler)
		vScrollModel.changed.add(scrollChangedHandler)

		watch(style) {
			val bR = it.borderRadius
			element.style.apply {
				borderTopLeftRadius = "${bR.topLeft.x}px ${bR.topLeft.y}px"
				borderTopRightRadius = "${bR.topRight.x}px ${bR.topRight.y}px"
				borderBottomRightRadius = "${bR.bottomRight.x}px ${bR.bottomRight.y}px"
				borderBottomLeftRadius = "${bR.bottomLeft.x}px ${bR.bottomLeft.y}px"
			}

		}
	}

	//-----------------------------------------------------
	// ChildRo element delegation
	//-----------------------------------------------------

	override fun onElementAdded(index: Int, element: UiComponent) {
		contents.addElement(index, element)
	}

	override fun onElementRemoved(index: Int, element: UiComponent) {
		contents.removeElement(element)
	}

	override val contentsWidth: Float
		get() {
			validate(ValidationFlags.LAYOUT)
			return contents.width
		}

	override val contentsHeight: Float
		get() {
			validate(ValidationFlags.LAYOUT)
			return contents.height
		}

	//-----------------------------------------------------
	// Validation
	//-----------------------------------------------------

	override fun updateLayout(explicitWidth: Float?, explicitHeight: Float?, out: Bounds) {
		val requireHScrolling = hScrollPolicy == ScrollPolicy.ON && explicitWidth != null
		val allowHScrolling = hScrollPolicy != ScrollPolicy.OFF && explicitWidth != null
		val requireVScrolling = vScrollPolicy == ScrollPolicy.ON && explicitHeight != null
		val allowVScrolling = vScrollPolicy != ScrollPolicy.OFF && explicitHeight != null

		if (!(requireHScrolling || requireVScrolling)) {
			// Size target without scrolling.
			contents.setSize(explicitWidth, explicitHeight)
		}
		var needsHScrollBar = allowHScrolling && (requireHScrolling || contents.width > explicitWidth!!)
		var needsVScrollBar = allowVScrolling && (requireVScrolling || contents.height > explicitHeight!!)

		hScrollBarVisible(false)
		vScrollBarVisible(false)
		if (needsHScrollBar && needsVScrollBar) {
			// Needs both scroll bars.
			hScrollBarVisible(true)
			vScrollBarVisible(true)
			contents.setSize(explicitWidth!! - vScrollBarW(), explicitHeight!! - hScrollBarH())
		} else if (needsHScrollBar) {
			// Needs horizontal scroll bar.
			hScrollBarVisible(true)
			contents.setSize(explicitWidth, if (explicitHeight == null) null else explicitHeight - hScrollBarH())
			needsVScrollBar = allowVScrolling && (requireVScrolling || contents.height > contents.explicitHeight!!)
			if (needsVScrollBar) {
				// Adding the horizontal scroll bar causes the vertical scroll bar to be needed.
				vScrollBarVisible(true)
				contents.setSize(explicitWidth!! - vScrollBarW(), explicitHeight!! - hScrollBarH())
			}
		} else if (needsVScrollBar) {
			// Needs vertical scroll bar.
			vScrollBarVisible(true)
			contents.setSize(if (explicitWidth == null) null else explicitWidth - vScrollBarW(), explicitHeight)
			needsHScrollBar = allowHScrolling && (requireHScrolling || contents.width > contents.explicitWidth!!)
			if (needsHScrollBar) {
				// Adding the horizontal scroll bar causes the vertical scroll bar to be needed.
				hScrollBarVisible(true)
				contents.setSize(explicitWidth!! - vScrollBarW(), explicitHeight!! - hScrollBarH())
			}
		}

		// Set the content mask to the explicit size of the contents stack, or the measured size if there was no bound.
		val contentsW = contents.explicitWidth ?: contents.width
		val contentsH = contents.explicitHeight ?: contents.height

		// Update the scroll models and scroll bar sizes.
		_hScrollModel.max = maxOf(0f, contents.width - contentsW)
		_vScrollModel.max = maxOf(0f, contents.height - contentsH)
		out.set(explicitWidth ?: contents.width, explicitHeight ?: contents.height)
	}

	private fun vScrollBarW(): Float {
		val e = element
		return (e.offsetWidth - e.clientWidth).toFloat()
	}

	private fun hScrollBarH(): Float {
		val e = element
		return (e.offsetHeight - e.clientHeight).toFloat()
	}

	private fun hScrollBarVisible(value: Boolean) {
		element.style.overflowX = if (value) "scroll" else "hidden"
	}

	private fun vScrollBarVisible(value: Boolean) {
		element.style.overflowY = if (value) "scroll" else "hidden"
	}

	private fun validateScroll() {
		contents.moveTo(-hScrollModel.value, -vScrollModel.value)
	}

	override fun dispose() {
		super.dispose()
		_hScrollModel.dispose()
		_vScrollModel.dispose()
	}
}

class DomInlineContainer : DomContainer() {

	/**
	 * Overridden so that we can separate the css transform from the actual transform to account for scrolling.
	 */
	override fun setTransform(value: Matrix4Ro) {
	}

	/**
	 * Overridden so that we can separate the css transform from the actual transform to account for scrolling.
	 */
	override fun setSimpleTranslate(x: Float, y: Float) {
	}
}

abstract class DomScrollModelBase(protected val element: HTMLElement) : ClampedScrollModel, Disposable {

	/**
	 * Dispatched when the min, max, or value properties have changed.
	 */
	private val _changed = Signal1<ClampedScrollModelRo>()
	override val changed: Signal<(ClampedScrollModelRo) -> Unit>
		get() = _changed

	private fun <T> bindable(initial: T): ReadWriteProperty<Any?, T> {
		return Delegates.observable(initial, {
			meta, old, new ->
			if (old != new) _changed.dispatch(this)}
		)
	}

	override var min by bindable(0f)
	override var max by bindable(0f)
	override var snap by bindable(0f)

	private val elementScrollHandler = {
		event: Event ->
		_changed.dispatch(this)
	}

	init {
		element.addEventListener("scroll", elementScrollHandler)
	}

	override fun dispose() {
		element.removeEventListener("scroll", elementScrollHandler)
	}
}

class DomScrollTopModel(element: HTMLElement) : DomScrollModelBase(element) {

	override var rawValue: Float
		get() {
			return element.scrollTop.toFloat()
		}
		set(value) {
			element.scrollTop = value.toDouble()
		}
}

class DomScrollLeftModel(element: HTMLElement) : DomScrollModelBase(element) {

	override var rawValue: Float
		get() {
			return element.scrollLeft.toFloat()
		}
		set(value) {
			element.scrollLeft = value.toDouble()
		}
}