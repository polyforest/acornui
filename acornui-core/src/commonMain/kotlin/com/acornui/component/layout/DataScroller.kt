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

package com.acornui.component.layout

import com.acornui.collection.ObservableList
import com.acornui.component.*
import com.acornui.component.layout.algorithm.virtual.ItemRendererOwner
import com.acornui.component.layout.algorithm.virtual.VirtualLayoutAlgorithm
import com.acornui.component.layout.algorithm.virtual.VirtualLayoutDirection
import com.acornui.component.scroll.*
import com.acornui.component.style.*
import com.acornui.behavior.Selection
import com.acornui.behavior.SelectionBase
import com.acornui.behavior.deselectNotContaining
import com.acornui.cursor.StandardCursors
import com.acornui.cursor.cursor
import com.acornui.recycle.IndexedPool
import com.acornui.recycle.Recycler
import com.acornui.recycle.disposeAndClear
import com.acornui.di.Owned
import com.acornui.di.own
import com.acornui.focus.Focusable
import com.acornui.input.Ascii
import com.acornui.input.KeyState
import com.acornui.input.interaction.MouseInteractionRo
import com.acornui.input.interaction.click
import com.acornui.input.mouseMove
import com.acornui.input.wheel
import com.acornui.math.*

// FIXME: #161 largest renderer?

class DataScroller<E : Any, out S : Style, out T : LayoutData>(
		owner: Owned,
		layoutAlgorithm: VirtualLayoutAlgorithm<S, T>,
		val layoutStyle: S
) : ContainerImpl(owner), Focusable {

	val style = bind(DataScrollerStyle())

	private val bottomContents = addChild(virtualList<E, S, T>(layoutAlgorithm, layoutStyle) {
		alpha = 0f
		interactivityMode = InteractivityMode.NONE
	})

	//---------------------------------------------------
	// Scrolling
	//---------------------------------------------------

	private val isVertical = layoutAlgorithm.direction == VirtualLayoutDirection.VERTICAL

	val scrollModel: ClampedScrollModel
		get() = scrollBar.scrollModel

	/**
	 * The maximum value this data scroller can scroll to.
	 */
	val scrollMax: Float
		get() {
			validate(ValidationFlags.LAYOUT)
			return scrollBar.scrollModel.max
		}

	private val scrollBarClipper = addChild(scrollRect())
	private val scrollBar = scrollBarClipper.addElement(if (isVertical) vScrollBar() else hScrollBar())

	/**
	 * The scroll area is just used for clipping, not scrolling.
	 */
	private val clipper = addChild(scrollRect())

	private val rowBackgrounds = clipper.addElement(container())
	private val rowBackgroundsCache = IndexedPool(Recycler(
			create = { rowBackgrounds.addElement(style.rowBackground(rowBackgrounds)) },
			configure = { it.visible = true },
			unconfigure = { it.visible = false }
	))

	private val contents = clipper.addElement(virtualList<E, S, T>(layoutAlgorithm, layoutStyle) {
		interactivityMode = InteractivityMode.CHILDREN
	})

	private val rowMap = HashMap<E, RowBackground>()

	private val _selection = own(DataScrollerSelection(contents, bottomContents, rowMap))
	val selection: Selection<E> = _selection

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
	private val keyState by KeyState

	init {
		isFocusContainer = true
		focusEnabled = true
		styleTags.add(DataScroller)
		cursor(StandardCursors.HAND)

		maxItems = 15
		scrollModel.changed.add {
			contents.indexPosition = scrollModel.value
		}
		watch(style) {
			rowBackgroundsCache.disposeAndClear()

			background?.dispose()
			background = addOptionalChild(0, it.background(this))
			scrollBarClipper.style.borderRadii = it.borderRadii
			clipper.style.borderRadii = it.borderRadii
		}

		wheel().add {
			val d = if (isVertical) it.deltaY else it.deltaX
			if (!it.handled && scrollModel.max > 0f && d != 0f && !keyState.keyIsDown(Ascii.CONTROL)) {
				it.handled = true
				tossScroller.stop()
				scrollModel.value += d / scrollBar.modelToPixels
			}
		}

		click().add {
			if (selectable && !it.handled) {
				val e = getElementUnderPosition(mousePosition(_mousePosition))
				if (e != null) {
					it.handled = true
					_selection.setSelectedItemsUser(listOf(e))
				}
			}
		}
	}

	private fun stageMouseMoveHandler(e: MouseInteractionRo) {
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
	 * Sets the nullRenderer factory for this list. The nullRenderer factory is responsible for creating nullRenderers
	 * to be used in this list.
	 */
	fun nullRendererFactory(value: ItemRendererOwner<T>.() -> ListRenderer) {
		contents.nullRendererFactory(value)
		bottomContents.nullRendererFactory(value)
	}

	/**
	 * The data list, as set via [data].
	 */
	val data: List<E?>
		get() = contents.data

	/**
	 * Sets the data source to the given observable list, and watches for changes.
	 */
	fun data(value: ObservableList<E?>?) {
		contents.data(value)
		bottomContents.data(value)
		_selection.data(value)
		_highlighted.data(value)
	}

	/**
	 * Sets the data source to the given non-observable list.
	 */
	fun data(value: List<E?>?) {
		contents.data(value)
		bottomContents.data(value)
		_selection.data(value)
		_highlighted.data(value)
	}

	fun emptyListRenderer(value: ItemRendererOwner<T>.() -> UiComponent) {
		contents.emptyListRenderer(value)
		bottomContents.emptyListRenderer(value)
	}

	override fun onActivated() {
		super.onActivated()
		stage.mouseMove().add(::stageMouseMoveHandler)
	}

	override fun onDeactivated() {
		super.onDeactivated()
		stage.mouseMove().remove(::stageMouseMoveHandler)
	}

	private fun updateHighlight() {
		val e = getElementUnderPosition(mousePosition(_mousePosition))
		_highlighted.setSelectedItemsUser(if (e == null) emptyList() else listOf(e))
	}

	private fun getElementUnderPosition(p: Vector2Ro): E? {
		for (i in 0..rowBackgroundsCache.lastIndex) {
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

		val pad = style.padding
		val w: Float? = pad.reduceWidth(explicitWidth)
		val h: Float? = pad.reduceHeight(explicitHeight)

		if (isVertical) {
			if (scrollPolicy != ScrollPolicy.OFF) {
				// First size as if the scroll bars are needed.
				val vScrollBarW = minOf(w ?: 0f, scrollBar.minWidth ?: 0f)
				val scrollAreaW = if (w == null) null else w - vScrollBarW

				if (h == null) {
					bottomContents.indexPosition = maxOf(0f, (data.size - maxItems).toFloat())
				} else {
					bottomContents.bottomIndexPosition = data.lastIndex.toFloat()
				}
				bottomContents.setSize(scrollAreaW, h)

				if (scrollPolicy == ScrollPolicy.ON || bottomContents.visiblePosition > 0f) {
					// Keep the scroll bar.
					contents.setSize(bottomContents.width, h ?: bottomContents.height)
					scrollBar.visible = true
					scrollBar.setSize(vScrollBarW, bottomContents.height)
					scrollBar.setPosition(pad.left + bottomContents.width, pad.top)
				} else {
					// Auto scroll policy and we don't need a scroll bar.
					scrollBar.visible = false
				}
			} else {
				scrollBar.visible = false
			}
			if (!scrollBar.visible) {
				contents.setSize(w, h)
				bottomContents.setSize(w, h)
			}
			out.set(pad.expandWidth2(bottomContents.width + (if (scrollBar.visible) scrollBar.width else 0f)), pad.expandHeight2(bottomContents.height))

			scrollBar.modelToPixels = bottomContents.height / maxOf(0.0001f, bottomContents.visibleBottomPosition - bottomContents.visiblePosition)
		} else {
			if (scrollPolicy != ScrollPolicy.OFF) {
				// First size as if the scroll bars are needed.
				val hScrollBarH = minOf(h ?: 0f, scrollBar.minHeight ?: 0f)
				val scrollAreaH = if (h == null) null else h - hScrollBarH
				if (w == null) {
					bottomContents.indexPosition = maxOf(0f, (data.size - maxItems).toFloat())
				} else {
					bottomContents.bottomIndexPosition = data.lastIndex.toFloat()
				}
				bottomContents.setSize(w, scrollAreaH)

				if (scrollPolicy == ScrollPolicy.ON || bottomContents.visiblePosition > 0f) {
					// Keep the scroll bar.
					contents.setSize(w ?: bottomContents.width, bottomContents.height)
					scrollBar.visible = true
					scrollBar.setSize(bottomContents.width, hScrollBarH)
					scrollBar.setPosition(pad.left, pad.top + bottomContents.height)
				} else {
					// Auto scroll policy and we don't need a scroll bar.
					scrollBar.visible = false
				}
			} else {
				scrollBar.visible = false
			}
			if (!scrollBar.visible) {
				contents.setSize(w, h)
				bottomContents.setSize(w, h)
			}
			out.set(pad.expandWidth2(bottomContents.width), pad.expandHeight2(bottomContents.height + (if (scrollBar.visible) scrollBar.height else 0f)))
			scrollBar.modelToPixels = bottomContents.width / maxOf(0.0001f, bottomContents.visibleBottomPosition - bottomContents.visiblePosition)
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
		rowBackgroundsCache.flip()

		clipper.setSize(bottomContents.width, bottomContents.height)
		clipper.setPosition(pad.left, pad.top)

		scrollBarClipper.setSize(out)
		background?.setSize(out)
	}

	private fun updateRowBackgroundForRenderer(renderer: ListRendererRo): RowBackground {
		val rowBackground = rowBackgroundsCache.obtain(renderer.index)
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

	/**
	 * The background of the data scroller.
	 */
	var background by prop(noSkinOptional)

	/**
	 * The padding between the background and elements.
	 */
	var padding by prop(Pad(0f))

	/**
	 * Used for clipping, this should match that of the background border radius.
	 */
	var borderRadii by prop(Corners(0f))

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

	fun data(value: List<E?>?) {
		val newData = value ?: emptyList()
		deselectNotContaining(newData.filterNotNull())
		data = newData
	}

	override fun walkSelectableItems(callback: (E) -> Unit) {
		for (i in 0..data.lastIndex) {
			val item = data[i] ?: continue
			callback(item)
		}
	}

	override fun onSelectionChanged(oldSelection: List<E>, newSelection: List<E>) {
		listA.selection.setSelectedItems(newSelection)
		listB.selection.setSelectedItems(newSelection)
		for (e in oldSelection - newSelection) {
			rowMap[e]?.toggled = false
		}
		for (e in newSelection - oldSelection) {
			rowMap[e]?.toggled = true
		}
	}
}

private class DataScrollerHighlight<E : Any>(private val rowMap: Map<E, RowBackground>) : SelectionBase<E>() {

	private var data = emptyList<E?>()

	fun data(value: List<E?>?) {
		val newData = value ?: emptyList()
		deselectNotContaining(newData.filterNotNull())
		data = newData
	}

	override fun walkSelectableItems(callback: (E) -> Unit) {
		for (i in 0..data.lastIndex) {
			val item = data[i] ?: continue
			callback(item)
		}
	}

	override fun onSelectionChanged(oldSelection: List<E>, newSelection: List<E>) {
		for (e in oldSelection - newSelection) {
			rowMap[e]?.highlighted = false
		}
		for (e in newSelection - oldSelection) {
			rowMap[e]?.highlighted = true
		}
	}
}
