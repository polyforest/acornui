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

package com.acornui.io.file

import com.acornui.test.*
import kotlin.test.*
import kotlin.test.Test

/**
 * @author nbilyk
 */
class FileEntryTest {

	@Test
	fun compare() {
		val arr = arrayOf(ManifestEntry("foo/Daz.txt", 0L, 0L, "unknown"),
				ManifestEntry("Caz.txt", 0L, 0L, "unknown"),
				ManifestEntry("foo/bar/Baz.txt", 0L, 0L, "unknown"),
				ManifestEntry("foo/bar/Caz.txt", 0L, 0L, "unknown"),
				ManifestEntry("foo/Baz.txt", 0L, 0L, "unknown"),
				ManifestEntry("Baz.txt", 0L, 0L, "unknown"),
				ManifestEntry("foo/bar/Daz.txt", 0L, 0L, "unknown"),
				ManifestEntry("Daz.txt", 0L, 0L, "unknown"),
				ManifestEntry("foo/Caz.txt", 0L, 0L, "unknown")
		)

		arr.sort()
		assertListEquals(arrayOf(ManifestEntry("Baz.txt", 0L, 0L, "unknown"),
				ManifestEntry("Caz.txt", 0L, 0L, "unknown"),
				ManifestEntry("Daz.txt", 0L, 0L, "unknown"),
				ManifestEntry("foo/Baz.txt", 0L, 0L, "unknown"),
				ManifestEntry("foo/Caz.txt", 0L, 0L, "unknown"),
				ManifestEntry("foo/Daz.txt", 0L, 0L, "unknown"),
				ManifestEntry("foo/bar/Baz.txt", 0L, 0L, "unknown"),
				ManifestEntry("foo/bar/Caz.txt", 0L, 0L, "unknown"),
				ManifestEntry("foo/bar/Daz.txt", 0L, 0L, "unknown")
		), arr)

		assertEquals("Baz.txt", ManifestEntry("Baz.txt", 0L, 0L, "unknown").name())
		assertEquals("Baz.txt", ManifestEntry("foo/bar/Baz.txt", 0L, 0L, "unknown").name())
		assertEquals("Caz.txt", ManifestEntry("foo/aaa/Caz.txt", 0L, 0L, "unknown").name())
	}

}