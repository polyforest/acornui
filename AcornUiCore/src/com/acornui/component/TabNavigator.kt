package com.acornui.component

import com.acornui.collection.forEachReversed2
import com.acornui.component.layout.HAlign
import com.acornui.component.layout.SizeConstraints
import com.acornui.component.layout.VAlign
import com.acornui.component.layout.algorithm.HorizontalLayoutContainer
import com.acornui.component.layout.algorithm.LayoutDataProvider
import com.acornui.component.layout.algorithm.hGroup
import com.acornui.component.layout.algorithm.scaleBox
import com.acornui.component.scroll.scrollArea
import com.acornui.component.style.*
import com.acornui.core.Disposable
import com.acornui.core.di.Owned
import com.acornui.core.di.OwnedImpl
import com.acornui.core.di.own
import com.acornui.core.graphics.Scaling
import com.acornui.core.input.interaction.ClickInteractionRo
import com.acornui.core.input.interaction.click
import com.acornui.factory.LazyInstance
import com.acornui.factory.lazyInstance
import com.acornui.math.Bounds
import com.acornui.signal.Cancel
import com.acornui.signal.Signal
import com.acornui.signal.Signal3
import com.acornui.signal.Signal4


open class TabNavigator(owner: Owned) : ContainerImpl(owner), LayoutDataProvider<StackLayoutData> {

	private val _userCurrentIndexChanged = own(Signal4<TabNavigator, Int, Int, Cancel>())

	/**
	 * Dispatched when the current tab index is about to change due to a tab click event.
	 * The handler should have the signature:
	 * (this: TabNavigator, previousIndex: Int, newIndex: Int, cancel: Cancel)
	 */
	val userCurrentIndexChanged: Signal<(TabNavigator, Int, Int, Cancel) -> Unit>
		get() = _userCurrentIndexChanged

	private val _currentIndexChanged = own(Signal3<TabNavigator, Int, Int>())

	/**
	 * Dispatched when the current tab index has changed. The handler should have the signature
	 * (this: TabNavigator, previousIndex: Int, newIndex: Int)
	 */
	val currentIndexChanged: Signal<(TabNavigator, Int, Int) -> Unit>
		get() = _currentIndexChanged

	val style = bind(TabNavigatorStyle())

	/**
	 * The scroll area in which the tab components will be placed.
	 */
	protected val contents = scrollArea()

	/**
	 * The container for the tab buttons.
	 */
	protected val tabBarContainer: UiComponent

	private lateinit var tabBar: HorizontalLayoutContainer

	private var background: UiComponent? = null
	private val _tabs = ArrayList<TabNavigatorTab>()

	/**
	 * Returns the list of tabs.
	 */
	val tabs: List<TabNavigatorTab>
		get() = _tabs

	private var _currentIndex = 0

	private var selectedTab: TabNavigatorTab? = null

	private val cancel = Cancel()

	private val tabClickHandler = { e: ClickInteractionRo ->
		if (!e.handled) {
			val index = tabBar.elements.indexOf(e.currentTarget)
			if (_currentIndex != index) {
				e.handled = true
				_userCurrentIndexChanged.dispatch(this, _currentIndex, index, cancel.reset())
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
			_currentIndexChanged.dispatch(this, previousIndex, value)
		}

	/**
	 * Adds the given tab.
	 */
	operator fun <T : TabNavigatorTab> T.unaryPlus(): T {
		return addTab(tabs.size, this)
	}

	/**
	 * Removes the given tab.
	 */
	operator fun <T : TabNavigatorTab> T.unaryMinus(): T {
		removeTab(this)
		return this
	}

	/**
	 * Adds the tab to the given index. If the tab is already added, it will be removed first and added to the new
	 * index.
	 */
	fun <T : TabNavigatorTab> addTab(index: Int, tab: T): T {
		if (tab.isDisposed) throw Exception("Tab is disposed.")
		var newIndex = index
		val oldIndex = tabs.indexOf(tab)
		if (oldIndex != -1) {
			if (newIndex == oldIndex) return tab // Element was added in the same spot it previously was.
			// Handle the case where after the element is removed, the new index needs to decrement to compensate.
			if (oldIndex < newIndex)
				newIndex--
			removeTab(oldIndex)
		}

		if (newIndex == 0) {
			if (_tabs.size > 0) {
				_tabs[0].button.styleTags.remove(DEFAULT_TAB_STYLE_FIRST)
			}
			tab.button.styleTags.add(DEFAULT_TAB_STYLE_FIRST)
		}
		if (newIndex == tabs.size) {
			if (_tabs.size > 0) {
				_tabs.last().button.styleTags.remove(DEFAULT_TAB_STYLE_LAST)
			}
			tab.button.styleTags.add(DEFAULT_TAB_STYLE_LAST)
		}
		tab.button.styleTags.add(DEFAULT_TAB_STYLE)

		_tabs.add(newIndex, tab)
		tabBar.addElement(newIndex, tab.button)
		tab.button.click().add(tabClickHandler)

		updateSelectedTab()
		tab.disposed.add(this::tabDisposedHandler)
		return tab
	}

	/**
	 * Removes the given tab.
	 * @param tab The tab to remove. If this is not in the _tabs list, an error will be thrown.
	 */
	fun removeTab(tab: TabNavigatorTab?): Boolean {
		if (tab == null) return false
		val index = _tabs.indexOf(tab)
		if (index == -1) return false
		removeTab(index)
		return true
	}

	/**
	 * Returns the tab at the given index.
	 * @param index Between 0 and `tabs.lastIndex`
	 * @return Returns the removed tab.
	 * @throws IndexOutOfBoundsException
	 */
	fun removeTab(index: Int): TabNavigatorTab {
		val r = _tabs.removeAt(index)
		val t = r.button
		if (index == 0) {
			if (_tabs.size > 0) {
				styleTags.add(DEFAULT_TAB_STYLE_FIRST)
			}
			t.styleTags.remove(DEFAULT_TAB_STYLE_FIRST)
		}
		if (index == _tabs.size) {
			if (_tabs.size > 0) {
				_tabs.last().button.styleTags.remove(DEFAULT_TAB_STYLE_LAST)
			}
			t.styleTags.remove(DEFAULT_TAB_STYLE_LAST)
		}
		t.click().remove(tabClickHandler)
		tabBar.removeElement(r.button)

		updateSelectedTab()
		r.disposed.remove(this::tabDisposedHandler)
		return r
	}

	fun setTabLabel(index: Int, newLabel: String) {
		val tab = _tabs.getOrNull(index) ?: throw IndexOutOfBoundsException(index)
		tab.button.label = newLabel.orSpace()
	}

	fun clearTabs(dispose: Boolean = true) {
		if (!_tabs.isNotEmpty()) return
		_tabs.first().button.styleTags.remove(DEFAULT_TAB_STYLE_FIRST)
		_tabs.last().button.styleTags.remove(DEFAULT_TAB_STYLE_LAST)
		if (dispose) {
			while (_tabs.isNotEmpty())
				_tabs.last().dispose()
		} else {
			tabBar.clearElements(dispose = false)
			_tabs.clear()
		}
		updateSelectedTab()
	}

	private fun tabDisposedHandler(tab: Owned) {
		removeTab(tab as TabNavigatorTab)
	}

	//-----------------------------------------------------

	private fun updateSelectedTab() {
		val newSelectedTab: TabNavigatorTab? = if (_currentIndex >= 0 && _currentIndex < _tabs.size) {
			_tabs[_currentIndex]
		} else {
			null
		}
		val lastSelectedTab = selectedTab
		if (newSelectedTab != lastSelectedTab) {
			selectedTab = newSelectedTab
			if (lastSelectedTab != null && !lastSelectedTab.isDisposed) {
				lastSelectedTab.button.toggled = false
				contents.removeElement(lastSelectedTab.content.instance)
			}
			if (newSelectedTab != null) {
				contents.addElement(newSelectedTab.content.instance)
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
		clearTabs(dispose = false) // The tabs this component owns will be disposed in the disposed signal.
		super.dispose()
		_currentIndexChanged.dispose()
	}

	companion object : StyleTag {
		val DEFAULT_TAB_STYLE = styleTag()
		val DEFAULT_TAB_STYLE_FIRST = styleTag()
		val DEFAULT_TAB_STYLE_LAST = styleTag()
	}
}

interface TabNavigatorTab : Owned, Disposable {
	val button: Button
	val content: LazyInstance<Owned, UiComponent>
}

class TabNavigatorTabImpl<S : Button, T : UiComponent>(
		owner: Owned,
		buttonFactory: Owned.() -> S,
		contentFactory: Owned.() -> T
) : OwnedImpl(owner), TabNavigatorTab {
	override val button: S = buttonFactory()
	override val content: LazyInstance<Owned, T> = lazyInstance(contentFactory)

	val instance: T
		get() = content.instance
}

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


fun <S : Button, T : UiComponent> Owned.tab(buttonFactory: Owned.() -> S, contentFactory: Owned.() -> T) = TabNavigatorTabImpl(this, buttonFactory, contentFactory)

fun <T : UiComponent> Owned.tab(label: String, contentFactory: Owned.() -> T) = tab({ button(label.orSpace()) }, contentFactory)

fun Owned.tabNavigator(init: ComponentInit<TabNavigator> = {}): TabNavigator {
	val t = TabNavigator(this)
	t.init()
	return t
}

private fun String.orSpace(): String {
	return if (this == "") "\u00A0" else this
}