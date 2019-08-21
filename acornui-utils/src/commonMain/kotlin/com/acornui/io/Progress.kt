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
import com.acornui.collection.sumByFloat2

/**
 * An interface indicating something that takes time to complete.
 */
interface Progress {

	/**
	 * The number of seconds currently loaded.
	 */
	val secondsLoaded: Float

	/**
	 * The total number of seconds estimated to load.
	 */
	val secondsTotal: Float

	val percentLoaded: Float
		get() = if (secondsTotal <= 0f) 1f else secondsLoaded / secondsTotal
}

val Progress.secondsRemaining: Float
	get() = secondsTotal - secondsLoaded

val Progress.isLoading: Boolean
	get() = percentLoaded < 1f

/**
 * Provides a way to add child [Progress] trackers.
 */
interface ProgressReporter : Parent<Progress>, Progress

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

	override val secondsLoaded: Float
		get() = _children.sumByFloat2 { it.secondsLoaded }

	override val secondsTotal: Float
		get() = _children.sumByFloat2 { it.secondsTotal }
}

object GlobalProgressReporter : ProgressReporterImpl()