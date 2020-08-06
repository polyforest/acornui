/*
 * Copyright 2020 Poly Forest, LLC
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

import com.acornui.component.ComponentInit
import com.acornui.component.Div
import com.acornui.component.WithNode
import com.acornui.component.layout.LayoutStyles.grid
import com.acornui.component.layout.LayoutStyles.hFlowGroup
import com.acornui.component.layout.LayoutStyles.hGroup
import com.acornui.component.layout.LayoutStyles.vFlowGroup
import com.acornui.component.layout.LayoutStyles.vGroup
import com.acornui.component.style.cssClass
import com.acornui.css.cssVar
import com.acornui.di.Context
import com.acornui.dom.addCssToHead
import com.acornui.dom.div
import com.acornui.skins.Theme
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

object LayoutStyles {

	val hGroup by cssClass()
	val vGroup by cssClass()
	val hFlowGroup by cssClass()
	val vFlowGroup by cssClass()
	val grid by cssClass()

	init {
		@Suppress("CssInvalidPropertyValue")
		addCssToHead(
			"""
			
$vGroup {
	display: flex;
	flex-direction: column;
	align-items: self-start;
}

$vGroup > *:not(:last-child) {
	margin-bottom: ${cssVar(Theme::gap)}
}

$hGroup {
	display: flex;
	flex-direction: row;
	align-items: baseline;
}
		
$hGroup > *:not(:last-child) {
	margin-right: ${cssVar(Theme::gap)}
}

$grid {
	display: grid;
	column-gap: ${cssVar(Theme::gap)};
  	row-gap: ${cssVar(Theme::gap)};
}

		"""
		)
	}
}

inline fun Context.vGroup(init: ComponentInit<Div> = {}): Div {
	contract { callsInPlace(init, InvocationKind.EXACTLY_ONCE) }
	return Div(this).apply {
		addClass(vGroup)
		init()
	}
}

inline fun Context.hGroup(init: ComponentInit<Div> = {}): Div {
	contract { callsInPlace(init, InvocationKind.EXACTLY_ONCE) }
	return Div(this).apply {
		addClass(hGroup)
		init()
	}
}

/**
 * A div wrapper to avoid the lack of gap support.
 */
open class FlowGroup(owner: Context) : Div(owner) {

	private val contents = addChild(div {
		addClass(contentsTag)
	})

	override fun onElementAdded(oldIndex: Int, newIndex: Int, element: WithNode) {
		contents.addElement(newIndex, element)
	}

	override fun onElementRemoved(index: Int, element: WithNode) {
		contents.removeElement(index)
	}

	init {
		addClass(styleTag)
	}

	companion object {
		val styleTag by cssClass()
		val contentsTag by cssClass()

		init {
			addCssToHead("""
$styleTag {
	display: block;
}				

$contentsTag {
	display: flex;
	flex-wrap: wrap;
	width: inherit;
	height: inherit;
	justify-content: inherit;
	justify-items: inherit;
	align-content: inherit;
	align-items: inherit;
	margin: 0 calc(-1 * ${cssVar(Theme::gap)}) calc(-1 * ${cssVar(Theme::gap)}) 0;
}

$contentsTag > * {
	margin: 0 ${cssVar(Theme::gap)} ${cssVar(Theme::gap)} 0;
}

$hFlowGroup > $contentsTag {
	flex-direction: row;
	align-items: baseline;
	width: 100%;
}

$vFlowGroup > $contentsTag {
	flex-direction: column;
	align-items: self-start;
	height: 100%;
}

			""")
		}
	}
}

inline fun Context.hFlowGroup(init: ComponentInit<FlowGroup> = {}): FlowGroup {
	contract { callsInPlace(init, InvocationKind.EXACTLY_ONCE) }
	return FlowGroup(this).apply {
		addClass(hFlowGroup)
		init()
	}
}

inline fun Context.vFlowGroup(init: ComponentInit<FlowGroup> = {}): FlowGroup {
	contract { callsInPlace(init, InvocationKind.EXACTLY_ONCE) }
	return FlowGroup(this).apply {
		addClass(vFlowGroup)
		init()
	}
}

inline fun Context.grid(init: ComponentInit<Div> = {}): Div {
	contract { callsInPlace(init, InvocationKind.EXACTLY_ONCE) }
	return Div(this).apply {
		addClass(grid)
		init()
	}
}