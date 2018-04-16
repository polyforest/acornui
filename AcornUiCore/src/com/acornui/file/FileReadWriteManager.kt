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

package com.acornui.file

import com.acornui.core.di.DKey
import com.acornui.io.NativeBuffer

interface FileReadWriteManager {

	suspend fun loadFromFileAsString(extensions: List<String>, defaultPath: String): String?
	suspend fun saveToFileAsString(extension: String, defaultPath: String, value: String): Boolean

	suspend fun loadFromFileAsBinary(extensions: List<String>, defaultPath: String): NativeBuffer<Byte>?
	suspend fun saveToFileAsBinary(extension: String, defaultPath: String, value: NativeBuffer<Byte>): Boolean

	companion object : DKey<FileReadWriteManager>
}