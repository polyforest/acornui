package com.acornui.core.tween

import com.acornui.collection.DualHashMap
import com.acornui.math.Interpolation

/**
 * A registry of tweens by their target and property so that tweens can be canceled and overwritten.
 */
object TweenRegistry {

	private val registry = DualHashMap<Any, String, Tween>(removeEmptyInnerMaps = true)

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
		tween.completed.addOnce({
			TweenRegistry.unregister(target, property)
		})
		registry.put(target, property, tween)
	}

	fun unregister(target: Any, property: String) {
		registry.remove(target, property)
	}
}

fun createPropertyTween(target: Any, property: String, duration: Float, ease: Interpolation, getter: () -> Float, setter: (Float) -> Unit, targetValue: Float, delay: Float = 0f, loop: Boolean = false): Tween {
	TweenRegistry.kill(target, property, finish = true)
	val tween = toFromTween(getter(), targetValue, duration, ease, delay, loop, setter)
	TweenRegistry.register(target, property, tween)
	return tween
}


fun killTween(target: Any, property: String, finish: Boolean = true) = TweenRegistry.kill(target, property, finish)
fun killTween(target: Any, finish: Boolean = true) = TweenRegistry.kill(target, finish)