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

package com.acornui.js.file

import com.acornui.file.FileReadWriteManager
import com.acornui.io.NativeBuffer

class JsFileReadWriteManager : FileReadWriteManager {

	override suspend fun loadFromFileAsString(extensions: List<String>, defaultPath: String): String? {
		TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
	}

	override suspend fun saveToFileAsString(extension: String, defaultPath: String, value: String): Boolean {
		TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
	}

	override suspend fun loadFromFileAsBinary(extensions: List<String>, defaultPath: String): NativeBuffer<Byte>? {
		TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
	}

	override suspend fun saveToFileAsBinary(extension: String, defaultPath: String, value: NativeBuffer<Byte>): Boolean {
		TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
	}
}