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

package com.acornui.jvm.loader

import com.acornui.core.assets.AssetTypes
import java.io.InputStream
import java.nio.charset.Charset

open class JvmTextLoader(
		path: String,
		private val charset: Charset,
		isAsync: Boolean
) : JvmAssetLoaderBase<String>(path, AssetTypes.TEXT, isAsync) {

	init {
		init()
	}

	override fun create(fis: InputStream): String {
		val size = if (bytesTotal <= 0) DEFAULT_BUFFER_SIZE else bytesTotal
		val bytes = fis.use { it.readBytes(size) }
		return bytes.toString(charset)
	}
}