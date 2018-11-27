/*
 * Copyright 2015 Nicholas Bilyk
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

/**
 * A list of validation bit flags Acorn internally uses.
 * Extended validation flags should start at 1 shl 16
 *
 * @author nbilyk
 */
object ValidationFlags {

	const val STYLES: Int = 1 shl 0

	const val PROPERTIES: Int = 1 shl 1

	/**
	 * A descendant has added, removed, or reordered a child.
	 */
	const val HIERARCHY_ASCENDING: Int = 1 shl 2

	/**
	 * An ancestor has changed.
	 */
	const val HIERARCHY_DESCENDING: Int = 1 shl 3

	const val SIZE_CONSTRAINTS: Int = 1 shl 4

	const val LAYOUT: Int = 1 shl 5
	const val LAYOUT_ENABLED: Int = 1 shl 6

	const val TRANSFORM: Int = 1 shl 7
	const val CONCATENATED_TRANSFORM: Int = 1 shl 8

	const val COLOR_TRANSFORM: Int = 1 shl 9
	const val CONCATENATED_COLOR_TRANSFORM: Int = 1 shl 10

	const val INTERACTIVITY_MODE: Int = 1 shl 11

	const val CAMERA: Int = 1 shl 12
	const val VIEWPORT: Int = 1 shl 13

	const val RESERVED_1: Int = 1 shl 14
	const val RESERVED_2: Int = 1 shl 15
}

fun Validatable.invalidateSize() {
	invalidate(ValidationFlags.SIZE_CONSTRAINTS)
	Unit
}

fun Validatable.invalidateLayout() {
	invalidate(ValidationFlags.LAYOUT)
	Unit
}

fun Validatable.invalidateProperties() {
	invalidate(ValidationFlags.PROPERTIES)
	Unit
}