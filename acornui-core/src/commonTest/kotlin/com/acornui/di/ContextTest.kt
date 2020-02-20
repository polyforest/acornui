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

package com.acornui.di

import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class ContextTest {

	@BeforeTest
	fun setup() {
		keyWithFactoryMain1.constructCount = 0
		keyWithFactoryMain2.constructCount = 0
		keyWithFactoryApp1.constructCount = 0
		keyWithFactoryApp2.constructCount = 0
	}

	@Test fun inject() {
		val key1 = contextKey<Int>()
		val key2 = contextKey<Int>()

		val c = ContextImpl(dependencyMapOf(key1 to 1, key2 to 2))
		assertEquals(2, c.inject(key2))
		assertEquals(1, c.inject(key1))
	}

	@Test fun injectOptional() {
		val key1 = contextKey<Int>()
		val key2 = contextKey<Int>()
		val key3 = contextKey<Int>()
		val c = ContextImpl(dependencyMapOf(key1 to 1, key2 to 2))
		assertEquals(null, c.injectOptional(key3))
	}

	@Test fun childDependencies() {
		val key1 = contextKey<String>()
		val key2 = contextKey<String>()
		val key3 = contextKey<String>()
		val key4 = contextKey<String>()
		val c = ContextImpl(dependencyMapOf(key1 to "should be overridden", key2 to "two"))
		c.childDependencies += dependencyMapOf(key1 to "overridden", key3 to "three")
		val d = ContextImpl(c)
		assertEquals(null, d.injectOptional(key4))
		assertEquals("overridden", d.injectOptional(key1))
		assertEquals("two", d.injectOptional(key2))
		assertEquals("three", d.injectOptional(key3))
	}

	private val keyWithFactoryMain1 = object : Context.Key<Int> {
		var constructCount = 0
		override val factory = dependencyFactory(ContextMarker.MAIN) {
			++constructCount
			1
		}
	}

	private val keyWithFactoryMain2 = object : Context.Key<Int> {
		var constructCount = 0
		override val factory = dependencyFactory(ContextMarker.MAIN) {
			++constructCount
			2
		}
	}

	private val keyWithFactoryApp1 = object : Context.Key<Float> {
		var constructCount = 0
		override val factory = dependencyFactory(ContextMarker.APPLICATION) {
			++constructCount
			1f
		}
	}

	private val keyWithFactoryApp2 = object : Context.Key<Float> {
		var constructCount = 0
		override val factory = dependencyFactory(ContextMarker.APPLICATION) {
			++constructCount
			2f
		}
	}

	@Test fun keysWithFactory() {
		val mainContext = ContextImpl(marker = ContextMarker.MAIN)
		val appContext1 = ContextImpl(mainContext, marker = ContextMarker.APPLICATION)
		val appContext2 = ContextImpl(mainContext, marker = ContextMarker.APPLICATION)
		// Repeatedly inject, assert expected result, then assert construction happens only as many times as needed
		assertEquals(1, appContext1.inject(keyWithFactoryMain1))
		assertEquals(1, appContext1.inject(keyWithFactoryMain1))
		assertEquals(1, appContext2.inject(keyWithFactoryMain1))
		assertEquals(1, appContext2.inject(keyWithFactoryMain1))
		assertEquals(1, mainContext.inject(keyWithFactoryMain1))
		assertEquals(1, mainContext.inject(keyWithFactoryMain1))
		assertEquals(2, mainContext.inject(keyWithFactoryMain2))
		assertEquals(2, mainContext.inject(keyWithFactoryMain2))
		assertEquals(2, appContext1.inject(keyWithFactoryMain2))
		assertEquals(2, appContext1.inject(keyWithFactoryMain2))
		assertEquals(2, appContext2.inject(keyWithFactoryMain2))
		assertEquals(2, appContext2.inject(keyWithFactoryMain2))

		assertEquals(1f, appContext1.inject(keyWithFactoryApp1))
		assertEquals(1f, appContext1.inject(keyWithFactoryApp1))
		assertEquals(1f, appContext2.inject(keyWithFactoryApp1))
		assertEquals(1f, appContext2.inject(keyWithFactoryApp1))
		assertEquals(2f, appContext1.inject(keyWithFactoryApp2))
		assertEquals(2f, appContext2.inject(keyWithFactoryApp2))
		assertFailsWith(Context.ContextMarkerNotFoundException::class) {
			mainContext.inject(keyWithFactoryApp1)
		}
		assertFailsWith(Context.ContextMarkerNotFoundException::class) {
			mainContext.inject(keyWithFactoryApp2)
		}
		assertEquals(2, keyWithFactoryApp1.constructCount)
		assertEquals(2, keyWithFactoryApp2.constructCount)
		assertEquals(1, keyWithFactoryMain1.constructCount)
		assertEquals(1, keyWithFactoryMain2.constructCount)
	}

}