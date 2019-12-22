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

import com.acornui.component.style.StyleBase
import com.acornui.component.style.StyleTag
import com.acornui.component.style.StyleType
import com.acornui.di.Owned
import com.acornui.graphic.Color
import com.acornui.graphic.ColorRo
import com.acornui.math.Bounds
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

interface RowBackground : UiComponent, Toggleable {

	var rowIndex: Int

	var highlighted: Boolean

	companion object : StyleTag
}

open class RowBackgroundImpl(owner: Owned) : ContainerImpl(owner), RowBackground {

	override var toggled: Boolean by validationProp(false, BACKGROUND_COLOR)
	override var highlighted: Boolean by validationProp(false, BACKGROUND_COLOR)
	override var rowIndex: Int by validationProp(0, BACKGROUND_COLOR)

	val style = bind(RowBackgroundStyle())

	private val bg = addChild(rect { style.backgroundColor = Color.WHITE })

	init {
		styleTags.add(RowBackground)

		validation.addNode(BACKGROUND_COLOR, ValidationFlags.STYLES, ::updateColor)
	}


	private fun updateColor() {
		bg.colorTint = calculateBackgroundColor()
	}

	protected open fun calculateBackgroundColor(): ColorRo {
		return if (toggled) {
			if (rowIndex % 2 == 0) {
				style.toggledEvenColor
			} else {
				style.toggledOddColor
			}
		} else {
			if (highlighted) {
				if (rowIndex % 2 == 0) {
					style.highlightedEvenColor
				} else {
					style.highlightedOddColor
				}
			} else {
				if (rowIndex % 2 == 0) {
					style.evenColor
				} else {
					style.oddColor
				}
			}
		}
	}

	override fun updateLayout(explicitWidth: Float?, explicitHeight: Float?, out: Bounds) {
		bg.setSize(explicitWidth, explicitHeight)
		out.set(bg.bounds)
	}

	fun invalidateBackgroundColor() = invalidate(BACKGROUND_COLOR)

	companion object {
		const val BACKGROUND_COLOR = 1 shl 16
	}
}

class RowBackgroundStyle : StyleBase() {
	override val type = Companion

	var toggledEvenColor: ColorRo by prop(Color(1f, 1f, 0f, 0.4f))
	var toggledOddColor: ColorRo by prop(Color(0.8f, 0.8f, 0f, 0.4f))
	var highlightedEvenColor: ColorRo by prop(Color(0f, 0f, 0f, 0.1f))
	var highlightedOddColor: ColorRo by prop(Color(0f, 0f, 0f, 0.1f))
	var evenColor: ColorRo by prop(Color(0f, 0f, 0f, 0.05f))
	var oddColor: ColorRo by prop(Color(1f, 1f, 1f, 0.05f))

	companion object : StyleType<RowBackgroundStyle>
}

inline fun Owned.rowBackground(init: ComponentInit<RowBackgroundImpl> = {}): RowBackgroundImpl  {
	contract { callsInPlace(init, InvocationKind.EXACTLY_ONCE) }
	val r = RowBackgroundImpl(this)
	r.init()
	return r
}
