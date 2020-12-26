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

package com.acornui.async

import com.acornui.Disposable
import com.acornui.signal.OnceSignal
import com.acornui.signal.Signal
import com.acornui.signal.unmanagedSignal
import com.acornui.time.sumByDuration
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine
import kotlin.time.Duration
import kotlin.time.milliseconds

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

	/**
	 * Dispatched when this task has completed.
	 * Implementations may vary whether or not the [isComplete] state may be reset and therefore whether or not this
	 * signal may be dispatched multiple times.
	 */
	val completed: Signal<Unit>

	/**
	 * True if this task has finished.
	 */
	val isComplete: Boolean

	val isLoading: Boolean
		get() = !isComplete

}

interface MutableProgress : Progress {

	/**
	 * The amount of time currently loaded.
	 */
	override var loaded: Duration

	/**
	 * The total number of seconds estimated to load.
	 */
	override var total: Duration

	/**
	 * Marks this progress tracker as completed.
	 */
	fun complete()
}

class ProgressImpl(
	override var total: Duration = Duration.ZERO
) : MutableProgress {

	override var loaded: Duration = Duration.ZERO

	override val completed = OnceSignal<Unit>()

	override var isComplete: Boolean = false
		private set

	/**
	 * Marks this progress tracker as completed.
	 * Dispatches [completed].
	 * If this tracker is already complete,
	 */
	override fun complete() {
		if (isComplete) return
		isComplete = true
		completed.dispatch(Unit)
	}
}

val Progress.percentLoaded: Double
	get() = if (total <= Duration.ZERO) 1.0 else loaded / total

val Progress.remaining: Duration
	get() = total - loaded

/**
 * Suspends the coroutine until this [Progress] instance has been marked as complete.
 */
suspend fun Progress.await() = suspendCoroutine<Unit> { cont ->
	completed.listen {
		cont.resume(Unit)
	}
}

/**
 * Provides a way to add child [Progress] trackers.
 */
interface ProgressReporter : Progress {

	/**
	 * The amount of time currently loaded for [pending] [Progress] objects.
	 */
	override val loaded: Duration
		get() = pending.sumByDuration { it.loaded }

	/**
	 * The total number of seconds estimated to load.
	 */
	override val total: Duration
		get() = pending.sumByDuration { it.total }

	/**
	 * A list of currently pending [Progress] objects.
	 */
	val pending: List<Progress>

	/**
	 * Returns true if the [pending] list is empty.
	 */
	override val isComplete: Boolean
		get() = pending.isEmpty()

	fun createReporter(initialTotal: Duration = 1.milliseconds): MutableProgress
}

open class ProgressReporterImpl : ProgressReporter, Disposable {

	private val children = mutableListOf<Progress>()

	override val pending: List<Progress> = children

	override val completed = unmanagedSignal<Unit>()

	override fun createReporter(initialTotal: Duration): MutableProgress {
		val p = ProgressImpl(initialTotal)
		p.completed.listen {
			children.remove(p)
			if (children.size == 0)
				completed.dispatch(Unit)
		}
		children.add(p)
		if (children.size == 1)
			completed.dispatch(Unit)
		return p
	}

	override fun dispose() {
		completed.dispose()
	}
}