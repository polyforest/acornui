/*
 * Derived from LibGDX by Nicholas Bilyk
 * https://github.com/libgdx
 * Copyright 2011 See https://github.com/libgdx/libgdx/blob/master/AUTHORS
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

package com.acornui.core.graphic

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
	 * The result is stored in the out parameter, which is also the return value.
	 */
	fun apply(sourceWidth: Float, sourceHeight: Float, targetWidth: Float, targetHeight: Float, out: Vector2): Vector2 {
		when (this) {
			FIT -> {
				val targetRatio = targetHeight / targetWidth
				val sourceRatio = sourceHeight / sourceWidth
				val scale = if (targetRatio > sourceRatio) targetWidth / sourceWidth else targetHeight / sourceHeight
				out.x = sourceWidth * scale
				out.y = sourceHeight * scale
			}
			FILL -> {
				val targetRatio = targetHeight / targetWidth
				val sourceRatio = sourceHeight / sourceWidth
				val scale = if (targetRatio < sourceRatio) targetWidth / sourceWidth else targetHeight / sourceHeight
				out.x = sourceWidth * scale
				out.y = sourceHeight * scale
			}
			FILL_X -> {
				val scale = targetWidth / sourceWidth
				out.x = sourceWidth * scale
				out.y = sourceHeight * scale
			}
			FILL_Y -> {
				val scale = targetHeight / sourceHeight
				out.x = sourceWidth * scale
				out.y = sourceHeight * scale
			}
			STRETCH -> {
				out.x = targetWidth
				out.y = targetHeight
			}
			STRETCH_X -> {
				out.x = targetWidth
				out.y = sourceHeight
			}
			STRETCH_Y -> {
				out.x = sourceWidth
				out.y = targetHeight
			}
			NONE -> {
				out.x = sourceWidth
				out.y = sourceHeight
			}
		}
		return out
	}
}