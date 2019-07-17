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

package com.acornui.filter

import com.acornui.collection.MutableListBase
import com.acornui.component.RenderContextRo
import com.acornui.core.Disposable
import com.acornui.core.Renderable
import com.acornui.core.di.Owned
import com.acornui.core.di.OwnedImpl
import com.acornui.core.di.inject
import com.acornui.core.renderContext
import com.acornui.function.as1
import com.acornui.gl.core.GlState
import com.acornui.graphic.ColorRo
import com.acornui.math.*
import com.acornui.observe.Observable
import com.acornui.reflect.observable
import com.acornui.signal.Signal1
import kotlin.properties.ReadWriteProperty

/**
 * A render filter wraps the drawing of a component.
 */
interface RenderFilter : Renderable, Observable {

	/**
	 * The contents this render filter should wrap.
	 */
	var contents: Renderable?

	/**
	 * Marks any bitmap caches (if there are any) as invalid and need to be redrawn.
	 */
	fun invalidateBitmapCache()

}

/**
 * The base class for render filters.
 */
abstract class RenderFilterBase(owner: Owned) : OwnedImpl(owner), RenderFilter, Disposable {

	private val _changed = Signal1<Observable>()
	override val changed = _changed.asRo()

	protected val glState by GlState

	protected var bitmapCacheIsValid = false
		private set

	var enabled: Boolean by bindable(true)

	/**
	 * True if this filter should be skipped.
	 */
	protected open val shouldSkipFilter: Boolean
		get() = !enabled

	override var contents: Renderable? = null
		set(value) {
			if (value === this) throw Exception("Cannot set contents to self.")
			field = value
			invalidateBitmapCache()
		}

	override fun invalidateBitmapCache() {
		bitmapCacheIsValid = false
	}

	/**
	 * The padding this filter should inflate the draw region.
	 */
	open val drawPadding: PadRo = Pad.EMPTY_PAD

	override val bounds: BoundsRo
		get() = contents?.bounds ?: Bounds.EMPTY_BOUNDS

	private val _drawRegion = MinMax()

	/**
	 * @see Renderable.drawRegion
	 */
	override val drawRegion: MinMaxRo
		get() = _drawRegion.set(contents?.drawRegion).inflate(drawPadding)

	protected fun <T> bindable(initial: T): ReadWriteProperty<Any?, T> = observable(initial) {
		_changed.dispatch(this)
	}

	/**
	 * Dispatches the [changed] signal.
	 */
	protected fun notifyChanged() {
		_changed.dispatch(this)
	}

	override val naturalRenderContext: RenderContextRo
		get() = contents?.naturalRenderContext ?: inject(RenderContextRo)

	override var renderContextOverride: RenderContextRo?
		get() = contents?.renderContextOverride
		set(value) {
			contents?.renderContextOverride = value
		}

	final override fun render() {
		val renderContext = renderContext
		if (shouldSkipFilter) contents?.render()
		else draw(renderContext.clipRegion, renderContext.modelTransform, renderContext.colorTint)
		bitmapCacheIsValid = true
	}

	/**
	 * Called from render if there is something to render.
	 *
	 * @param clip [RenderContextRo.clipRegion]
	 * @param transform [RenderContextRo.modelTransform]
	 * @param tint [RenderContextRo.colorTint]
	 */
	protected abstract fun draw(clip: MinMaxRo, transform: Matrix4Ro, tint: ColorRo)

	override fun dispose() {
		super.dispose()
		contents = null
		_changed.dispose()
	}
}

class RenderFilterList(
		tail: Renderable
) : MutableListBase<RenderFilter>(), Renderable, Observable, Disposable {

	private val _list = ArrayList<RenderFilter>()
	private val _changed = Signal1<Observable>()
	override val changed = _changed.asRo()

	private var _tail: Renderable = tail
	private var head: Renderable = tail

	override fun removeAt(index: Int): RenderFilter {
		val element = _removeAt(index)
		_changed.dispatch(this)
		return element
	}

	private fun _removeAt(index: Int): RenderFilter {
		val element = _list.removeAt(index)
		_list.getOrNull(index - 1)?.contents = _list.getOrNull(index) ?: tail
		element.contents = null
		element.changed.remove(::changedHandler.as1)
		if (index == 0) {
			head = _list.firstOrNull() ?: tail
		}
		return element
	}

	private fun changedHandler() {
		_changed.dispatch(this)
	}

	/**
	 * The renderable that will always be drawn at the end of this list.
	 */
	var tail: Renderable
		get() = _tail
		set(value) {
			_tail = value
			if (head == _tail)
				head = value
			_list.lastOrNull()?.contents = value
		}

	override fun add(index: Int, element: RenderFilter) {
		val oldIndex = indexOf(element)
		if (oldIndex != -1)
			_removeAt(oldIndex)
		val newIndex = if (oldIndex == -1 || oldIndex >= index) index else index - 1
		_list.add(newIndex, element)
		element.contents = _list.getOrNull(newIndex + 1) ?: tail
		_list.getOrNull(newIndex - 1)?.contents = element
		element.changed.add(::changedHandler.as1)
		_changed.dispatch(this)

		if (index == 0)
			head = element
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
		old.changed.remove(::changedHandler.as1)
		element.changed.add(::changedHandler.as1)
		_changed.dispatch(this)
		return old
	}

	//-------------------------------------------
	// Renderable
	//-------------------------------------------

	override val drawRegion: MinMaxRo
		get() = head.drawRegion

	override var renderContextOverride: RenderContextRo?
		get() = head.renderContextOverride
		set(value) {
			head.renderContextOverride = value
		}

	override val naturalRenderContext: RenderContextRo
		get() = head.naturalRenderContext

	override fun render() = head.render()

	override val bounds: BoundsRo
		get() = head.bounds

	//-------------------------------------------

	override fun dispose() {
		clear()
		_changed.dispose()
	}
}
