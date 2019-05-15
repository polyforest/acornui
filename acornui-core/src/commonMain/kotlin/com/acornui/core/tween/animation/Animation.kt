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

package com.acornui.core.tween.animation

import com.acornui.collection.ArrayList
import com.acornui.collection.sortedInsertionIndex
import com.acornui.component.*
import com.acornui.core.Disposable
import com.acornui.core.di.Owned
import com.acornui.component.AtlasComponent
import com.acornui.component.atlas
import com.acornui.core.time.onTick
import com.acornui.core.tween.*
import com.acornui.graphic.Color
import com.acornui.logging.Log
import com.acornui.math.*
import kotlin.math.tan

/**
 * A UI Component representing an animation library item.
 */
interface SymbolInstance : UiComponent {
	val libraryItem: LibraryItem
}

class ImageInstance(
		private val texture: TextureComponent,
		override val libraryItem: ImageLibraryItem
) : SymbolInstance, UiComponent by texture

class CustomInstance(
		private val component: UiComponent
) : SymbolInstance, UiComponent by component {

	override val libraryItem = object : LibraryItem {
		override val itemType = LibraryItemType.CUSTOM
	}
}

class AtlasInstance(
		private val atlas: AtlasComponent,
		override val libraryItem: AtlasLibraryItem
) : SymbolInstance, UiComponent by atlas

/**
 * The visual component for an animation tween.
 */
class AnimationInstance(
		owner: Owned,
		val bundle: AnimationBundle,
		override val libraryItem: AnimationLibraryItem
) : ElementContainerImpl<SymbolInstance>(owner), SymbolInstance {

	val tween: TimelineTween
	private var driver: Disposable? = null

	init {
		tween = timelineTween()
		tween.loopAfter = true
		for (i in libraryItem.timeline.layers.lastIndex downTo 0) {
			val layer = libraryItem.timeline.layers[i]
			if (!layer.visible) continue
			val component = +createComponentFromLibrary(bundle, layer.symbolName)
			component.visible = false
			tween.add(LayerTween(libraryItem.timeline.duration, layer, component, bundle.easings))
		}
		start()
	}

	/**
	 * Starts driving the tween. (This is automatically done after initialization).
	 * @see Tween.paused
	 */
	fun start() {
		if (driver != null) return
		driver = onTick {
			tween.update(it)
		}
	}

	/**
	 * Stops driving the tween. This would be called if a custom tween updater is used instead. For pause/play
	 * functionality, use [Tween.paused]
	 * @see Tween.paused
	 */
	fun stop() {
		driver?.dispose()
		driver = null
	}
}

private class LayerTween(override val duration: Float, layer: Layer, private val target: UiComponent, globalEasings: Map<String, AnimationEasing>) : TweenBase() {

	private val frames = ArrayList<KeyFrame>()

	override val durationInv: Float = 1f / duration

	private val transform = Matrix4()
	private val origin = Vector3()
	private val position = Vector3()
	private val scale = Vector3(1f, 1f, 1f)
	private val rotation = Vector3()
	private val color = Color.WHITE.copy()

	private var shearXZ = 0f
	private var shearYZ = 0f

	init {
		target.visible = false
		target.customTransform = transform
		for (keyFrame in layer.keyFrames) {
			val props = ArrayList(PROPS.size) {
				val dataProp = keyFrame.props[PROPS[it]]
				if (dataProp == null) null
				else {
					val easing = if (dataProp.easing == null)
						Easing.stepped
					else {
						val animEasing = keyFrame.easings[dataProp.easing] ?: globalEasings[dataProp.easing]
						if (animEasing == null) {
							Log.warn("Easing not found: ${dataProp.easing}")
							Easing.stepped
						} else {
							if (animEasing.points.isEmpty()) {
								EasingCache(Easing.linear)
							} else {
								EasingCache(Bezier(animEasing.points))
							}
						}
					}
					Prop(dataProp.value, easing)
				}
			}
			frames.add(KeyFrame(keyFrame.time, props))
		}
	}

	override fun updateToTime(lastTime: Float, newTime: Float, apparentLastTime: Float, apparentNewTime: Float, jump: Boolean) {
		val frameIndex: Int = frames.sortedInsertionIndex(apparentNewTime, matchForwards = true) {
			time, frame ->
			time.compareTo(frame.time)
		} - 1
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
		transform.idt()
		transform.trn(position)
		if (!rotation.isZero()) {
			quat.setEulerAngles(rotation.x, rotation.y, rotation.z)
			transform.rotate(quat)
		}
		transform.shearZ(tan(shearXZ), tan(shearYZ))
		transform.scale(scale)

		transform.translate(-origin.x, -origin.y, -origin.z)
		target.invalidate(ValidationFlags.TRANSFORM)

		target.colorTint = color

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
			PropType.SHEAR_YZ to { v -> shearYZ = v },

			//			SHEAR_XY,
//			SHEAR_ZY,
//
//			SHEAR_ZX,
//			SHEAR_YX,
//

			PropType.COLOR_R to { v -> color.r = v },
			PropType.COLOR_G to { v -> color.g = v },
			PropType.COLOR_B to { v -> color.b = v },
			PropType.COLOR_A to { v -> color.a = v }
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

	/**
	 * Wraps an interpolation object and caches the result so subsequent eases with the same alpha skip calculation.
	 */
	private class EasingCache(val wrapped: Interpolation) : Interpolation {

		private var lastAlpha: Float = 0f
		private var lastResult: Float = 0f
		override fun apply(alpha: Float): Float {
			if (alpha == lastAlpha) return lastResult
			lastAlpha = alpha
			lastResult = wrapped.apply(alpha)
			return lastResult
		}
	}

	companion object {
		private val PROPS = PropType.values()
		private val quat = Quaternion()
	}
}

fun Owned.animationComponent(bundle: AnimationBundle, libraryItemName: String, init: ComponentInit<AnimationInstance> = {}): AnimationInstance {
	val c = createComponentFromLibrary(bundle, libraryItemName) as? AnimationInstance ?: throw Exception("Library item is not an animation.")
	c.init()
	return c
}


fun Owned.createComponentFromLibrary(bundle: AnimationBundle, libraryItemName: String, init: ComponentInit<UiComponent> = {}): SymbolInstance {
	val libraryItem = bundle.library[libraryItemName] ?: throw Exception("library item not found with name: $libraryItemName")
	val component = when (libraryItem.itemType) {
		LibraryItemType.CUSTOM -> throw Exception("Cannot create a component with a custom library type.")
		LibraryItemType.IMAGE -> ImageInstance(textureC((libraryItem as ImageLibraryItem).path), libraryItem)
		LibraryItemType.ATLAS -> {
			libraryItem as AtlasLibraryItem
			AtlasInstance(atlas(libraryItem.atlasPath, libraryItem.regionName), libraryItem)
		}
		LibraryItemType.ANIMATION -> AnimationInstance(this, bundle, libraryItem as AnimationLibraryItem)
	}
	component.init()
	return component
}
