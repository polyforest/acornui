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

package com.acornui.texturepacker

import com.acornui.serialization.From
import com.acornui.serialization.Reader
import com.acornui.serialization.floatArray

/**
 * Represents metadata for a source image before packing.
 * Allows data to be set that affects cropping, scaling, 9-slices, and padding.
 * @author nbilyk
 */
data class ImageMetadata(

		/**
		 * Used for 9 patches. A float array of left, top, right, bottom
		 */
		val splits: List<Float>? = null

)

object ImageMetadataSerializer : From<ImageMetadata> {
	override fun read(reader: Reader): ImageMetadata {
		return ImageMetadata(
				splits = reader.floatArray("splits")?.toList()
		)
	}
}