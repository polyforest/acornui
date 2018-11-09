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

package com.acornui.core.time

import com.acornui.core.di.Owned
import com.acornui.core.di.Scoped


/**
 * Measures the amount of time (in milliseconds) a block of code takes to execute.
 */
inline fun Scoped.measure(inner:() -> Unit) {
	val start = time.nanoElapsed()
	inner()
	val end = time.nanoElapsed()
	println("Time: ${(end - start) / 1000000.0}")
}

private var _measuredTotalTime: Double = 0.0
private var _measuredIterations: Int = 0

/**
 * Measures a block of code, accumulating the total time until printFrameMeasure() is called, which will then
 * print the average execution time.
 * @see [printFrameMeasure]
 */
fun Scoped.averagedMeasure(inner:() -> Unit) {
	_measuredIterations++
	val start = time.nanoElapsed()
	inner()
	val end = time.nanoElapsed()
	_measuredTotalTime += end - start
}

/**
 * Prints the average time it took to execute the blocks from [averagedMeasure]. Resets the number of iterations
 * and total time.
 */
fun printFrameMeasure() {
	if (_measuredIterations == 0) return
	println("Total time: ${_measuredTotalTime / 1000000.0} Iterations: ${_measuredIterations} Average Time: ${_measuredTotalTime / _measuredIterations.toDouble() / 1000000.0}")
	_measuredIterations = 0
	_measuredTotalTime = 0.0
}