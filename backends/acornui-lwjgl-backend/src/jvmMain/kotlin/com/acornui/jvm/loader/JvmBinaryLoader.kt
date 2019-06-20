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

package com.acornui.jvm.loader

import com.acornui.core.asset.AssetType
import com.acornui.io.NativeReadByteBuffer
import com.acornui.io.JvmByteBuffer
import com.acornui.io.readAllBytes2
import java.io.InputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder

open class JvmBinaryLoader(
		path: String
) : JvmAssetLoaderBase<NativeReadByteBuffer>(path, AssetType.BINARY) {

	init {
		init()
	}

	override suspend fun create(inputStream: InputStream): NativeReadByteBuffer {
		val byteArray = inputStream.use {
			it.readAllBytes2()
		}
		val buffer = ByteBuffer.wrap(byteArray)
		buffer.order(ByteOrder.LITTLE_ENDIAN)
		return JvmByteBuffer(buffer)
	}
}
