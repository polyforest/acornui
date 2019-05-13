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

import com.acornui.component.*
import com.acornui.component.style.*
import com.acornui.core.cursor.StandardCursors
import com.acornui.core.cursor.cursor
import com.acornui.core.di.Owned
import com.acornui.core.di.own
import com.acornui.core.input.interaction.*
import com.acornui.core.input.mouseDown
import com.acornui.core.input.mouseOver
import com.acornui.core.tween.driveTween
import com.acornui.core.tween.killTween
import com.acornui.core.tween.tweenAlpha
import com.acornui.math.Easing
import com.acornui.math.Vector2
import kotlin.properties.Delegates

abstract class ScrollBarBase(owner: Owned) : ContainerImpl(owner) {

	protected val _scrollModel = own(ScrollModelImpl())

	val scrollModel: ClampedScrollModel
		get() = _scrollModel

	val style = bind(ScrollBarStyle())

	/**
	 * The value to add or subtract to the scroll model on step up or step down button press.
	 */
	var stepSize = 5f

	/**
	 * The value to multiply against the scroll model to convert to pixels.
	 * In other words, how many pixels per 1 unit on the scroll model.
	 */
	var modelToPixels by Delegates.observable(1f) {
		prop, old, new ->
		if (new.isNaN() || new.isInfinite())
			throw Exception("modelToPixels may not be NaN")
		invalidate(ValidationFlags.LAYOUT)
	}

	private val thumbOffset = Vector2()
	private val positionTmp = Vector2()

	// Children
	protected var decrementButton: UiComponent? = null
	protected var incrementButton: UiComponent? = null
	protected var thumb: UiComponent? = null
	protected var track: UiComponent? = null

	init {
		scrollModel.changed.add { activeAnim(); invalidate(ValidationFlags.LAYOUT) }

		watch(style) {
			track?.dispose()
			track = addChild(it.track(this))
			val track = track!!
			track.cursor(StandardCursors.HAND)

			decrementButton?.dispose()
			decrementButton = addChild(it.decrementButton(this))
			decrementButton!!.mouseDown().add(::decrementPressHandler)
			incrementButton?.dispose()
			incrementButton = addChild(it.incrementButton(this))
			incrementButton!!.mouseDown().add(::incrementPressHandler)

			val oldThumbAlpha = thumb?.alpha ?: it.inactiveAlpha
			thumb?.dispose()
			thumb = addChild(it.thumb(this))
			val thumb = thumb!!
			thumb.alpha = oldThumbAlpha
			thumb.cursor(StandardCursors.HAND)
			if (it.pageMode) {
				track.mouseDown().add(::trackPressHandler)
				val thumbDrag = thumb.dragAttachment(0f)
				thumbDrag.dragStart.add(::dragStartHandler)
				thumbDrag.drag.add(::thumbDragHandler)
			} else {
				thumb.interactivityMode = InteractivityMode.NONE
				track.dragAttachment(0f)
				track.drag().add(::trackDragHandler)
			}
			thumb.mouseOver().add { activeAnim() }
		}
	}

	private fun activeAnim() {
		if (style.inactiveAlpha == 1f) return
		val thumb = thumb ?: return
		killTween(thumb, "alpha", finish = false)
		thumb.alpha = 1f
		driveTween(thumb.tweenAlpha(style.alphaDuration, Easing.pow2Out, style.inactiveAlpha, 0.5f))
	}

	private fun decrementPressHandler(event: MouseInteractionRo) {
		stepDec()
	}

	private fun incrementPressHandler(event: MouseInteractionRo) {
		stepInc()
	}

	private fun trackPressHandler(event: MouseInteractionRo) {
		event.handled = true
		event.preventDefault() // Prevent dom selection
		mousePosition(positionTmp)
		val previous = scrollModel.value
		var newValue = getModelValue(positionTmp)
		val pageSize = pageSize()
		if (newValue > previous + pageSize) newValue = previous + pageSize
		if (newValue < previous - pageSize) newValue = previous - pageSize
		scrollModel.value = newValue
	}

	private fun trackDragHandler(event: DragInteractionRo) {
		positionTmp.set(event.position)
		canvasToLocal(positionTmp)
		val newValue = getModelValue(positionTmp)
		scrollModel.value = newValue
	}

	private fun dragStartHandler(event: DragInteractionRo) {
		val thumb = thumb!!
		mousePosition(thumbOffset).sub(thumb.x, thumb.y)
	}

	private fun thumbDragHandler(event: DragInteractionRo) {
		mousePosition(positionTmp).sub(thumbOffset)
		scrollModel.rawValue = getModelValue(positionTmp)
	}

	open fun stepDec() {
		scrollModel.value = (scrollModel.rawValue - stepSize / modelToPixels)
	}

	open fun stepInc() {
		scrollModel.value = (scrollModel.rawValue + stepSize / modelToPixels)
	}

	open fun pageUp() {
		scrollModel.value = (scrollModel.rawValue - pageSize())
	}

	open fun pageDown() {
		scrollModel.value = (scrollModel.rawValue + pageSize())
	}

	protected abstract fun refreshThumbPosition()
	protected abstract fun getModelValue(position: Vector2): Float
	protected abstract fun minTrack(): Float
	protected abstract fun maxTrack(): Float

	private var _explicitPageSize: Float? = null

	/**
	 * Returns the explicit page size if it was set, or the size of the track divided by the modelToPixels ratio.
	 */
	fun pageSize(): Float {
		if (_explicitPageSize != null) return _explicitPageSize!!
		return (maxTrack() - minTrack()) / modelToPixels
	}

	fun pageSize(value: Float?) {
		_explicitPageSize = value
	}

}

class ScrollBarStyle : StyleBase() {

	override val type: StyleType<ScrollBarStyle> = ScrollBarStyle

	/**
	 * If no explicit width for horizontal scroll bars or explicit height for vertical scroll bars, this size will be
	 * used.
	 */
	var defaultSize by prop(0f)
	var decrementButton by prop(noSkin)
	var incrementButton by prop(noSkin)
	var track by prop(noSkin)
	var thumb by prop(noSkin)

	/**
	 * If true, interaction is by dragging the thumb, or clicking the track to step pages.
	 * If false, interaction is by dragging the track.
	 */
	var pageMode by prop(true)

	/**
	 * When the scroll bar is inactive, the track will tween to this alpha value.
	 */
	var inactiveAlpha by prop(1f)
	var alphaDuration by prop(0.5f)

	companion object : StyleType<ScrollBarStyle>
}

