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

package com.acornui.component

import com.acornui.component.layout.clampWidth
import com.acornui.component.style.StyleTag
import com.acornui.cursor.StandardCursor
import com.acornui.cursor.cursor
import com.acornui.di.Context
import com.acornui.input.interaction.DragEventRo
import com.acornui.input.interaction.drag
import com.acornui.math.Bounds
import com.acornui.math.maxOf4
import com.acornui.math.vec2
import kotlin.math.floor

open class HDivider(owner: Context) : ElementContainerImpl<UiComponent>(owner) {

	val style = bind(DividerStyle())

	private var _dividerBar: UiComponent? = null
	private var _handle: UiComponent? = null
	private var _left: UiComponent? = null
	private var _right: UiComponent? = null

	private var _split: Double = 0.5

	private val dragTmp = vec2()

	init {
		addClass(HDivider)
		watch(style) {
			_dividerBar?.dispose()
			_handle?.dispose()

			val dividerBar = addChild(it.divideBar(this))
			_dividerBar = dividerBar
			dividerBar.drag().add(::dividerDragHandler)
			dividerBar.cursor(StandardCursor.EW_RESIZE)
			val handle = addChild(it.handle(this))
			_handle = handle
			handle.interactivityMode = InteractivityMode.NONE
		}
	}

	private fun dividerDragHandler(event: DragEventRo) {
//		mousePosition(dragTmp)
		event.position
		split(dragTmp.x / width)
	}

	override fun onElementAdded(oldIndex: Int, newIndex: Int, element: UiComponent) {
		super.onElementAdded(oldIndex, newIndex, element)
		refreshParts()
	}

	override fun onElementRemoved(index: Int, element: UiComponent) {
		super.onElementRemoved(index, element)
		refreshParts()
	}

	private fun refreshParts() {
		_left = _children.getOrNull(0)
		_right = _children.getOrNull(1)
	}

	fun split(): Double = _split

	/**
	 * The split is a 0.0-1.0 range representing the percent of the explicit width to divide this container between
	 * the [_left] and [_right] components.
	 */
	fun split(value: Double) {
		val clamped = com.acornui.math.clamp(value, 0.0, 1.0)
		if (_split == clamped) return
		_split = clamped
		invalidate(ValidationFlags.LAYOUT)
	}

	override fun updateLayout(explicitBounds: ExplicitBounds): Bounds {
		_dividerBar?.size(null, explicitHeight)

		val dividerBarWidth = _dividerBar?.width ?: 0.0
		val leftWidth: Double
		val rightWidth: Double
		if (explicitWidth != null) {
			// Bound the right side first, then bound the left. Left side trumps right.
			val w = explicitWidth - dividerBarWidth
			val rW = _right?.clampWidth(w * (1.0 - _split)) ?: 0.0
			leftWidth = floor((_left?.clampWidth(w - rW) ?: 0.0))
			rightWidth = minOf(rW, w - leftWidth)
			_left?.size(leftWidth, explicitHeight)
			_right?.size(rightWidth, explicitHeight)
		} else {
			_left?.size(null, explicitHeight)
			_right?.size(null, explicitHeight)
			leftWidth = _left?.width ?: 0.0
			rightWidth = _right?.width ?: 0.0
		}
		out.width = maxOf(explicitWidth ?: 0.0, leftWidth + dividerBarWidth + rightWidth)
		out.height = maxOf4(explicitHeight ?: 0.0, _left?.height ?: 0.0, _right?.height ?: 0.0, _handle?.minHeight ?: 0.0)
		_left?.position(0.0, 0.0)
		if (_dividerBar != null)
			_dividerBar!!.position(leftWidth, 0.0)
		_right?.position(leftWidth + dividerBarWidth, 0.0)
		if (_handle != null) {
			val handle = _handle!!
			handle.size(null, null)
			if (handle.height > out.height) handle.size(null, out.height) // Don't let the handle be too large.
			handle.position((leftWidth + dividerBarWidth * 0.5) - handle.width * 0.5, (out.height - handle.height) * 0.5)
		}
	}

	companion object : StyleTag

}

fun Context.hDivider(init: ComponentInit<HDivider>): HDivider {
	val h = HDivider(this)
	h.init()
	return h
}
