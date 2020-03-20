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

import kotlin.math.log2

/**
 * A list of validation bit flags Acorn internally uses.
 * Extended validation flags should start at `1 shl 16`
 *
 * @author nbilyk
 */
object ValidationFlags {

	/**
	 * A style property has changed. (Cascades)
	 */
	const val STYLES: Int = 1 shl 0

	/**
	 * A flag reserved for the general properties of a component. UiComponentImpl does not contain a validation node
	 * for this by default.
	 */
	const val PROPERTIES: Int = 1 shl 1

	/**
	 * A descendant has added, removed, or reordered a child.
	 */
	const val HIERARCHY_ASCENDING: Int = 1 shl 2

	/**
	 * An ancestor has changed.
	 */
	const val HIERARCHY_DESCENDING: Int = 1 shl 3

	/**
	 * The size and position of this component's children need to be changed.
	 */
	const val LAYOUT: Int = 1 shl 4

	/**
	 * Whether a component should be included in layout has changed. (includeInLayout or visible)
	 */
	const val LAYOUT_ENABLED: Int = 1 shl 5

	/**
	 * The global transformation of a component. (Cascades)
	 */
	const val TRANSFORM: Int = 1 shl 6

	/**
	 * The global color tint of a component. (Cascades)
	 */
	const val COLOR_TINT: Int = 1 shl 7

	/**
	 * [InteractiveElementRo.interactivityModeInherited]. (Cascades)
	 */
	const val INTERACTIVITY_MODE: Int = 1 shl 8

	/**
	 * View-projection matrix. (Cascades)
	 * When the camera is changed, this should be invalidated.
	 */
	const val VIEW_PROJECTION: Int = 1 shl 9

	/**
	 * Global vertices.
	 */
	const val VERTICES_GLOBAL: Int = 1 shl 10

	/**
	 * The canvas region to which the component draws.
	 */
	const val DRAW_REGION: Int = 1 shl 11

	const val RESERVED_1: Int = 1 shl 12
	const val RESERVED_2: Int = 1 shl 13
	const val RESERVED_3: Int = 1 shl 14
	const val RESERVED_4: Int = 1 shl 15

	/**
	 * Prints out the name of the flag for reserved flags, or the power of two for non-reserved flags.
	 */
	fun flagToString(flag: Int): String = when (flag) {
		STYLES -> "STYLES"
		PROPERTIES -> "PROPERTIES"
		HIERARCHY_ASCENDING -> "HIERARCHY_ASCENDING"
		HIERARCHY_DESCENDING -> "HIERARCHY_DESCENDING"
		LAYOUT -> "LAYOUT"
		LAYOUT_ENABLED -> "LAYOUT_ENABLED"

		TRANSFORM -> "TRANSFORM"
		COLOR_TINT -> "COLOR_TINT"

		INTERACTIVITY_MODE -> "INTERACTIVITY_MODE"

		VIEW_PROJECTION -> "VIEW_PROJECTION"
		VERTICES_GLOBAL -> "VERTICES_GLOBAL"
		DRAW_REGION -> "DRAW_REGION"

		RESERVED_1 -> "RESERVED_1"
		RESERVED_2 -> "RESERVED_2"
		RESERVED_3 -> "RESERVED_3"
		RESERVED_4 -> "RESERVED_4"
		else -> log2(flag.toDouble()).toInt().toString()
	}

	/**
	 * Using the ValidationFlags list, prints out a comma separated list of the flags this bit mask contains.
	 * For non-reserved flags (flags at least 1 shl 16), the power of two will be printed.
	 * @see ValidationFlags
	 * @see ValidationFlags.flagToString
	 */
	fun flagsToString(flags: Int): String {
		var str = ""
		for (i in 0..31) {
			val flag = 1 shl i
			if (flag and flags > 0) {
				if (str.isNotEmpty()) str += ","
				str += flagToString(flag)
			}
		}
		return str
	}
}

fun Validatable.invalidateLayout() {
	invalidate(ValidationFlags.LAYOUT)
}

fun Validatable.invalidateProperties() {
	invalidate(ValidationFlags.PROPERTIES)
}

fun Validatable.invalidateViewProjection() {
	invalidate(ValidationFlags.VIEW_PROJECTION)
}