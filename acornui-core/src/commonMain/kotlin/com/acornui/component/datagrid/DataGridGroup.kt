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

package com.acornui.component.datagrid

import com.acornui.collection.Filter
import com.acornui.collection.ObservableList
import com.acornui.di.Context
import com.acornui.observe.ObservableBase

open class DataGridGroup<E> : ObservableBase() {

	/**
	 * Creates a header to the group. This should not include background or collapse arrow.
	 * The header is cached, so this method should not return inconsistent results.
	 */
	open fun createHeader(owner: Context, list: ObservableList<E>): DataGridGroupHeader {
		throw Exception("A header cell was requested, but createHeader was not implemented.")
	}

	/**
	 * The filter function for what rows will be shown below this group's header. The filter functions do not need
	 * to be mutually exclusive, but if they aren't, rows may be shown multiple times.
	 */
	var filter by watched<Filter<E>?>(null)

	/**
	 * If false, this group, along with its header, will not be shown.
	 */
	var visible by watched(true)

	/**
	 * If true, this group will be collapsed.
	 */
	var collapsed by watched(false)

	/**
	 * If false, a header row will not be shown.
	 */
	var showHeader by watched(true)

	/**
	 * If false, a footer row will not be shown.
	 */
	var showFooter by watched(false)

	/**
	 * If [showFooter] is true, and showFooterWhenCollapsed is true, a footer row will be displayed even when the group
	 * is collapsed.
	 */
	var showFooterWhenCollapsed by watched(true)
}
