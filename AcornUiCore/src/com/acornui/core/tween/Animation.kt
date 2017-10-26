/*
 * Copyright 2017 Nicholas Bilyk
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

package com.acornui.core.tween

/**
 * A data class representing an animation.
 */
data class AnimationBundle(
		val library: Any,
		/**
		 * Global easings.
		 */
		val easings: Map<String, AnimationEasing>,
		val animations: List<Animation>
)

data class Animation(
		val name: String,
		val timeline: Timeline)

data class Timeline(val layers: List<Layer>)

data class Layer(
		val name: String,
		val symbolName: String,
		val visible: Boolean,
		val keyFrames: List<KeyFrame>
)

data class KeyFrame(
		val time: Float,
		val easings: Map<String, AnimationEasing>,
		val props: Map<PropType, Prop>
)

data class Prop(

		/**
		 * The value this property should become at this key frame's time.
		 */
		val value: Float,

		/**
		 * The name of the [AnimationEasing] object.
		 * This will first be looked for on the keyframe, and then on the animation bundle.
		 * Null if this property is not interpolated.
		 */
		val easing: String?
)

data class AnimationEasing(

		/**
		 * x, y, ...
		 */
		val curve: List<Float>
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

	SKEW_XZ,
	SKEW_YZ,

	SKEW_ZX,
	SKEW_YX,

	SKEW_XY,
	SKEW_ZY,

	COLOR_R,
	COLOR_G,
	COLOR_B,
	COLOR_A
}