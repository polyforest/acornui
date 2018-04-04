@file:Suppress("UNUSED_ANONYMOUS_PARAMETER")

package com.acornui.component.layout

import com.acornui.collection.*
import com.acornui.component.*
import com.acornui.component.layout.algorithm.virtual.ItemRendererOwner
import com.acornui.component.layout.algorithm.virtual.VirtualLayoutAlgorithm
import com.acornui.component.style.Style
import com.acornui.core.behavior.Selection
import com.acornui.core.behavior.SelectionBase
import com.acornui.core.di.Owned
import com.acornui.core.di.own
import com.acornui.core.focus.FocusContainer
import com.acornui.math.Bounds
import com.acornui.math.MathUtils


interface VirtualLayoutContainer<S, out T : LayoutData> : Container {

	val layoutAlgorithm: VirtualLayoutAlgorithm<S, T>

	val style: S

}

/**
 * A virtualized list of components, with no clipping or scrolling. This is a lower-level component, used by the [DataScroller].
 */
class VirtualList<E, S : Style, out T : LayoutData>(
		owner: Owned,
		rendererFactory: ItemRendererOwner<T>.() -> ListItemRenderer<E>,
		override val layoutAlgorithm: VirtualLayoutAlgorithm<S, T>,
		style : S,
		val data: ObservableList<E>
) : ContainerImpl(owner), FocusContainer, ItemRendererOwner<T>, VirtualLayoutContainer<S, T> {

	override val style: S = bind(style)

	override fun createLayoutData(): T {
		return layoutAlgorithm.createLayoutData()
	}

	override var focusOrder: Float = 0f

	private var _visiblePosition: Float? = null

	/**
	 * Returns the index of the first visible renderer. This is represented as a fraction, so for example if the
	 * renderer representing index 3 is the first item visible, and it is half within bounds (including the gap),
	 * then 3.5 will be returned.
	 */
	val visiblePosition: Float
		get() {
			validate(ValidationFlags.LAYOUT)
			if (_visiblePosition == null) {
				// Calculate the current position.
				val lastIndex = data.lastIndex
				_visiblePosition = 0f
				for (i in 0.._activeRenderers.lastIndex) {
					val renderer = _activeRenderers[i]
					val itemOffset = layoutAlgorithm.getOffset(width, height, renderer, renderer.index, lastIndex, isReversed = false, props = style)
					_visiblePosition = renderer.index - itemOffset
					if (itemOffset > -1) {
						break
					}
				}
			}
			return _visiblePosition!!
		}

	private var _visibleBottomPosition: Float? = null

	/**
	 * Returns the index of the last visible renderer. This is represented as a fraction, so for example if the
	 * renderer representing index 9 is the last item visible, and it is half within bounds (including the gap),
	 * then 8.5 will be returned.
	 */
	val visibleBottomPosition: Float
		get() {
			if (_visibleBottomPosition == null) {
				// Calculate the current bottomPosition.
				validate(ValidationFlags.LAYOUT)
				_visibleBottomPosition = data.lastIndex.toFloat()
				val lastIndex = data.lastIndex
				for (i in _activeRenderers.lastIndex downTo 0) {
					val renderer = _activeRenderers[i]
					val itemOffset = layoutAlgorithm.getOffset(width, height, renderer, renderer.index, lastIndex, isReversed = true, props = style)
					_visibleBottomPosition = renderer.index + itemOffset
					if (itemOffset > -1) {
						break
					}
				}
			}
			return _visibleBottomPosition!!
		}

	//---------------------------------------------------
	// Properties
	//---------------------------------------------------

	var maxItems by validationProp(15, ValidationFlags.LAYOUT)

	/**
	 * The percent buffer out of bounds an item renderer can be before it is recycled.
	 */
	var buffer by validationProp(0.15f, ValidationFlags.LAYOUT)

	private var _indexPosition: Float? = null

	/**
	 * If set, then the layout will start with an item represented by the data at this index, then work its way
	 * forwards.
	 */
	var indexPosition: Float?
		get() = _indexPosition
		set(value) {
			if (_indexPosition == value) return
			_indexPosition = value
			_bottomIndexPosition = null
			invalidate(ValidationFlags.LAYOUT)
		}

	private var _bottomIndexPosition: Float? = null

	/**
	 * If this is set, then the layout will start with the last item represented by this bottomIndexPosition, and
	 * work its way backwards.
	 */
	var bottomIndexPosition: Float?
		get() = _bottomIndexPosition
		set(value) {
			if (_bottomIndexPosition == value) return
			_bottomIndexPosition = value
			_indexPosition = null
			invalidate(ValidationFlags.LAYOUT)
		}

	//---------------------------------------------------
	// Item Renderer Pooling
	//---------------------------------------------------

	private val pool = VirtualListPool {
		rendererFactory()
	}

	private val cache = SmartCache(pool)

	/**
	 * If set, this is invoked when an item renderer has been obtained from the pool.
	 */
	var onRendererObtained: ((ListItemRenderer<E>) -> Unit)?
		get() = pool.onObtained
		set(value) {
			pool.onObtained = value
		}

	/**
	 * If set, this is invoked when an item renderer has been returned to the pool.
	 */
	var onRendererFreed: ((ListItemRenderer<E>) -> Unit)?
		get() = pool.onFreed
		set(value) {
			pool.onFreed = value
		}


	//---------------------------------------------------
	// Children
	//---------------------------------------------------

	private val _activeRenderers = ArrayList<ListItemRenderer<E>>()

	/**
	 * Returns a list of currently active renderers. There will be renderers in this list beyond the visible bounds,
	 * but within the buffer.
	 */
	val activeRenderers: List<ListItemRendererRo<E>>
		get() {
			validate(ValidationFlags.LAYOUT)
			return _activeRenderers
		}

	private val _selection: SelectionBase<E> = own(VirtualListSelection(data, _activeRenderers))
	val selection: Selection<E>
		get() = _selection

	private val dataAddedHandler = {
		index: Int, element: E ->
		invalidate(ValidationFlags.LAYOUT)
		Unit
	}

	private val dataRemovedHandler = {
		index: Int, element: E ->
		invalidate(ValidationFlags.LAYOUT)
		Unit
	}

	private val dataChangedHandler = {
		index: Int, oldElement: E, newElement: E ->
		invalidate(ValidationFlags.LAYOUT)
		Unit
	}

	private val dataResetHandler = {
		invalidate(ValidationFlags.LAYOUT)
		Unit
	}

	init {
		// Invalidate the layout on any dataView changes.
		data.added.add(dataAddedHandler)
		data.removed.add(dataRemovedHandler)
		data.changed.add(dataChangedHandler)
		data.reset.add(dataResetHandler)
	}

	private val laidOutRenderers = ArrayList<ListItemRenderer<E>>()

	override fun updateLayout(explicitWidth: Float?, explicitHeight: Float?, out: Bounds) {
		// Clear the cached visible position and visible bottom position.
		_visiblePosition = null
		_visibleBottomPosition = null

		cache.hold(_activeRenderers)
		_activeRenderers.clear()

		val isReversed = _bottomIndexPosition != null
		val startIndex = MathUtils.clamp(if (isReversed) _bottomIndexPosition!! else _indexPosition ?: 0f, 0f, data.lastIndex.toFloat())

		// Starting at the set position, render as many items as we can until we go out of bounds,
		// then go back to the beginning and reverse direction until we go out of bounds again.
		val currentIndex = if (isReversed) MathUtils.ceil(startIndex) else MathUtils.floor(startIndex)
		layoutElements(explicitWidth, explicitHeight, currentIndex, startIndex, isReversed, previousElement = null, laidOutRenderers = laidOutRenderers)

		val first = if (isReversed) laidOutRenderers.lastOrNull() else laidOutRenderers.firstOrNull()

		val resumeIndex = if (isReversed) currentIndex + 1 else currentIndex - 1
		layoutElements(explicitWidth, explicitHeight, resumeIndex, startIndex, !isReversed, previousElement = first, laidOutRenderers = laidOutRenderers)

		out.clear()
		layoutAlgorithm.measure(explicitWidth, explicitHeight, laidOutRenderers, style, out)
		if (explicitWidth != null && explicitWidth > out.width) out.width = explicitWidth
		if (explicitHeight != null && explicitHeight > out.height) out.height = explicitHeight

		laidOutRenderers.clear() // We don't need to keep this list, it was just for measurement.

		// Deactivate and remove all old entries if they haven't been recycled.
		cache.forEach {
			removeChild(it)
		}
		cache.flush()
	}

	/**
	 * Renders the items starting at the given index until no more items will fit in the available dimensions.
	 *
	 * @param explicitWidth
	 * @param explicitHeight
	 * @param currentIndex
	 * @param startIndex
	 * @param isReversed
	 *
	 * @param laidOutRenderers The list to fill with the item renderers that were laid out by this call.
	 * [activeRenderers] is populated with the item renderers that were created by this call.
	 *
	 * @return
	 */
	private fun layoutElements(explicitWidth: Float?, explicitHeight: Float?, currentIndex: Int, startIndex: Float, isReversed: Boolean, previousElement: LayoutElement?, laidOutRenderers: MutableList<ListItemRenderer<E>>) {
		val n = data.size
		var skipped = 0
		val d = if (isReversed) -1 else 1
		@Suppress("NAME_SHADOWING") var previousElement = previousElement
		@Suppress("NAME_SHADOWING") var currentIndex = currentIndex
		var displayIndex = currentIndex
		while (currentIndex >= 0 && currentIndex < n && skipped < MAX_SKIPPED && _activeRenderers.size < maxItems) {
			val data: E = data[currentIndex]
			val element = cache.obtain(currentIndex, isReversed)
			if (currentIndex != element.index) element.index = currentIndex

			if (data != element.data) element.data = data

			val elementSelected = selection.getItemIsSelected(data)
			if (element.toggled != elementSelected)
				element.toggled = elementSelected

			if (element.parent == null)
				addChild(element)
			if (isReversed) _activeRenderers.add(0, element)
			else _activeRenderers.add(element)

			if (element.shouldLayout) {
				layoutAlgorithm.updateLayoutEntry(explicitWidth, explicitHeight, element, displayIndex, startIndex, n - 1, previousElement, isReversed, style)
				previousElement = element

				if (layoutAlgorithm.shouldShowRenderer(explicitWidth, explicitHeight, element, style)) {
					// Within bounds and good to show.
					skipped = 0

					if (isReversed) laidOutRenderers.add(0, element)
					else laidOutRenderers.add(element)
					displayIndex += d
				} else {
					// We went out of bounds, time to stop iteration.
					break
				}
			} else {
				skipped++
			}
			currentIndex += d
		}
	}

	override fun dispose() {
		super.dispose()
		data.added.remove(dataAddedHandler)
		data.removed.remove(dataRemovedHandler)
		data.changed.remove(dataChangedHandler)
		data.reset.remove(dataResetHandler)
		pool.disposeAndClear()
	}

	companion object {
		const val MAX_SKIPPED = 5
	}
}

/**
 * A layer between the creation and the pool that first seeks items from the same index, thus reducing the frequency
 * of changes to the data and index properties on the item renderers.
 */
private class SmartCache<E>(private val pool: Pool<ListItemRenderer<E>>) {

	var enabled: Boolean = true

	private val cache = HashMap<Int, ListItemRenderer<E>>()
	private val indices = ArrayList<Int>()

	fun obtain(index: Int, isReversed: Boolean): ListItemRenderer<E> {
		if (!enabled) return pool.obtain()
		val existing = cache[index]
		return if (existing == null) {
			// We don't have the exact item, but take the next one least likely to be used.
			if (cache.isEmpty()) {
				pool.obtain()
			} else {
				val index2 = indices[if (isReversed) indices.indexOfFirst2 { cache.containsKey(it) } else indices.indexOfLast2 { cache.containsKey(it) }]
				val existing2 = cache[index2]!!
				cache.remove(index2)
				existing2
			}
		} else {
			cache.remove(index)
			existing
		}
	}

	fun hold(elements: List<ListItemRenderer<E>>) {
		for (i in 0..elements.lastIndex) {
			hold(elements[i])
		}
	}

	/**
	 * Holds onto the element until the next [flush]
	 */
	fun hold(element: ListItemRenderer<E>) {
		val i = element.index
		cache[i] = element
		indices.add(i)
	}

	/**
	 * Iterates over each held item in the cache.
	 */
	fun forEach(callback: (renderer: ListItemRenderer<E>) -> Unit) {
		for (i in 0..indices.lastIndex) {
			val index = indices[i]
			val element = cache[index]
			if (element != null)
				callback(element)
		}
	}

	/**
	 * Returns all unused held items to the pool.
	 */
	fun flush() {
		if (indices.isNotEmpty()) {
			for (i in 0..indices.lastIndex) {
				// Returns all elements that were not reused back to the provider.
				val index = indices[i]
				val element = cache[index]
				if (element != null) {
					element.index = -1
					element.data = null
					pool.free(element)
				}
			}
			cache.clear()
			indices.clear()
		}
	}
}

fun <E, S : Style, T : LayoutData> Owned.virtualList(
		rendererFactory: ItemRendererOwner<T>.() -> ListItemRenderer<E>,
		layoutAlgorithm: VirtualLayoutAlgorithm<S, T>,
		style: S,
		data: ObservableList<E>, init: ComponentInit<VirtualList<E, S, T>> = {}): VirtualList<E, S, T> {
	val c = VirtualList(this, rendererFactory, layoutAlgorithm, style, data)
	c.init()
	return c
}

class VirtualListSelection<E>(private val data: List<E>, private val activeRenderers: List<ListItemRenderer<E>>) : SelectionBase<E>() {
	override fun walkSelectableItems(callback: (E) -> Unit) {
		for (i in 0..data.lastIndex) {
			callback(data[i])
		}
	}

	override fun onItemSelectionChanged(item: E, selected: Boolean) {
		for (i in 0..activeRenderers.lastIndex) {
			val renderer = activeRenderers[i]
			if (renderer.data == item) {
				renderer.toggled = selected
				break
			}
		}
	}
}

private class VirtualListPool<E>(factory: () -> ListItemRenderer<E>) : ObjectPool<ListItemRenderer<E>>(8, factory) {

	/**
	 * If set, this is invoked when an object has been obtained from the pool.
	 */
	var onObtained: ((ListItemRenderer<E>) -> Unit)? = null

	/**
	 * If set, this is invoked when an object has been returned to the pool.
	 */
	var onFreed: ((ListItemRenderer<E>) -> Unit)? = null

	override fun obtain(): ListItemRenderer<E> {
		val obj = super.obtain()
		onObtained?.invoke(obj)
		return obj
	}

	override fun free(obj: ListItemRenderer<E>) {
		onFreed?.invoke(obj)
		super.free(obj)
	}
}


interface ListItemRendererRo<out E> : ItemRendererRo<E>, ToggleableRo {

	/**
	 * The index of the data in the List this ItemRenderer represents.
	 */
	val index: Int
}

interface ListItemRenderer<E> : ListItemRendererRo<E>, ItemRenderer<E>, Toggleable {

	override var index: Int

}