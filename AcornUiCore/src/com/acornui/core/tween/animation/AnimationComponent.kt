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

package com.acornui.core.tween.animation

import com.acornui.collection.ArrayList
import com.acornui.collection.sortedInsertionIndex
import com.acornui.component.*
import com.acornui.core.di.Owned
import com.acornui.core.graphics.atlas
import com.acornui.core.tween.Tween
import com.acornui.core.tween.TweenBase
import com.acornui.core.tween.driveTween
import com.acornui.core.tween.timelineTween
import com.acornui.math.*

/**
 * The visual component for an animation tween.
 */
class AnimationComponent(owner: Owned, bundle: AnimationBundle, animation: AnimationLibraryItem) : ContainerImpl(owner) {

	val tween: Tween

	init {
		tween = timelineTween()
		tween.loopAfter = true
		for (i in animation.timeline.layers.lastIndex downTo 0) {
			val layer = animation.timeline.layers[i]
			if (!layer.visible) continue
			val component = addChild(createComponentFromLibrary(bundle, layer.symbolName))
			tween.add(LayerTween(animation.timeline.duration, layer, component, bundle.easings))
		}
		driveTween(tween)
	}

}

private class LayerTween(override val duration: Float, layer: Layer, private val target: UiComponent, globalEasings: Map<String, AnimationEasing>) : TweenBase() {

	private val frames = ArrayList<KeyFrame>()

	override val durationInv: Float = 1f / duration

	private val transform = Matrix4()
	private val origin = Vector3()
	private val position = Vector3()
	private val scale = Vector3()
	private val rotation = Vector3()

	private var shearXZ = 0f
	private var shearYZ = 0f

	init {
		target.customTransform = transform
		for (keyFrame in layer.keyFrames) {
			val props = ArrayList(PROPS.size) {
				val dataProp = keyFrame.props[PROPS[it]]
				if (dataProp == null) null
				else Prop(dataProp.value, if (dataProp.easing == null) Easing.stepped else Easing.linear) // TODO: Calculate bezier
			}
			frames.add(KeyFrame(keyFrame.time, props))
		}
	}

	override fun updateToTime(lastTime: Float, newTime: Float, apparentLastTime: Float, apparentNewTime: Float, jump: Boolean) {
		val frameIndex: Int = frames.sortedInsertionIndex(apparentNewTime, {
			time, frame ->
			time.compareTo(frame.time)
		}, matchForwards = true) - 1
		val frame = frames[maxOf(0, frameIndex)]
		val nextFrame = frames.getOrNull(frameIndex + 1) ?: frame
		for (i in 0..frame.props.lastIndex) {
			val prop = frame.props[i] ?: continue
			val endProp = nextFrame.props[i] ?: prop
			if (apparentNewTime <= frame.time) {
				setValue(PROPS[i], prop.value)
			} else if (apparentNewTime >= nextFrame.time) {
				setValue(PROPS[i], endProp.value)
			} else {
				val alpha = MathUtils.clamp((apparentNewTime - frame.time) / (nextFrame.time - frame.time), 0f, 1f)
				val eased = prop.easing.apply(alpha)
				setValue(PROPS[i], eased * (endProp.value - prop.value) + prop.value)
			}
		}

//		val values = transform.values
//		values[0] = scale.x
//		values[1] = shearYZ
//		values[2] = 0f
//		values[3] = 0f
//
//		values[4] = shearXZ
//		values[5] = scale.y
//		values[6] = 0f
//		values[7] = 0f
//
//		values[8] = 0f
//		values[9] = 0f
//		values[10] = scale.z
//		values[11] = 0f
//
//		values[12] = position.x
//		values[13] = position.y
//		values[14] = position.z
//		values[15] = 1f



		transform.idt()

		transform.trn(position)
		transform.scl(scale)

		if (!rotation.isZero()) {
			quat.setEulerAnglesRad(rotation.y, rotation.x, rotation.z)
			transform.rotate(quat)
		}
		transform.shear(shearXZ, shearYZ, 0f, 0f, 0f, 0f)

		transform.translate(-origin.x, -origin.y, -origin.z)
		target.invalidate(ValidationFlags.TRANSFORM)

	}

	private val setFunctions = hashMapOf<PropType, (value: Float) -> Unit>(
			PropType.VISIBLE to { v -> target.visible = v == 1f },

			PropType.X to { v -> position.x = v },
			PropType.Y to { v -> position.y = v },
			PropType.Z to { v -> position.y = v },

			PropType.ORIGIN_X to { v -> origin.x = v },
			PropType.ORIGIN_Y to { v -> origin.y = v },
			PropType.ORIGIN_Z to { v -> origin.z = v },

			PropType.SCALE_X to { v -> scale.x = v },
			PropType.SCALE_Y to { v -> scale.y = v },
			PropType.SCALE_Z to { v -> scale.z = v },

			PropType.ROTATION_X to { v -> rotation.x = v },
			PropType.ROTATION_Y to { v -> rotation.y = v },
			PropType.ROTATION_Z to { v -> rotation.z = v },

			PropType.SHEAR_XZ to { v -> shearXZ = v },
			PropType.SHEAR_YZ to { v -> shearYZ = v }

//			SHEAR_XY,
//			SHEAR_ZY,
//
//			SHEAR_ZX,
//			SHEAR_YX,
//

//			COLOR_R,
//			COLOR_G,
//			COLOR_B,
//			COLOR_A
	)

	private fun setValue(propType: PropType, value: Float) {
		setFunctions[propType]?.invoke(value)
	}

	private data class KeyFrame(
			val time: Float,

			/**
			 * A list of properties to animate, indexed by the [PropType] ordinal.
			 */
			val props: List<Prop?>
	)

	private data class Prop(
			val value: Float,
			val easing: Interpolation
	)

	companion object {
		private val PROPS = PropType.values()
		private val quat = Quaternion()
	}
}

fun Owned.animationComponent(bundle: AnimationBundle, libraryItemName: String, init: ComponentInit<AnimationComponent> = {}): AnimationComponent {
	val c = createComponentFromLibrary(bundle, libraryItemName) as? AnimationComponent ?: throw Exception("Library item is not an animation.")
	c.init()
	return c
}

fun Owned.createComponentFromLibrary(bundle: AnimationBundle, libraryItemName: String, init: ComponentInit<UiComponent> = {}): UiComponent {
	val libraryItem = bundle.library[libraryItemName] ?: throw Exception("library item not found with name: $libraryItemName")
	val component = when (libraryItem.itemType) {
		LibraryItemType.IMAGE -> image((libraryItem as ImageLibraryItem).path)
		LibraryItemType.ATLAS -> {
			libraryItem as AtlasLibraryItem
			atlas(libraryItem.atlasPath, libraryItem.regionName)
		}
		LibraryItemType.ANIMATION -> AnimationComponent(this, bundle, libraryItem as AnimationLibraryItem)
	}
	component.init()
	return component
}