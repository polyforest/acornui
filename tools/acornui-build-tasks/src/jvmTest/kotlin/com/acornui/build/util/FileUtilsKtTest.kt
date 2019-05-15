///*
// * Copyright 2017 Nicholas Bilyk
// *
// * Licensed under the Apache License, Version 2.0 (the "License");
// * you may not use this file except in compliance with the License.
// * You may obtain a copy of the License at
// *
// *     http://www.apache.org/licenses/LICENSE-2.0
// *
// * Unless required by applicable law or agreed to in writing, software
// * distributed under the License is distributed on an "AS IS" BASIS,
// * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// * See the License for the specific language governing permissions and
// * limitations under the License.
// */
//
//package com.acornui.build.util
//
//import kotlin.test.AfterTest
//import kotlin.test.assertFalse
//import kotlin.test.assertTrue
//import kotlin.test.BeforeTest
//import kotlin.test.Test
//import java.io.File
//
//class FileUtilsKtTest {
//
//	private lateinit var src: File
//	private lateinit var out: File
//
//	@BeforeTest
//	fun before() {
//		src = File("testOut/outOfDateSrc_${++id}")
//		File("resources/outOfDate").copyRecursively(src)
//
//		out = File("testOut/outOfDate_$id")
//		out.mkdirs()
//	}
//
//	@AfterTest
//	fun after() {
//		src.deleteRecursively()
//		out.deleteRecursively()
//	}
//
//	@Test
//	fun sourcesAreNewer() {
//		assertFalse(sourcesAreNewer(File(src, "doesNotExist"), out))
//
//		copy()
//		assertFalse(sourcesAreNewer(src, out))
//
//		File(src, "a/a.txt").touch()
//		assertTrue(sourcesAreNewer(src, out))
//
//	}
//
//	@Test
//	fun sourcesAreNewerAddedOrRemovedFile() {
//		copy()
//		val tmp = File(src, "a/temp.txt")
//		assertFalse(tmp.exists())
//		tmp.createNewFile()
//		assertTrue(sourcesAreNewer(src, out))
//
//		copy()
//		assertFalse(sourcesAreNewer(src, out))
//		tmp.delete()
//		assertTrue(sourcesAreNewer(src, out))
//	}
//
//	private fun copy() {
//		out.deleteRecursively()
//		src.copyRecursively(out)
//		Thread.sleep(1) // To ensure that new modifications don't come out to the same modified date.
//	}
//
//	companion object {
//		private var id = 0
//	}
//
//}