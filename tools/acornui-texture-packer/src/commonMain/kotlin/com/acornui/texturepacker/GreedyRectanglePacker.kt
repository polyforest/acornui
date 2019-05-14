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

import com.acornui.logging.Log
import com.acornui.math.IntRectangle

/**
 * @author nbilyk
 */
class GreedyRectanglePacker(private val settings: PackerAlgorithmSettingsData) : RectanglePacker {

	private val SIZES = arrayOf(Pair(16, 16), Pair(32, 32), Pair(64, 32), Pair(64, 64), Pair(128, 128), Pair(256, 128), Pair(256, 256), Pair(512, 256), Pair(512, 512), Pair(1024, 512), Pair(1024, 1024))
	private val MAX_PAGES = 20

	override fun pack(inputRectangles: Iterable<PackerRectangleData>): List<PackerPageData> {
		val remaining = ArrayList<PackerRectangleData>()
		remaining.addAll(inputRectangles)

		// Sorts the rectangles in descending order by difficulty to place.
		remaining.sortByDescending { it.difficulty }

		val pages = ArrayList<PackerPageData>()
		fillPages(remaining, pages)

		return pages
	}

	private fun fillPages(remaining: MutableList<PackerRectangleData>, pages: MutableList<PackerPageData>) {

		//		if (region.width > largestDimension) largestDimension = region.width
		//		if (region.height > largestDimension) largestDimension = region.height

		var largestSizeIndex = 0
		for (i in SIZES.lastIndex downTo 0) {
			val (w, h) = SIZES[i]
			if (w <= settings.pageMaxWidth && h <= settings.pageMaxHeight) {
				largestSizeIndex = i
				break
			}
		}

		val largestPage = SIZES[largestSizeIndex]

		// Validate that all regions can fit on the largest page.
		val maxPageRectangle = IntRectangle(settings.paddingX, settings.paddingY, largestPage.first - settings.paddingX * 2, largestPage.second - settings.paddingY * 2)
		val remainingIt = remaining.iterator()
		while (remainingIt.hasNext()) {
			val region = remainingIt.next()
			if (!maxPageRectangle.canContain(region.width, region.height)) {
				Log.error("Region ${region.name} does not fit in the max page size.")
				remainingIt.remove()
			}
		}

		while (remaining.isNotEmpty()) {
			var left = 0
			var right = largestSizeIndex

			// Find the best size of page to fit the regions.
			while (right > left) {
				val mid = (left + right) shr 1

				val (pageWidth, pageHeight) = SIZES[mid]
				val remainingTest = ArrayList<PackerRectangleData>()
				remainingTest.addAll(remaining)
				fillPage(pageWidth, pageHeight, ArrayList<PackerRectangleData>(), remainingTest, true)

				if (remainingTest.isNotEmpty()) {
					left = mid + 1
				} else {
					right = mid
				}
			}

			// We found the best size; do the page.
			val placed = ArrayList<PackerRectangleData>()
			val (pageWidth, pageHeight) = SIZES[left]
			fillPage(pageWidth, pageHeight, placed, remaining, false)

			val page = PackerPageData(pageWidth, pageHeight, placed)
			pages.add(page)
			if (pages.size > MAX_PAGES) throw Exception("Exceeded $MAX_PAGES pages, there may be a problem with this texture packing algorithm.")
		}

	}

	private fun fillPage(pageWidth: Int, pageHeight: Int, placed: MutableList<PackerRectangleData>, remaining: MutableList<PackerRectangleData>, testMode: Boolean) {
		val buckets = ArrayList<IntRectangle>()
		addBucket(buckets, IntRectangle(settings.paddingX, settings.paddingY, pageWidth - settings.paddingX * 2, pageHeight - settings.paddingY * 2))
		val remainingIt = remaining.iterator()
		while (remainingIt.hasNext()) {
			val regionToFit = remainingIt.next()
			val minIndex = getAreaIndex(regionToFit.bounds.area, buckets)
			var bucketCandidate: IntRectangle? = null
			for (i in minIndex..buckets.lastIndex) {
				bucketCandidate = buckets[i]
				if (bucketCandidate.canContain(regionToFit.width, regionToFit.height)) {
					break
				} else if (bucketCandidate.canContain(regionToFit.height, regionToFit.width)) {
					regionToFit.toggleRotated()
					break
				} else {
					bucketCandidate = null
				}
			}
			if (bucketCandidate != null) {
				remainingIt.remove() // Remove the region from the remaining list, we found it a home.
				regionToFit.bounds.x = bucketCandidate.x
				regionToFit.bounds.y = bucketCandidate.y
				placed.add(regionToFit)
				splitBucket(bucketCandidate, regionToFit.bounds, buckets)
			} else {
				if (testMode) return // We need to increase the page size.
			}
		}
	}

	/**
	 * When putting a region inside a bucket, it removes that bucket and adds two smaller ones (If they are big enough to fit a region).
	 */
	private fun splitBucket(bucket: IntRectangle, bounds: IntRectangle, buckets: MutableList<IntRectangle>) {
		buckets.remove(bucket)
		val padX = settings.paddingX
		val padY = settings.paddingY
		val bucketW = bucket.width
		val bucketH = bucket.height
		val w: Int = bucketW - bounds.width - padX
		val h: Int = bucketH - bounds.height - padY
		if (w * bucketH + bounds.width * h > h * bucketW + bounds.height * w) {
			// The two rectangles have a larger combined area when splitting horizontally first.
			addBucket(buckets, IntRectangle(bucket.x + bounds.width + padX, bucket.y, w, bucketH))
			addBucket(buckets, IntRectangle(bucket.x, bucket.y + bounds.height + padY, bounds.width, h))
		} else {
			// The two IntRectangles have a larger combined area when splitting vertically first.
			addBucket(buckets, IntRectangle(bucket.x + bounds.width + padX, bucket.y, w, bounds.height))
			addBucket(buckets, IntRectangle(bucket.x, bucket.y + bounds.height + padY, bucketW, h))
		}
	}

	private fun addBucket(buckets: MutableList<IntRectangle>, bucket: IntRectangle) {
		if (bucket.isEmpty) return
		val index = getAreaIndex(bucket.area, buckets)
		buckets.add(index, bucket)
	}

	private fun getAreaIndex(area: Int, buckets: MutableList<IntRectangle>): Int {
		var left = 0
		var right = buckets.size
		while (right > left) {
			val mid = (right + left) shr 1
			val midValue = buckets[mid]
			if (area >= midValue.area) {
				left = mid + 1
			} else {
				right = mid
			}
		}
		return left
	}
}

/**
 * Longer rectangles are harder to place.
 */
val PackerRectangleData.difficulty: Int
	get(): Int {
		return bounds.width + bounds.height
	}
