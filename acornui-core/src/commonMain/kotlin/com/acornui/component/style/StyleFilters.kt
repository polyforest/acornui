/*
 * Copyright 2017 Nicholas Bilyk
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


interface StyleFilter {
	operator fun invoke(target: StyleableRo): StyleableRo?
}

object AlwaysFilter : StyleFilter {
	override fun invoke(target: StyleableRo): StyleableRo? = target
}

object NeverFilter : StyleFilter {
	override fun invoke(target: StyleableRo): StyleableRo? = null
}

/**
 * Returns the target if both [operandA] or [operandB] passes.
 */
class AndStyleFilter(private val operandA: StyleFilter, private val operandB: StyleFilter) : StyleFilter {
	override fun invoke(target: StyleableRo): StyleableRo? {
		if (operandA(target) != null && operandB(target) != null) return target
		return null
	}
}

infix fun StyleFilter.and(other: StyleFilter): StyleFilter = AndStyleFilter(this, other)

/**
 * Returns the target if [operand] does not pass.
 */
class NotStyleFilter(private val operand: StyleFilter) : StyleFilter {
	override fun invoke(target: StyleableRo): StyleableRo? {
		if (operand(target) == null) return target
		return null
	}
}

fun not(target: StyleFilter): StyleFilter = NotStyleFilter(target)

/**
 * If [operandA] passes, [operandB] will be executed with the result from [operandA].
 */
class AndThenStyleFilter(private val operandA: StyleFilter, private val operandB: StyleFilter) : StyleFilter {
	override fun invoke(target: StyleableRo): StyleableRo? {
		val resultA = operandA(target) ?: return null
		return operandB(resultA)
	}
}

infix fun StyleFilter.andThen(other: StyleFilter) = AndThenStyleFilter(this, other)

/**
 * Returns the target if either [operandA] or [operandB] passes.
 */
class OrStyleFilter(private val operandA: StyleFilter, private val operandB: StyleFilter) : StyleFilter {
	override fun invoke(target: StyleableRo): StyleableRo? {
		if (operandA(target) != null || operandB(target) != null) return target
		return null
	}
}

infix fun StyleFilter.or(other: StyleFilter) = OrStyleFilter(this, other)

/**
 * Any ancestor passes the given child filter.
 */
class AncestorStyleFilter(private val operand: StyleFilter) : StyleFilter {
	override fun invoke(target: StyleableRo): StyleableRo? {
		target.walkStyleableAncestry {
			val r = operand(it)
			if (r != null) return r
		}
		return null
	}
}

fun withAncestor(operand: StyleFilter) = AncestorStyleFilter(operand)
fun withAnyAncestor(vararg operand: StyleFilter): StyleFilter {
	var ret: StyleFilter = NeverFilter
	for (filter in operand) {
		ret = ret or AncestorStyleFilter(filter)
	}
	return ret
}

/**
 * The direct parent passes the given child filter.
 */
class ParentStyleFilter(private val operand: StyleFilter) : StyleFilter {
	override fun invoke(target: StyleableRo): StyleableRo? {
		val p = target.styleParent ?: return null
		return operand(p)
	}
}

fun withParent(operand: StyleFilter) = ParentStyleFilter(operand)

/**
 * The target contains the given tag.
 */
@Deprecated("Use the Style tag as a filter", ReplaceWith("tag"))
class TargetStyleFilter(private val tag: StyleTag) : StyleFilter {
	override fun invoke(target: StyleableRo): StyleableRo? {
		if (target.styleTags.contains(tag)) {
			return target
		}
		return null
	}
}
