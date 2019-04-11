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

import com.acornui.collection.MutableListBase
import com.acornui.core.Disposable
import com.acornui.core.Renderable
import com.acornui.core.di.Owned
import com.acornui.core.di.Scoped
import com.acornui.core.di.inject
import com.acornui.core.graphic.Window
import com.acornui.function.as1
import com.acornui.math.MinMax
import com.acornui.math.MinMaxRo
import com.acornui.reflect.observable
import kotlin.properties.ReadWriteProperty

/**
 * A render filter wraps the drawing of a component.
 *
 */
interface RenderFilter : Renderable {

	/**
	 * The contents this render filter should wrap.
	 */
	var contents: Renderable?

}

/**
 * The base class for render filters.
 */
abstract class RenderFilterBase(private val owner: Owned) : RenderFilter, Scoped, Disposable {

	final override val injector = owner.injector

	private val window = inject(Window)

	override val visible: Boolean = true

	var enabled: Boolean by bindable(true)

	override var contents: Renderable? = null

	override fun canvasDrawRegion(out: MinMax): MinMax = contents!!.canvasDrawRegion(out)

	protected fun <T> bindable(initial: T): ReadWriteProperty<Any?, T> = observable(initial) {
		window.requestRender()
	}

	init {
		owner.disposed.add(this::dispose.as1)
	}

	protected open fun renderContents(clip: MinMaxRo) {
		val contents = contents ?: return
		if (contents.visible)
			contents.render(clip)
	}

	override fun dispose() {
		owner.disposed.remove(this::dispose.as1)
		contents = null
	}
}

class RenderFilterList(
		tail: Renderable?
) : MutableListBase<RenderFilter>() {

	private var _tail: Renderable? = tail

	/**
	 * The renderable that will always be drawn at the end of this list.
	 */
	var tail: Renderable?
		get() = _tail
		set(value) {
			_tail = value
			_list.lastOrNull()?.contents = value
		}


	private val _list = ArrayList<RenderFilter>()
	override fun add(index: Int, element: RenderFilter) {
		_list.add(index, element)
		element.contents = _list.getOrNull(index + 1) ?: tail
		_list.getOrNull(index - 1)?.contents = element
	}

	override val size: Int
		get() = _list.size

	override fun get(index: Int): RenderFilter = _list[index]

	override fun removeAt(index: Int): RenderFilter {
		val element = _list.removeAt(index)
		element.contents = null
		_list.getOrNull(index - 1)?.contents = _list.getOrNull(index) ?: tail
		return element
	}

	override fun set(index: Int, element: RenderFilter): RenderFilter {
		val old = _list[index]
		old.contents = null
		_list[index] = element
		_list.getOrNull(index - 1)?.contents = element
		element.contents = _list.getOrNull(index + 1) ?: tail
		return old
	}
}