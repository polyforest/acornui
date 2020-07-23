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
import com.acornui.component.DivComponent
import com.acornui.component.WithNode
import com.acornui.component.layout.LayoutStyles.hFlowGroup
import com.acornui.component.layout.LayoutStyles.hGroup
import com.acornui.component.layout.LayoutStyles.vGroup
import com.acornui.component.layout.LayoutStyles.vFlowGroup
import com.acornui.component.style.StyleTag
import com.acornui.css.cssVar
import com.acornui.di.Context
import com.acornui.dom.addCssToHead
import com.acornui.dom.div
import com.acornui.skins.Theme
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

object LayoutStyles {

	val hGroup = StyleTag("hGroup")
	val vGroup = StyleTag("vGroup")
	val hFlowGroup = StyleTag("hFlowGroup")
	val vFlowGroup = StyleTag("vFlowGroup")

	init {
		@Suppress("CssInvalidPropertyValue")
		addCssToHead(
			"""
			
$vGroup {
	display: inline-flex;
	flex-direction: column;
	align-items: self-start;
}

$vGroup > *:not(:last-child) {
	margin-bottom: ${cssVar(Theme::gap)}
}

$hGroup {
	display: inline-flex;
	flex-direction: row;
	align-items: baseline;
}
		
$hGroup > *:not(:last-child) {
	margin-right: ${cssVar(Theme::gap)}
}

$hFlowGroup, $vFlowGroup {
	display: inline-flex;
	flex-wrap: wrap;
	margin: 0 calc(-1 * ${cssVar(Theme::gap)}) calc(-1 * ${cssVar(Theme::gap)}) 0;
}

$hFlowGroup {
	flex-direction: row;
	width: 100%;
	align-items: baseline;
}

$vFlowGroup {
	flex-direction: column;
	height: 100%;
	align-items: self-start;
}

$hFlowGroup > *, $vFlowGroup > * {
	margin: 0 ${cssVar(Theme::gap)} ${cssVar(Theme::gap)} 0;
}

$vGroup, $hGroup, $hFlowGroup, $vFlowGroup {
	
	padding: ${cssVar(Theme::padding)};
}

		"""
		)
	}
}

inline fun Context.vGroup(init: ComponentInit<DivComponent> = {}): DivComponent {
	contract { callsInPlace(init, InvocationKind.EXACTLY_ONCE) }
	return DivComponent(this).apply {
		addClass(vGroup)
		init()
	}
}

inline fun Context.hGroup(init: ComponentInit<DivComponent> = {}): DivComponent {
	contract { callsInPlace(init, InvocationKind.EXACTLY_ONCE) }
	return DivComponent(this).apply {
		addClass(hGroup)
		init()
	}
}

/**
 * A div wrapper to avoid the lack of gap support.
 */
open class FlowGroup(owner: Context) : DivComponent(owner) {

	val contents = addChild(div {
		addClass(hFlowGroup)
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
		val styleTag = StyleTag("FlowGroup")
		val contentsTag = StyleTag("contents")

		init {
			addCssToHead("""
$styleTag {
	display: inline-flex;
	overflow: hidden;
}				

$contentsTag {
}
			""")
		}
	}
}

inline fun Context.hFlowGroup(init: ComponentInit<FlowGroup> = {}): FlowGroup {
	contract { callsInPlace(init, InvocationKind.EXACTLY_ONCE) }
	return FlowGroup(this).apply {
		init()
	}
}

inline fun Context.vFlowGroup(init: ComponentInit<FlowGroup> = {}): FlowGroup {
	contract { callsInPlace(init, InvocationKind.EXACTLY_ONCE) }
	return FlowGroup(this).apply {
		contents.removeClass(hFlowGroup)
		contents.addClass(vFlowGroup)
		init()
	}
}