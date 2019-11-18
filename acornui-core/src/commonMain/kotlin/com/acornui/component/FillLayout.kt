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

package com.acornui.component

import com.acornui.collection.forEach2
import com.acornui.component.layout.*
import com.acornui.component.layout.algorithm.LayoutAlgorithm
import com.acornui.component.style.NoopStyle
import com.acornui.di.Owned
import com.acornui.math.Bounds
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract
import kotlin.jvm.JvmName

/**
 * FillLayout sizes all elements to the explicit size and positions all elements at 0f, 0f
 */
class FillLayout : LayoutAlgorithm<NoopStyle, NoopLayoutData> {

	override val style = NoopStyle()

	override fun layout(explicitWidth: Float?, explicitHeight: Float?, elements: List<LayoutElement>, out: Bounds) {
		elements.forEach2 { element ->
			element.setSize(explicitWidth, explicitHeight)
			element.moveTo(0f, 0f)
			if (element.width > out.width)
				out.width = element.width
			if (element.height > out.height)
				out.height = element.height
			if (element.baseline > out.baseline)
				out.baseline = element.baseline
		}
	}

	override fun createLayoutData(): NoopLayoutData = NoopLayoutData

}

open class FillLayoutContainer<E : UiComponent>(owner: Owned) : ElementLayoutContainer<NoopStyle, NoopLayoutData, E>(owner, FillLayout())

@JvmName("fillGroupT")
inline fun <E : UiComponent> Owned.fillGroup(init: ComponentInit<FillLayoutContainer<E>> = {}): FillLayoutContainer<E> {
	contract { callsInPlace(init, InvocationKind.EXACTLY_ONCE) }
	return FillLayoutContainer<E>(this).apply(init)
}

inline fun Owned.fillGroup(init: ComponentInit<FillLayoutContainer<UiComponent>> = {}): FillLayoutContainer<UiComponent> {
	contract { callsInPlace(init, InvocationKind.EXACTLY_ONCE) }
	return fillGroup<UiComponent>(init)
}
