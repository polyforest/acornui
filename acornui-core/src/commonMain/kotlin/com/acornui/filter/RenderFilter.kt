/*
 * Copyright 2018 Nicholas Bilyk
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

package com.acornui.filter

import com.acornui.component.UiComponentRo
import com.acornui.core.di.Injector
import com.acornui.core.di.Scoped
import com.acornui.core.di.inject
import com.acornui.core.graphic.Window
import com.acornui.math.MinMaxRo

/**
 * A render filter wraps the drawing of a component.
 *
 */
interface RenderFilter {

	/**
	 * If true (default), this filter will be used.
	 */
	var enabled: Boolean

	/**
	 * Before the component is drawn, this will be invoked. This will be in the order of the component's render filters.
	 * @param clip The visible region (in viewport coordinates.)
	 */
	fun beforeRender(clip: MinMaxRo)

	/**
	 * After the component is drawn, this will be invoked. This will be in the reverse order of the component's render
	 * filters.
	 * @param clip The visible region (in viewport coordinates.)
	 */
	fun afterRender(clip: MinMaxRo)


}

/**
 * The base class for render filters.
 */
abstract class RenderFilterBase(protected val target: UiComponentRo) : RenderFilter, Scoped {

	override val injector: Injector = target.injector

	private val window = target.inject(Window)

	final override var enabled: Boolean = true
		set(value) {
			if (value != field) {
				field = value
				window.requestRender()
			}
		}
}