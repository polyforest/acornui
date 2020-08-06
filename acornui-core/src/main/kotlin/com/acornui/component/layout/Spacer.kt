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

package com.acornui.component.layout

import com.acornui.component.ComponentInit
import com.acornui.component.Div
import com.acornui.css.Length
import com.acornui.css.px
import com.acornui.di.Context
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

inline fun Context.spacer(width: Length = 0.px, height: Length = 0.px, init: ComponentInit<Div> = {}): Div  {
	contract { callsInPlace(init, InvocationKind.EXACTLY_ONCE) }
	val s = Div(this)
	s.size(width, height)
	s.init()
	return s
}
