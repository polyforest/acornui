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

import com.acornui.recycle.Clearable
import com.acornui.math.Range2
import com.acornui.math.Range2Ro

interface SizeConstraintsRo {

	/**
	 * The minimum and maximum width.
	 */
	val width: Range2Ro<Float>

	/**
	 * The minimum and maximum height.
	 */
	val height: Range2Ro<Float>
}

/**
 * An object representing the minimum and maximum dimensions of an object.
 */
data class SizeConstraints(

		/**
		 * The minimum and maximum width.
		 */
		override val width: Range2<Float> = Range2(),

		/**
		 * The minimum and maximum height.
		 */
		override val height: Range2<Float> = Range2()

) : Clearable, SizeConstraintsRo {

	/**
	 * Bound the width and height by another size constraints object.
	 * This will result in the following adjustment:
	 * width.min = max(widthA, widthB)
	 * height.min = max(heightA, heightB)
	 * width.max = min(widthA, widthB)
	 * height.max = min(heightA, heightB)
	 */
	fun bound(sizeConstraints: SizeConstraintsRo) {
		width.bound(sizeConstraints.width)
		height.bound(sizeConstraints.height)
	}

	/**
	 * Sets this object to match the values of the [other] object.
	 */
	fun set(other: SizeConstraintsRo): SizeConstraints {
		width.set(other.width)
		height.set(other.height)
		return this
	}

	override fun clear() {
		width.clear()
		height.clear()
	}
}
