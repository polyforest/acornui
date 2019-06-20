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

import java.io.InputStream
import java.util.*

private const val MAX_BUFFER_SIZE = Integer.MAX_VALUE - 8

/**
 * Jvm 9's readAllBytes method, but for Jvm 6+
 */
fun InputStream.readAllBytes2(): ByteArray {
	var buf = ByteArray(DEFAULT_BUFFER_SIZE)
	var capacity = buf.size
	var nread = 0
	var n: Int
	while (true) {
		// read to EOF which may read more or less than initial buffer size
		while (true) {
			n = read(buf, nread, capacity - nread)
			if (n <= 0) break
			nread += n
		}

		// if the last call to read returned -1, then we're done
		if (n < 0)
			break

		// need to allocate a larger buffer
		capacity = if (capacity <= MAX_BUFFER_SIZE - capacity) {
			capacity shl 1
		} else {
			if (capacity == MAX_BUFFER_SIZE)
				throw OutOfMemoryError("Required array size too large")
			MAX_BUFFER_SIZE
		}
		buf = Arrays.copyOf(buf, capacity)
	}
	return if (capacity == nread) buf else Arrays.copyOf(buf, nread)
}
