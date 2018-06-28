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

package com.acornui.jvm.io

import com.acornui.core.io.BufferFactory
import com.acornui.io.ReadWriteNativeBuffer
import com.acornui.io.ReadWriteNativeByteBuffer
import org.lwjgl.BufferUtils

/**
 * @author nbilyk
 */
class JvmBufferFactory : BufferFactory {

	override fun byteBuffer(capacity: Int): ReadWriteNativeByteBuffer {
		return JvmByteBuffer(BufferUtils.createByteBuffer(capacity))
	}

	override fun shortBuffer(capacity: Int): ReadWriteNativeBuffer<Short> {
		return JvmShortBuffer(BufferUtils.createShortBuffer(capacity))
	}

	override fun intBuffer(capacity: Int): ReadWriteNativeBuffer<Int> {
		return JvmIntBuffer(BufferUtils.createIntBuffer(capacity))
	}

	override fun floatBuffer(capacity: Int): ReadWriteNativeBuffer<Float> {
		return JvmFloatBuffer(BufferUtils.createFloatBuffer(capacity))
	}

	override fun doubleBuffer(capacity: Int): ReadWriteNativeBuffer<Double> {
		return JvmDoubleBuffer(BufferUtils.createDoubleBuffer(capacity))
	}

}