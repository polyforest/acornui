/*
 * Copyright 2018 Nicholas Bilyk
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

package com.acornui.jvm.loader

import com.acornui.core.assets.AssetType
import com.acornui.io.NativeReadByteBuffer
import com.acornui.jvm.io.JvmByteBuffer
import java.io.InputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder

open class JvmBinaryLoader(
		path: String,
		workScheduler: WorkScheduler<NativeReadByteBuffer>
) : JvmAssetLoaderBase<NativeReadByteBuffer>(path, AssetType.BINARY, workScheduler) {

	init {
		init()
	}

	override fun create(inputStream: InputStream): NativeReadByteBuffer {
		val byteArray = inputStream.use {
			it.readAllBytes()
		}
		val buffer = ByteBuffer.wrap(byteArray)
		buffer.order(ByteOrder.LITTLE_ENDIAN)
		return JvmByteBuffer(buffer)
	}
}