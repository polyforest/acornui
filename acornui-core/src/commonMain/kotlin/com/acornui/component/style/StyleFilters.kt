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

package com.acornui.component.style

import com.acornui.collection.Filter

typealias StyleFilter = Filter<StylableRo>

/**
 * Any ancestor passes the given child filter.
 */
fun withAncestor(filter: StyleFilter): StyleFilter = { target ->
	target.findStylableAncestor {
		filter(it)
	} != null
}

fun withAncestor(tag: StyleTag) = withAncestor(tag.filter)

/**
 * Any ancestor passes any of the given child filters.
 */
fun withAnyAncestor(vararg filters: StyleFilter): StyleFilter = { target ->
	target.findStylableAncestor { ancestor ->
		filters.any { it(ancestor) }
	} != null
}

fun withAnyAncestor(tag: StyleTag) = withAnyAncestor(tag.filter)

/**
 * The direct parent passes the given child filter.
 */
fun withParent(operand: StyleFilter): StyleFilter = {
	val p = it.styleParent
	p != null && operand(p)
}

fun withParent(tag: StyleTag) = withParent(tag.filter)

val StyleTag.filter: StyleFilter
	get() = {
		it.styleTags.contains(this)
	}