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
import com.acornui.core.Disposable
import com.acornui.core.di.Owned
import com.acornui.core.di.Scoped
import com.acornui.core.di.inject
import com.acornui.core.graphic.Window
import com.acornui.function.as1
import com.acornui.math.MinMaxRo
import com.acornui.reflect.observable
import kotlin.properties.ReadWriteProperty

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
	fun beforeRender(target: UiComponentRo, clip: MinMaxRo)

	/**
	 * After the component is drawn, this will be invoked. This will be in the reverse order of the component's render
	 * filters.
	 * @param clip The visible region (in viewport coordinates.)
	 */
	fun afterRender(target: UiComponentRo, clip: MinMaxRo)


}

/**
 * The base class for render filters.
 */
abstract class RenderFilterBase(private val owner: Owned) : RenderFilter, Scoped, Disposable {

	final override val injector = owner.injector

	private val window = inject(Window)

	final override var enabled: Boolean by bindable(true)

	protected fun <T> bindable(initial: T): ReadWriteProperty<Any?, T> = observable(initial) {
		window.requestRender()
	}

	init {
		owner.disposed.add(this::dispose.as1)
	}

	override fun dispose() {
		owner.disposed.remove(this::dispose.as1)
	}
}