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

@file:Suppress("unused", "MemberVisibilityCanBePrivate", "UNUSED_PARAMETER")

package com.acornui.component.scroll

import com.acornui.component.*
import com.acornui.component.layout.algorithm.LayoutDataProvider
import com.acornui.component.style.*
import com.acornui.di.Context
import com.acornui.focus.FocusChangedEventRo
import com.acornui.input.Ascii
import com.acornui.input.KeyState
import com.acornui.input.wheel
import com.acornui.math.*
import com.acornui.tween.Tween
import com.acornui.tween.createPropertyTween
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract
import kotlin.jvm.JvmName
import kotlin.math.floor

/**
 * A container with scrolling.
 */
open class ScrollArea<E : UiComponent>(
		owner: Context
) : ElementContainerImpl<E>(owner), LayoutDataProvider<StackLayoutData> {

	val style = bind(ScrollAreaStyle())
	private val keyState by KeyState

	override fun createLayoutData(): StackLayoutData = StackLayoutData()

	protected val scrollRect = scrollRect {
		wheel().add { event ->
			if (!event.handled && !keyState.keyIsDown(Ascii.CONTROL)) {
				if (vScrollModel.max > 0f && event.deltaY != 0f) {
					event.handled = true
					vScrollBar.userChange(vScrollBar.inputValue + event.deltaY)
				}
				if (hScrollModel.max > 0f && event.deltaX != 0f) {
					event.handled = true
					hScrollBar.userChange(hScrollBar.inputValue + event.deltaX)
				}
			}
		}
	}

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

	private var tossScroller: TossScroller? = null
	private var tossBinding: TossScrollModelBinding? = null

	private var tossScrolling: Boolean
		get() = _tossScrolling
		set(value) {
			if (_tossScrolling == value) return
			_tossScrolling = value
			if (value) {
				tossScroller = TossScroller(this)
				tossBinding = TossScrollModelBinding(tossScroller!!) {
					hScrollBar.userChange(hScrollBar.inputValue - it.x)
					vScrollBar.userChange(vScrollBar.inputValue - it.y)
				}
			} else {
				tossScroller?.dispose()
				tossScroller = null
				tossBinding?.dispose()
				tossBinding = null
			}
		}


	init {
		styleTags.add(ScrollArea)
		validation.addNode(SCROLLING, ValidationFlags.LAYOUT, ::validateScroll)

		styleTags.add(HBAR_STYLE)
		styleTags.add(VBAR_STYLE)

		addChild(scrollRect)

		hScrollBar.layoutInvalidatingFlags = ValidationFlags.LAYOUT
		vScrollBar.layoutInvalidatingFlags = ValidationFlags.LAYOUT
		addChild(hScrollBar)
		addChild(vScrollBar)

		hScrollModel.changed.add(::scrollChangedHandler)
		vScrollModel.changed.add(::scrollChangedHandler)

		watch(style) {
			tossScrolling = it.tossScrolling
			scrollRect.style.borderRadii = it.borderRadii

			corner?.dispose()
			corner = addChild(it.corner(this))
		}
	}

	private fun scrollChangedHandler(m: ScrollModelRo) {
		invalidate(SCROLLING)
		Unit
	}

	/**
	 * Stops the current toss, if there is one.
	 */
	fun stopToss() {
		tossScroller?.stop()
	}

	override fun onActivated() {
		super.onActivated()
		// Must call super.onActivated first so that the priority of this scroll area's changed handler is less than
		// that of nested scroll areas.
		focusManager.focusedChanged.add(::focusChangedHandler)
	}

	override fun onDeactivated() {
		super.onDeactivated()
		focusManager.focusedChanged.remove(::focusChangedHandler)
	}

	private fun focusChangedHandler(event: FocusChangedEventRo) {
		val new = event.new
		if (new != null && event.options.scrollToFocused && style.autoScrollToFocused && isAncestorOf(new) && tossScroller?.userIsActive != true) {
			scrollTo(new)
		}
	}

	override fun onElementAdded(oldIndex: Int, newIndex: Int, element: E) {
		contents.addElement(newIndex, element)
	}

	override fun onElementRemoved(index: Int, element: E) {
		contents.removeElement(element)
	}

	private var _contentsWidth = 0f

	/**
	 * The unclipped width of the contents.
	 */
	val contentsWidth: Float
		get() {
			validate(ValidationFlags.LAYOUT)
			return _contentsWidth
		}

	private var _contentsHeight = 0f

	/**
	 * The unclipped height of the contents.
	 */
	val contentsHeight: Float
		get() {
			validate(ValidationFlags.LAYOUT)
			return _contentsHeight
		}

	private val tmpBounds = MinMax()

	fun scrollTo(target: UiComponentRo, pad: PadRo = Pad(10f)) {
		tmpBounds.set(target.bounds)
		target.localToCanvas(tmpBounds)
		canvasToLocal(tmpBounds)
		tmpBounds.xMin += hScrollModel.value
		tmpBounds.xMax += hScrollModel.value
		tmpBounds.yMin += vScrollModel.value
		tmpBounds.yMax += vScrollModel.value
		tmpBounds.inflate(pad)
		scrollTo(tmpBounds)
	}

	private val tmpMinMax = MinMax()

	/**
	 * Scrolls the minimum distance to show the given bounding rectangle.
	 */
	fun scrollTo(bounds: RectangleRo) = scrollTo(tmpMinMax.set(bounds))

	/**
	 * Scrolls the minimum distance to show the given bounding MinMax.
	 * Note: This does not validate the scroll area's layout first.
	 */
	fun scrollTo(bounds: MinMaxRo) {
		stopToss()
		val hScrollModel = hScrollBar.scrollModel
		val vScrollModel = vScrollBar.scrollModel
		val contentsSetW = _contentsWidth - hScrollModel.max
		val contentsSetH = _contentsHeight - vScrollModel.max
		if (bounds.xMin >= hScrollModel.value || bounds.xMax <= hScrollModel.value + contentsSetW) {
			if (bounds.xMax > hScrollModel.value + contentsSetW)
				hScrollModel.value = bounds.xMax - contentsSetW
			if (bounds.xMin < hScrollModel.value)
				hScrollModel.value = bounds.xMin
		}
		if (bounds.yMin >= vScrollModel.value || bounds.yMax <= vScrollModel.value + contentsSetH) {
			if (bounds.yMax > vScrollModel.value + contentsSetH)
				vScrollModel.value = bounds.yMax - contentsSetH
			if (bounds.yMin < vScrollModel.value)
				vScrollModel.value = bounds.yMin
		}
	}

	/**
	 * Sets the [hScrollModel] value to [x] and the [vScrollModel] value to [y].
	 */
	fun scrollTo(x: Float, y: Float) {
		val hScrollModel = hScrollBar.scrollModel
		val vScrollModel = vScrollBar.scrollModel
		hScrollModel.value = x
		vScrollModel.value = y
	}

	override fun updateLayout(explicitWidth: Float?, explicitHeight: Float?, out: Bounds) {
		val requireHScrolling = hScrollPolicy == ScrollPolicy.ON && explicitWidth != null
		val allowHScrolling = hScrollPolicy != ScrollPolicy.OFF && explicitWidth != null
		val requireVScrolling = vScrollPolicy == ScrollPolicy.ON && explicitHeight != null
		val allowVScrolling = vScrollPolicy != ScrollPolicy.OFF && explicitHeight != null

		if (!(requireHScrolling || requireVScrolling)) {
			// Size target without scrolling.
			contents.size(explicitWidth, explicitHeight)
		}
		var needsHScrollBar = allowHScrolling && (requireHScrolling || contents.width > explicitWidth!! + 0.1f)
		var needsVScrollBar = allowVScrolling && (requireVScrolling || contents.height > explicitHeight!! + 0.1f)
		val vScrollBarW = vScrollBar.naturalWidth
		val hScrollBarH = hScrollBar.naturalHeight

		if (needsHScrollBar && needsVScrollBar) {
			// Needs both scroll bars.
			contents.size(explicitWidth!! - vScrollBarW, explicitHeight!! - hScrollBarH)
		} else if (needsHScrollBar) {
			// Needs horizontal scroll bar.
			contents.size(explicitWidth, if (explicitHeight == null) null else explicitHeight - hScrollBarH)
			needsVScrollBar = allowVScrolling && (requireVScrolling || contents.height > contents.explicitHeight!! + 0.1f)
			if (needsVScrollBar) {
				// Adding the horizontal scroll bar causes the vertical scroll bar to be needed.
				contents.size(explicitWidth!! - vScrollBarW, explicitHeight!! - hScrollBarH)
			}
		} else if (needsVScrollBar) {
			// Needs vertical scroll bar.
			contents.size(if (explicitWidth == null) null else explicitWidth - vScrollBarW, explicitHeight)
			needsHScrollBar = allowHScrolling && (requireHScrolling || contents.width > contents.explicitWidth!! + 0.1f)
			if (needsHScrollBar) {
				// Adding the vertical scroll bar causes the horizontal scroll bar to be needed.
				contents.size(explicitWidth!! - vScrollBarW, explicitHeight!! - hScrollBarH)
			}
		}
		scrollRect.size(contents.explicitWidth, contents.explicitHeight)

		// Set the content mask to the explicit size of the contents stack, or the measured size if there was no bound.
		val contentsSetW = scrollRect.explicitWidth ?: contents.width
		val contentsSetH = scrollRect.explicitHeight ?: contents.height
		scrollRect.size(contentsSetW, contentsSetH)
		val vScrollBarW2 = if (needsVScrollBar) vScrollBarW else 0f
		val hScrollBarH2 = if (needsHScrollBar) hScrollBarH else 0f

		out.set(
				explicitWidth ?: scrollRect.contentsWidth + vScrollBarW2,
				explicitHeight ?: scrollRect.contentsHeight + hScrollBarH2
		)

		// Update the scroll models and scroll bar sizes.
		if (needsHScrollBar) {
			hScrollBar.visible = true
			hScrollBar.size(explicitWidth!! - vScrollBarW2, hScrollBarH)
			hScrollBar.position(0f, out.height - hScrollBarH)
			hScrollBar.setScaling(minOf(1f, hScrollBar.explicitWidth!! / hScrollBar.width), 1f)
		} else {
			hScrollBar.visible = false
		}
		if (needsVScrollBar) {
			vScrollBar.visible = true
			vScrollBar.size(vScrollBarW, explicitHeight!! - hScrollBarH2)
			vScrollBar.position(out.width - vScrollBarW, 0f)
			vScrollBar.setScaling(1f, minOf(1f, vScrollBar.explicitHeight!! / vScrollBar.height))
		} else {
			vScrollBar.visible = false
		}
		val corner = corner!!
		if (needsHScrollBar && needsVScrollBar) {
			corner.size(vScrollBarW, hScrollBarH)
			corner.position(explicitWidth!! - vScrollBarW, explicitHeight!! - hScrollBarH)
			corner.visible = true
		} else {
			corner.visible = false
		}

		hScrollBar.scrollModel.max = maxOf(0f, scrollRect.contentsWidth - contentsSetW)
		vScrollBar.scrollModel.max = maxOf(0f, scrollRect.contentsHeight - contentsSetH)

		scrollRect.getAttachment<TossScroller>(TossScroller)?.enabled = needsHScrollBar || needsVScrollBar

		_contentsWidth = scrollRect.contentsWidth
		_contentsHeight = scrollRect.contentsHeight
	}

	protected open fun validateScroll() {
		val xScroll = floor(hScrollModel.value)
		val yScroll = floor(vScrollModel.value)
		scrollRect.scrollTo(xScroll, yScroll)
	}

	override fun dispose() {
		super.dispose()
		hScrollModel.changed.remove(::scrollChangedHandler)
		vScrollModel.changed.remove(::scrollChangedHandler)
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

fun ScrollArea<*>.tweenScrollX(duration: Float, ease: Interpolation, toScrollX: Float, delay: Float = 0f): Tween {
	return createPropertyTween(this, "scrollX", duration, ease, { hScrollModel.value }, { scrollTo(it, vScrollModel.value) }, toScrollX, delay)
}

fun ScrollArea<*>.tweenScrollY(duration: Float, ease: Interpolation, toScrollY: Float, delay: Float = 0f): Tween {
	return createPropertyTween(this, "scrollY", duration, ease, { vScrollModel.value }, { scrollTo(hScrollModel.value, it) }, toScrollY, delay)
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

@JvmName("scrollAreaT")
inline fun <E : UiComponent> Context.scrollArea(init: ComponentInit<ScrollArea<E>> = {}): ScrollArea<E> {
	contract { callsInPlace(init, InvocationKind.EXACTLY_ONCE) }
	return ScrollArea<E>(this).apply(init)
}

inline fun Context.scrollArea(init: ComponentInit<ScrollArea<UiComponent>> = {}): ScrollArea<UiComponent> {
	contract { callsInPlace(init, InvocationKind.EXACTLY_ONCE) }
	return scrollArea<UiComponent>(init)
}

class ScrollAreaStyle : StyleBase() {

	override val type: StyleType<ScrollAreaStyle> = Companion

	var corner by prop(noSkin)

	var tossScrolling by prop(false)

	/**
	 *
	 */
	var borderRadii by prop<CornersRo>(Corners())

	/**
	 * If true, the scroll area will automatically scroll to show the focused element.
	 */
	var autoScrollToFocused by prop(true)

	companion object : StyleType<ScrollAreaStyle>
}

inline fun scrollAreaStyle(init: ComponentInit<ScrollAreaStyle> = {}): ScrollAreaStyle {
	contract { callsInPlace(init, InvocationKind.EXACTLY_ONCE) }
	return ScrollAreaStyle().apply(init)
}