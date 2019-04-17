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
import com.acornui.core.di.OwnedImpl
import com.acornui.function.as1
import com.acornui.graphic.ColorRo
import com.acornui.math.Matrix4Ro
import com.acornui.math.MinMax
import com.acornui.math.MinMaxRo
import com.acornui.observe.Observable
import com.acornui.reflect.observable
import com.acornui.signal.Signal1
import kotlin.properties.ReadWriteProperty

/**
 * A render filter wraps the drawing of a component.
 *
 */
interface RenderFilter : Renderable, Observable {

	/**
	 * The contents this render filter should wrap.
	 */
	var contents: Renderable?

}

/**
 * The base class for render filters.
 */
abstract class RenderFilterBase(owner: Owned) : OwnedImpl(owner), RenderFilter, Disposable {

	private val _changed = Signal1<Observable>()
	override val changed = _changed.asRo()

	var enabled: Boolean by bindable(true)

	/**
	 * True if this filter should be skipped.
	 */
	protected open val shouldSkipFilter: Boolean
		get() = !enabled

	override var contents: Renderable? = null

	override fun drawRegion(out: MinMax): MinMax = contents!!.drawRegion(out)

	protected fun <T> bindable(initial: T): ReadWriteProperty<Any?, T> = observable(initial) {
		_changed.dispatch(this)
	}

	final override fun render(clip: MinMaxRo, transform: Matrix4Ro, tint: ColorRo) {
		if (shouldSkipFilter) contents?.render(clip, transform, tint)
		else draw(clip, transform, tint)
	}

	abstract fun draw(clip: MinMaxRo, transform: Matrix4Ro, tint: ColorRo)

	override fun dispose() {
		super.dispose()
		contents = null
		_changed.dispose()
	}
}

class RenderFilterList(
		tail: Renderable?
) : MutableListBase<RenderFilter>(), Observable, Disposable {

	private val _list = ArrayList<RenderFilter>()
	private val _changed = Signal1<Observable>()
	override val changed = _changed.asRo()

	private var _tail: Renderable? = tail

	override fun removeAt(index: Int): RenderFilter {
		val element = _list.removeAt(index)
		element.contents = null
		_list.getOrNull(index - 1)?.contents = _list.getOrNull(index) ?: tail
		element.changed.remove(::notifyChanged.as1)
		_changed.dispatch(this)
		return element
	}

	private fun notifyChanged() {
		_changed.dispatch(this)
	}

	/**
	 * The renderable that will always be drawn at the end of this list.
	 */
	var tail: Renderable?
		get() = _tail
		set(value) {
			_tail = value
			_list.lastOrNull()?.contents = value
		}

	override fun add(index: Int, element: RenderFilter) {
		_list.add(index, element)
		element.contents = _list.getOrNull(index + 1) ?: tail
		_list.getOrNull(index - 1)?.contents = element
		element.changed.add(::notifyChanged.as1)
		_changed.dispatch(this)
	}

	override val size: Int
		get() = _list.size

	override fun get(index: Int): RenderFilter = _list[index]

	override fun set(index: Int, element: RenderFilter): RenderFilter {
		val old = _list[index]
		old.contents = null
		_list[index] = element
		_list.getOrNull(index - 1)?.contents = element
		element.contents = _list.getOrNull(index + 1) ?: tail
		old.changed.remove(::notifyChanged.as1)
		element.changed.add(::notifyChanged.as1)
		_changed.dispatch(this)
		return old
	}

	override fun dispose() {
		clear()
		_changed.dispose()
	}
}