/*
 * Copyright 2020 Poly Forest, LLC
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

@file:Suppress("ObjectPropertyName")

package com.acornui.component.scroll

import com.acornui.Disposable
import com.acornui.component.ComponentInit
import com.acornui.component.Div
 import com.acornui.component.style.cssClass
import com.acornui.css.cssVar
import com.acornui.di.Context
import com.acornui.dom.addCssToHead
import com.acornui.input.scrolled
import com.acornui.own
import com.acornui.signal.signal
import com.acornui.skins.Theme
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

open class ScrollArea(
		owner: Context
) : Div(owner) {

	init {
		addClass(ScrollAreaStyle.scrollArea)
	}

	/**
	 * The horizontal overflow property.
	 * @see Overflow
	 */
	var overflowX: String
		get() = dom.style.overflowX
		set(value) {
			dom.style.overflowX = value
		}

	/**
	 * The vertical overflow property.
	 * @see Overflow
	 */
	var overflowY: String
		get() = dom.style.overflowY
		set(value) {
			dom.style.overflowY = value
		}

	private val _hScrollModel = own(object : ScrollRectScrollModel() {

		override var rawValue: Double
			get() = dom.scrollLeft
			set(value) {
				dom.scrollLeft = value
			}

		override val max: Double
			get() = (dom.scrollWidth - dom.clientWidth).toDouble()
	})

	val hScrollModel: ScrollModelFixedBounds = _hScrollModel

	private val _vScrollModel = own(object : ScrollRectScrollModel() {

		override var rawValue: Double
			get() = dom.scrollTop
			set(value) {
				dom.scrollTop = value
			}

		override val max: Double
			get() = (dom.scrollHeight - dom.clientHeight).toDouble()
	})

	val vScrollModel: ClampedScrollModelRo = _vScrollModel


	private abstract inner class ScrollRectScrollModel  : ScrollModelFixedBounds, Disposable {

		override val changed = signal<ClampedScrollModelRo>()

		override val min: Double = 0.0

		override val snap: Double = 0.0

		private var lastValue = 0.0

		init {
			scrolled.listen {
				val newValue = rawValue
				if (lastValue != newValue) {
					lastValue = newValue
					changed.dispatch(this)
				}
			}
		}

		override fun dispose() {
			changed.dispose()
		}
	}

}

object ScrollAreaStyle {

	val scrollArea by cssClass()

	init {
		addCssToHead("""
$scrollArea {
overflow: auto;
padding: ${cssVar(Theme::padding)};
}
		""")
	}
}

inline fun Context.scrollArea(init: ComponentInit<ScrollArea> = {}): ScrollArea {
	contract { callsInPlace(init, InvocationKind.EXACTLY_ONCE) }
	val s = ScrollArea(this)
	s.init()
	return s
}