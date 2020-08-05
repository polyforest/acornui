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

@file:Suppress("PropertyName", "MemberVisibilityCanBePrivate", "unused", "CssReplaceWithShorthandSafely",
	"CssInvalidPropertyValue"
)

package com.acornui.component

import com.acornui.component.input.Button
import com.acornui.component.layout.hGroup
import com.acornui.component.style.CommonStyleTags
 import com.acornui.component.style.cssClass
import com.acornui.css.cssVar
import com.acornui.di.Context
import com.acornui.dom.addCssToHead
import com.acornui.dom.div
import com.acornui.dom.handle
import com.acornui.dom.isHandled
import com.acornui.input.Event
import com.acornui.input.clicked
import com.acornui.signal.signal
import com.acornui.skins.Theme
import org.w3c.dom.HTMLElement
import org.w3c.dom.Node
import org.w3c.dom.get
import org.w3c.dom.set
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

open class TabNavigator(owner: Context) : DivComponent(owner) {

	class SelectedTabChangeEvent(
		val previousTab: String?,
		val newTab: String?
	) : Event()

	/**
	 * Dispatched when the current tab is about to change due to a user event.
	 */
	val currentTabChanging = signal<SelectedTabChangeEvent>()

	/**
	 * Dispatched when the current tab has changed due to a user event.
	 * This is not cancelable.
	 */
	val currentTabChanged = signal<SelectedTabChangeEvent>()

	/**
	 * The container for the tab buttons.
	 */
	val tabs = addChild(hGroup {
		addClass(tabsStyle)
	})

	/**
	 * Runs the given block with the [tabs] group as the receiver.
	 */
	fun tabs(init: ComponentInit<DivComponent>) {
		tabs.init()
	}

	/**
	 * The scroll area in which the tab components will be placed.
	 *
	 * Example:
	 * ```
	 * tabNavigator {
	 *   tabs {
	 *     +tab("one", "One")
	 *     +tab("two", "Two")
	 *   }
	 *
	 *   +div {
	 *     tab = "one"
	 *     +"One"
	 *   }
	 *   +div {
	 *     tab = "two"
	 *     +"Two"
	 *   }
	 * }
	 * ```
	 */
	protected val contents = addChild(div {
		addClass(Panel.panelColorsStyle)
		addClass(contentsStyle)
	})

	init {
		addClass(styleTag)

		tabs.clicked.listen {
			if (!it.isHandled && !it.defaultPrevented) {
				it.handle()
				var p = it.target.unsafeCast<Node>()
				while (p.parentNode != dom && p.parentNode != tabs.dom) {
					p = p.parentNode!!
				}
				val tab = p.unsafeCast<HTMLElement>().tab
				if (tab != null)
					setCurrentTabUser(tab)
			}
		}
	}

	/**
	 * Sets the currently selected tab, dispatching [currentTabChanged].
	 * This will first fire [currentTabChanging]. If [Event.preventDefault] is not called, then [currentTab] will
	 * be changed and [currentTabChanged] will be dispatched.
	 */
	fun setCurrentTabUser(value: String?) {
		val previous = currentTab
		val e = SelectedTabChangeEvent(previous, value)
		currentTabChanging.dispatch(e)
		if (e.defaultPrevented) return
		currentTab = value
		currentTabChanged.dispatch(e)
	}

	var currentTab: String? = null
		set(value) {
			if (field == value) return
			field = value
			contents.elements.forEach {
				val el = it.dom.unsafeCast<HTMLElement>()
				val selected = (value != null && el.dataset["tab"] == value)
				el.style.display = if (selected) "unset" else "none"
			}
			tabs.elements.forEach {
				val el = it.dom.unsafeCast<HTMLElement>()
				val selected = (value != null && el.dataset["tab"] == value)
				if (selected)
					el.classList.add(CommonStyleTags.toggled.className)
				else
					el.classList.remove(CommonStyleTags.toggled.className)
			}
		}

	override fun onElementAdded(oldIndex: Int, newIndex: Int, element: WithNode) {
		val el = element.dom.unsafeCast<HTMLElement>()
		if (currentTab == null)
			currentTab = el.tab
		el.style.display = if (el.tab == currentTab) "unset" else "none"
		contents.addElement(newIndex, element)
	}

	override fun onElementRemoved(index: Int, element: WithNode) {
		contents.removeElement(index)
	}

	//-----------------------------------------------------


	companion object {
		val styleTag by cssClass()
		val tabsStyle by cssClass()
		val contentsStyle by cssClass()

		init {

			addCssToHead("""
$styleTag {
	display: flex;
	flex-direction: column;
	align-items: stretch;
	background: transparent;
	border-radius: ${cssVar(Theme::borderRadius)};
}

$contentsStyle {
	padding: ${cssVar(Theme::padding)};
	flex-grow: 1;
	overflow: auto;
	border-top-left-radius: 0;
}

$tabsStyle {
	flex-grow: 0;
	padding: 0;
	clip-path: polygon(-10% -10%, 110% -10%, 110% 100%, -10% 100%);
	--${Theme::gap.name}: 0;
}

$tabsStyle *:focus {
    border-color: ${cssVar(Theme::focus)};
	box-shadow: inset 0 0 0 ${cssVar(Theme::focusThickness)} ${cssVar(Theme::focus)};
}

$tabsStyle > div {
	border-bottom-left-radius: 0;
	border-bottom-right-radius: 0;
}
			""")
		}
	}
}

/**
 * Constructs a new Button with the "tab" data property set.
 */
inline fun Context.tab(tabName: String, label: String = "", init: ComponentInit<Button> = {}): Button {
	contract { callsInPlace(init, InvocationKind.EXACTLY_ONCE) }
	val t = Button(this)
	t.dataset["tab"] = tabName
	t.label = label
	t.init()
	return t
}

inline fun Context.tabNavigator(init: ComponentInit<TabNavigator> = {}): TabNavigator {
	contract { callsInPlace(init, InvocationKind.EXACTLY_ONCE) }
	val t = TabNavigator(this)
	t.init()
	return t
}

/**
 * Shorthand for setting the 'tab' key for this component's dataset.
 * This is how tab buttons correspond to their elements.
 */
var UiComponent.tab: String?
	get() = dom.tab
	set(value) {
		dom.tab = value
	}

private var HTMLElement.tab: String?
	get() = dataset["tab"]
	set(value) {
		if (value == null)
			removeAttribute("data-tab")
		else
			dataset["tab"] = value
	}