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

package com.acornui.collection

/**
 * A typealias for a method that takes one input argument and returns a Boolean.
 * A filter or predicate function.
 * Filter functions should never cause side effects - The element or collections they filter should not be modified.
 */
typealias Filter<E> = (E) -> Boolean

/**
 * Always returns true.
 */
val AlwaysFilter = { _: Any? -> true }

/**
 * Always returns false.
 */
val NeverFilter = { _: Any? -> false }

/**
 * AND logic.
 *
 * ```
 * Inputs | Output
 * T   T  | T
 * F   T  | F
 * T   F  | F
 * F   F  | F
 * ```
 */
infix fun <E> Filter<E>.and(other: Filter<E>): Filter<E> = { this(it) && other(it) }

/**
 * OR logic.
 *
 * ```
 * Inputs | Output
 * T   T  | T
 * F   T  | T
 * T   F  | T
 * F   F  | F
 * ```
 */
infix fun <E> Filter<E>.or(other: Filter<E>): Filter<E> = { this(it) || other(it) }

/**
 * NOR logic.
 *
 * ```
 * Inputs | Output
 * T   T  | F
 * F   T  | F
 * T   F  | F
 * F   F  | T
 * ```
 */
infix fun <E> Filter<E>.nor(other: Filter<E>): Filter<E> = { !this(it) && !other(it) }

/**
 * Exclusive OR logic.
 *
 * ```
 * Inputs | Output
 * T   T  | F
 * F   T  | T
 * T   F  | T
 * F   F  | F
 * ```
 */
infix fun <E> Filter<E>.xor(other: Filter<E>): Filter<E> = { this(it) != other(it) }

/**
 * Exclusive NOR logic.
 * Returns true if this(it) == other(it)
 *
 * ```
 * Inputs | Output
 * T   T  | T
 * F   T  | F
 * T   F  | F
 * F   F  | T
 * ```
 */
infix fun <E> Filter<E>.xnor(other: Filter<E>): Filter<E> = { this(it) == other(it) }

/**
 * Not operator.
 * Changes the target filter from true to false and false to true.
 *
 * ```
 * Inputs | Output
 * T | F
 * F | T
 * ```
 */
fun <E> not(target: Filter<E>): Filter<E> = { !target(it) }
