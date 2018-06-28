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

package com.acornui.core.io

import com.acornui.io.ReadWriteNativeBuffer
import com.acornui.io.ReadWriteNativeByteBuffer

// TODO: When we migrate to new build system, switch this factory with expects/actual

/**
 * @author nbilyk
 */
interface BufferFactory {
	fun byteBuffer(capacity: Int): ReadWriteNativeByteBuffer
	fun shortBuffer(capacity: Int): ReadWriteNativeBuffer<Short>
	fun intBuffer(capacity: Int): ReadWriteNativeBuffer<Int>
	//	public fun longBuffer(capacity: Int): ReadWriteNativeBuffer<Long>  // JS doesn't seem to have an Int64Array
	fun floatBuffer(capacity: Int): ReadWriteNativeBuffer<Float>

	fun doubleBuffer(capacity: Int): ReadWriteNativeBuffer<Double>

	companion object {
		lateinit var instance: BufferFactory
	}
}