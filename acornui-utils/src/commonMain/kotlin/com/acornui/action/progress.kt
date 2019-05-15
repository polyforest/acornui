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

package com.acornui.action

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
