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

	override var toggled: Boolean by validationProp(false, ValidationFlags.PROPERTIES)
	override var highlighted: Boolean by validationProp(false, ValidationFlags.PROPERTIES)
	override var rowIndex: Int by validationProp(0, ValidationFlags.PROPERTIES)

	val style = bind(RowBackgroundStyle())

	private val bg = addChild(rect())

	init {
		styleTags.add(RowBackground)
		watch(style) {
			invalidate(ValidationFlags.PROPERTIES)
		}
	}

	override fun updateProperties() {
		if (toggled) {
			if (rowIndex % 2 == 0) {
				bg.style.backgroundColor = style.selectedEvenColor
			} else {
				bg.style.backgroundColor = style.selectedOddColor
			}
		} else {
			if (highlighted) {
				if (rowIndex % 2 == 0) {
					bg.style.backgroundColor = style.highlightedEvenColor
				} else {
					bg.style.backgroundColor = style.highlightedOddColor
				}
			} else {
				if (rowIndex % 2 == 0) {
					bg.style.backgroundColor = style.evenColor
				} else {
					bg.style.backgroundColor = style.oddColor
				}
			}
		}
	}

	override fun updateLayout(explicitWidth: Float?, explicitHeight: Float?, out: Bounds) {
		bg.setSize(explicitWidth, explicitHeight)
		out.set(bg.bounds)
	}
}

class RowBackgroundStyle : StyleBase() {
	override val type = Companion

	var selectedEvenColor: ColorRo by prop(Color(1f, 1f, 0f, 0.4f))
	var selectedOddColor: ColorRo by prop(Color(0.8f, 0.8f, 0f, 0.4f))
	var highlightedEvenColor: ColorRo by prop(Color(0f, 0f, 0f, 0.1f))
	var highlightedOddColor: ColorRo by prop(Color(1f, 1f, 1f, 0.1f))
	var evenColor: ColorRo by prop(Color(0f, 0f, 0f, 0.05f))
	var oddColor: ColorRo by prop(Color(1f, 1f, 1f, 0.05f))

	companion object : StyleType<RowBackgroundStyle>
}

fun Owned.rowBackground(init: ComponentInit<RowBackgroundImpl> = {}): RowBackgroundImpl {
	val r = RowBackgroundImpl(this)
	r.init()
	return r
}
