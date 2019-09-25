/*
 * Copyright 2016 Nicholas Bilyk
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
import com.acornui.component.RenderContextRo
import com.acornui.component.UiComponentImpl
import com.acornui.di.Owned
import com.acornui.setCamera
import com.acornui.time.onTick
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

/**
 * A Spine scene is a component that allows for spine skeletons to be added and rendered.
 * Created by nbilyk on 6/11/2016.
 */
class SpineScene(owner: Owned) : UiComponentImpl(owner) {

	var flipY = true

	private val _children = ArrayList<SkeletonComponent>()

	val children: List<SkeletonComponent>
		get() = _children

	/**
	 * If true, all animations will be paused.
	 */
	var isPaused: Boolean = false

	init {
		onTick {
			tick(it)
		}
	}

	operator fun <P : SkeletonComponent> P.unaryPlus(): P {
		addChild(_children.size, this)
		return this
	}

	operator fun <P : SkeletonComponent> P.unaryMinus(): P {
		removeChild(this)
		return this
	}

	fun addChild(child: SkeletonComponent) = addChild(_children.size, child)

	fun addChild(index: Int, child: SkeletonComponent) {
		child.skeleton.flipY = flipY
		_children.add(index, child)
		if (isActive) child.activate()
	}

	fun removeChild(child: SkeletonComponent): Boolean {
		val index = _children.indexOf(child)
		if (index == -1) return false
		removeChild(index)
		return true
	}

	fun removeChild(index: Int): SkeletonComponent {
		val child = _children[index]
		_children.removeAt(index)
		if (isActive) child.deactivate()
		return child
	}

	override fun onActivated() {
		super.onActivated()
		for (i in 0.._children.lastIndex) {
			_children[i].activate()
		}
	}

	override fun onDeactivated() {
		super.onDeactivated()
		for (i in 0.._children.lastIndex) {
			_children[i].deactivate()
		}
	}

	fun tick(tickTime: Float) {
		if (isPaused) return
		for (i in 0.._children.lastIndex) {
			_children[i].tick(tickTime)
		}
		window.requestRender()
	}

	override fun render(renderContext: RenderContextRo) {
		glState.setCamera(renderContext, useModel = true)
		val colorTint = renderContext.colorTint

		for (i in 0.._children.lastIndex) {
			_children[i].draw(glState, colorTint)
		}
	}

}

inline fun Owned.spineScene(init: ComponentInit<SpineScene> = {}): SpineScene  {
	contract { callsInPlace(init, InvocationKind.EXACTLY_ONCE) }
	val s = SpineScene(this)
	s.init()
	return s
}