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

package com.acornui.recycle

/**
 * A Recycler is an ObjectPool that executes [configure] on objects retrieved via [obtain], and [unconfigure] on
 * objects freed via [free].
 *
 * @param create Constructs the new object. This will be invoked when the pool is exhausted.
 * @param configure Invoked on objects retrieved via [obtain]. This includes newly constructed objects.
 * @param unconfigure Invoked on objects returned via [free].
 */
class Recycler<T>(

		/**
		 * Constructs a new element
		 */
		create: () -> T,
		private val configure: (T) -> Unit,
		private val unconfigure: (T) -> Unit
) : ObjectPool<T>(create) {

	override fun obtain(): T {
		val obj = super.obtain()
		configure(obj)
		return obj
	}

	override fun free(obj: T) {
		unconfigure(obj)
		super.free(obj)
	}
}
