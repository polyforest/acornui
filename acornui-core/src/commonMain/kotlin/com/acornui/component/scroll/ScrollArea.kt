/*
 * Copyright 2019 PolyForest
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

package com.acornui.component.scroll

import com.acornui.component.*
import com.acornui.component.layout.algorithm.LayoutDataProvider
import com.acornui.component.style.*
import com.acornui.core.di.Owned
import com.acornui.core.floor
import com.acornui.core.input.interaction.WheelInteractionRo
import com.acornui.core.input.wheel
import com.acornui.core.time.callLater
import com.acornui.core.tween.Tween
import com.acornui.core.tween.createPropertyTween
import com.acornui.math.*

/**
 * A container with scrolling.
 */
open class ScrollArea(
		owner: Owned
) : ElementContainerImpl<UiComponent>(owner), LayoutDataProvider<StackLayoutData> {

	val style = bind(ScrollAreaStyle())

	override fun createLayoutData(): StackLayoutData = StackLayoutData()

	protected val scrollRect = scrollRect()

	protected val contents = scrollRect.addElement(stack())

	/**
	 * The interactivity mode of the contents.  This can be set to [InteractivityMode.CHILDREN] if the scroll area is
	 * an overlay.  Note that this will prevent toss scrolling or mouse wheel from working.
	 */
	var contentsInteractivityMode: InteractivityMode
		get() = interactivityMode
		set(value) {
			contents.interactivityMode = value
			scrollRect.interactivityMode = value
		}

	/**
	 * The layout for the contents stack.
	 */
	val stackStyle: StackLayoutStyle
		get() = contents.style

	private val hScrollBar = HScrollBar(this)
	private val vScrollBar = VScrollBar(this)
	private var corner: UiComponent? = null

	val hScrollModel: ClampedScrollModel
		get() = hScrollBar.scrollModel

	val vScrollModel: ClampedScrollModel
		get() = vScrollBar.scrollModel

	private var _tossScrolling = false
	var hScrollPolicy: ScrollPolicy by validationProp(ScrollPolicy.AUTO, ValidationFlags.LAYOUT)
	var vScrollPolicy: ScrollPolicy by validationProp(ScrollPolicy.AUTO, ValidationFlags.LAYOUT)

	private val wheelHandler = {
		event: WheelInteractionRo ->
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
		invalidate(SCROLLING)
		Unit
	}

	init {
		styleTags.add(ScrollArea)
		validation.addNode(SCROLLING, ValidationFlags.LAYOUT, this::validateScroll)

		styleTags.add(HBAR_STYLE)
		styleTags.add(VBAR_STYLE)

		scrollRect.wheel().add(wheelHandler)
		addChild(scrollRect)

		hScrollBar.layoutInvalidatingFlags = ValidationFlags.SIZE_CONSTRAINTS
		vScrollBar.layoutInvalidatingFlags = ValidationFlags.SIZE_CONSTRAINTS
		addChild(hScrollBar)
		addChild(vScrollBar)

		hScrollModel.changed.add(scrollChangedHandler)
		vScrollModel.changed.add(scrollChangedHandler)

		watch(style) {
			tossScrolling = it.tossScrolling
			scrollRect.style.borderRadii = it.borderRadius

			corner?.dispose()
			corner = it.corner(this)
			addChildAfter(corner!!, vScrollBar)
		}
	}

	override fun onActivated() {
		super.onActivated()
		focusManager.focusedChanged.add(this::focusChangedHandler)
	}

	override fun onDeactivated() {
		super.onDeactivated()
		focusManager.focusedChanged.remove(this::focusChangedHandler)
	}

	private fun focusChangedHandler(old: UiComponentRo?, new: UiComponentRo?) {
		if (style.autoScrollToFocused && new != null && isAncestorOf(new)) {
			callLater {
				// Inside a callLater because scrollTo invokes validation and focus changes may happen within validation.
				scrollTo(new)
			}
		}
	}

	override fun onElementAdded(oldIndex: Int, newIndex: Int, element: UiComponent) {
		contents.addElement(newIndex, element)
	}

	override fun onElementRemoved(index: Int, element: UiComponent) {
		contents.removeElement(element)
	}

	/**
	 * The unclipped width of the contents.
	 */
	val contentsWidth: Float
		get() {
			validate(ValidationFlags.LAYOUT)
			return scrollRect.contentsWidth
		}

	/**
	 * The unclipped height of the contents.
	 */
	val contentsHeight: Float
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
		}
		var needsHScrollBar = allowHScrolling && (requireHScrolling || contents.width > explicitWidth!! + 0.1f)
		var needsVScrollBar = allowVScrolling && (requireVScrolling || contents.height > explicitHeight!! + 0.1f)
		val vScrollBarW = vScrollBar.minWidth ?: 0f
		val hScrollBarH = hScrollBar.minHeight ?: 0f

		if (needsHScrollBar && needsVScrollBar) {
			// Needs both scroll bars.
			contents.setSize(explicitWidth!! - vScrollBarW, explicitHeight!! - hScrollBarH)
		} else if (needsHScrollBar) {
			// Needs horizontal scroll bar.
			contents.setSize(explicitWidth, if (explicitHeight == null) null else explicitHeight - hScrollBarH)
			needsVScrollBar = allowVScrolling && (requireVScrolling || contents.height > contents.explicitHeight!! + 0.1f)
			if (needsVScrollBar) {
				// Adding the horizontal scroll bar causes the vertical scroll bar to be needed.
				contents.setSize(explicitWidth!! - vScrollBarW, explicitHeight!! - hScrollBarH)
			}
		} else if (needsVScrollBar) {
			// Needs vertical scroll bar.
			contents.setSize(if (explicitWidth == null) null else explicitWidth - vScrollBarW, explicitHeight)
			needsHScrollBar = allowHScrolling && (requireHScrolling || contents.width > contents.explicitWidth!! + 0.1f)
			if (needsHScrollBar) {
				// Adding the vertical scroll bar causes the horizontal scroll bar to be needed.
				contents.setSize(explicitWidth!! - vScrollBarW, explicitHeight!! - hScrollBarH)
			}
		}
		scrollRect.setSize(contents.explicitWidth, contents.explicitHeight)

		// Set the content mask to the explicit size of the contents stack, or the measured size if there was no bound.
		val contentsSetW = scrollRect.explicitWidth ?: contents.width
		val contentsSetH = scrollRect.explicitHeight ?: contents.height
		scrollRect.setSize(contentsSetW, contentsSetH)
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

	protected open fun validateScroll() {
		val xScroll = hScrollModel.value.floor()
		val yScroll = vScrollModel.value.floor()
		scrollRect.scrollTo(xScroll, yScroll)
	}

	override fun dispose() {
		super.dispose()
		hScrollModel.changed.remove(scrollChangedHandler)
		vScrollModel.changed.remove(scrollChangedHandler)
		tossScrolling = false
	}

	companion object : StyleTag {
		val VBAR_STYLE = styleTag()
		val HBAR_STYLE = styleTag()

		/**
		 * The validation flag used for scrolling.
		 */
		const val SCROLLING: Int = 1 shl 16
	}
}


private val tmpBounds = MinMax()

fun ScrollArea.scrollTo(target: UiComponentRo, pad: PadRo = Pad(10f)) {
	tmpBounds.set(0f, 0f, target.width, target.height)
	target.localToGlobal(tmpBounds)
	globalToLocal(tmpBounds)
	tmpBounds.xMin += hScrollModel.value
	tmpBounds.xMax += hScrollModel.value
	tmpBounds.yMin += vScrollModel.value
	tmpBounds.yMax += vScrollModel.value
	tmpBounds.inflate(pad)
	scrollTo(tmpBounds)
}

/**
 * Scrolls the minimum distance to show the given bounding rectangle.
 */
fun ScrollArea.scrollTo(bounds: RectangleRo) {
	validate(ValidationFlags.LAYOUT)
	if (bounds.x < hScrollModel.value)
		hScrollModel.value = bounds.x
	if (bounds.y < vScrollModel.value)
		vScrollModel.value = bounds.y
	val contentsSetW = contentsWidth - hScrollModel.max
	if (bounds.right > hScrollModel.value + contentsSetW)
		hScrollModel.value = bounds.right - contentsSetW
	val contentsSetH = contentsHeight - vScrollModel.max
	if (bounds.bottom > vScrollModel.value + contentsSetH)
		vScrollModel.value = bounds.bottom - contentsSetH
}

/**
 * Scrolls the minimum distance to show the given bounding MinMax.
 */
fun ScrollArea.scrollTo(bounds: MinMaxRo) {
	validate(ValidationFlags.LAYOUT)
	val contentsSetW = contentsWidth - hScrollModel.max
	val contentsSetH = contentsHeight - vScrollModel.max
	if (bounds.xMin >= hScrollModel.value || bounds.xMax <= hScrollModel.value + contentsSetW) {
		if (bounds.xMin < hScrollModel.value)
			hScrollModel.value = bounds.xMin
		if (bounds.xMax > hScrollModel.value + contentsSetW)
			hScrollModel.value = bounds.xMax - contentsSetW
	}
	if (bounds.yMin >= vScrollModel.value || bounds.yMax <= vScrollModel.value + contentsSetH) {
		if (bounds.yMin < vScrollModel.value)
			vScrollModel.value = bounds.yMin
		if (bounds.yMax > vScrollModel.value + contentsSetH)
			vScrollModel.value = bounds.yMax - contentsSetH
	}
}

fun ScrollArea.tweenScrollX(duration: Float, ease: Interpolation, toScrollX: Float, delay: Float = 0f): Tween {
	return createPropertyTween(this, "scrollX", duration, ease, { hScrollModel.value }, { hScrollModel.value = it }, toScrollX, delay)
}

fun ScrollArea.tweenScrollY(duration: Float, ease: Interpolation, toScrollY: Float, delay: Float = 0f): Tween {
	return createPropertyTween(this, "scrollY", duration, ease, { vScrollModel.value }, { vScrollModel.value = it }, toScrollY, delay)
}

enum class ScrollPolicy {
	OFF,
	ON,
	AUTO
}

fun ScrollPolicy.toCssString(): String {
	return when (this) {
		ScrollPolicy.OFF -> "hidden"
		ScrollPolicy.ON -> "scroll"
		ScrollPolicy.AUTO -> "auto"
	}
}

fun Owned.scrollArea(init: ComponentInit<ScrollArea> = {}): ScrollArea {
	val s = ScrollArea(this)
	s.init()
	return s
}

class ScrollAreaStyle : StyleBase() {

	override val type: StyleType<ScrollAreaStyle> = Companion

	var corner by prop(noSkin)

	var tossScrolling by prop(false)

	var borderRadius: CornersRo by prop(Corners())

	/**
	 * If true, the scroll area will automatically scroll to show the focused element.
	 */
	var autoScrollToFocused by prop(true)

	companion object : StyleType<ScrollAreaStyle>
}