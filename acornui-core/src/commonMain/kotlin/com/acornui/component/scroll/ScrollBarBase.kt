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

package com.acornui.component.scroll

import com.acornui.Disposable
import com.acornui.closeTo
import com.acornui.component.*
import com.acornui.component.layout.HAlign
import com.acornui.component.layout.LayoutElementRo
import com.acornui.component.layout.VAlign
import com.acornui.component.layout.algorithm.BasicLayoutData
import com.acornui.component.layout.algorithm.LayoutDataProvider
import com.acornui.component.style.*
import com.acornui.cursor.StandardCursor
import com.acornui.cursor.cursor
import com.acornui.di.Context
import com.acornui.di.own
import com.acornui.function.as1
import com.acornui.input.interaction.*
import com.acornui.input.mouseDown
import com.acornui.input.mouseMove
import com.acornui.input.touchMove
import com.acornui.input.touchStart
import com.acornui.math.Bounds
import com.acornui.math.Easing
import com.acornui.math.MathUtils.clamp
import com.acornui.math.Vector2Ro
import com.acornui.math.vec2
import com.acornui.time.callLater
import com.acornui.time.onTick
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract
import kotlin.properties.Delegates
import kotlin.time.Duration
import kotlin.time.seconds

abstract class ScrollBarBase(owner: Context) : ContainerImpl(owner), ScrollBar {

	override fun createLayoutData() = ScrollBarLayoutData()

	private lateinit var thumbDrag: DragAttachment
	private val _scrollModel = own(ScrollModelImpl())
	val scrollModel: ClampedScrollModel = _scrollModel

	val style = bind(ScrollBarStyle())

	/**
	 * The value to add or subtract to the scroll model on step up or step down button press.
	 */
	var stepSize = 5f

	/**
	 * The value to multiply against the scroll model to convert to points.
	 * In other words, how many points per 1 unit on the scroll model.
	 */
	var modelToPoints by Delegates.observable(1f) {
		prop, old, new ->
		check(!(new.isNaN() || new.isInfinite())) { "modelToPoints may not be NaN" }
		invalidate(ValidationFlags.LAYOUT)
	}

	private val thumbOffset = vec2()
	private val positionTmp = vec2()

	// Children
	protected var decrementButton: UiComponent? = null
	protected var incrementButton: UiComponent? = null
	protected var thumb: UiComponent? = null
	protected var track: UiComponent? = null

	private var previousScrollValue = -1f
	private var alphaTick: Disposable? = null
	private var fadeTimer: Duration = Duration.ZERO

	private val mouseOrTouchState = own(MouseOrTouchState(this))

	init {
		styleTags.add(ScrollBar)
		val rollOverCursor = cursor(StandardCursor.HAND)
		rollOverCursor.enabled = false

		scrollModel.changed.add {
			rollOverCursor.enabled = it.max > it.min
			updateThumbLayout(width, height, thumb!!)
		}

		callLater {
			// Add this handler in a call later
			scrollModel.changed.add {
				if (!previousScrollValue.closeTo(scrollModel.value)) {
					// Only make the scroll bar opaque if the scroll model's value has changed by a non-negligible amount.
					previousScrollValue = it.value
					activeAnim()
				}
			}
		}

		watch(style) {
			track?.dispose()
			track = addChild(it.track(this))
			track!!.interactivityMode = InteractivityMode.NONE

			decrementButton?.dispose()
			decrementButton = addOptionalChild(it.decrementButton(this))
			decrementButton?.enableDownRepeat()
			decrementButton?.mouseDown()?.add(::decrementPressHandler)

			incrementButton?.dispose()
			incrementButton = addOptionalChild(it.incrementButton(this))
			incrementButton?.enableDownRepeat()
			incrementButton?.mouseDown()?.add(::incrementPressHandler)

			thumb?.dispose()
			thumb = addChild(it.thumb(this))
			val thumb = thumb!!
			thumb.layoutInvalidatingFlags = ValidationFlags.LAYOUT
			thumb.focusEnabled = false
			thumbDrag = thumb.dragAttachment(0f)
			thumbDrag.dragStart.add(::dragStartHandler.as1)
			thumbDrag.drag.add(::thumbDragHandler)

			onAlphaTick(0f) // Set the thumb's alpha
		}
		mouseMove().add(::activeAnim.as1)
		touchMove().add(::activeAnim.as1)
		mouseDown().add(::activeAnim.as1)
		touchStart().add(::activeAnim.as1)

		enableDownRepeat()

		mouseDown().add(::pageModePressHandler)
		dragAttachment(0f).drag.add(::dragHandler)
	}

	private fun onAlphaTick(dT: Float) {
		fadeTimer = if (mouseOrTouchState.isOver)
			style.fadeOutDelay + style.fadeOutDuration
		else
			fadeTimer - dT.toDouble().seconds

		val thumb = thumb ?: return
		val p = clamp(1f - (fadeTimer / style.fadeOutDuration).toFloat(), 0f, 1f)
		thumb.alpha = style.fadeOutEasing.apply(1f, style.inactiveAlpha, p)

		if (fadeTimer <= Duration.ZERO) {
			alphaTick?.dispose()
			alphaTick = null
		}
	}

	private fun activeAnim() {
		fadeTimer = style.fadeOutDelay + style.fadeOutDuration
		if (alphaTick == null && style.inactiveAlpha != 1f)
			alphaTick = onTick(::onAlphaTick)
	}

	private fun decrementPressHandler(event: MouseInteractionRo) {
		event.handled = true
		stepDec()
	}

	private fun incrementPressHandler(event: MouseInteractionRo) {
		event.handled = true
		stepInc()
	}

	private var pageDirection = false

	/**
	 * When pressing down on the track
	 */
	private fun pageModePressHandler(event: MouseInteractionRo) {
		if (!style.pageMode || event.target !== this || thumbDrag.isDragging) return
		val track = track ?: return
		val thumb = thumb ?: return
		event.handled = true
		positionTmp.set(event.localX + track.x - thumb.width * 0.5f, event.localY + track.y - thumb.height * 0.5f)
		val newValue = getModelValue(positionTmp)
		val isPositive = newValue > scrollModel.value
		if (!event.isFabricated)
			pageDirection = isPositive
		if (pageDirection == isPositive)
			scrollModel.value += if (isPositive) pageSize else -pageSize
	}

	private fun dragHandler(event: DragInteractionRo) {
		if (style.pageMode || event.target !== this || thumbDrag.isDragging) return
		event.handled = true
		val track = track ?: return
		val thumb = thumb ?: return
		positionTmp.set(event.positionLocal.x + track.x - thumb.width * 0.5f, event.positionLocal.y + track.y - thumb.height * 0.5f)
		val newValue = getModelValue(positionTmp)
		scrollModel.value = newValue
	}

	private fun dragStartHandler() {
		val thumb = thumb!!
		mousePosition(thumbOffset).sub(thumb.x, thumb.y)
	}

	private fun thumbDragHandler(event: DragInteractionRo) {
		event.handled = true
		mousePosition(positionTmp).sub(thumbOffset)
		scrollModel.rawValue = getModelValue(positionTmp)
	}

	open fun stepDec() {
		scrollModel.value = (scrollModel.rawValue - stepSize / modelToPoints)
	}

	open fun stepInc() {
		scrollModel.value = (scrollModel.rawValue + stepSize / modelToPoints)
	}

	open fun pageUp() {
		scrollModel.value = (scrollModel.rawValue - pageSize)
	}

	open fun pageDown() {
		scrollModel.value = (scrollModel.rawValue + pageSize)
	}

	protected abstract fun getModelValue(position: Vector2Ro): Float
	protected abstract val minTrack: Float
	protected abstract val maxTrack: Float

	private var _explicitPageSize: Float? = null

	/**
	 * Returns the explicit page size if it was set, or the size of the track divided by the modelToPoints ratio.
	 */
	var pageSize: Float
		get() {
			if (_explicitPageSize != null) return _explicitPageSize!!
			return (maxTrack - minTrack) / modelToPoints
		}
		set(value) {
			_explicitPageSize = value
		}

	/**
	 * Sets the explicit page size. If set to null, the measured page size will be used.
	 * Measured size: `((maxTrack - minTrack) / modelToPoints)`
	 */
	fun pageSize(value: Float?) {
		_explicitPageSize = value
	}

	val naturalWidth: Float
		get() {
			validate(ValidationFlags.STYLES)
			return style.naturalWidth
		}

	val naturalHeight: Float
		get() {
			validate(ValidationFlags.STYLES)
			return style.naturalHeight
		}


	override fun updateLayout(explicitWidth: Float?, explicitHeight: Float?, out: Bounds) {
		val decrementButton = decrementButton
		val incrementButton = incrementButton
		val track = track!!
		val thumb = thumb!!
		val w = explicitWidth ?: maxOf(minWidth, style.naturalWidth)
		val h = explicitHeight ?: maxOf(minHeight, style.naturalHeight)

		val decrementButtonLd = decrementButton?.layoutDataCast
		decrementButton?.setSize(decrementButtonLd?.getPreferredWidth(null), decrementButtonLd?.getPreferredHeight(null))

		val incrementButtonLd = incrementButton?.layoutDataCast
		incrementButton?.setSize(incrementButtonLd?.getPreferredWidth(null), incrementButtonLd?.getPreferredHeight(null))

		updatePartsLayout(w, h, decrementButton, incrementButton, track)

		updateThumbLayout(w, h, thumb)
		out.set(w, h)
	}

	/**
	 * Update the size and position of the increment, decrement, and track.
	 * @param width The explicit or natural width.
	 * @param height The explicit or natural height.
	 * @param decrementButton The decrement button skin part.
	 * @param incrementButton The increment button skin part.
	 * @param track The track skin part.
	 */
	protected abstract fun updatePartsLayout(width: Float, height: Float, decrementButton: UiComponent?, incrementButton: UiComponent?, track: UiComponent)

	/**
	 * Update the size and position of the thumb.
	 * This is called from both [updateLayout] and when the scroll model is changed.
	 *
	 * @param width The explicit or natural width.
	 * @param height The explicit or natural height.
	 * @param thumb The thumb skin part.
	 */
	protected abstract fun updateThumbLayout(width: Float, height: Float, thumb: UiComponent)

	@Suppress("UNCHECKED_CAST")
	protected val LayoutElementRo.layoutDataCast: ScrollBarLayoutData?
		get() = this.layoutData as ScrollBarLayoutData?

}

class ScrollBarStyle : StyleBase() {

	override val type: StyleType<ScrollBarStyle> = ScrollBarStyle

	/**
	 * If no explicit width is set, or [UiComponentImpl.defaultWidth] is set, this width will be used.
	 */
	var naturalWidth by prop(10f)

	/**
	 * If no explicit height is set, or [UiComponentImpl.defaultHeight] is set, this height will be used.
	 */
	var naturalHeight by prop(10f)

	var decrementButton by prop<ScrollBarSkinPartOptional>(noSkinOptional)
	var incrementButton by prop<ScrollBarSkinPartOptional>(noSkinOptional)
	var track by prop<ScrollBarSkinPart>(noSkin)
	var thumb by prop<ScrollBarSkinPart>(noSkin)

	/**
	 * If true (default), clicking the track steps [ScrollBarBase.pageSize].
	 * If false, pressing/dragging the track snaps scroll value to the cursor position.
	 *
	 * NB: If this is true, the skin for the track should enable down repeat.
	 * [com.acornui.input.interaction.enableDownRepeat].
	 */
	var pageMode by prop(true)

	/**
	 * When the scroll bar is inactive, the track will tween to this alpha value.
	 * The scroll bar will be considered active if the scroll model value changes or there is user interactivity.
	 */
	var inactiveAlpha by prop(1f)

	/**
	 * The duration to tween the alpha to [inactiveAlpha].
	 */
	var fadeOutDuration by prop(0.5.seconds)

	/**
	 * The delay after no interactivity before fading out.
	 */
	var fadeOutDelay by prop(0.7.seconds)

	/**
	 * The easing to apply when transitioning into inactive alpha.
	 */
	var fadeOutEasing by prop(Easing.pow2Out)

	companion object : StyleType<ScrollBarStyle>
}

interface ScrollBar : Context, LayoutDataProvider<ScrollBarLayoutData> {
	companion object : StyleTag
}

typealias ScrollBarSkinPart = ScrollBar.() -> UiComponent
typealias ScrollBarSkinPartOptional = ScrollBar.() -> UiComponent?

class ScrollBarLayoutData : BasicLayoutData() {

	/**
	 * For horizontal scroll bars, this is the skin part's vertical alignment.
	 *
	 * NB: [VAlign.BASELINE] is not supported here.
	 */
	var verticalAlign: VAlign by bindable(VAlign.MIDDLE)

	/**
	 * For vertical scroll bars, this is the skin part's horizontal alignment.
	 */
	var horizontalAlign: HAlign by bindable(HAlign.CENTER)

}

inline fun scrollBarStyle(init: ComponentInit<ScrollBarStyle> = {}): ScrollBarStyle {
	contract { callsInPlace(init, InvocationKind.EXACTLY_ONCE) }
	return ScrollBarStyle().apply(init)
}