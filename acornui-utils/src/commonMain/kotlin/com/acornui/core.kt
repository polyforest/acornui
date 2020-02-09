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

package com.acornui

interface Disposable {

	/**
	 * Prepares this object for garbage collection.
	 * A disposed object should no longer be used or referenced.
	 */
	fun dispose()
}

fun <A : Comparable<A>> A?.compareTo(other: A?): Int {
	if (this == null && other == null) return 0
	if (this == null) return -1
	if (other == null) return 1
	return compareTo(other)
}

typealias EqualityCheck<E> = (a: E, b: E) -> Boolean

/**
 * Converts a lambda to a [Disposable] object, where the lambda is called on [Disposable.dispose].
 */
fun (()->Any?).toDisposable(): Disposable {
	return object : Disposable {
		override fun dispose() {
			this@toDisposable()
		}
	}
}

/**
 * Used to mark parts of the Acorn API as not being ready for public.
 */
@Experimental(Experimental.Level.WARNING)
annotation class ExperimentalAcorn

/**
 * Marks that a method where no matter how many times it's invoked, given the same inputs will always return the same
 * outputs.
 * 
 * Currently this is just a marker interface, but may be used for caching purposes in the future.
 */
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.SOURCE)
annotation class Idempotent