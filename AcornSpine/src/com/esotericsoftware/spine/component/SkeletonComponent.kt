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

package com.esotericsoftware.spine.component

import com.acornui.component.ComponentInit
import com.acornui.gl.core.GlState
import com.acornui.graphics.ColorRo
import com.acornui.math.Matrix4Ro
import com.esotericsoftware.spine.Skeleton
import com.esotericsoftware.spine.animation.AnimationState
import com.esotericsoftware.spine.animation.AnimationStateData
import com.esotericsoftware.spine.renderer.SkeletonMeshRenderer
import com.esotericsoftware.spine.renderer.SkeletonRenderer

/**
 * An encapsulation of the parts needed to animate and render a single skeleton.
 */
open class SkeletonComponent(
		val loadedSkeleton: LoadedSkeleton,
		val renderer: SkeletonRenderer = SkeletonMeshRenderer
) {

	val skeleton: Skeleton
	val animationState: AnimationState

	var isPaused: Boolean = false

	init {
		skeleton = Skeleton(loadedSkeleton.skeletonData, loadedSkeleton.textureAtlasData)
		animationState = AnimationState(skeleton, AnimationStateData(loadedSkeleton.skeletonData))
	}

	fun activate() {
		for (loadedSkin in loadedSkeleton.loadedSkins.values) {
			for (texture in loadedSkin.pageTextures.values) {
				texture.refInc()
			}
		}
	}

	fun deactivate() {
		for (loadedSkin in loadedSkeleton.loadedSkins.values) {
			for (texture in loadedSkin.pageTextures.values) {
				texture.refDec()
			}
		}
	}

	fun tick(stepTime: Float) {
		if (isPaused || stepTime <= 0f) return
		animationState.update(stepTime)
		animationState.apply(skeleton)
		skeleton.updateWorldTransform()
	}

	fun draw(glState: GlState, concatenatedTransform: Matrix4Ro, concatenatedColorTint: ColorRo) {
		renderer.draw(loadedSkeleton, skeleton, glState, concatenatedTransform, concatenatedColorTint)
	}

}

fun skeletonComponent(loadedSkeleton: LoadedSkeleton, init: ComponentInit<SkeletonComponent> = {}): SkeletonComponent {
	val s = SkeletonComponent(loadedSkeleton)
	s.init()
	return s
}