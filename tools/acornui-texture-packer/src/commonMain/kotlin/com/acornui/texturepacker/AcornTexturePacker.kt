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

package com.acornui.texturepacker

import com.acornui.collection.ArrayIterator
import com.acornui.collection.ArrayList
import com.acornui.asset.AssetManager
import com.acornui.asset.AssetType
import com.acornui.graphic.AtlasPageData
import com.acornui.graphic.AtlasRegionData
import com.acornui.graphic.RgbData
import com.acornui.io.file.Directory
import com.acornui.gl.core.TexturePixelFormat
import com.acornui.graphic.Color
import com.acornui.math.IntRectangle
import com.acornui.serialization.Serializer

/**
 * @author nbilyk
 */
class AcornTexturePacker(
		private val assets: AssetManager,
		private val json: Serializer<String>) {

	suspend fun pack(root: Directory, settingsFilename: String = "_packSettings.json", quiet: Boolean = false): PackedTextureData {
		val settingsFile = root.getFile(settingsFilename) ?: throw Exception("$settingsFilename is missing")
		val settingsJson = assets.load(settingsFile.path, AssetType.TEXT).await()
		val settings = json.read(settingsJson, TexturePackerSettingsSerializer)
		return pack(root, settings, quiet)
	}

	/**
	 * Packs the images in the provided root directory. When complete, the onCompleted callback will be invoked with the
	 * packed atlas data.
	 *
	 * No files will be created in this step; use an atlas writer such as JvmTextureAtlasWriter to write to the filesystem.
	 *
	 * @param root  The root directory to recurse (up to settings.maxDirectoryDepth)
	 * @param settings The configuration variables to use when packing.
	 */
	suspend fun pack(root: Directory, settings: TexturePackerSettingsData, quiet: Boolean = false): PackedTextureData {
		settings.validate()

		val packer: RectanglePacker = when (settings.packAlgorithm) {
			TexturePackAlgorithm.BEST -> MaxRectsPacker(settings.algorithmSettings)
			TexturePackAlgorithm.GREEDY -> GreedyRectanglePacker(settings.algorithmSettings)
		}

		val imageSources = loadImageSources(root, settings)

		// Create an array of PackerRectangle objects from the sources.
		val rectangles = Array(imageSources.size) {
			val imageSource = imageSources[it]
			PackerRectangleData(IntRectangle(0, 0, imageSource.rgbData.width, imageSource.rgbData.height), false, it, imageSource.path)
		}

		// Calculate how the pages should be laid out.
		val packedRectanglePages = packer.pack(ArrayIterator(rectangles), quiet)

		// Create the image data for each page.
		val packedPages = createPackedPages(imageSources, packedRectanglePages, settings)

		if (settings.algorithmSettings.addWhitePixel) {
			for (packedPage in packedPages.pages) {
				packedPage.first.setPixel(0, 0, Color.WHITE)
			}
		}
		return packedPages
	}

	/**
	 * Scours a given directory, populating a list of SourceImageData objects, which represent the bitmap data of an
	 * image and its sibling metadata.
	 */
	private suspend fun loadImageSources(root: Directory, settings: TexturePackerSettingsData): ArrayList<SourceImageData> {
		val imageSources = ArrayList<SourceImageData>()
		root.walkFilesTopDown(settings.maxDirectoryDepth).forEach {
			fileEntry ->
			if (fileEntry.hasExtension("png") || fileEntry.hasExtension("jpg")) {
				val metadataFile = fileEntry.siblingFile(fileEntry.nameNoExtension + IMAGE_METADATA_EXTENSION)
				val rgbLoader = assets.load(fileEntry.path, AssetType.RGB_DATA)

				// If the image has a corresponding metadata file, load that, otherwise, use default metadata settings.
				val metadata: ImageMetadata = if (metadataFile != null) {
					val metadataJson = assets.load(metadataFile.path, AssetType.TEXT).await()
					json.read(metadataJson, ImageMetadataSerializer)
				} else ImageMetadata()

				var rgbData = rgbLoader.await()
				val padding: List<Int>
				if (settings.stripWhitespace && rgbData.hasAlpha) {
					padding = rgbData.calculateWhitespace(settings.alphaThreshold)
					if (padding.sum() > 0) {
						// There is whitespace, strip it. Strip it good.
						rgbData = rgbData.copySubRgbData(padding[0], padding[1], rgbData.width - padding[0] - padding[2], rgbData.height - padding[1] - padding[3])
					}
				} else {
					padding = listOf(0, 0, 0, 0)
				}
				if (rgbData.width != 0 && rgbData.height != 0) {
					val imageSource = SourceImageData(
							path = fileEntry.path,
							relativePath = root.relativePath(fileEntry),
							rgbData = rgbData,
							metadata = metadata,
							padding = padding
					)
					imageSources.add(imageSource)
				}
			}
		}
		return imageSources
	}

	/**
	 * Creates RgbData objects for each atlas page, populating them with the regions, and generates the atlas data
	 * models.
	 */
	private fun createPackedPages(imageSources: List<SourceImageData>, packedRectanglePages: List<PackerPageData>, settings: TexturePackerSettingsData): PackedTextureData {
		return PackedTextureData(List(packedRectanglePages.size) {
			val packedRectanglePage: PackerPageData = packedRectanglePages[it]

			// Create the RgbData
			val pageRgbData = RgbData(packedRectanglePage.width, packedRectanglePage.height, settings.pixelFormat == TexturePixelFormat.RGBA)
			for (packerRegion in packedRectanglePage.regions) {
				val imageSource = imageSources[packerRegion.originalIndex]
				if (packerRegion.isRotated) imageSource.rgbData.rotate90CW()
				pageRgbData.setRect(packerRegion.bounds.x, packerRegion.bounds.y, imageSource.rgbData)
			}

			// Create the AtlasPage value object.
			val atlasPage = AtlasPageData(
					texturePath = "", // Will be set later in the atlas writer.
					width = packedRectanglePage.width,
					height = packedRectanglePage.height,
					pixelFormat = settings.pixelFormat,
					premultipliedAlpha = settings.premultipliedAlpha,
					filterMin = settings.filterMin,
					filterMag = settings.filterMag,
					regions = ArrayList(packedRectanglePage.regions.size) {
						val packerRegion = packedRectanglePage.regions[it]
						val imageSource = imageSources[packerRegion.originalIndex]
						AtlasRegionData(
								bounds = IntRectangle(packerRegion.bounds.x, packerRegion.bounds.y, packerRegion.bounds.width - settings.algorithmSettings.paddingX, packerRegion.bounds.height - settings.algorithmSettings.paddingY),
								name = imageSource.relativePath,
								isRotated = packerRegion.isRotated,
								splits = imageSource.metadata.splits,
								padding = imageSource.padding
						)
					},
					hasWhitePixel = settings.algorithmSettings.addWhitePixel
			)

			Pair(pageRgbData, atlasPage)
		}, settings)
	}

	companion object {
		private const val IMAGE_METADATA_EXTENSION = ".meta.json"
	}
}

/**
 * The product of the AcornTexturePacker. Contains the bitmap and model data ready to write for each page.
 */
data class PackedTextureData(
		val pages: List<Pair<RgbData, AtlasPageData>>,
		val settings: TexturePackerSettingsData)

/**
 * A representation of the image bitmap data, and its corresponding metadata.
 */
private data class SourceImageData(
		val path: String,
		val relativePath: String,
		val rgbData: RgbData,
		val metadata: ImageMetadata,
		val padding: List<Int>)

/**
 * A rectangle for the RectanglePacker to position and rotate.
 */
data class PackerRectangleData(

		/**
		 * The bounds of this rectangle. x, y values will be set on output.
		 * The width and height values will be swapped if isRotated is true.
		 */
		val bounds: IntRectangle,

		/**
		 * True if the packer algorithm rotated this rectangle.
		 */
		var isRotated: Boolean,

		/**
		 * The original index of this rectangle before the packer algorithm rearranges it.
		 * Note: The packer is not responsible for setting this value.
		 */
		val originalIndex: Int,

		/**
		 * Used only for logging and error reporting.
		 */
		val name: String) {

	/**
	 * Rotates the region (or unrotates it if it was already rotated.)
	 */
	fun toggleRotated(): PackerRectangleData {
		val tmp = bounds.width
		bounds.width = bounds.height
		bounds.height = tmp
		isRotated = !isRotated
		return this
	}

	var x: Int
		get(): Int = bounds.x
		set(value) {
			bounds.x = value
		}

	var y: Int
		get(): Int = bounds.y
		set(value) {
			bounds.y = value
		}

	var width: Int
		get(): Int = bounds.width
		set(value) {
			bounds.width = value
		}

	var height: Int
		get(): Int = bounds.height
		set(value) {
			bounds.height = value
		}

}

/**
 * The positioned regions the RectanglePacker placed.
 */
data class PackerPageData(

		/**
		 * The page width
		 */
		val width: Int,

		/**
		 * The page height
		 */
		val height: Int,

		/**
		 * The packer rectangles on this page.
		 */
		val regions: List<PackerRectangleData>)

/**
 * An interface to an algorithm that places rectangles within pages.
 */
interface RectanglePacker {

	/**
	 * Packs the input rectangles into pages of the provided size.
	 * @return Returns a list of PackerPage objects, each with a list of the rectangles provided from inputImages.
	 */
	fun pack(inputRectangles: Iterable<PackerRectangleData>, quiet: Boolean): List<PackerPageData>
}

/**
 * Returns an int array of [left, top, right, bottom] representing how many pixels on each side passes the alpha
 * threshold.
 * @param alphaThreshold 0f means the pixels must be fully transparent to be stripped, 1f will strip everything.
 */
private fun RgbData.calculateWhitespace(alphaThreshold: Float): List<Int> {
	var left = 0
	for (x in 0..width - 1) {
		var passes = true
		for (y in 0..height - 1) {
			if (getAlpha(x, y) > alphaThreshold) {
				passes = false
				break
			}
		}
		if (passes) left++
		else break
	}

	var right = 0
	for (x in width - 1 downTo left + 1) {
		var passes = true
		for (y in 0..height - 1) {
			if (getAlpha(x, y) > alphaThreshold) {
				passes = false
				break
			}
		}
		if (passes) right++
		else break
	}

	var top = 0
	for (y in 0..height - 1) {
		var passes = true
		for (x in left..width - right - 1) {
			if (getAlpha(x, y) > alphaThreshold) {
				passes = false
				break
			}
		}
		if (passes) top++
		else break
	}

	var bottom = 0
	for (y in height - 1 downTo top + 1) {
		var passes = true
		for (x in left..width - right - 1) {
			if (getAlpha(x, y) > alphaThreshold) {
				passes = false
				break
			}
		}
		if (passes) bottom++
		else break
	}
	return listOf(left, top, right, bottom)
}
