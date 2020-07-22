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

import com.acornui.component.layout.clampHeight
import com.acornui.component.style.ObservableBase
import com.acornui.component.style.StyleTag
import com.acornui.component.style.StyleType
import com.acornui.component.style.noSkin
import com.acornui.cursor.StandardCursor
import com.acornui.cursor.cursor
import com.acornui.di.Context
import com.acornui.input.interaction.DragEventRo
import com.acornui.input.interaction.drag
import com.acornui.math.Bounds
import com.acornui.math.maxOf4
import com.acornui.math.vec2
import kotlin.math.floor

open class VDivider(owner: Context) : ElementContainerImpl<UiComponent>(owner) {


	val style by layoutProp((DividerStyle())

	private var _dividerBar: UiComponent? = null
	private var _handle: UiComponent? = null
	private var _top: UiComponent? = null
	private var _bottom: UiComponent? = null

	private var _split: Double = 0.5

	private val _mouse = vec2()

	init {
		addClass(VDivider)
		watch(style) {
			_dividerBar?.dispose()
			_handle?.dispose()

			val dividerBar = addChild(it.divideBar(this))
			_dividerBar = dividerBar
			dividerBar.drag().add(::dividerDragHandler)
			dividerBar.cursor(StandardCursor.NS_RESIZE)
			val handle = addChild(it.handle(this))
			handle.interactivityMode = InteractivityMode.NONE
			_handle = handle
		}
	}

	private fun dividerDragHandler(event: DragEventRo) {
		mousePosition(_mouse)
		split(_mouse.y / height)
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
		_top = _children.getOrNull(0)
		_bottom = _children.getOrNull(1)
	}

	fun split(): Double = _split

	/**
	 * The split is a 0.0-1.0 range representing the percent of the explicit height to divide this container between
	 * the [_top] and [_bottom] components.
	 */
	fun split(value: Double) {
		val clamped = com.acornui.math.clamp(value, 0.0, 1.0)
		if (_split == clamped) return
		_split = clamped
		invalidate(ValidationFlags.LAYOUT)
	}

	override fun updateLayout(explicitBounds: ExplicitBounds): Bounds {
		_dividerBar?.size(explicitWidth, null)

		val dividerBarHeight = _dividerBar?.height ?: 0.0
		val topHeight: Double
		val bottomHeight: Double
		if (explicitHeight != null) {
			// Bound the bottom side first, then bound the top. Top side trumps bottom.
			val h = explicitHeight - dividerBarHeight
			val bH = _bottom?.clampHeight(h * (1.0 - _split)) ?: 0.0
			topHeight = floor((_top?.clampHeight(h - bH) ?: 0.0))
			bottomHeight = minOf(bH, h - topHeight)
			_top?.size(explicitWidth, topHeight)
			_bottom?.size(explicitWidth, bottomHeight)
		} else {
			_top?.size(explicitWidth, null)
			_bottom?.size(explicitWidth, null)
			topHeight = _top?.height ?: 0.0
			bottomHeight = _bottom?.height ?: 0.0
		}
		out.width = maxOf4(explicitWidth ?: 0.0, _top?.width ?: 0.0, _bottom?.width ?: 0.0, _handle?.minWidth ?: 0.0)
		out.height = maxOf(explicitHeight ?: 0.0, topHeight + dividerBarHeight + bottomHeight)
		_top?.position(0.0, 0.0)
		if (_dividerBar != null)
			_dividerBar!!.position(0.0, (topHeight + dividerBarHeight * 0.5) - _dividerBar!!.height * 0.5)
		if (_handle != null) {
			val handle = _handle!!
			handle.size(null, null)
			if (handle.width > out.width) handle.size(out.width, null) // Don't let the handle be too large.
			handle.position((out.width - handle.width) * 0.5, (topHeight + dividerBarHeight * 0.5) - handle.height * 0.5)
		}
		_bottom?.position(0.0, topHeight + dividerBarHeight)
	}

	companion object : StyleTag

}

class DividerStyle() : ObservableBase() {

	override val type: StyleType<DividerStyle> = DividerStyle

	/**
	 * A factory for the vertical bar dividing the two sections.
	 */
	var divideBar by prop(noSkin)

	/**
	 * A factory for the handle asset that will be centered on the divider bar.
	 */
	var handle by prop(noSkin)

	companion object : StyleType<DividerStyle>
}

fun Context.vDivider(init: ComponentInit<VDivider>): VDivider {
	val v = VDivider(this)
	v.init()
	return v
}
