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

package com.acornui.core.assets

import com.acornui.core.audio.Music
import com.acornui.core.audio.SoundFactory
import com.acornui.core.graphics.RgbData
import com.acornui.core.graphics.Texture
import com.acornui.io.NativeReadByteBuffer

/**
 * AssetType represents the type of asset an AssetLoader will load.
 *
 * Note: Custom asset types may be created, but be sure to use a unique name.
 */
@Suppress("unused")
data class AssetType<T>(val name: String) {

	companion object {

		/**
		 * Loads as UTF-8 text.
		 */
		val TEXT: AssetType<String> = AssetType("text")

		val BINARY:AssetType<NativeReadByteBuffer> = AssetType("binary")

		val TEXTURE: AssetType<Texture> = AssetType("texture")

		/**
		 * An in-memory sound.
		 */
		val SOUND: AssetType<SoundFactory> = AssetType("audio")

		/**
		 * A Streaming sound.
		 */
		val MUSIC: AssetType<Music> = AssetType("music")

		// Currently only available in the JvmHeadless backend.
		val RGB_DATA: AssetType<RgbData> = AssetType("rgbData")
	}
}