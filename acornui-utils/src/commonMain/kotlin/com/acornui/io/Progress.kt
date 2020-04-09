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

package com.acornui.io

import com.acornui.ChildRo
import com.acornui.Parent
import com.acornui.ParentRo
import com.acornui.async.delay
import com.acornui.time.sumByDuration
import kotlin.time.Duration
import kotlin.time.seconds

/**
 * An interface indicating something that takes time to complete.
 */
interface Progress {

	/**
	 * The amount of time currently loaded.
	 */
	val loaded: Duration

	/**
	 * The total number of seconds estimated to load.
	 */
	val total: Duration

	val isComplete: Boolean
		get() = loaded >= total

	val isLoading: Boolean
		get() = if (total <= Duration.ZERO) false else loaded < total

}

data class ProgressImpl(override var loaded: Duration = Duration.ZERO, override var total: Duration = Duration.ZERO) : Progress

val Progress.percentLoaded: Double
	get() = if (total <= Duration.ZERO) 1.0 else loaded / total

val Progress.remaining: Duration
	get() = total - loaded

/**
 * Suspends the coroutine until this [Progress] instance has finished loading, for at least [interval].
 * @param interval The minimum buffer the progress instance must remain completed.
 */
suspend fun Progress.await(interval: Duration = 0.1.seconds) {
	var loadedCheck = 0
	while (loadedCheck < 10) {
		if (isLoading) {
			loadedCheck = 0
			delay(interval)
		} else {
			loadedCheck++
			delay(interval / 10.0)
		}
	}
}

/**
 * Provides a way to add child [Progress] trackers.
 */
interface ProgressReporter : Parent<Progress>, Progress

inline fun <R> ProgressReporter.addWork(expectedDuration: Duration, inner: () -> R): R {
	val p = ProgressImpl(total = expectedDuration)
	addChild(p)
	val result = inner()
	removeChild(p)
	return result
}

open class ProgressReporterImpl : ProgressReporter {

	override val parent: ParentRo<ChildRo>? = null

	private val _children = mutableListOf<Progress>()
	override val children: List<Progress> = _children

	override fun <S : Progress> addChild(index: Int, child: S): S {
		_children.add(index, child)
		return child
	}

	override fun removeChild(index: Int): Progress {
		return _children.removeAt(index)
	}

	override val loaded: Duration
		get() = _children.sumByDuration { it.loaded }

	override val total: Duration
		get() = _children.sumByDuration { it.total }
}