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

package com.acornui.gl.component

import com.acornui.component.*
import com.acornui.component.scroll.*
import com.acornui.core.di.Owned
import com.acornui.core.floor
import com.acornui.core.input.interaction.WheelInteraction
import com.acornui.core.input.wheel
import com.acornui.math.Bounds

/**
 * A container with scrolling.
 */
open class GlScrollArea(
		owner: Owned
) : ElementContainerImpl<UiComponent>(owner), ScrollArea {

	override final val style = bind(ScrollAreaStyle())

	private val scrollRect = scrollRect()

	private val contents = scrollRect.addElement(stack())

	override val stackStyle: StackLayoutStyle
		get() = contents.style

	private val hScrollBar = HScrollBar(this)
	private val vScrollBar = VScrollBar(this)
	private var corner: UiComponent? = null

	override final val hScrollModel: ClampedScrollModel
		get() = hScrollBar.scrollModel

	override final val vScrollModel: ClampedScrollModel
		get() = vScrollBar.scrollModel

	private var _tossScrolling = false
	override var hScrollPolicy: ScrollPolicy by validationProp(ScrollPolicy.AUTO, ValidationFlags.LAYOUT)
	override var vScrollPolicy: ScrollPolicy by validationProp(ScrollPolicy.AUTO, ValidationFlags.LAYOUT)

	private val wheelHandler = {
		event: WheelInteraction ->
		vScrollModel.value += event.deltaY
		hScrollModel.value += event.deltaX
	}

	private var tossScroller: TossScroller? = null
	private var tossBinding: TossScrollModelBinding? = null

	private var tossScrolling: Boolean
		get() = _tossScrolling
		set(value) {
			if (_tossScrolling == value) return
			_tossScrolling = value
			if (value) {
				tossScroller = TossScroller(this)
				tossBinding = TossScrollModelBinding(tossScroller!!, hScrollModel, vScrollModel)
			} else {
				tossScroller?.dispose()
				tossScroller = null
				tossBinding?.dispose()
				tossBinding = null
			}
		}

	private val scrollChangedHandler = {
		_: ScrollModelRo ->
		invalidate(ScrollArea.SCROLLING)
		Unit
	}

	init {
		styleTags.add(ScrollArea)
		validation.addNode(ScrollArea.SCROLLING, ValidationFlags.LAYOUT, this::validateScroll)

		hScrollBar.includeInLayout = false
		vScrollBar.includeInLayout = false
		styleTags.add(ScrollArea.HBAR_STYLE)
		styleTags.add(ScrollArea.VBAR_STYLE)

		scrollRect.wheel().add(wheelHandler)
		addChild(scrollRect)

		addChild(hScrollBar)
		addChild(vScrollBar)

		hScrollModel.changed.add(scrollChangedHandler)
		vScrollModel.changed.add(scrollChangedHandler)

		watch(style) {
			tossScrolling = it.tossScrolling
			scrollRect.borderRadius = it.borderRadius

			corner?.dispose()
			corner = it.corner(this)
			addChildAfter(corner!!, vScrollBar)
		}
	}

	override fun onElementAdded(index: Int, element: UiComponent) {
		contents.addElement(index, element)
	}

	override fun onElementRemoved(index: Int, element: UiComponent) {
		contents.removeElement(element)
	}

	override val contentsWidth: Float
		get() {
			validate(ValidationFlags.LAYOUT)
			return scrollRect.contentsWidth
		}

	override val contentsHeight: Float
		get() {
			validate(ValidationFlags.LAYOUT)
			return scrollRect.contentsHeight
		}

	override fun updateLayout(explicitWidth: Float?, explicitHeight: Float?, out: Bounds) {
		val requireHScrolling = hScrollPolicy == ScrollPolicy.ON && explicitWidth != null
		val allowHScrolling = hScrollPolicy != ScrollPolicy.OFF && explicitWidth != null
		val requireVScrolling = vScrollPolicy == ScrollPolicy.ON && explicitHeight != null
		val allowVScrolling = vScrollPolicy != ScrollPolicy.OFF && explicitHeight != null

		if (!(requireHScrolling || requireVScrolling)) {
			// Size target without scrolling.
			contents.setSize(explicitWidth, explicitHeight)
			println("Contents Size ${contents.bounds}")
		}
		var needsHScrollBar = allowHScrolling && (requireHScrolling || contents.width > explicitWidth!!)
		var needsVScrollBar = allowVScrolling && (requireVScrolling || contents.height > explicitHeight!!)
		val vScrollBarW = vScrollBar.minWidth ?: 0f
		val hScrollBarH = hScrollBar.minHeight ?: 0f

		if (needsHScrollBar && needsVScrollBar) {
			// Needs both scroll bars.
			contents.setSize(explicitWidth!! - vScrollBarW, explicitHeight!! - hScrollBarH)
		} else if (needsHScrollBar) {
			// Needs horizontal scroll bar.
			contents.setSize(explicitWidth, if (explicitHeight == null) null else explicitHeight - hScrollBarH)
			needsVScrollBar = allowVScrolling && (requireVScrolling || contents.height > contents.explicitHeight!!)
			if (needsVScrollBar) {
				// Adding the horizontal scroll bar causes the vertical scroll bar to be needed.
				contents.setSize(explicitWidth!! - vScrollBarW, explicitHeight!! - hScrollBarH)
			}
		} else if (needsVScrollBar) {
			// Needs vertical scroll bar.
			contents.setSize(if (explicitWidth == null) null else explicitWidth - vScrollBarW, explicitHeight)
			needsHScrollBar = allowHScrolling && (requireHScrolling || contents.width > contents.explicitWidth!!)
			if (needsHScrollBar) {
				// Adding the vertical scroll bar causes the horizontal scroll bar to be needed.
				contents.setSize(explicitWidth!! - vScrollBarW, explicitHeight!! - hScrollBarH)
			}
		}
		scrollRect.setSize(contents.explicitWidth, contents.explicitHeight)

		// Set the content mask to the explicit size of the contents stack, or the measured size if there was no bound.
		val contentsSetW = scrollRect.explicitWidth ?: contents.width
		val contentsSetH = scrollRect.explicitHeight ?: contents.height
		scrollRect.maskSize(contentsSetW, contentsSetH)
		val vScrollBarW2 = if (needsVScrollBar) vScrollBarW else 0f
		val hScrollBarH2 = if (needsHScrollBar) hScrollBarH else 0f

		out.set(explicitWidth ?: scrollRect.contentsWidth + vScrollBarW2, explicitHeight ?: scrollRect.contentsHeight + hScrollBarH2)

		// Update the scroll models and scroll bar sizes.
		if (needsHScrollBar) {
			hScrollBar.visible = true
			hScrollBar.setSize(explicitWidth!! - vScrollBarW2, hScrollBarH)
			hScrollBar.moveTo(0f, out.height - hScrollBarH)
			hScrollBar.setScaling(minOf(1f, hScrollBar.explicitWidth!! / hScrollBar.width), 1f)
		} else {
			hScrollBar.visible = false
		}
		if (needsVScrollBar) {
			vScrollBar.visible = true
			vScrollBar.setSize(vScrollBarW, explicitHeight!! - hScrollBarH2)
			vScrollBar.moveTo(out.width - vScrollBarW, 0f)
			vScrollBar.setScaling(1f, minOf(1f, vScrollBar.explicitHeight!! / vScrollBar.height))
		} else {
			vScrollBar.visible = false
		}
		val corner = corner!!
		if (needsHScrollBar && needsVScrollBar) {
			corner.setSize(vScrollBarW, hScrollBarH)
			corner.moveTo(explicitWidth!! - vScrollBarW, explicitHeight!! - hScrollBarH)
			corner.visible = true
		} else {
			corner.visible = false
		}

		hScrollModel.max = maxOf(0f, scrollRect.contentsWidth - contentsSetW)
		vScrollModel.max = maxOf(0f, scrollRect.contentsHeight - contentsSetH)

		scrollRect.getAttachment<TossScroller>(TossScroller)?.enabled = needsHScrollBar || needsVScrollBar
	}

	protected fun validateScroll() {
		scrollRect.scrollTo(hScrollModel.value.floor(), vScrollModel.value.floor())
	}

	override fun dispose() {
		super.dispose()
		hScrollModel.changed.remove(scrollChangedHandler)
		vScrollModel.changed.remove(scrollChangedHandler)
		tossScrolling = false
	}
}