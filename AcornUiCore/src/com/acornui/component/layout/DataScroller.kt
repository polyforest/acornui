package com.acornui.component.layout

import com.acornui.collection.ActiveList
import com.acornui.collection.ObservableList
import com.acornui.component.*
import com.acornui.component.layout.algorithm.virtual.ItemRendererOwner
import com.acornui.component.layout.algorithm.virtual.VirtualLayoutAlgorithm
import com.acornui.component.layout.algorithm.virtual.VirtualLayoutDirection
import com.acornui.component.scroll.*
import com.acornui.component.style.*
import com.acornui.core.behavior.Selection
import com.acornui.core.behavior.SelectionBase
import com.acornui.core.di.Owned
import com.acornui.core.di.own
import com.acornui.core.focus.Focusable
import com.acornui.core.input.wheel
import com.acornui.math.Bounds

// TODO: largest renderer?
// TODO: I don't love the virtual layout algorithms.

class DataScroller<E, S : Style, out T : LayoutData>(
		owner: Owned,
		rendererFactory: ItemRendererOwner<T>.() -> ListItemRenderer<E>,
		layoutAlgorithm: VirtualLayoutAlgorithm<S, T>,
		val layoutStyle: S,
		val data: ObservableList<E> = ActiveList()
) : ContainerImpl(owner), Focusable {

	override var focusEnabled: Boolean = false // Layout containers by default are not focusable.
	override var focusOrder: Float = 0f
	override var highlight: UiComponent? by createSlot()

	val style = bind(DataScrollerStyle())

	val bottomContents = addChild(virtualList(rendererFactory, layoutAlgorithm, layoutStyle, data) {
		alpha = 0f
		interactivityMode = InteractivityMode.NONE
	})

	private val contents = virtualList(rendererFactory, layoutAlgorithm, layoutStyle, data)

	private val _selection = own(DataScrollerSelection(data, contents, bottomContents))

	val selection: Selection<E>
		get() = _selection

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
	private val clipper = addChild(scrollRect {
		includeInLayout = false // If the clipper changes size, it doesn't affect this data scroller's size.
		+contents
	})

	/**
	 * Determines the behavior of whether or not the scroll bar is displayed.
	 */
	var scrollPolicy by validationProp(ScrollPolicy.AUTO, ValidationFlags.LAYOUT)

	private val tossScroller = contents.enableTossScrolling()
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

	/**
	 * If set, this is invoked when an item renderer has been obtained from the pool.
	 */
	var onRendererObtained: ((ListItemRenderer<E>) -> Unit)?
		get() = contents.onRendererObtained
		set(value) {
			contents.onRendererObtained = value
			bottomContents.onRendererObtained = value
		}

	/**
	 * If set, this is invoked when an item renderer has been returned to the pool.
	 */
	var onRendererFreed: ((ListItemRenderer<E>) -> Unit)?
		get() = contents.onRendererFreed
		set(value) {
			contents.onRendererFreed = value
			bottomContents.onRendererFreed = value
		}


	//---------------------------------------------------
	// Children
	//---------------------------------------------------

	private var background: UiComponent? = null

	init {
		styleTags.add(DataScroller)
		maxItems = 15
		scrollModel.changed.add {
			contents.indexPosition = scrollModel.value
		}
		watch(style) {
			background?.dispose()
			background = addOptionalChild(0, it.background(this))
		}

		wheel().add {
			tossScroller.stop()
			scrollModel.value += (if (isVertical) it.deltaY else it.deltaX) / scrollBar.modelToPixels
		}
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
			scrollBar.modelToPixels = out.height / (bottomContents.visibleBottomPosition - bottomContents.visiblePosition)
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
			scrollBar.modelToPixels = out.width / (bottomContents.visibleBottomPosition - bottomContents.visiblePosition)
		}
		tossBinding.modelToPixelsX = scrollBar.modelToPixels
		tossBinding.modelToPixelsY = scrollBar.modelToPixels
		scrollBar.scrollModel.max = bottomContents.visiblePosition

		clipper.maskSize(contents.explicitWidth ?: 0f, contents.explicitHeight ?: 0f)
		clipper.setSize(contents.explicitWidth, contents.explicitHeight)
		background?.setSize(out)
		highlight?.setSize(out)
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

private class DataScrollerSelection<E>(private val data: List<E>, private val listA: VirtualList<E, *, *>, private val listB: VirtualList<E, *, *>) : SelectionBase<E>() {
	override fun walkSelectableItems(callback: (E) -> Unit) {
		for (i in 0..data.lastIndex) {
			callback(data[i])
		}
	}

	override fun onItemSelectionChanged(item: E, selected: Boolean) {
		listA.selection.setItemIsSelected(item, selected)
		listB.selection.setItemIsSelected(item, selected) // Only necessary if variable sizes.
	}
}