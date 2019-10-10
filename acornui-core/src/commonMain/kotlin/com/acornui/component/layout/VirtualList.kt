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

@file:Suppress("UNUSED_ANONYMOUS_PARAMETER", "ConvertTwoComparisonsToRangeCheck")

package com.acornui.component.layout

import com.acornui.EqualityCheck
import com.acornui.behavior.Selection
import com.acornui.behavior.SelectionBase
import com.acornui.behavior.retainAll
import com.acornui.collection.ObservableList
import com.acornui.collection.forEach2
import com.acornui.collection.unshift
import com.acornui.component.*
import com.acornui.component.layout.algorithm.virtual.ItemRendererOwner
import com.acornui.component.layout.algorithm.virtual.VirtualLayoutAlgorithm
import com.acornui.component.style.Style
import com.acornui.di.Owned
import com.acornui.di.own
import com.acornui.function.as2
import com.acornui.function.as3
import com.acornui.math.Bounds
import com.acornui.math.MathUtils
import com.acornui.recycle.IndexedPool
import com.acornui.recycle.ObjectPool
import com.acornui.recycle.Pool
import com.acornui.recycle.disposeAndClear
import kotlin.jvm.JvmName
import kotlin.math.ceil
import kotlin.math.floor


interface VirtualLayoutContainer<S, out T : LayoutData> : Container {

	val layoutAlgorithm: VirtualLayoutAlgorithm<S, T>

	val layoutStyle: S

}

/**
 * A virtualized list of components, with no clipping or scrolling. This is a lower-level component, used by the [DataScroller].
 */
class VirtualList<E : Any, S : Style, out T : LayoutData>(
		owner: Owned,
		override val layoutAlgorithm: VirtualLayoutAlgorithm<S, T>,
		layoutStyle: S
) : ContainerImpl(owner), ItemRendererOwner<T>, VirtualLayoutContainer<S, T> {

	constructor(owner: Owned,
				layoutAlgorithm: VirtualLayoutAlgorithm<S, T>,
				style: S,
				data: ObservableList<E?>
	) : this(owner, layoutAlgorithm, style) {
		data(data)
	}

	constructor(owner: Owned,
				layoutAlgorithm: VirtualLayoutAlgorithm<S, T>,
				style: S,
				data: List<E?>
	) : this(owner, layoutAlgorithm, style) {
		data(data)
	}

	private var _data: List<E?> = emptyList()

	/**
	 * The data list, as set via [data].
	 */
	val data: List<E?>
		get() = _data

	override val layoutStyle: S = bind(layoutStyle)

	override fun createLayoutData(): T {
		return layoutAlgorithm.createLayoutData()
	}

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
				val lastIndex = _data.lastIndex
				_visiblePosition = 0f
				for (i in 0.._activeRenderers.lastIndex) {
					val renderer = _activeRenderers[i]
					val itemOffset = layoutAlgorithm.getOffset(width, height, renderer, renderer.index, lastIndex, isReversed = false, props = layoutStyle)
					_visiblePosition = maxOf(0f, renderer.index - itemOffset)
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
			validate(ValidationFlags.LAYOUT)
			if (_visibleBottomPosition == null) {
				// Calculate the current bottomPosition.
				_visibleBottomPosition = _data.lastIndex.toFloat()
				val size = _data.size.toFloat()
				val lastIndex = _data.lastIndex
				for (i in _activeRenderers.lastIndex downTo 0) {
					val renderer = _activeRenderers[i]
					val itemOffset = layoutAlgorithm.getOffset(width, height, renderer, renderer.index, lastIndex, isReversed = true, props = layoutStyle)
					_visibleBottomPosition = minOf(size, renderer.index + itemOffset)
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
			invalidateLayout()
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
			invalidateLayout()
		}

	//-------------------------------------------------
	// Null item renderers
	//-------------------------------------------------

	private var _nullRendererFactory: ItemRendererOwner<T>.() -> ListRenderer = { nullItemRenderer() }

	/**
	 * Sets the nullRenderer factory for this list. The nullRenderer factory is responsible for creating nullRenderers
	 * to be used in this list.
	 */
	fun nullRendererFactory(value: ItemRendererOwner<T>.() -> ListRenderer) {
		if (_nullRendererFactory === value) return
		_nullRendererFactory = value
		nullRendererPool.disposeAndClear()
	}

	private val nullRendererPool = ObjectPool { _nullRendererFactory() }

	private val nullRendererCache = IndexedPool(nullRendererPool)

	//-------------------------------------------------
	// Item renderers
	//-------------------------------------------------

	private var _rendererFactory: ItemRendererOwner<T>.() -> ListItemRenderer<E> = { simpleItemRenderer() }

	/**
	 * Sets the renderer factory for this list. The renderer factory is responsible for creating renderers to be used
	 * in this list.
	 */
	fun rendererFactory(value: ItemRendererOwner<T>.() -> ListItemRenderer<E>) {
		if (_rendererFactory === value) return
		_rendererFactory = value
		rendererPool.disposeAndClear()
	}

	private val rendererPool = ObjectPool { _rendererFactory() }

	private val rendererCache = IndexedPool(rendererPool)

	private var _emptyListRenderer: UiComponent? = null

	fun emptyListRenderer(value: ItemRendererOwner<T>.() -> UiComponent) {
		_emptyListRenderer?.dispose()
		_emptyListRenderer = addOptionalChild(value.invoke(this)).apply {
			this?.visible = false
		}
	}

	//---------------------------------------------------
	// Children
	//---------------------------------------------------

	/**
	 * All renderers, null and non-null.
	 */
	private val _activeRenderers = ArrayList<ListRendererRo>()

	/**
	 * Returns a list of currently active renderers, both null and non-null.
	 *
	 * Note: For active renderers there will be renderers in this list beyond the visible bounds, but within the buffer.
	 */
	val activeRenderers: List<ListRendererRo>
		get() {
			validate(ValidationFlags.LAYOUT)
			return _activeRenderers
		}

	/**
	 * Returns a list of currently active non-null item renderers.
	 */
	val activeItemRenderers: List<ListItemRendererRo<E>>
		get() {
			validate(ValidationFlags.LAYOUT)
			return rendererCache
		}

	/**
	 * Returns a list of currently active null renderers.
	 */
	val activeNullRenderers: List<ListRendererRo>
		get() {
			validate(ValidationFlags.LAYOUT)
			return nullRendererCache
		}

	private val _selection = own(VirtualListSelection(rendererCache))
	val selection: Selection<E> = _selection

	private var observableData: ObservableList<E?>? = null

	private fun unwatchWrappedList() {
		val old = observableData ?: return
		old.added.remove(::invalidateLayout.as2)
		old.removed.remove(::invalidateLayout.as2)
		old.changed.remove(::invalidateLayout.as3)
		old.reset.remove(::invalidateLayout)
		observableData = null
		_data = emptyList()
	}

	/**
	 * Sets the data source to the given non-observable list.
	 */
	fun data(source: List<E?>?) {
		if (_data === source) return
		unwatchWrappedList()
		_data = source ?: emptyList()
		_selection.data(_data)
		invalidateLayout()
	}

	/**
	 * Sets the data source to the given observable list, and watches for changes.
	 */
	fun data(source: ObservableList<E?>?) {
		if (observableData === source) return
		unwatchWrappedList()
		observableData = source
		_data = source ?: emptyList()
		_selection.data(_data)
		if (source != null) {
			source.added.add(::invalidateLayout.as2)
			source.removed.add(::invalidateLayout.as2)
			source.changed.add(::invalidateLayout.as3)
			source.reset.add(::invalidateLayout)
		}
		invalidateLayout()
	}

	private val laidOutRenderers = ArrayList<UiComponent>()

	override fun updateLayout(explicitWidth: Float?, explicitHeight: Float?, out: Bounds) {
		// Clear the cached visible position and visible bottom position.
		_visiblePosition = null
		_visibleBottomPosition = null
		_activeRenderers.clear()

		val isReversed = _bottomIndexPosition != null
		val startIndex = MathUtils.clamp(if (isReversed) _bottomIndexPosition!! else _indexPosition
				?: 0f, 0f, _data.lastIndex.toFloat())

		// Starting at the set position, render as many items as we can until we go out of bounds,
		// then go back to the beginning and reverse direction until we go out of bounds again.
		val currentIndex = if (isReversed) ceil(startIndex).toInt() else floor(startIndex).toInt()
		layoutElements(explicitWidth, explicitHeight, currentIndex, startIndex, isReversed, previousElement = null, laidOutRenderers = laidOutRenderers)

		val first = if (isReversed) laidOutRenderers.lastOrNull() else laidOutRenderers.firstOrNull()

		val resumeIndex = if (isReversed) currentIndex + 1 else currentIndex - 1
		layoutElements(explicitWidth, explicitHeight, resumeIndex, startIndex, !isReversed, previousElement = first, laidOutRenderers = laidOutRenderers)

		out.clear()
		layoutAlgorithm.measure(explicitWidth, explicitHeight, laidOutRenderers, layoutStyle, out)

		// Deactivate and remove all old entries if they haven't been recycled.
		nullRendererCache.forEachUnused { _, it ->
			removeChild(it)
		}
		nullRendererCache.flip()

		rendererCache.forEachUnused { _, it ->
			removeChild(it)
		}
		rendererCache.flip()

		if (_data.isEmpty()) {
			val r = _emptyListRenderer
			if (r != null) {
				r.visible = true
				laidOutRenderers.add(r)
				layoutAlgorithm.updateLayoutEntry(explicitWidth, explicitHeight, r, 0, 0f, 0, null, false, layoutStyle)
				layoutAlgorithm.measure(explicitWidth, explicitHeight, laidOutRenderers, layoutStyle, out)
			}
		} else {
			_emptyListRenderer?.visible = false
		}

		if (explicitWidth != null && explicitWidth > out.width) out.width = explicitWidth
		if (explicitHeight != null && explicitHeight > out.height) out.height = explicitHeight

		laidOutRenderers.clear() // We don't need to keep this list, it was just for measurement.
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
	private fun layoutElements(explicitWidth: Float?, explicitHeight: Float?, currentIndex: Int, startIndex: Float, isReversed: Boolean, previousElement: LayoutElementRo?, laidOutRenderers: MutableList<UiComponent>) {
		val n = _data.size
		var skipped = 0
		val d = if (isReversed) -1 else 1
		@Suppress("NAME_SHADOWING") var previousElement = previousElement
		@Suppress("NAME_SHADOWING") var currentIndex = currentIndex
		var displayIndex = currentIndex
		while (currentIndex >= 0 && currentIndex < n && skipped < MAX_SKIPPED && rendererCache.obtainedSize + nullRendererCache.obtainedSize < maxItems) {
			val data: E? = _data[currentIndex]

			val element: ListRenderer = if (data == null) {
				val nullItemRenderer = nullRendererCache.obtain(currentIndex)
				nullItemRenderer.index = currentIndex
				nullItemRenderer
			} else {
				val itemRenderer = rendererCache.obtain(currentIndex)
				itemRenderer.index = currentIndex
				itemRenderer.data = data
				val elementSelected = selection.getItemIsSelected(data)
				itemRenderer.toggled = elementSelected
				itemRenderer
			}

			if (isReversed) _activeRenderers.unshift(element) else _activeRenderers.add(element)

			if (element.parent == null)
				addChild(element)

			if (element.shouldLayout) {
				layoutAlgorithm.updateLayoutEntry(explicitWidth, explicitHeight, element, displayIndex, startIndex, n - 1, previousElement, isReversed, layoutStyle)
				previousElement = element

				if (layoutAlgorithm.shouldShowRenderer(explicitWidth, explicitHeight, element, layoutStyle)) {
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
		unwatchWrappedList()

		nullRendererCache.flip()
		nullRendererPool.clear()

		rendererCache.flip()
		rendererPool.clear()
	}

	companion object {
		const val MAX_SKIPPED = 5
	}
}

fun <E : Any, S : Style, T : LayoutData> Owned.virtualList(
		layoutAlgorithm: VirtualLayoutAlgorithm<S, T>,
		style: S,
		init: ComponentInit<VirtualList<E, S, T>> = {}): VirtualList<E, S, T> {
	val c = VirtualList<E, S, T>(this, layoutAlgorithm, style)
	c.init()
	return c
}

class VirtualListSelection<E : Any>(private val activeRenderers: List<ListItemRenderer<E>>) : SelectionBase<E>() {

	private var data: List<E?> = emptyList()

	fun data(value: List<E?>) {
		retainAll(value.filterNotNull())
		data = value
	}

	override fun walkSelectableItems(callback: (E) -> Unit) {
		for (i in 0..data.lastIndex) {
			val item = data[i] ?: continue
			callback(item)
		}
	}

	override fun onSelectionChanged(oldSelection: Set<E>, newSelection: Set<E>) {
		activeRenderers.forEach2 {
			it.toggled = getItemIsSelected(it.data!!)
		}
	}
}

interface ListRendererRo : UiComponentRo {

	/**
	 * The index of the data in the List this item renderer represents.
	 */
	val index: Int
}

interface ListRenderer : ListRendererRo, UiComponent {

	override var index: Int
}

interface ListItemRendererRo<out E> : ItemRendererRo<E>, ToggleableRo, ListRendererRo

interface ListItemRenderer<E> : ListItemRendererRo<E>, ItemRenderer<E>, Toggleable, ListRenderer


/**
 * Recycles a list of list item renderers, creating or disposing renderers only as needed.
 * This method will automatically add and remove the renderers from the receiver element container.
 *
 * @receiver The element container on which to add item renderers.
 * @param data The updated set of data items.
 * @param existingElements The stale list of item renderers. This will be modified to reflect the new item renderers.
 * @param configure If set, when the element is recycled, configure will be called after the [ListItemRenderer.data] and
 * [ListItemRenderer.index] properties have been set.
 * @param unconfigure If set, when the element is returned to a managed object pool, unconfigure will be called.
 * ([ListItemRenderer.data] will automatically be set to null)
 * @param equality If set, uses custom equality rules. This guides how to know whether an item can be recycled or not.
 * @param factory Used to create new item renderers as needed.
 */
@JvmName("recycleListItemRenderers")
fun <E, T : ListItemRenderer<E>> ElementContainer<T>.recycleItemRenderers(
		data: Iterable<E>?,
		existingElements: MutableList<T> = elements,
		configure: (element: T, item: E, index: Int) -> Unit = { _, _, _ -> },
		unconfigure: (element: T) -> Unit = {},
		equality: EqualityCheck<E?> = { a, b -> a == b },
		factory: ElementContainer<T>.() -> T
) {
	@Suppress("UNCHECKED_CAST")
	val pool = createOrReuseAttachment(RendererPoolKey(factory)) {
		ObjectPool { factory() }
	} as Pool<T>
	com.acornui.recycle.recycle(
			data,
			existingElements,
			factory = { _, _ -> pool.obtain() },
			configure = {
				element, item, index ->
				if (element.data != item)
					element.data = item
				if (element.index != index)
					element.index = index
				addElement(index, element)
				configure(element, item, index)
			},
			disposer = {
				unconfigure(it)
				it.data = null
				pool.free(it)
			},
			retriever = { it.data },
			equality = equality
	)
}