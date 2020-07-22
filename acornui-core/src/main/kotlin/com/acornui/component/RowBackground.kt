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

package com.acornui.component

import com.acornui.component.style.StyleTag
import com.acornui.component.style.StyleTagToggle
import com.acornui.di.Context
import com.acornui.dom.addCssToHead
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

interface RowBackground : UiComponent {

	var rowIndex: Int

	var highlighted: Boolean

	var toggled: Boolean

}

open class RowBackgroundImpl(owner: Context) : DivComponent(owner), RowBackground {

	override var toggled: Boolean by StyleTagToggle(toggledStyle)
	override var highlighted: Boolean by StyleTagToggle(highlightedStyle)
	override var rowIndex: Int = 0
		set(value) {
			val newIsEven = value % 2 == 0
			isEven = newIsEven
			isOdd = !newIsEven
			field = value
		}

	private var isEven by StyleTagToggle(evenStyle)
	private var isOdd by StyleTagToggle(oddStyle)

	init {
		addClass(styleTag)
		isEven = true
	}

	companion object {
		val styleTag = StyleTag("RowBackgroundImpl")
		val toggledStyle = StyleTag("RowBackgroundImpl_toggled")
		val highlightedStyle = StyleTag("RowBackgroundImpl_highlighted")
		val evenStyle = StyleTag("RowBackgroundImpl_even")
		val oddStyle = StyleTag("RowBackgroundImpl_odd")

		init {
			addCssToHead("""

$styleTag$evenStyle {
    background-color: white;
}

$styleTag$oddStyle {
    background-color: #e1e2e3ff;
}

$styleTag$evenStyle$highlightedStyle {
    background-color: #efefc3ff;
}

$styleTag$oddStyle$highlightedStyle {
    background-color: #dbdbb3ff;
}

$styleTag$evenStyle$toggledStyle {
    background-color: #f3f3b5ff;
}

$styleTag$oddStyle$toggledStyle {
    background-color: #eff0a9ff;
}
			""")
		}
	}
}

inline fun Context.rowBackground(init: ComponentInit<RowBackgroundImpl> = {}): RowBackgroundImpl  {
	contract { callsInPlace(init, InvocationKind.EXACTLY_ONCE) }
	val r = RowBackgroundImpl(this)
	r.init()
	return r
}
