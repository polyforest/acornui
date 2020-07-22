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

import com.acornui.component.style.StyleTag
import com.acornui.di.Context
import com.acornui.dom.addCssToHead
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

class Rect(
		owner: Context
) : DivComponent(owner) {

	init {
		addClass(styleTag)
	}

	companion object {
		val styleTag = StyleTag("Rect")

		init {
			// So rects are visible by default.
			addCssToHead("""
				$styleTag {
				  background-color: black;
				  user-select: none;
				  width: 100px;
				  height: 50px;
				  display: inline-block;
				}
			""")
		}
	}
}

inline fun Context.rect(init: ComponentInit<Rect> = {}): Rect {
	contract { callsInPlace(init, InvocationKind.EXACTLY_ONCE) }
	return Rect(this).apply(init)
}
