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

package com.acornui.graphic.lighting

import com.acornui.graphic.Color
import com.acornui.math.vec3
import com.acornui.recycle.Clearable


/**
 * A light that goes in all directions for a given radius.

 * @author nbilyk
 */
class PointLight : Clearable {

	val color = Color.WHITE.copy()
	val position = vec3()
	var radius = 0f

	/**
	 * positive x, negative x, positive y, negative y, positive z, negative z
	 */
	val shadowSidesEnabled = booleanArrayOf(true, true, true, true, true, true)

	/**
	 * Sets this point light to match the properties of [other] point light.
	 * @return Returns this point light for chaining.
	 */
	fun set(other: PointLight): PointLight {
		color.set(other.color)
		position.set(other.position)
		radius = other.radius
		return this
	}

	override fun clear() {
		color.set(Color.WHITE)
		position.clear()
		radius = 0f
	}

	companion object {

		val EMPTY_POINT_LIGHT = PointLight()

	}

}

fun pointLight(init: PointLight.() -> Unit = {}): PointLight {
	val p = PointLight()
	p.init()
	return p
}
