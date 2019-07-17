package com.acornui.filter

import com.acornui.collection.MutableListBase
import com.acornui.component.RenderContextRo
import com.acornui.core.Disposable
import com.acornui.core.Renderable
import com.acornui.function.as1
import com.acornui.math.BoundsRo
import com.acornui.math.MinMaxRo
import com.acornui.observe.Observable
import com.acornui.signal.Signal1

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