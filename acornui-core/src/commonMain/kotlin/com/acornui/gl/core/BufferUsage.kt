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

enum class BufferUsage(val value: Int) {

	/**
	 * The user will be changing the data after every use. Or almost every use.
	 */
	STREAM(Gl20.STREAM_DRAW),

	/**
	 * The user will set the data once.
	 */
	STATIC(Gl20.STATIC_DRAW),

	/**
	 * The user will set the data occasionally.
	 */
	DYNAMIC(Gl20.DYNAMIC_DRAW)

}
