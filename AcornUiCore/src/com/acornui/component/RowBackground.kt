/*
 * Copyright 2017 Nicholas Bilyk
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

import com.acornui.component.style.*
import com.acornui.core.di.Owned
import com.acornui.graphics.Color
import com.acornui.graphics.ColorRo
import com.acornui.math.Bounds


interface RowBackground : UiComponent, Toggleable {

	var rowIndex: Int

	var highlighted: Boolean

	companion object : StyleTag
}

class RowBackgroundImpl(owner: Owned) : ContainerImpl(owner), RowBackground {

	override var toggled: Boolean by validationProp(false, BACKGROUND_COLOR)
	override var highlighted: Boolean by validationProp(false, BACKGROUND_COLOR)
	override var rowIndex: Int by validationProp(0, BACKGROUND_COLOR)

	val style = bind(RowBackgroundStyle())

	private val bg = addChild(rect { style.backgroundColor = Color.WHITE })

	init {
		styleTags.add(RowBackground)

		validation.addNode(BACKGROUND_COLOR, ValidationFlags.STYLES, this::updateColor)
	}


	private fun updateColor() {
		if (toggled) {
			if (rowIndex % 2 == 0) {
				bg.colorTint = style.toggledEvenColor
			} else {
				bg.colorTint = style.toggledOddColor
			}
		} else {
			if (highlighted) {
				if (rowIndex % 2 == 0) {
					bg.colorTint = style.highlightedEvenColor
				} else {
					bg.colorTint = style.highlightedOddColor
				}
			} else {
				if (rowIndex % 2 == 0) {
					bg.colorTint = style.evenColor
				} else {
					bg.colorTint = style.oddColor
				}
			}
		}
	}


	override fun updateLayout(explicitWidth: Float?, explicitHeight: Float?, out: Bounds) {
		bg.setSize(explicitWidth, explicitHeight)
		out.set(bg.bounds)
	}

	companion object {
		private const val BACKGROUND_COLOR = 1 shl 16
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

fun Owned.rowBackground(init: ComponentInit<RowBackgroundImpl> = {}): RowBackgroundImpl {
	val r = RowBackgroundImpl(this)
	r.init()
	return r
}
