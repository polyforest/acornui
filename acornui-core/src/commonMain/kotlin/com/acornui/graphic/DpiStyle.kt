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

package com.acornui.graphic

import com.acornui.component.ComponentInit
import com.acornui.component.style.StyleBase
import com.acornui.component.style.StyleType
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

/**
 * DpiStyle provides a way to have style properties and textures dependent on window dpi scaling.
 *
 */
open class DpiStyle : StyleBase() {

	override val type: StyleType<*> = Companion

	/**
	 * The x scaling of dp to pixels.
	 * This is typically updated by the [com.acornui.skins.WindowScalingAttachment] initialized by the skin to match
	 * the window's dpi scaling.
	 */
	var scaleX: Float by prop(1f)

	/**
	 * The y scaling of dp to pixels.
	 * This is typically updated by the [com.acornui.skins.WindowScalingAttachment] initialized by the skin to match
	 * the window's dpi scaling.
	 */
	var scaleY: Float by prop(1f)

	/**
	 * If the nearest texture size found is within this percent margin to the dpi scaling, don't scale the texture,
	 * otherwise, pick the closest texture and scale to the desired dp.
	 */
	var scalingSnapAffordance: Float by prop(0.25f)
	
	companion object : StyleType<DpiStyle>
}

/**
 *
 */
fun scaleIfPastAffordance(actual: Float, desired: Float, affordancePercent: Float): Float {
	val m = actual / desired
	val m2 = if (m >= 1f) m else 1f / m
	return if (m2 - 1f < affordancePercent) 1f else m
}

inline fun dpiStyle(init: ComponentInit<DpiStyle> = {}): DpiStyle {
	contract { callsInPlace(init, InvocationKind.EXACTLY_ONCE) }
	return DpiStyle().apply(init)
}