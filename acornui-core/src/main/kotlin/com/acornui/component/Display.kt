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

package com.acornui.component

object Display {

	/**
	 * Displays an element as an inline element (like `<span>`). Any height and width properties will have no effect.
 	 */
	const val INLINE = "inline"

	/**
	 * Displays an element as a block element (like `<p>`). It starts on a new line, and takes up the whole width.
 	 */				
	const val BLOCK = "block"

	/**
	 * Makes the container disappear, making the child elements children of the element the next level up in the DOM.
	 */
	const val CONTENTS = "contents"

	/**
	 * Displays an element as a block-level flex container.
	 */
	const val FLEX = "flex"

	/**
	 * Displays an element as a block-level grid container.
	 */
	const val GRID = "grid"

	/**
	 * Displays an element as an inline-level block container. The element itself is formatted as an inline element, but 
	 * you can apply height and width values.
	 */
	const val INLINE_BLOCK = "inline-block"

	/**
	 * Displays an element as an inline-level flex container.
	 */
	const val INLINE_FLEX = "inline-flex"

	/**
	 * Displays an element as an inline-level grid container.
	 */
	const val INLINE_GRID = "inline-grid"

	/**
	 * The element is displayed as an inline-level table.
	 */
	const val INLINE_TABLE = "inline-table"

	/**
	 * Let the element behave like a `<li>` element.
	 */
	const val LIST_ITEM = "list-item"

	/**
	 * Displays an element as either block or inline, depending on context.
	 */
	const val RUN_IN = "run-in"

	/**
	 * Let the element behave like a `<table>` element.
	 */
	const val TABLE = "table"

	/**
	 * Let the element behave like a `<caption>` element.
	 */
	const val TABLE_CAPTION = "table-caption"

	/**
	 * Let the element behave like a `<colgroup>` element.
	 */
	const val TABLE_COLUMN_GROUP = "table-column-group"

	/**
	 * Let the element behave like a `<thead>` element.
	 */
	const val TABLE_HEADER_GROUP = "table-header-group"

	/**
	 * Let the element behave like a `<tfoot>` element.
	 */
	const val TABLE_FOOTER_GROUP = "table-footer-group"

	/**
	 * Let the element behave like a `<tbody>` element.
	 */
	const val TABLE_ROW_GROUP = "table-row-group"

	/**
	 * Let the element behave like a `<td>` element.
	 */
	const val TABLE_CELL = "table-cell"

	/**
	 * Let the element behave like a `<col>` element.
	 */
	const val TABLE_COLUMN = "table-column"

	/**
	 * Let the element behave like a `<tr>` element.
	 */
	const val TABLE_ROW = "table-row"

	/**
	 * The element is completely removed.
	 */
	const val NONE = "none"

	/**
	 * Sets this property to its default value.
	 */
	const val INITIAL = "initial"

	/**
	 * Inherits this property from its parent element.
	 */
	const val INHERIT = "inherit"
}  