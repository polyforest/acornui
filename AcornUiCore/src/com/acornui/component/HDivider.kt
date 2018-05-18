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

package com.acornui.component

import com.acornui.component.layout.SizeConstraints
import com.acornui.component.layout.clampWidth
import com.acornui.component.style.StyleTag
import com.acornui.core.cursor.StandardCursors
import com.acornui.core.cursor.cursor
import com.acornui.core.di.Owned
import com.acornui.core.floor
import com.acornui.core.input.interaction.DragInteractionRo
import com.acornui.core.input.interaction.drag
import com.acornui.math.Bounds
import com.acornui.math.MathUtils
import com.acornui.math.Vector2
import com.acornui.math.maxOf4

open class HDivider(owner: Owned) : ElementContainerImpl<UiComponent>(owner) {

	val style = bind(DividerStyle())

	private var _dividerBar: UiComponent? = null
	private var _handle: UiComponent? = null
	private var _left: UiComponent? = null
	private var _right: UiComponent? = null

	private var _split: Float = 0.5f

	private val _mouse = Vector2()

	init {
		styleTags.add(HDivider)
		watch(style) {
			_dividerBar?.dispose()
			_handle?.dispose()

			val dividerBar = addChild(it.divideBar(this))
			_dividerBar = dividerBar
			dividerBar.drag().add(this::dividerDragHandler)
			dividerBar.cursor(StandardCursors.RESIZE_E)
			val handle = addChild(it.handle(this))
			_handle = handle
			handle.interactivityMode = InteractivityMode.NONE
		}
	}

	private fun dividerDragHandler(event: DragInteractionRo) {
		mousePosition(_mouse)
		split(_mouse.x / width)
	}

	override fun onElementAdded(index: Int, element: UiComponent) {
		super.onElementAdded(index, element)
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

	fun split(): Float = _split

	/**
	 * The split is a 0f-1f range representing the percent of the explicit width to divide this container between
	 * the [_left] and [_right] components.
	 */
	fun split(value: Float) {
		val clamped = MathUtils.clamp(value, 0f, 1f)
		if (_split == clamped) return
		_split = clamped
		invalidate(ValidationFlags.LAYOUT)
	}

	override fun updateSizeConstraints(out: SizeConstraints) {
		var mW = 0f
		if (_left != null) {
			out.height.bound(_left!!.sizeConstraints.height)
			mW += _left!!.minWidth ?: 0f
		}
		mW += _handle?.width ?: 0f
		if (_right != null) {
			out.height.bound(_right!!.sizeConstraints.height)
			mW += _right!!.minWidth ?: 0f
		}
		out.width.min = mW
	}

	override fun updateLayout(explicitWidth: Float?, explicitHeight: Float?, out: Bounds) {
		_dividerBar?.setSize(null, explicitHeight)

		val dividerBarWidth = _dividerBar?.width ?: 0f
		val leftWidth: Float
		val rightWidth: Float
		if (explicitWidth != null) {
			// Bound the right side first, then bound the left. Left side trumps right.
			val w = explicitWidth - dividerBarWidth
			val rW = _right?.clampWidth(w * (1f - _split)) ?: 0f
			leftWidth = (_left?.clampWidth(w - rW) ?: 0f).floor()
			rightWidth = minOf(rW, w - leftWidth)
			_left?.setSize(leftWidth, explicitHeight)
			_right?.setSize(rightWidth, explicitHeight)
		} else {
			_left?.setSize(null, explicitHeight)
			_right?.setSize(null, explicitHeight)
			leftWidth = _left?.width ?: 0f
			rightWidth = _right?.width ?: 0f
		}
		out.width = maxOf(explicitWidth ?: 0f, leftWidth + dividerBarWidth + rightWidth)
		out.height = maxOf4(explicitHeight ?: 0f, _left?.height ?: 0f, _right?.height ?: 0f, _handle?.minHeight ?: 0f)
		_left?.moveTo(0f, 0f)
		if (_dividerBar != null)
			_dividerBar!!.moveTo(leftWidth, 0f)
		_right?.moveTo(leftWidth + dividerBarWidth, 0f)
		if (_handle != null) {
			val handle = _handle!!
			handle.setSize(null, null)
			if (handle.height > out.height) handle.setSize(null, out.height) // Don't let the handle be too large.
			handle.moveTo((leftWidth + dividerBarWidth * 0.5f) - handle.width * 0.5f, (out.height - handle.height) * 0.5f)
		}
	}

	companion object : StyleTag

}

fun Owned.hDivider(init: ComponentInit<HDivider>): HDivider {
	val h = HDivider(this)
	h.init()
	return h
}