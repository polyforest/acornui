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
package com.acornui

import com.acornui.component.*
import com.acornui.component.parentWalk
import com.acornui.math.Vector3
import com.acornui.math.maxOf4
import com.acornui.math.minOf4
import com.acornui.time.callLater

@ExperimentalAcorn
fun debugWhyCantSee(target: UiComponent) {
	target.callLater { canSee(target) }
}

private fun canSee(target: UiComponentRo, print: Boolean = true): Boolean {
	target.stage.update()
	var canSee = true

	target.parentWalk {
		if (!it.visible) {
			if (print) println("${debugFullPath(it)} is not visible")
			canSee = false
		}
		if (it.alpha <= 0.1f) {
			if (print) println("${debugFullPath(it)} has low opacity")
			canSee = false
		}
		if (it.width <= 4f) {
			if (print) println("${debugFullPath(it)} has width ${it.width}")
			canSee = false
		}
		if (it.height <= 4f) {
			if (print) println("${debugFullPath(it)} has height ${it.height}")
			canSee = false
		}
		if (it !is Stage) {
			if (it.parent == null) {
				if (print) println("${debugFullPath(it)} is not on the stage")
				canSee = false
			}
		}
		true
	}
	canSee = isInBounds(target, print) && canSee
	if (print && canSee) println("Unknown reason for invisibility.")
	return canSee
}

/**
 * Returns true if the element isn't out of the bounds of any of its ancestors.
 */
private fun isInBounds(target: UiComponentRo, print: Boolean): Boolean {
	var canSee = true
	val topLeftGlobal = target.localToGlobal(Vector3(0f, 0f))
	val topRightGlobal = target.localToGlobal(Vector3(target.width, 0f))
	val bottomRightGlobal = target.localToGlobal(Vector3(target.width, target.height))
	val bottomLeftGlobal = target.localToGlobal(Vector3(0f, target.height))
	val topLeftLocal = Vector3()
	val topRightLocal = Vector3()
	val bottomRightLocal = Vector3()
	val bottomLeftLocal = Vector3()

	target.parentWalk {
		if (it == target) {
			true
		} else {
			it.globalToLocal(topLeftLocal.set(topLeftGlobal))
			it.globalToLocal(topRightLocal.set(topRightGlobal))
			it.globalToLocal(bottomRightLocal.set(bottomRightGlobal))
			it.globalToLocal(bottomLeftLocal.set(bottomLeftGlobal))
			val left = minOf4(topLeftLocal.x, topRightLocal.x, bottomRightLocal.x, bottomRightLocal.x)
			val top = minOf4(topLeftLocal.y, topRightLocal.y, bottomRightLocal.y, bottomRightLocal.y)
			val right = maxOf4(topLeftLocal.x, topRightLocal.x, bottomRightLocal.x, bottomRightLocal.x)
			val bottom = maxOf4(topLeftLocal.y, topRightLocal.y, bottomRightLocal.y, bottomRightLocal.y)

			if (right < 0f) {
				if (print) println("${debugFullPath(target)} is offscreen left for parent ${debugFullPath(it)}: $right")
				canSee = false
			}
			if (bottom < 0f) {
				if (print) println("${debugFullPath(target)} is offscreen top for parent ${debugFullPath(it)}: $bottom")
				canSee = false
			}
			if (left > it.width) {
				if (print) println("${debugFullPath(target)} is offscreen right for parent ${debugFullPath(it)}: $left > ${it.width}")
				canSee = false
			}
			if (top > it.height) {
				if (print) println("${debugFullPath(target)} is offscreen bottom for parent ${debugFullPath(it)}: $top > ${it.height}")
				canSee = false
			}
			canSee
		}
	}

	return canSee
}

@ExperimentalAcorn
fun debugFullPath(target: UiComponentRo): String {
	val ancestry = target.ancestry(ArrayList())
	ancestry.reverse()
	return ancestry.joinToString(".")
}
