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

import com.acornui.core.di.Owned
import com.acornui.core.di.dKey

interface HtmlComponent : UiComponent {

	var html: String

	companion object {
		val FACTORY_KEY = dKey<(owner: Owned) -> HtmlComponent>()
	}
}

fun Owned.htmlComponent(html: String, init: ComponentInit<HtmlComponent> = {}): HtmlComponent {
	val c = injector.inject(HtmlComponent.FACTORY_KEY)(this)
	c.html = html
	c.init()
	return c
}

fun Owned.htmlComponent(init: ComponentInit<HtmlComponent> = {}): HtmlComponent {
	val c = injector.inject(HtmlComponent.FACTORY_KEY)(this)
	c.init()
	return c
}