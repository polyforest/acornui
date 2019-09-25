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
import com.acornui.di.Owned
import com.acornui.di.dKey
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

interface HtmlComponent : UiComponent {

	val boxStyle: BoxStyle

	var html: String

	companion object : StyleTag {
		val FACTORY_KEY = dKey<(owner: Owned) -> HtmlComponent>()
	}
}

inline fun Owned.htmlComponent(html: String, init: ComponentInit<HtmlComponent> = {}): HtmlComponent  {
	contract { callsInPlace(init, InvocationKind.EXACTLY_ONCE) }
	val c = injector.inject(HtmlComponent.FACTORY_KEY)(this)
	c.html = html
	c.init()
	return c
}

inline fun Owned.htmlComponent(init: ComponentInit<HtmlComponent> = {}): HtmlComponent  {
	contract { callsInPlace(init, InvocationKind.EXACTLY_ONCE) }
	val c = injector.inject(HtmlComponent.FACTORY_KEY)(this)
	c.init()
	return c
}
