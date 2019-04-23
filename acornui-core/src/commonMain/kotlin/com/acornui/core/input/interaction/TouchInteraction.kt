package com.acornui.core.input.interaction

import com.acornui.recycle.Clearable
import com.acornui.recycle.ClearableObjectPool
import com.acornui.component.UiComponentRo
import com.acornui.component.canvasToLocal
import com.acornui.core.input.InteractionEventBase
import com.acornui.core.input.InteractionEventRo
import com.acornui.core.input.InteractionType
import com.acornui.math.Vector2
import com.acornui.math.Vector2Ro
import kotlin.math.sqrt

interface TouchInteractionRo : InteractionEventRo {

	/**
	 * The number of milliseconds from the Unix epoch.
	 */
	val timestamp: Long

	/**
	 * A list of all the Touch objects representing individual points of contact whose states changed between the
	 * previous touch event and this one.
	 */
	val changedTouches: List<TouchRo>

	/**
	 * A list of all the Touch objects representing all current points of contact with the surface, regardless of
	 * target or changed status.
	 */
	val touches: List<TouchRo>

	companion object {

		val TOUCH_START = InteractionType<TouchInteractionRo>("touchStart")
		val TOUCH_MOVE = InteractionType<TouchInteractionRo>("touchMove")
		val TOUCH_END = InteractionType<TouchInteractionRo>("touchEnd")
		val TOUCH_CANCEL = InteractionType<TouchInteractionRo>("touchCancel")

	}

}

class TouchInteraction : TouchInteractionRo, InteractionEventBase() {

	/**
	 * The number of milliseconds from the Unix epoch.
	 */
	override var timestamp: Long = 0

	/**
	 * A list of all the Touch objects representing individual points of contact whose states changed between the
	 * previous touch event and this one.
	 */
	override val changedTouches: MutableList<Touch> = ArrayList()

	/**
	 * A list of all the Touch objects representing all current points of contact with the surface, regardless of
	 * target or changed status.
	 */
	override val touches: MutableList<Touch> = ArrayList()

	override fun localize(currentTarget: UiComponentRo) {
		super.localize(currentTarget)
		for (i in 0..changedTouches.lastIndex) {
			changedTouches[i].localize(currentTarget)
		}
		for (i in 0..touches.lastIndex) {
			touches[i].localize(currentTarget)
		}
	}

	fun set(event: TouchInteractionRo) {
		type = event.type
		clearTouches()
		changedTouches.addTouches(event.changedTouches)
		touches.addTouches(event.touches)

		timestamp = event.timestamp
	}

	fun clearTouches() {
		clearTouches(changedTouches)
		clearTouches(touches)
	}

	private fun clearTouches(touches: MutableList<Touch>) {
		for (i in 0..touches.lastIndex) {
			Touch.free(touches[i])
		}
		touches.clear()
	}

	override fun clear() {
		super.clear()
		changedTouches.clear()
		timestamp = 0L
		clearTouches()
	}

	companion object {

		/**
		 * Fills the [toList] with clones of the touches in [fromList]
		 */
		private fun MutableList<Touch>.addTouches(fromList: List<TouchRo>) {
			for (i in 0..fromList.lastIndex) {
				val fromTouch = fromList[i]
				val newTouch = Touch.obtain()
				newTouch.set(fromTouch)
				add(newTouch)
			}
		}
	}

}

interface TouchRo {

	/**
	 * The x position of the mouse event relative to the root canvas.
	 */
	val canvasX: Float

	/**
	 * The y position of the mouse event relative to the root canvas.
	 */
	val canvasY: Float

	val currentTarget: UiComponentRo?

	val identifier: Int

	/**
	 * The x position of the mouse event relative to the [currentTarget].
	 */
	val localX: Float

	/**
	 * The y position of the mouse event relative to the [currentTarget].
	 */
	val localY: Float

	/**
	 * The distance of this touch point to the other touch point (in canvas coordinates)
	 */
	fun dst(other: TouchRo): Float {
		val xD = other.canvasX - canvasX
		val yD = other.canvasY - canvasY
		return sqrt((xD * xD + yD * yD))
	}
}

class Touch private constructor(): TouchRo, Clearable {

	/**
	 * The x position of the mouse event relative to the root canvas.
	 */
	override var canvasX: Float = 0f

	/**
	 * The y position of the mouse event relative to the root canvas.
	 */
	override var canvasY: Float = 0f

	override var currentTarget: UiComponentRo? = null

	override var identifier: Int = -1

	private var _localPositionIsValid = false
	private val _localPosition: Vector2 = Vector2()

	/**
	 * The position of the mouse event relative to the [currentTarget].
	 */
	private fun localPosition(): Vector2Ro {
		if (!_localPositionIsValid) {
			_localPositionIsValid = true
			_localPosition.set(canvasX, canvasY)
			currentTarget!!.canvasToLocal(_localPosition)
		}
		return _localPosition
	}

	/**
	 * The x position of the mouse event relative to the [currentTarget].
	 */
	override val localX: Float
		get() = localPosition().x

	/**
	 * The y position of the mouse event relative to the [currentTarget].
	 */
	override val localY: Float
		get() = localPosition().y

	fun localize(currentTarget: UiComponentRo) {
		this.currentTarget = currentTarget
		_localPositionIsValid = false
	}

	override fun clear() {
		canvasX = 0f
		canvasY = 0f
		currentTarget = null
		identifier = -1
		_localPositionIsValid = false
	}

	fun set(otherTouch: TouchRo) {
		canvasX = otherTouch.canvasX
		canvasY = otherTouch.canvasY
		currentTarget = otherTouch.currentTarget
		identifier = otherTouch.identifier
		_localPositionIsValid = false
	}

	companion object {

		private val pool = ClearableObjectPool { Touch() }
		fun obtain(): Touch = pool.obtain()
		fun free(touch: Touch) = pool.free(touch)
	}
}