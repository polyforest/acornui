@file:Suppress("UNUSED_ANONYMOUS_PARAMETER")

package com.acornui.component.layout

import com.acornui.collection.*
import com.acornui.component.*
import com.acornui.component.layout.algorithm.virtual.ItemRendererOwner
import com.acornui.component.layout.algorithm.virtual.VirtualLayoutAlgorithm
import com.acornui.component.style.Style
import com.acornui.core.behavior.Selection
import com.acornui.core.behavior.SelectionBase
import com.acornui.core.behavior.deselectNotContaining
import com.acornui.core.cache.IndexedCache
import com.acornui.core.di.Owned
import com.acornui.core.di.own
import com.acornui.core.focus.FocusContainer
import com.acornui.function.as2
import com.acornui.function.as3
import com.acornui.math.Bounds
import com.acornui.math.MathUtils


interface VirtualLayoutContainer<S, out T : LayoutData> : Container {

	val layoutAlgorithm: VirtualLayoutAlgorithm<S, T>

	val style: S

}

/**
 * A virtualized list of components, with no clipping or scrolling. This is a lower-level component, used by the [DataScroller].
 */
class VirtualList<E : Any, S : Style, out T : LayoutData>(
		owner: Owned,
		override val layoutAlgorithm: VirtualLayoutAlgorithm<S, T>,
		style : S
) : ContainerImpl(owner), FocusContainer, ItemRendererOwner<T>, VirtualLayoutContainer<S, T> {

	constructor(owner: Owned,
				layoutAlgorithm: VirtualLayoutAlgorithm<S, T>,
				style : S,
				data: ObservableList<E>
	) : this(owner, layoutAlgorithm, style) {
		data(data)
	}

	constructor(owner: Owned,
				layoutAlgorithm: VirtualLayoutAlgorithm<S, T>,
				style : S,
				data: List<E>
	) : this(owner, layoutAlgorithm, style) {
		data(data)
	}

	private var data: List<E> = emptyList()

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
				for (i in 0..rendererCache.lastIndex) {
					val renderer = rendererCache[i]
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
			validate(ValidationFlags.LAYOUT)
			if (_visibleBottomPosition == null) {
				// Calculate the current bottomPosition.
				_visibleBottomPosition = data.lastIndex.toFloat()
				val lastIndex = data.lastIndex
				for (i in rendererCache.lastIndex downTo 0) {
					val renderer = rendererCache[i]
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

	private var _nullRendererFactory: ItemRendererOwner<T>.() -> NullListItemRenderer = { nullItemRenderer() }

	/**
	 * Sets the nullRenderer factory for this list. The nullRenderer factory is responsible for creating nullRenderers to be used
	 * in this list.
	 */
	fun nullRendererFactory(value: ItemRendererOwner<T>.() -> NullListItemRenderer) {
		if (_nullRendererFactory === value) return
		_nullRendererFactory = value
		nullRendererPool.disposeAndClear()
	}

	private val nullRendererPool = ObjectPool { _nullRendererFactory() }

	private val nullRendererCache = IndexedCache(nullRendererPool)

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

	private val rendererCache = IndexedCache(rendererPool)

	//---------------------------------------------------
	// Children
	//---------------------------------------------------

	/**
	 * Returns a list of currently active renderers. There will be renderers in this list beyond the visible bounds,
	 * but within the buffer.
	 */
	val activeRenderers: List<ListItemRendererRo<E>>
		get() {
			validate(ValidationFlags.LAYOUT)
			return rendererCache
		}

	private val _selection = own(VirtualListSelection(rendererCache))
	val selection: Selection<E>
		get() = _selection

	private var observableData: ObservableList<E>? = null

	private fun unwatchWrappedList() {
		val old = observableData ?: return
		old.added.remove(this::invalidateLayout.as2)
		old.removed.remove(this::invalidateLayout.as2)
		old.changed.remove(this::invalidateLayout.as3)
		old.reset.remove(this::invalidateLayout)
		observableData = null
		data = emptyList()
	}

	/**
	 * Sets the data source to the given non-observable list.
	 */
	fun data(source: List<E>) {
		if (data === source) return
		unwatchWrappedList()
		data = source
		_selection.data(source)
		invalidateLayout()
	}

	/**
	 * Sets the data source to the given observable list, and watches for changes.
	 */
	fun data(source: ObservableList<E>) {
		if (observableData === source) return
		unwatchWrappedList()
		observableData = source
		data = source
		_selection.data(source)
		source.added.add(this::invalidateLayout.as2)
		source.removed.add(this::invalidateLayout.as2)
		source.changed.add(this::invalidateLayout.as3)
		source.reset.add(this::invalidateLayout)

		invalidateLayout()
	}

	private val laidOutRenderers = ArrayList<ListItemRenderer<E>>()

	override fun updateLayout(explicitWidth: Float?, explicitHeight: Float?, out: Bounds) {
		// Clear the cached visible position and visible bottom position.
		_visiblePosition = null
		_visibleBottomPosition = null

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
		rendererCache.forEachUnused {
			removeChild(it)
		}
		rendererCache.flip()
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
		while (currentIndex >= 0 && currentIndex < n && skipped < MAX_SKIPPED && rendererCache.size < maxItems) {
			val data: E = data[currentIndex]
			val element = rendererCache.obtain(currentIndex)
			if (currentIndex != element.index) element.index = currentIndex

			if (data != element.data) element.data = data

			val elementSelected = selection.getItemIsSelected(data)
			if (element.toggled != elementSelected)
				element.toggled = elementSelected

			if (element.parent == null)
				addChild(element)

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

		unwatchWrappedList()

		nullRendererCache.flip()
		nullRendererPool.disposeAndClear()

		rendererCache.flip()
		rendererPool.disposeAndClear()
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

	private var data: List<E> = emptyList()

	fun data(value: List<E>) {
		deselectNotContaining(value)
		data = value
	}

	override fun walkSelectableItems(callback: (E) -> Unit) {
		for (i in 0..data.lastIndex) {
			callback(data[i])
		}
	}

	override fun onItemSelectionChanged(item: E, selected: Boolean) {
		activeRenderers.forEach2 {
			if (it.data == item) {
				it.toggled = selected
				return
			}
		}
	}
}

interface ListItemRendererRo<out E> : ItemRendererRo<E>, ToggleableRo {

	/**
	 * The index of the data in the List this item renderer represents.
	 */
	val index: Int
}

interface ListItemRenderer<E> : ListItemRendererRo<E>, ItemRenderer<E>, Toggleable {

	override var index: Int

}

interface NullListItemRendererRo : UiComponentRo {

	/**
	 * The index of the data in the List this item renderer represents.
	 */
	val index: Int
}

interface NullListItemRenderer : NullListItemRendererRo, UiComponent {

	override var index: Int

}