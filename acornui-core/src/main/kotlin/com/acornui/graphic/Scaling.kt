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

package com.acornui.graphic

import com.acornui.math.*

/**
 * Describes the different scaling strategies.
 * @author nbilyk
 */
enum class Scaling {

	/**
	 * Scales the source to fit the target while keeping the same aspect ratio. This may cause the source to be smaller
	 * than the target in one direction.
	 */
	FIT,

	/**
	 * Scales the source to fill the target while keeping the same aspect ratio. This may cause the source to be larger
	 * than the target in one direction.
	 */
	FILL,

	/**
	 * Scales the source to fill the target in the x direction while keeping the same aspect ratio. This may cause the
	 * source to be smaller or larger than the target in the y direction.
	 */
	FILL_X,

	/**
	 * Scales the source to fill the target in the y direction while keeping the same aspect ratio. This may cause the
	 * source to be smaller or larger than the target in the x direction.
	 */
	FILL_Y,

	/**
	 * Scales the source to fill the target. This may cause the source to not keep the same aspect ratio.
	 */
	STRETCH,

	/**
	 * Scales the source to fill the target in the x direction, without changing the y direction. This may cause the
	 * source to not keep the same aspect ratio.
	 */
	STRETCH_X,

	/**
	 * Scales the source to fill the target in the y direction, without changing the x direction. This may cause the
	 * source to not keep the same aspect ratio.
	 */
	STRETCH_Y,

	/**
	 * The source is not scaled.
	 */
	NONE;

	/**
	 * Applies the scaling strategy to the provided source and target dimensions.
	 */
	fun apply(
		sourceWidth: Double,
		sourceHeight: Double,
		targetWidth: Double,
		targetHeight: Double
	): Vector2 {
		return when (this) {
			FIT -> {
				val targetRatio = targetHeight / targetWidth
				val sourceRatio = sourceHeight / sourceWidth
				val scale = if (targetRatio > sourceRatio) targetWidth / sourceWidth else targetHeight / sourceHeight
				vec2(sourceWidth * scale, sourceHeight * scale)
			}
			FILL -> {
				val targetRatio = targetHeight / targetWidth
				val sourceRatio = sourceHeight / sourceWidth
				val scale = if (targetRatio < sourceRatio) targetWidth / sourceWidth else targetHeight / sourceHeight
				vec2(sourceWidth * scale, sourceHeight * scale)
			}
			FILL_X -> {
				val scale = targetWidth / sourceWidth
				vec2(sourceWidth * scale, sourceHeight * scale)
			}
			FILL_Y -> {
				val scale = targetHeight / sourceHeight
				vec2(sourceWidth * scale, sourceHeight * scale)
			}
			STRETCH -> {
				vec2(targetWidth, targetHeight)
			}
			STRETCH_X -> {
				vec2(targetWidth, sourceHeight)
			}
			STRETCH_Y -> {
				vec2(sourceWidth, targetHeight)
			}
			NONE -> {
				vec2(sourceWidth, sourceHeight)
			}
		}
	}
}

/**
 * Applies the scaling strategy to the provided source and target dimensions.
 */
fun Scaling.apply(
	source: Vector2,
	target: Vector2
): Vector2 = apply(source.x, source.y, target.x, target.y)