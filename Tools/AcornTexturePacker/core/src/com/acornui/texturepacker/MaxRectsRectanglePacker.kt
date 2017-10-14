/*******************************************************************************
 * Copyright 2011 See AUTHORS file.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/

package com.acornui.texturepacker

import com.acornui.math.IntRectangle
import com.acornui.math.MathUtils
import java.util.*


/**
 * Packs pages of images using the maximal rectangles bin packing algorithm by Jukka Jylänki. A brute force binary search is used
 * to pack into the smallest bin possible.
 * @author Nathan Sweet
 */
class MaxRectsPacker(algorithmSettings: PackerAlgorithmSettingsData) : RectanglePacker {

	private val settings = MaxRectsSettings()
	private val rectComparator = RectComparator()
	private val methods = FreeRectChoiceHeuristic.values()
	private val maxRects = MaxRects(settings)

	init {
		settings.maxHeight = algorithmSettings.pageMaxHeight
		settings.maxWidth = algorithmSettings.pageMaxWidth
		settings.paddingX = algorithmSettings.paddingX
		settings.paddingY = algorithmSettings.paddingY
		settings.rotation = algorithmSettings.allowRotation
		settings.pot = algorithmSettings.powerOfTwo
		settings.edgePadding = algorithmSettings.edgePadding
		settings.addWhitePixel = algorithmSettings.addWhitePixel
	}

	override fun pack(inputRectangles: Iterable<PackerRectangleData>): List<PackerPageData> {
		// Finesse the input to match what the LibGdx max rects algorithm expects.
		val inputRects = ArrayList<Rect>()
		for (rectangle in inputRectangles) {
			val rect = Rect()
			rect.name = rectangle.name
			rect.x = rectangle.x
			rect.y = rectangle.y
			rect.width = rectangle.width
			rect.height = rectangle.height
			rect.originalIndex = rectangle.originalIndex
			inputRects.add(rect)
		}
		val pages = pack(inputRects)

		// Then convert the output to what Acorn expects.
		val packerPages = Array(pages.size, {
			pageIndex ->
			val page = pages[pageIndex]
			PackerPageData(
					page.width,
					page.height,
					List(page.outputRects.size, {
						rectIndex ->
						val rect = page.outputRects[rectIndex]
						PackerRectangleData(
								IntRectangle(rect.x, rect.y, rect.width, rect.height),
								rect.rotated,
								rect.originalIndex,
								rect.name)
					})
			)
		})
		return packerPages.toList()
	}

	private fun pack(inputRects: MutableList<Rect>): MutableList<Page> {
		var rects = inputRects
		if (settings.fast) {
			if (settings.rotation) {
				// Sort by longest side if rotation is enabled.
				Collections.sort(rects) {
					o1, o2 ->
					val n1 = if (o1.width > o1.height) o1.width else o1.height
					val n2 = if (o2.width > o2.height) o2.width else o2.height
					n2 - n1
				}
			} else {
				// Sort only by width (largest to smallest) if rotation is disabled.
				Collections.sort(rects) { o1, o2 -> o2.width - o1.width }
			}
		}

		val pages = ArrayList<Page>()
		while (rects.size > 0) {
			val result = packPage(rects)
			pages.add(result)
			rects = result.remainingRects
		}
		return pages
	}

	private fun packPage(inputRects: MutableList<Rect>): Page {
		var minWidth = Integer.MAX_VALUE
		var minHeight = Integer.MAX_VALUE
		val padW = if (settings.edgePadding) settings.paddingX * 2 else 0
		val padH = if (settings.edgePadding) settings.paddingY * 2 else 0
		val maxWidth = settings.maxWidth
		val maxHeight = settings.maxHeight

		// Check that all images fit and find the minimum size.
		for (i in 0..inputRects.lastIndex) {
			val rect = inputRects[i]
			minWidth = minOf(minWidth, rect.width)
			minHeight = minOf(minHeight, rect.height)
			if (settings.rotation) {
				if ((rect.width > maxWidth - padW || rect.height > maxHeight - padH) && (rect.width > maxHeight - padH || rect.height > maxWidth - padW)) {
					throw Exception("Image does not fit with max page size " + maxWidth + "x" + maxHeight + if (settings.edgePadding) (" and padding " + settings.paddingX + "," + settings.paddingY) else "" + ": " + rect)
				}
			} else {
				if (rect.width > maxWidth - padW) {
					throw Exception("Image does not fit with max page width " + maxWidth + if (settings.edgePadding) (" and edge paddingX " + settings.paddingX) else "" + ": " + rect)
				}
				if (rect.height > maxHeight - padH) {
					throw Exception("Image does not fit in max page height " + maxHeight + if (settings.edgePadding) (" and paddingY " + settings.paddingY) else "" + ": " + rect)
				}
			}
		}
		minWidth = maxOf(minWidth, settings.minWidth)
		minHeight = maxOf(minHeight, settings.minHeight)

		if (!settings.silent) System.out.print("Packing")

		// Find the minimal page size that fits all rects.
		var bestResult: Page? = null
		if (settings.square) {
			val minSize = maxOf(minWidth, minHeight)
			val maxSize = minOf(maxWidth, maxHeight)
			val sizeSearch = BinarySearch(minSize, maxSize, if (settings.fast) 25 else 15, settings.pot)
			var size = sizeSearch.reset()
			var i = 0
			while (size != -1) {
				val result = packAtSize(true, size, size, inputRects)
				if (!settings.silent) {
					if (++i % 70 == 0) System.out.println()
					System.out.print(".")
				}
				bestResult = getBest(bestResult, result)
				size = sizeSearch.next(result == null)
			}
			if (!settings.silent) System.out.println()
			// Rects don't fit on one page. Fill a whole page and return.
			if (bestResult == null) bestResult = packAtSize(false, maxSize, maxSize, inputRects)
			Collections.sort(bestResult!!.outputRects, rectComparator)
			bestResult.width = maxOf(bestResult.width, bestResult.height)
			bestResult.height = maxOf(bestResult.width, bestResult.height)
			return bestResult
		} else {
			val widthSearch = BinarySearch(minWidth, maxWidth, if (settings.fast) 25 else 15, settings.pot)
			val heightSearch = BinarySearch(minHeight, maxHeight, if (settings.fast) 25 else 15, settings.pot)
			var width = widthSearch.reset()
			var i = 0
			var height = if (settings.square) width else heightSearch.reset()
			while (true) {
				var bestWidthResult: Page? = null
				while (width != -1) {
					val result = packAtSize(true, width, height, inputRects)
					if (!settings.silent) {
						if (++i % 70 == 0) System.out.println()
						System.out.print(".")
					}
					bestWidthResult = getBest(bestWidthResult, result)
					width = widthSearch.next(result == null)
					if (settings.square) height = width
				}
				bestResult = getBest(bestResult, bestWidthResult)
				if (settings.square) break
				height = heightSearch.next(bestWidthResult == null)
				if (height == -1) break
				width = widthSearch.reset()
			}
			if (!settings.silent) System.out.println()
			// Rects don't fit on one page. Fill a whole page and return.
			if (bestResult == null)
				bestResult = packAtSize(false, maxWidth, maxHeight, inputRects)
			Collections.sort(bestResult!!.outputRects, rectComparator)
			return bestResult
		}
	}

	/**
	 * @param fully If true, the only results that pack all rects will be considered. If false, all results are
	 * considered, not all rects may be packed.
	 */
	private fun packAtSize(fully: Boolean, width: Int, height: Int, inputRects: MutableList<Rect>): Page? {
		var bestResult: Page? = null
		var i = -1
		val n = methods.size
		while (++i < n) {
			maxRects.initPage(width, height)
			val result: Page
			if (!settings.fast) {
				result = maxRects.pack(inputRects, methods[i])
			} else {
				val remaining = ArrayList<Rect>()
				var ii = 0
				val nn = inputRects.size
				while (ii < nn) {
					val rect = inputRects[ii]
					if (maxRects.insert(rect, methods[i]) == null) {
						while (ii < nn)
							remaining.add(inputRects[ii++])
					}
					ii++
				}
				result = maxRects.getResult()
				result.remainingRects = remaining
			}
			if (fully && result.remainingRects.size > 0) continue
			if (result.outputRects.size == 0) continue
			bestResult = getBest(bestResult, result)
		}
		return bestResult
	}

	private fun getBest(result1: Page?, result2: Page?): Page? {
		if (result1 == null) return result2
		if (result2 == null) return result1
		return if (result1.occupancy > result2.occupancy) result1 else result2
	}


}

class BinarySearch(min: Int, max: Int, fuzziness: Int, var pot: Boolean) {
	var min: Int = 0
	var max: Int = 0
	var fuzziness: Int = 0
	var low: Int = 0
	var high: Int = 0
	var current: Int = 0

	init {
		this.fuzziness = if (pot) 0 else fuzziness
		this.min = if (pot) (Math.log(MathUtils.nextPowerOfTwo(min).toDouble()) / Math.log(2.0)).toInt() else min
		this.max = if (pot) (Math.log(MathUtils.nextPowerOfTwo(max).toDouble()) / Math.log(2.0)).toInt() else max
	}

	fun reset(): Int {
		low = min
		high = max
		current = (low + high).ushr(1)
		return if (pot) Math.pow(2.0, current.toDouble()).toInt() else current
	}

	fun next(result: Boolean): Int {
		if (low >= high) return -1
		if (result)
			low = current + 1
		else
			high = current - 1
		current = (low + high).ushr(1)
		if (Math.abs(low - high) < fuzziness) return -1
		return if (pot) Math.pow(2.0, current.toDouble()).toInt() else current
	}
}

/**
 * Maximal rectangles bin packing algorithm. Adapted from this C++ public domain source:
 * http://clb.demon.fi/projects/even-more-rectangle-bin-packing
 * @author Jukka Jylänki
 * @author Nathan Sweet
 */
private class MaxRects(val settings: MaxRectsSettings) {
	var binWidth: Int = 0
	var binHeight: Int = 0
	val usedRectangles = ArrayList<Rect>()
	val freeRectangles = ArrayList<Rect>()

	fun initPage(width: Int, height: Int) {
		binWidth = width
		binHeight = height

		usedRectangles.clear()
		freeRectangles.clear()

		val padX = if (settings.edgePadding) settings.paddingX else 0
		val padY = if (settings.edgePadding) settings.paddingY else 0

		if (settings.addWhitePixel) {
			val right = Rect()
			right.x = 1 + settings.paddingX
			right.y = padY
			right.width = width - padX - right.x
			right.height = height - padY * 2
			freeRectangles.add(right)

			val bottom = Rect()
			bottom.x = padX
			bottom.y = 1 + settings.paddingY
			bottom.width = width - padX * 2
			bottom.height = height - padY - bottom.y
			freeRectangles.add(bottom)
		} else {
			val n = Rect()
			n.x = padX
			n.y = padY
			n.width = width - padX * 2
			n.height = height - padY * 2
			freeRectangles.add(n)
		}
	}

	/**
	 * Packs a single image. Order is defined externally.
	 */
	fun insert(rect: Rect, method: FreeRectChoiceHeuristic): Rect? {
		val newNode = placeAndScoreRect(rect, method)
		if (newNode.height == 0) return null

		var numRectanglesToProcess = freeRectangles.size
		var i = 0
		while (i < numRectanglesToProcess) {
			if (splitFreeNode(freeRectangles[i], newNode)) {
				freeRectangles.removeAt(i)
				--i
				--numRectanglesToProcess
			}
			++i
		}

		pruneFreeList()

		val bestNode = Rect()
		bestNode.set(rect)
		bestNode.score1 = newNode.score1
		bestNode.score2 = newNode.score2
		bestNode.x = newNode.x
		bestNode.y = newNode.y
		bestNode.width = newNode.width
		bestNode.height = newNode.height
		bestNode.rotated = newNode.rotated

		usedRectangles.add(bestNode)
		return bestNode
	}

	/**
	 * For each rectangle, packs each one then chooses the best and packs that. Slow!
	 */
	fun pack(rects: MutableList<Rect>, method: FreeRectChoiceHeuristic): Page {
		val r = ArrayList(rects)
		while (r.size > 0) {
			var bestRectIndex = -1
			val bestNode = Rect()
			bestNode.score1 = Integer.MAX_VALUE
			bestNode.score2 = Integer.MAX_VALUE

			// Find the next rectangle that packs best.
			for (i in 0..r.size - 1) {
				val newNode = placeAndScoreRect(r[i], method)
				if (newNode.score1 < bestNode.score1 || (newNode.score1 == bestNode.score1 && newNode.score2 < bestNode.score2)) {
					bestNode.set(r[i])
					bestNode.score1 = newNode.score1
					bestNode.score2 = newNode.score2
					bestNode.x = newNode.x
					bestNode.y = newNode.y
					bestNode.width = newNode.width
					bestNode.height = newNode.height
					bestNode.rotated = newNode.rotated
					bestRectIndex = i
				}
			}

			if (bestRectIndex == -1) break

			placeRect(bestNode)
			r.removeAt(bestRectIndex)
		}

		val result = getResult()
		result.remainingRects = r
		return result
	}

	fun getResult(): Page {
		var w = 0
		var h = 0
		for (i in 0..usedRectangles.size - 1) {
			val rect = usedRectangles[i]
			w = maxOf(w, rect.x + rect.width)
			h = maxOf(h, rect.y + rect.height)
		}
		val result = Page()
		result.outputRects = ArrayList(usedRectangles)
		result.occupancy = getOccupancy()
		if (settings.pot) {
			result.width = MathUtils.nextPowerOfTwo(w)
			result.height = MathUtils.nextPowerOfTwo(h)
		} else {
			result.width = w
			result.height = h
		}
		return result
	}

	private fun placeRect(node: Rect) {
		var numRectanglesToProcess = freeRectangles.size
		var i = 0
		while (i < numRectanglesToProcess) {
			if (splitFreeNode(freeRectangles[i], node)) {
				freeRectangles.removeAt(i)
				--i
				--numRectanglesToProcess
			}
			i++
		}

		pruneFreeList()

		usedRectangles.add(node)
	}

	private fun placeAndScoreRect(rect: Rect, method: FreeRectChoiceHeuristic): Rect {
		val width = rect.width + settings.paddingX
		val height = rect.height + settings.paddingY
		val rotatedWidth = rect.height + settings.paddingX
		val rotatedHeight = rect.width + settings.paddingY
		val rotate = settings.rotation

		val newNode: Rect = when (method) {
			FreeRectChoiceHeuristic.BestShortSideFit -> {
				findPositionForNewNodeBestShortSideFit(width, height, rotatedWidth, rotatedHeight, rotate)
			}
			FreeRectChoiceHeuristic.BottomLeftRule -> {
				findPositionForNewNodeBottomLeft(width, height, rotatedWidth, rotatedHeight, rotate)
			}
			FreeRectChoiceHeuristic.ContactPointRule -> {
				val newNode = findPositionForNewNodeContactPoint(width, height, rotatedWidth, rotatedHeight, rotate)
				newNode.score1 = -newNode.score1 // Reverse since we are minimizing, but for contact point score bigger is better.
				newNode
			}
			FreeRectChoiceHeuristic.BestLongSideFit -> {
				findPositionForNewNodeBestLongSideFit(width, height, rotatedWidth, rotatedHeight, rotate)
			}
			FreeRectChoiceHeuristic.BestAreaFit -> {
				findPositionForNewNodeBestAreaFit(width, height, rotatedWidth, rotatedHeight, rotate)
			}
		}

		// Cannot fit the current rectangle.
		if (newNode.height == 0) {
			newNode.score1 = Integer.MAX_VALUE
			newNode.score2 = Integer.MAX_VALUE
		}

		return newNode
	}

	/**
	 * Computes the ratio of used surface area.
	 */
	private fun getOccupancy(): Float {
		var usedSurfaceArea = 0
		for (i in 0..usedRectangles.size - 1)
			usedSurfaceArea += usedRectangles[i].width * usedRectangles[i].height
		return usedSurfaceArea.toFloat() / (binWidth * binHeight).toFloat()
	}

	private fun findPositionForNewNodeBottomLeft(width: Int, height: Int, rotatedWidth: Int, rotatedHeight: Int, rotate: Boolean): Rect {
		val bestNode = Rect()

		bestNode.score1 = Integer.MAX_VALUE // best y, score2 is best x

		for (i in 0..freeRectangles.size - 1) {
			// Try to place the rectangle in upright (non-rotated) orientation.
			if (freeRectangles[i].width >= width && freeRectangles[i].height >= height) {
				val topSideY = freeRectangles[i].y + height
				if (topSideY < bestNode.score1 || (topSideY == bestNode.score1 && freeRectangles[i].x < bestNode.score2)) {
					bestNode.x = freeRectangles[i].x
					bestNode.y = freeRectangles[i].y
					bestNode.width = width
					bestNode.height = height
					bestNode.score1 = topSideY
					bestNode.score2 = freeRectangles[i].x
					bestNode.rotated = false
				}
			}
			if (rotate && freeRectangles[i].width >= rotatedWidth && freeRectangles[i].height >= rotatedHeight) {
				val topSideY = freeRectangles[i].y + rotatedHeight
				if (topSideY < bestNode.score1 || (topSideY == bestNode.score1 && freeRectangles[i].x < bestNode.score2)) {
					bestNode.x = freeRectangles[i].x
					bestNode.y = freeRectangles[i].y
					bestNode.width = rotatedWidth
					bestNode.height = rotatedHeight
					bestNode.score1 = topSideY
					bestNode.score2 = freeRectangles[i].x
					bestNode.rotated = true
				}
			}
		}
		return bestNode
	}

	private fun findPositionForNewNodeBestShortSideFit(width: Int, height: Int, rotatedWidth: Int, rotatedHeight: Int, rotate: Boolean): Rect {
		val bestNode = Rect()
		bestNode.score1 = Integer.MAX_VALUE

		for (i in 0..freeRectangles.size - 1) {
			// Try to place the rectangle in upright (non-rotated) orientation.
			if (freeRectangles[i].width >= width && freeRectangles[i].height >= height) {
				val leftoverHoriz = Math.abs(freeRectangles[i].width - width)
				val leftoverVert = Math.abs(freeRectangles[i].height - height)
				val shortSideFit = minOf(leftoverHoriz, leftoverVert)
				val longSideFit = maxOf(leftoverHoriz, leftoverVert)

				if (shortSideFit < bestNode.score1 || (shortSideFit == bestNode.score1 && longSideFit < bestNode.score2)) {
					bestNode.x = freeRectangles[i].x
					bestNode.y = freeRectangles[i].y
					bestNode.width = width
					bestNode.height = height
					bestNode.score1 = shortSideFit
					bestNode.score2 = longSideFit
					bestNode.rotated = false
				}
			}

			if (rotate && freeRectangles[i].width >= rotatedWidth && freeRectangles[i].height >= rotatedHeight) {
				val flippedLeftoverHoriz = Math.abs(freeRectangles[i].width - rotatedWidth)
				val flippedLeftoverVert = Math.abs(freeRectangles[i].height - rotatedHeight)
				val flippedShortSideFit = minOf(flippedLeftoverHoriz, flippedLeftoverVert)
				val flippedLongSideFit = maxOf(flippedLeftoverHoriz, flippedLeftoverVert)

				if (flippedShortSideFit < bestNode.score1 || (flippedShortSideFit == bestNode.score1 && flippedLongSideFit < bestNode.score2)) {
					bestNode.x = freeRectangles[i].x
					bestNode.y = freeRectangles[i].y
					bestNode.width = rotatedWidth
					bestNode.height = rotatedHeight
					bestNode.score1 = flippedShortSideFit
					bestNode.score2 = flippedLongSideFit
					bestNode.rotated = true
				}
			}
		}

		return bestNode
	}

	private fun findPositionForNewNodeBestLongSideFit(width: Int, height: Int, rotatedWidth: Int, rotatedHeight: Int, rotate: Boolean): Rect {
		val bestNode = Rect()

		bestNode.score2 = Integer.MAX_VALUE

		for (i in 0..freeRectangles.size - 1) {
			// Try to place the rectangle in upright (non-rotated) orientation.
			if (freeRectangles[i].width >= width && freeRectangles[i].height >= height) {
				val leftoverHoriz = Math.abs(freeRectangles[i].width - width)
				val leftoverVert = Math.abs(freeRectangles[i].height - height)
				val shortSideFit = minOf(leftoverHoriz, leftoverVert)
				val longSideFit = maxOf(leftoverHoriz, leftoverVert)

				if (longSideFit < bestNode.score2 || (longSideFit == bestNode.score2 && shortSideFit < bestNode.score1)) {
					bestNode.x = freeRectangles[i].x
					bestNode.y = freeRectangles[i].y
					bestNode.width = width
					bestNode.height = height
					bestNode.score1 = shortSideFit
					bestNode.score2 = longSideFit
					bestNode.rotated = false
				}
			}

			if (rotate && freeRectangles[i].width >= rotatedWidth && freeRectangles[i].height >= rotatedHeight) {
				val leftoverHoriz = Math.abs(freeRectangles[i].width - rotatedWidth)
				val leftoverVert = Math.abs(freeRectangles[i].height - rotatedHeight)
				val shortSideFit = minOf(leftoverHoriz, leftoverVert)
				val longSideFit = maxOf(leftoverHoriz, leftoverVert)

				if (longSideFit < bestNode.score2 || (longSideFit == bestNode.score2 && shortSideFit < bestNode.score1)) {
					bestNode.x = freeRectangles[i].x
					bestNode.y = freeRectangles[i].y
					bestNode.width = rotatedWidth
					bestNode.height = rotatedHeight
					bestNode.score1 = shortSideFit
					bestNode.score2 = longSideFit
					bestNode.rotated = true
				}
			}
		}
		return bestNode
	}

	private fun findPositionForNewNodeBestAreaFit(width: Int, height: Int, rotatedWidth: Int, rotatedHeight: Int, rotate: Boolean): Rect {
		val bestNode = Rect()

		bestNode.score1 = Integer.MAX_VALUE // best area fit, score2 is best short side fit

		for (i in 0..freeRectangles.size - 1) {
			val areaFit = freeRectangles[i].width * freeRectangles[i].height - width * height

			// Try to place the rectangle in upright (non-rotated) orientation.
			if (freeRectangles[i].width >= width && freeRectangles[i].height >= height) {
				val leftoverHoriz = Math.abs(freeRectangles[i].width - width)
				val leftoverVert = Math.abs(freeRectangles[i].height - height)
				val shortSideFit = minOf(leftoverHoriz, leftoverVert)

				if (areaFit < bestNode.score1 || (areaFit == bestNode.score1 && shortSideFit < bestNode.score2)) {
					bestNode.x = freeRectangles[i].x
					bestNode.y = freeRectangles[i].y
					bestNode.width = width
					bestNode.height = height
					bestNode.score2 = shortSideFit
					bestNode.score1 = areaFit
					bestNode.rotated = false
				}
			}

			if (rotate && freeRectangles[i].width >= rotatedWidth && freeRectangles[i].height >= rotatedHeight) {
				val leftoverHoriz = Math.abs(freeRectangles[i].width - rotatedWidth)
				val leftoverVert = Math.abs(freeRectangles[i].height - rotatedHeight)
				val shortSideFit = minOf(leftoverHoriz, leftoverVert)

				if (areaFit < bestNode.score1 || (areaFit == bestNode.score1 && shortSideFit < bestNode.score2)) {
					bestNode.x = freeRectangles[i].x
					bestNode.y = freeRectangles[i].y
					bestNode.width = rotatedWidth
					bestNode.height = rotatedHeight
					bestNode.score2 = shortSideFit
					bestNode.score1 = areaFit
					bestNode.rotated = true
				}
			}
		}
		return bestNode
	}

	// / Returns 0 if the two intervals i1 and i2 are disjoint, or the length of their overlap otherwise.
	private fun commonIntervalLength(i1start: Int, i1end: Int, i2start: Int, i2end: Int): Int {
		if (i1end < i2start || i2end < i1start) return 0
		return minOf(i1end, i2end) - maxOf(i1start, i2start)
	}

	private fun contactPointScoreNode(x: Int, y: Int, width: Int, height: Int): Int {
		var score = 0

		if (x == 0 || x + width == binWidth) score += height
		if (y == 0 || y + height == binHeight) score += width

		for (i in 0..usedRectangles.size - 1) {
			if (usedRectangles[i].x == x + width || usedRectangles[i].x + usedRectangles[i].width == x)
				score += commonIntervalLength(usedRectangles[i].y, usedRectangles[i].y + usedRectangles[i].height, y, y + height)
			if (usedRectangles[i].y == y + height || usedRectangles[i].y + usedRectangles[i].height == y)
				score += commonIntervalLength(usedRectangles[i].x, usedRectangles[i].x + usedRectangles[i].width, x, x + width)
		}
		return score
	}

	private fun findPositionForNewNodeContactPoint(width: Int, height: Int, rotatedWidth: Int, rotatedHeight: Int, rotate: Boolean): Rect {
		val bestNode = Rect()

		bestNode.score1 = -1 // best contact score

		for (i in 0..freeRectangles.size - 1) {
			// Try to place the rectangle in upright (non-rotated) orientation.
			if (freeRectangles[i].width >= width && freeRectangles[i].height >= height) {
				val score = contactPointScoreNode(freeRectangles[i].x, freeRectangles[i].y, width, height)
				if (score > bestNode.score1) {
					bestNode.x = freeRectangles[i].x
					bestNode.y = freeRectangles[i].y
					bestNode.width = width
					bestNode.height = height
					bestNode.score1 = score
					bestNode.rotated = false
				}
			}
			if (rotate && freeRectangles[i].width >= rotatedWidth && freeRectangles[i].height >= rotatedHeight) {
				// This was width,height -- bug fixed?
				val score = contactPointScoreNode(freeRectangles[i].x, freeRectangles[i].y, rotatedWidth, rotatedHeight)
				if (score > bestNode.score1) {
					bestNode.x = freeRectangles[i].x
					bestNode.y = freeRectangles[i].y
					bestNode.width = rotatedWidth
					bestNode.height = rotatedHeight
					bestNode.score1 = score
					bestNode.rotated = true
				}
			}
		}
		return bestNode
	}

	private fun splitFreeNode(freeNode: Rect, usedNode: Rect): Boolean {
		// Test with SAT if the rectangles even intersect.
		if (usedNode.x >= freeNode.x + freeNode.width || usedNode.x + usedNode.width <= freeNode.x || usedNode.y >= freeNode.y + freeNode.height || usedNode.y + usedNode.height <= freeNode.y)
			return false

		if (usedNode.x < freeNode.x + freeNode.width && usedNode.x + usedNode.width > freeNode.x) {
			// New node at the top side of the used node.
			if (usedNode.y > freeNode.y && usedNode.y < freeNode.y + freeNode.height) {
				val newNode = Rect(freeNode)
				newNode.height = usedNode.y - newNode.y
				freeRectangles.add(newNode)
			}

			// New node at the bottom side of the used node.
			if (usedNode.y + usedNode.height < freeNode.y + freeNode.height) {
				val newNode = Rect(freeNode)
				newNode.y = usedNode.y + usedNode.height
				newNode.height = freeNode.y + freeNode.height - (usedNode.y + usedNode.height)
				freeRectangles.add(newNode)
			}
		}

		if (usedNode.y < freeNode.y + freeNode.height && usedNode.y + usedNode.height > freeNode.y) {
			// New node at the left side of the used node.
			if (usedNode.x > freeNode.x && usedNode.x < freeNode.x + freeNode.width) {
				val newNode = Rect(freeNode)
				newNode.width = usedNode.x - newNode.x
				freeRectangles.add(newNode)
			}

			// New node at the right side of the used node.
			if (usedNode.x + usedNode.width < freeNode.x + freeNode.width) {
				val newNode = Rect(freeNode)
				newNode.x = usedNode.x + usedNode.width
				newNode.width = freeNode.x + freeNode.width - (usedNode.x + usedNode.width)
				freeRectangles.add(newNode)
			}
		}

		return true
	}

	private fun pruneFreeList() {
		/*
					 * /// Would be nice to do something like this, to avoid a Theta(n^2) loop through each pair. /// But unfortunately it
					 * doesn't quite cut it, since we also want to detect containment. /// Perhaps there's another way to do this faster than
					 * Theta(n^2).
					 *
					 * if (freeRectangles.size() > 0) clb::sort::QuickSort(&freeRectangles[0], freeRectangles.size(), NodeSortCmp);
					 *
					 * for(int i = 0; i < freeRectangles.size()-1; i++) if (freeRectangles[i].x == freeRectangles[i+1].x && freeRectangles[i].y
					 * == freeRectangles[i+1].y && freeRectangles[i].width == freeRectangles[i+1].width && freeRectangles[i].height ==
					 * freeRectangles[i+1].height) { freeRectangles.erase(freeRectangles.begin() + i); --i; }
					 */

		// / Go through each pair and remove any rectangle that is redundant.
		var i = 0
		while (i < freeRectangles.size) {
			var j = i + 1
			while (j < freeRectangles.size) {
				if (isContainedIn(freeRectangles[i], freeRectangles[j])) {
					freeRectangles.removeAt(i)
					--i
					break
				}
				if (isContainedIn(freeRectangles[j], freeRectangles[i])) {
					freeRectangles.removeAt(j)
					--j
				}
				++j
			}
			i++
		}
	}

	private fun isContainedIn(a: Rect, b: Rect): Boolean {
		return a.x >= b.x && a.y >= b.y && a.x + a.width <= b.x + b.width && a.y + a.height <= b.y + b.height
	}
}

enum class FreeRectChoiceHeuristic {

	/**
	 * BSSF: Positions the rectangle against the short side of a free rectangle into which it fits the best.
	 */
	BestShortSideFit,
	/**
	 * BLSF: Positions the rectangle against the long side of a free rectangle into which it fits the best.
	 */
	BestLongSideFit,

	/**
	 * BAF: Positions the rectangle into the smallest free rect into which it fits.
	 */
	BestAreaFit,

	/**
	 * BL: Does the Tetris placement.
	 */
	BottomLeftRule,

	/**
	 * CP: Chooses the placement where the rectangle touches other rects as much as possible.
	 */
	ContactPointRule
}

private class RectComparator : Comparator<Rect> {
	override fun compare(o1: Rect, o2: Rect): Int {
		return o1.name.compareTo(o2.name)
	}
}

/**
 * @author Nathan Sweet
 */
private data class Page(
		var outputRects: MutableList<Rect> = ArrayList(),
		var remainingRects: MutableList<Rect> = ArrayList(),
		var occupancy: Float = 0f,
		var width: Int = 0,
		var height: Int = 0
)

private class MaxRectsSettings {
	var addWhitePixel: Boolean = false
	var fast: Boolean = false
	var maxHeight: Int = 1024
	var maxWidth: Int = 1024
	var minHeight: Int = 16
	var minWidth: Int = 16
	var paddingX: Int = 2
	var paddingY: Int = 2
	var edgePadding: Boolean = true
	var pot: Boolean = true
	var rotation: Boolean = false
	var silent: Boolean = false
	var square: Boolean = false
}

/**
 * @author Nathan Sweet
 */
private class Rect() {
	var name: String = ""
	var x: Int = 0
	var y: Int = 0
	var width: Int = 0
	var height: Int = 0 // Portion of page taken by this region, including padding.
	var rotated: Boolean = false
	var originalIndex: Int = 0

	var score1: Int = 0
	var score2: Int = 0

	constructor(other: Rect) : this() {
		x = other.x
		y = other.y
		width = other.width
		height = other.height
	}

	fun set(rect: Rect) {
		name = rect.name
		x = rect.x
		y = rect.y
		width = rect.width
		height = rect.height
		rotated = rect.rotated
		originalIndex = rect.originalIndex
		score1 = rect.score1
		score2 = rect.score2
	}

	override fun toString(): String {
		return name + "[" + x + "," + y + " " + width + "x" + height + "]"
	}
}

