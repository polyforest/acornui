package com.acornui.component

import com.acornui.component.layout.HAlign
import com.acornui.component.layout.SizeConstraints
import com.acornui.component.layout.VAlign
import com.acornui.component.layout.algorithm.*
import com.acornui.component.scroll.scrollArea
import com.acornui.component.style.*
import com.acornui.core.di.Owned
import com.acornui.core.graphics.Scaling
import com.acornui.core.input.interaction.ClickInteraction
import com.acornui.core.input.interaction.ClickInteractionRo
import com.acornui.core.input.interaction.click
import com.acornui.factory.LazyInstance
import com.acornui.factory.disposeInstance
import com.acornui.math.Bounds
import com.acornui.signal.Cancel
import com.acornui.signal.Signal3
import com.acornui.signal.Signal4


open class TabNavigator(owner: Owned) : ContainerImpl(owner), LayoutDataProvider<StackLayoutData> {

	/**
	 * Dispatched when the current tab index is about to change due to a tab click event.
	 * The handler should have the signature:
	 * (this: TabNavigator, previousIndex: Int, newIndex: Int, cancel: Cancel)
	 */
	val userCurrentIndexChanged = Signal4<TabNavigator, Int, Int, Cancel>()

	/**
	 * Dispatched when the current tab index has changed. The handler should have the signature
	 * (this: TabNavigator, previousIndex: Int, newIndex: Int)
	 */
	val currentIndexChanged = Signal3<TabNavigator, Int, Int>()



	val style = bind(TabNavigatorStyle())

	protected val contents = scrollArea()
	protected val tabBarContainer: UiComponent
	protected lateinit var tabBar: HorizontalLayoutContainer

	private var background: UiComponent? = null
	private val tabs = ArrayList<TabNavigatorTab>()

	protected var _currentIndex = 0

	private var selectedTab: TabNavigatorTab? = null

	private val cancel = Cancel()

	private val tabClickHandler = {
		e: ClickInteractionRo ->
		if (!e.handled) {
			val index = tabBar.elements.indexOf(e.currentTarget)
			if (_currentIndex != index) {
				e.handled = true
				userCurrentIndexChanged.dispatch(this, _currentIndex, index, cancel.reset())
				if (!cancel.canceled()) {
					currentIndex = index
				}
			}
		}
	}

	init {
		styleTags.add(TabNavigator)
		addChild(contents)
		tabBarContainer = scaleBox {
			style.scaling = Scaling.STRETCH_X
			style.horizontalAlign = HAlign.LEFT
			style.verticalAlign = VAlign.BOTTOM
			tabBar = +hGroup {
				style.verticalAlign = VAlign.BOTTOM
			} layout {
				maxScaleX = 1f
				maxScaleY = 1f
			}
		}
		addChild(tabBarContainer)

		watch(style) {
			background?.dispose()
			background = addChild(0, it.background(this))
			tabBar.style.gap = it.tabGap
		}
	}

	override fun createLayoutData(): StackLayoutData = StackLayoutData()

	var currentIndex: Int
		get() = _currentIndex
		set(value) {
			val previousIndex = _currentIndex
			if (value == previousIndex) return
			_currentIndex = value
			updateSelectedTab()
			currentIndexChanged.dispatch(this, previousIndex, value)
		}

	val numTabs: Int
		get() = tabBar.elements.size

	fun addTab(value: String, component: UiComponent) {
		addTab(numTabs, value, component)
	}

	fun addTab(index: Int, tab: String, component: UiComponent) {
		addTab(index, tab, { component })
	}

	fun addTab(tab: String, factory: Owned.() -> UiComponent) {
		addTab(numTabs, tab, factory)
	}

	fun addTab(index: Int, tab: String, factory: Owned.() -> UiComponent) {
		val tabButton = button {
			label = tab.orSpace()
			styleTags.add(DEFAULT_TAB_STYLE)
		}
		addTab(index, tabButton, factory)
	}

	fun addTab(tab: Button, component: UiComponent) {
		addTab(tabBar.elements.size, tab, { component })
	}

	fun addTab(tab: Button, factory: Owned.() -> UiComponent) {
		addTab(tabBar.elements.size, tab, factory)
	}

	fun addTab(index: Int, tab: Button, factory: Owned.() -> UiComponent) {
		addTab(index, TabNavigatorTab(
				LazyInstance<Owned, UiComponent>(this, factory), tab)
		)
	}

	fun addTab(index: Int, tab: TabNavigatorTab) {
		if (index == 0) {
			if (tabs.size > 0) {
				tabs[0].button.styleTags.remove(DEFAULT_TAB_STYLE_FIRST)
			}
			styleTags.add(DEFAULT_TAB_STYLE_FIRST)
		}
		if (index == numTabs) {
			if (tabs.size > 0) {
				tabs.last().button.styleTags.remove(DEFAULT_TAB_STYLE_LAST)
			}
			styleTags.add(DEFAULT_TAB_STYLE_LAST)
		}
		tab.button.click().add(tabClickHandler)

		tabs.add(index, tab)
		tabBar.addElement(index, tab.button)
		updateSelectedTab()
	}

	fun removeTab(index: Int, dispose: Boolean = true): TabNavigatorTab {
		if (index < 0 || index >= tabs.size)
			throw IllegalArgumentException("Index out of bounds.")
		val r = tabs.removeAt(index)
		val t = r.button
		if (index == 0) {
			if (tabs.size > 0) {
				styleTags.add(DEFAULT_TAB_STYLE_FIRST)
			}
			t.styleTags.remove(DEFAULT_TAB_STYLE_FIRST)
		}
		if (index == tabs.size) {
			if (tabs.size > 0) {
				tabs.last().button.styleTags.remove(DEFAULT_TAB_STYLE_LAST)
			}
			t.styleTags.remove(DEFAULT_TAB_STYLE_LAST)
		}
		t.click().remove(tabClickHandler)
		tabBar.removeElement(index)

		updateSelectedTab()
		if (dispose) {
			r.component.disposeInstance()
			r.button.dispose()
		}
		return r
	}

	fun setTabLabel(index: Int, newLabel: String) {
		val tab = tabs[index]
		val tabButton = tab.button
		if (tabButton is Labelable) {
			tabButton.label = (newLabel.orSpace())
		}
	}

	fun clearTabs(dispose: Boolean = false) {
		if (tabs.isNotEmpty()) {
			tabs.first().button.styleTags.remove(DEFAULT_TAB_STYLE_FIRST)
			tabs.last().button.styleTags.remove(DEFAULT_TAB_STYLE_LAST)
		}
		tabs.clear()
		tabBar.clearElements(dispose)
		contents.clearElements(dispose)
		updateSelectedTab()
	}

	//-----------------------------------------------------

	protected fun updateSelectedTab() {
		val newSelectedTab: TabNavigatorTab?
		if (_currentIndex >= 0 && _currentIndex < tabs.size) {
			newSelectedTab = tabs[_currentIndex]
		} else {
			newSelectedTab = null
		}
		val lastSelectedTab = selectedTab
		if (newSelectedTab != lastSelectedTab) {
			selectedTab = newSelectedTab
			if (lastSelectedTab != null) {
				lastSelectedTab.button.toggled = false
				contents.removeElement(lastSelectedTab.component.instance)
			}
			if (newSelectedTab != null) {
				contents.addElement(newSelectedTab.component.instance)
				newSelectedTab.button.toggled = true
			}
		}
	}

	override fun updateSizeConstraints(out: SizeConstraints) {
		out.width.min = maxOf(tabBar.minWidth ?: 0f, contents.minWidth ?: 0f)
		out.height.min = (tabBar.minHeight ?: 0f) + (contents.minHeight ?: 0f) + style.vGap
	}

	override fun updateLayout(explicitWidth: Float?, explicitHeight: Float?, out: Bounds) {
		tabBarContainer.setSize(explicitWidth, null)
		val tabBarHeight = tabBarContainer.height + style.vGap
		val contentsHeight = if (explicitHeight == null) null else explicitHeight - tabBarHeight
		contents.setSize(explicitWidth, contentsHeight)
		contents.moveTo(0f, tabBarHeight)
		background!!.moveTo(0f, tabBarHeight)
		background!!.setSize(contents.width, contents.height)
		out.width = maxOf(contents.width, tabBarContainer.width)
		out.height = contents.height + tabBarHeight
	}

	override fun dispose() {
		super.dispose()
		currentIndexChanged.dispose()
	}

	companion object : StyleTag {
		val DEFAULT_TAB_STYLE = styleTag()
		val DEFAULT_TAB_STYLE_FIRST = styleTag()
		val DEFAULT_TAB_STYLE_LAST = styleTag()
	}
}

data class TabNavigatorTab(
		val component: LazyInstance<Owned, UiComponent>,
		val button: Button
)

class TabNavigatorStyle : StyleBase() {

	override val type: StyleType<TabNavigatorStyle> = TabNavigatorStyle

	/**
	 * The horizontal gap between tabs.
	 */
	var tabGap by prop(0f)

	/**
	 * The vertical gap between the tabs and the contents.
	 */
	var vGap by prop(-1f)

	/**
	 * The component to be placed in the background of the contents.
	 */
	var background by prop(noSkin)

	companion object : StyleType<TabNavigatorStyle>
}

fun Owned.tabNavigator(init: ComponentInit<TabNavigator> = {}): TabNavigator {
	val t = TabNavigator(this)
	t.init()
	return t
}

private fun String.orSpace(): String {
	return if (this == "") "\u00A0" else this
}