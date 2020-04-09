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

package com.acornui.tween.animation

import com.acornui.collection.sortedInsertionIndex
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * A data class representing an animation.
 */
@Serializable
data class AnimationBundle(

		val library: Map<String, LibraryItem>,

		/**
		 * Global easings.
		 * A map of easingId -> [FloatArray] object.
		 */
		val easings: Map<String, FloatArray>
)


@Serializable
sealed class LibraryItem {

	@Serializable
	@SerialName("atlas")
	data class AtlasLibraryItem(val atlasPath: String, val regionName: String) : LibraryItem()

	@Serializable
	@SerialName("image")
	data class ImageLibraryItem(val path: String) : LibraryItem()

	@Serializable
	@SerialName("animation")
	data class AnimationLibraryItem(val timeline: Timeline) : LibraryItem()

	@Serializable
	@SerialName("custom")
	object CustomLibraryItem : LibraryItem()
}

@Serializable
data class Timeline(
		val duration: Float,
		val layers: List<Layer>,

		/**
		 * A map of label name to times.
		 */
		val labels: Map<String, Float>
) {

	/**
	 * The times of the labels, in ascending order.
	 */
	val labelTimes = labels.values.sorted()
}

@Serializable
data class Layer(
		val name: String,
		val symbolName: String,
		val visible: Boolean,
		val keyFrames: List<KeyFrame>
) {

	fun getKeyFrameIndexAtTime(time: Float): Int {
		return keyFrames.sortedInsertionIndex(time) {
			t, frame ->
			t.compareTo(frame.time)
		}
	}
}

@Serializable
data class KeyFrame(
		val time: Float,

		/**
		 * Easings local to the frame.
		 * A map of easingId -> [FloatArray] object.
		 * If the easing is not found, global easings will be checked.
		 */
		val easings: Map<String, FloatArray>,
		val props: Map<PropType, Prop>
)

@Serializable
data class Prop(

		/**
		 * The value this property should become at this key frame's time.
		 */
		val value: Float,

		/**
		 * The name of the [FloatArray] object.
		 * This will first be looked for on the keyframe, and then on the animation bundle.
		 * Null if this property is not interpolated.
		 */
		val easing: String? = null
)

enum class PropType {
	VISIBLE,

	X,
	Y,
	Z,

	ORIGIN_X,
	ORIGIN_Y,
	ORIGIN_Z,

	SCALE_X,
	SCALE_Y,
	SCALE_Z,

	ROTATION_X,
	ROTATION_Y,
	ROTATION_Z,

	SHEAR_XZ,
	SHEAR_YZ,

	SHEAR_XY,
	SHEAR_ZY,

	SHEAR_ZX,
	SHEAR_YX,

	COLOR_R,
	COLOR_G,
	COLOR_B,
	COLOR_A
}
