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

package com.acornui.tween

import com.acornui.collection.*
import com.acornui.di.Context
import com.acornui.math.Interpolation
import com.acornui.signal.addOnce
import kotlin.time.Duration

/**
 * A registry of tweens by their target and property so that tweens can be cancelled and overwritten.
 */
object TweenRegistry {

	private val registry: MutableMultiMap2<Any, String, Tween> = multiMap2()

	fun contains(target: Any, property: String): Boolean {
		return registry[target, property] != null
	}

	fun kill(target: Any, property: String, finish: Boolean = true) {
		val tween = registry[target, property]
		if (finish) tween?.finish()
		else tween?.complete()
	}

	fun kill(target: Any, finish: Boolean = true) {
		val targetTweens = registry.remove(target) ?: return
		for (i in targetTweens.values) {
			if (finish) i.finish()
			else i.complete()
		}
	}

	fun register(target: Any, property: String, tween: Tween) {
		tween.completed.addOnce {
			unregister(target, property)
		}
		registry[target][property] = tween
	}

	fun unregister(target: Any, property: String) {
		registry.remove(target, property)
	}
}

fun createPropertyTween(target: Any, property: String, duration: Duration, ease: Interpolation, getter: () -> Double, setter: (Double) -> Unit, targetValue: Double, delay: Duration = Duration.ZERO, loop: Boolean = false): Tween {
	TweenRegistry.kill(target, property, finish = true)
	val tween = toFromTween(getter(), targetValue, duration, ease, delay, loop, setter)
	TweenRegistry.register(target, property, tween)
	return tween
}


fun killTween(target: Any, property: String, finish: Boolean = true) = TweenRegistry.kill(target, property, finish)
fun killTween(target: Any, finish: Boolean = true) = TweenRegistry.kill(target, finish)
