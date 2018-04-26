package com.acornui.component.layout

import com.acornui.collection.ObservableList
import com.acornui.component.*
import com.acornui.component.layout.algorithm.virtual.ItemRendererOwner
import com.acornui.component.layout.algorithm.virtual.VirtualLayoutAlgorithm
import com.acornui.component.layout.algorithm.virtual.VirtualLayoutDirection
import com.acornui.component.scroll.*
import com.acornui.component.style.*
import com.acornui.core.behavior.Selection
import com.acornui.core.behavior.SelectionBase
import com.acornui.core.behavior.deselectNotContaining
import com.acornui.core.cache.IndexedCache
import com.acornui.core.cache.disposeAndClear
import com.acornui.core.cache.hideAndFlip
import com.acornui.core.di.Owned
import com.acornui.core.di.own
import com.acornui.core.focus.Focusable
import com.acornui.core.input.interaction.MouseInteractionRo
import com.acornui.core.input.interaction.click
import com.acornui.core.input.mouseMove
import com.acornui.core.input.wheel
import com.acornui.math.Bounds
import com.acornui.math.Vector2
import com.acornui.math.Vector2Ro

// TODO: largest renderer?

class DataScroller<E : Any, out S : Style, out T : LayoutData>(
		owner: Owned,
		layoutAlgorithm: VirtualLayoutAlgorithm<S, T>,
		val layoutStyle: S
) : ContainerImpl(owner), Focusable {

	override var focusEnabled: Boolean = false // Layout containers by default are not focusable.
	override var focusOrder: Float = 0f
	override var highlight: UiComponent? by createSlot()

	val style = bind(DataScrollerStyle())

	private val bottomContents = addChild(virtualList<E, S, T>(layoutAlgorithm, layoutStyle) {
		alpha = 0f
		interactivityMode = InteractivityMode.NONE
	})

	//---------------------------------------------------
	// Scrolling
	//---------------------------------------------------

	private val isVertical = layoutAlgorithm.direction == VirtualLayoutDirection.VERTICAL

	val scrollModel: ScrollModel
		get() = scrollBar.scrollModel

	private val scrollBar = addChild(if (isVertical) vScrollBar() else hScrollBar())

	/**
	 * The scroll area is just used for clipping, not scrolling.
	 */
	private val clipper = addChild(scrollRect())

	private val rowBackgrounds = clipper.addElement(container())
	private val rowBackgroundsCache = IndexedCache {
		style.rowBackground(rowBackgrounds)
	}

	private val contents = clipper.addElement(virtualList<E, S, T>(layoutAlgorithm, layoutStyle) {
		interactivityMode = InteractivityMode.CHILDREN
	})

	private val rowMap = HashMap<E, RowBackground>()

	private val _selection = own(DataScrollerSelection(contents, bottomContents, rowMap))

	val selection: Selection<E>
		get() = _selection

	private val _highlighted = own(DataScrollerHighlight(rowMap))

	val highlighted: Selection<E>
		get() = _highlighted


	/**
	 * Determines the behavior of whether or not the scroll bar is displayed.
	 */
	var scrollPolicy by validationProp(ScrollPolicy.AUTO, ValidationFlags.LAYOUT)

	private val tossScroller = clipper.enableTossScrolling()
	private val tossBinding = own(TossScrollModelBinding(tossScroller,
			if (isVertical) ScrollModelImpl() else scrollBar.scrollModel,
			if (!isVertical) ScrollModelImpl() else scrollBar.scrollModel))

	//---------------------------------------------------
	// Properties
	//---------------------------------------------------

	/**
	 * The maximum number of items to render before scrolling.
	 * Note that invisible renderers are counted.
	 * If there are explicit dimensions, those are still honored for virtualization.
	 */
	var maxItems: Int
		get() = bottomContents.maxItems
		set(value) {
			contents.maxItems = value + 1
			bottomContents.maxItems = value
		}

	//---------------------------------------------------
	// Item Renderer Pooling
	//---------------------------------------------------

	var selectable: Boolean = true
	var highlightable: Boolean = true

	private var background: UiComponent? = null

	private val _mousePosition = Vector2()

	private val stageMouseMoveHandler = { e: MouseInteractionRo ->
		if (highlightable) updateHighlight()
	}

	/**
	 * Sets the renderer factory for this list. The renderer factory is responsible for creating renderers to be used
	 * in this scroller.
	 */
	fun rendererFactory(value: ItemRendererOwner<T>.() -> ListItemRenderer<E>) {
		contents.rendererFactory(value)
		bottomContents.rendererFactory(value)
	}

	/**
	 * The data list, as set via [data].
	 */
	val data: List<E?>
		get() = contents.data

	/**
	 * Sets the data source to the given observable list, and watches for changes.
	 */
	fun data(value: ObservableList<E?>) {
		contents.data(value)
		bottomContents.data(value)
		_selection.data(value)
		_highlighted.data(value)
	}

	/**
	 * Sets the data source to the given non-observable list.
	 */
	fun data(value: List<E?>) {
		contents.data(value)
		bottomContents.data(value)
		_selection.data(value)
		_highlighted.data(value)
	}

	init {
		styleTags.add(DataScroller)
		maxItems = 15
		scrollModel.changed.add {
			contents.indexPosition = scrollModel.value
		}
		watch(style) {
			rowBackgroundsCache.disposeAndClear()

			background?.dispose()
			background = addOptionalChild(0, it.background(this))
		}

		wheel().add {
			tossScroller.stop()
			scrollModel.value += (if (isVertical) it.deltaY else it.deltaX) / scrollBar.modelToPixels
		}

		click().add {
			if (selectable && !it.handled) {
				val e = getElementUnderPosition(mousePosition(_mousePosition))
				if (e != null) {
					it.handled = true
					selection.selectedItem = e
				}
			}
		}
	}

	override fun onActivated() {
		super.onActivated()
		stage.mouseMove().add(stageMouseMoveHandler)
	}

	override fun onDeactivated() {
		super.onDeactivated()
		stage.mouseMove().remove(stageMouseMoveHandler)
	}

	private fun updateHighlight() {
		val e = getElementUnderPosition(mousePosition(_mousePosition))
		highlighted.selectedItem = e
	}

	private fun getElementUnderPosition(p: Vector2Ro): E? {
		for (i in 0 .. rowBackgroundsCache.lastIndex) {
			val bg = rowBackgroundsCache[i]
			if (p.x >= bg.x && p.y >= bg.y && p.x < bg.right && p.y < bg.bottom) {
				return data.getOrNull(bg.rowIndex)
			}
		}
		return null
	}

	override fun updateLayout(explicitWidth: Float?, explicitHeight: Float?, out: Bounds) {
		// The typical ScrollArea implementations optimize for the case of not needing scroll bars, but the
		// DataScroller optimizes for the case of needing one. That is, size first with the scroll bar, and if it's not
		// needed, remove it.
		if (isVertical) {
			if (scrollPolicy != ScrollPolicy.OFF) {
				// First size as if the scroll bars are needed.
				val vScrollBarW = minOf(explicitWidth ?: 0f, scrollBar.minWidth ?: 0f)
				val scrollAreaW = if (explicitWidth == null) null else explicitWidth - vScrollBarW

				if (explicitHeight == null) {
					bottomContents.indexPosition = maxOf(0f, (data.size - maxItems).toFloat())
				} else {
					bottomContents.bottomIndexPosition = data.lastIndex.toFloat()
				}
				bottomContents.setSize(scrollAreaW, explicitHeight)

				if (scrollPolicy == ScrollPolicy.ON || bottomContents.visiblePosition > 0f) {
					// Keep the scroll bar.
					contents.setSize(bottomContents.width, explicitHeight ?: bottomContents.height)
					scrollBar.visible = true
					scrollBar.setSize(vScrollBarW, bottomContents.height)
					scrollBar.setPosition(bottomContents.width, 0f)
				} else {
					// Auto scroll policy and we don't need a scroll bar.
					scrollBar.visible = false
				}
			} else {
				scrollBar.visible = false
			}
			if (!scrollBar.visible) {
				contents.setSize(explicitWidth, explicitHeight)
				bottomContents.setSize(explicitWidth, explicitHeight)
			}
			out.set(bottomContents.width + (if (scrollBar.visible) scrollBar.width else 0f), bottomContents.height)

			scrollBar.modelToPixels = out.height / maxOf(0.0001f, bottomContents.visibleBottomPosition - bottomContents.visiblePosition)
		} else {
			if (scrollPolicy != ScrollPolicy.OFF) {
				// First size as if the scroll bars are needed.
				val hScrollBarH = minOf(explicitHeight ?: 0f, scrollBar.minHeight ?: 0f)
				val scrollAreaH = if (explicitHeight == null) null else explicitHeight - hScrollBarH
				if (explicitWidth == null) {
					bottomContents.indexPosition = maxOf(0f, (data.size - maxItems).toFloat())
				} else {
					bottomContents.bottomIndexPosition = data.lastIndex.toFloat()
				}
				bottomContents.setSize(explicitWidth, scrollAreaH)

				if (scrollPolicy == ScrollPolicy.ON || bottomContents.visiblePosition > 0f) {
					// Keep the scroll bar.
					contents.setSize(explicitWidth ?: bottomContents.width, bottomContents.height)
					scrollBar.visible = true
					scrollBar.setSize(bottomContents.width, hScrollBarH)
					scrollBar.setPosition(0f, bottomContents.height)
				} else {
					// Auto scroll policy and we don't need a scroll bar.
					scrollBar.visible = false
				}
			} else {
				scrollBar.visible = false
			}
			if (!scrollBar.visible) {
				contents.setSize(explicitWidth, explicitHeight)
				bottomContents.setSize(explicitWidth, explicitHeight)
			}
			out.set(bottomContents.width, bottomContents.height + (if (scrollBar.visible) scrollBar.height else 0f))
			scrollBar.modelToPixels = out.width / maxOf(0.0001f, bottomContents.visibleBottomPosition - bottomContents.visiblePosition)
		}
		tossBinding.modelToPixelsX = scrollBar.modelToPixels
		tossBinding.modelToPixelsY = scrollBar.modelToPixels
		scrollBar.scrollModel.max = bottomContents.visiblePosition

		rowMap.clear()

		val itemRenderers = contents.activeItemRenderers
		for (i in 0..itemRenderers.lastIndex) {
			val itemRenderer = itemRenderers[i]
			val rowBackground = updateRowBackgroundForRenderer(itemRenderer)
			val e = itemRenderer.data ?: continue
			rowMap[e] = rowBackground
			rowBackground.toggled = _selection.getItemIsSelected(e)
			rowBackground.highlighted = _highlighted.getItemIsSelected(e)
		}
		val nullItemRenderers = contents.activeNullRenderers
		for (i in 0..nullItemRenderers.lastIndex) {
			val rowBackground = updateRowBackgroundForRenderer(nullItemRenderers[i])
			rowBackground.toggled = false
			rowBackground.highlighted = false
		}

		rowBackgroundsCache.hideAndFlip()

		clipper.setSize(bottomContents.width, bottomContents.height)
		background?.setSize(out)
		highlight?.setSize(out)
	}

	private fun updateRowBackgroundForRenderer(renderer: ListRendererRo): RowBackground {
		val rowBackground = rowBackgroundsCache.obtain(renderer.index)
		if (rowBackground.parent == null) {
			rowBackgrounds.addElement(rowBackground)
		}
		rowBackground.visible = true
		rowBackground.rowIndex = renderer.index
		if (isVertical) {
			rowBackground.setSize(bottomContents.width, renderer.height)
			rowBackground.moveTo(0f, renderer.y)
		} else {
			rowBackground.setSize(renderer.width, bottomContents.height)
			rowBackground.moveTo(renderer.x, 0f)
		}
		return rowBackground
	}

	companion object : StyleTag
}

class DataScrollerStyle : StyleBase() {

	override val type: StyleType<DataScrollerStyle> = DataScrollerStyle

	var background by prop(noSkinOptional)

	/**
	 * The background for each row.
	 */
	var rowBackground by prop<Owned.() -> RowBackground>({ rowBackground() })

	companion object : StyleType<DataScrollerStyle>
}

private class DataScrollerSelection<E : Any>(
		private val listA: VirtualList<E, *, *>,
		private val listB: VirtualList<E, *, *>,
		private val rowMap: Map<E, RowBackground>
) : SelectionBase<E>() {

	private var data = emptyList<E?>()

	fun data(value: List<E?>) {
		deselectNotContaining(value)
		data = value
	}

	override fun walkSelectableItems(callback: (E) -> Unit) {
		for (i in 0..data.lastIndex) {
			val item = data[i] ?: continue
			callback(item)
		}
	}

	override fun onItemSelectionChanged(item: E, selected: Boolean) {
		listA.selection.setItemIsSelected(item, selected)
		listB.selection.setItemIsSelected(item, selected) // Only necessary if variable sizes.
		rowMap[item]?.toggled = selected
	}
}

private class DataScrollerHighlight<E : Any>(private val rowMap: Map<E, RowBackground>) : SelectionBase<E>() {

	private var data = emptyList<E?>()

	fun data(value: List<E?>) {
		getSelectedItems(false, ArrayList()).forEach {
			if (!value.contains(it))
				setItemIsSelected(it, false)

		}
		data = value
	}

	override fun walkSelectableItems(callback: (E) -> Unit) {
		for (i in 0..data.lastIndex) {
			val item = data[i] ?: continue
			callback(item)
		}
	}

	override fun onItemSelectionChanged(item: E, selected: Boolean) {
		rowMap[item]?.highlighted = selected
	}
}