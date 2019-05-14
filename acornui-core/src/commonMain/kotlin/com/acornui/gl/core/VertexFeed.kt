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

package com.acornui.gl.core

/**
 * A [VertexFeed] provides a way to push vertex information.
 */
interface VertexFeed {

	/**
	 * The number of vertex components currently in the feed.
	 */
	val vertexComponentsCount: Int

	/**
	 * The vertex attributes for this feed, this should never change.
	 */
	val vertexAttributes: VertexAttributes

	fun putVertexComponent(value: Float)

}

/**
 * An [IndexFeed] provides a way to push indices to an index buffer.
 */
interface IndexFeed {

	/**
	 * The number of indices currently in the feed.
	 */
	val indicesCount: Int

	/**
	 * Returns the currently highest index pushed.
	 * This will be -1 if no indices have been pushed.
	 */
	val highestIndex: Short

	/**
	 * Sets the index at the current buffer position.
	 * This will also update the [highestIndex] property.
	 */
	fun putIndex(index: Short)

}

/**
 * For convenience, casts the index Int to a Short
 */
fun IndexFeed.putIndex(index: Int) {
	putIndex(index.toShort())
}

fun IndexFeed.putIndices(value: List<Int>) {
	val n = highestIndex + 1
	for (i in 0..value.lastIndex) {
		putIndex(n + value[i])
	}
}

fun IndexFeed.putIndices(value: IntArray) {
	val n = highestIndex + 1
	for (i in 0..value.lastIndex) {
		putIndex(n + value[i])
	}
}

fun IndexFeed.putIndicesReversed(value: IntArray) {
	val n = highestIndex + 1
	for (i in 0..value.lastIndex) {
		putIndex(n + value[value.lastIndex - i])
	}
}

fun IndexFeed.putIndices(value: ShortArray) {
	val n = highestIndex + 1
	for (i in 0..value.lastIndex) {
		putIndex(n + value[i])
	}
}

@Deprecated("Renamed to putQuadIndices", ReplaceWith("putQuadIndices"))
fun IndexFeed.pushQuadIndices() = putQuadIndices()

fun IndexFeed.putQuadIndices() {
	val n = highestIndex + 1
	putIndex(n + 0)
	putIndex(n + 1)
	putIndex(n + 2)
	putIndex(n + 2)
	putIndex(n + 3)
	putIndex(n + 0)
}

fun IndexFeed.putCcwQuadIndices() {
	val n = highestIndex + 1
	putIndex(n + 0)
	putIndex(n + 3)
	putIndex(n + 2)
	putIndex(n + 2)
	putIndex(n + 1)
	putIndex(n + 0)
}

fun IndexFeed.putTriangleIndices() {
	val n = highestIndex + 1
	putIndex(n + 0)
	putIndex(n + 1)
	putIndex(n + 2)
}
