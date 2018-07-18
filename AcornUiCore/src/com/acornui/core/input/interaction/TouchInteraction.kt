package com.acornui.core.input.interaction

import com.acornui.collection.Clearable
import com.acornui.collection.ClearableObjectPool
import com.acornui.component.InteractiveElementRo
import com.acornui.component.UiComponentRo
import com.acornui.core.input.InteractionEventBase
import com.acornui.core.input.InteractionEventRo
import com.acornui.core.input.InteractionType
import com.acornui.math.MathUtils
import com.acornui.math.Vector2
import com.acornui.math.Vector2Ro
import kotlin.math.sqrt

interface TouchInteractionRo : InteractionEventRo {

	/**
	 * The number of milliseconds from the Unix epoch.
	 */
	val timestamp: Long

	/**
	 * A list of all the Touch objects that are both currently in contact with the touch surface and were also started
	 * on the same element that is the target of the event.
	 */
	val targetTouches: List<TouchRo>

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
		val TOUCH_ENTER = InteractionType<TouchInteractionRo>("touchEnter")
		val TOUCH_LEAVE = InteractionType<TouchInteractionRo>("touchLeave")
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
	 * A list of all the Touch objects that are both currently in contact with the touch surface and were also started
	 * on the same element that is the target of the event.
	 */
	override val targetTouches: MutableList<Touch> = ArrayList()

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
		for (i in 0..targetTouches.lastIndex) {
			targetTouches[i].localize(currentTarget)
		}
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
		targetTouches.addTouches(event.targetTouches)
		changedTouches.addTouches(event.changedTouches)
		touches.addTouches(event.touches)

		timestamp = event.timestamp
	}

	fun clearTouches() {
		clearTouches(targetTouches)
		clearTouches(changedTouches)
		clearTouches(touches)
	}

	private fun clearTouches(touches: MutableList<Touch>) {
		for (i in 0..touches.lastIndex) {
			touches[i].free()
		}
		touches.clear()
	}

	override fun clear() {
		super.clear()
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

	val radiusX: Float
	val radiusY: Float
	val rotationAngle: Float

	val target: InteractiveElementRo?
	val currentTarget: InteractiveElementRo?

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

class Touch : TouchRo, Clearable {

	/**
	 * The x position of the mouse event relative to the root canvas.
	 */
	override var canvasX: Float = 0f

	/**
	 * The y position of the mouse event relative to the root canvas.
	 */
	override var canvasY: Float = 0f

	override var radiusX: Float = 0f
	override var radiusY: Float = 0f
	override var rotationAngle: Float = 0f

	override var target: InteractiveElementRo? = null
	override var currentTarget: InteractiveElementRo? = null

	override var identifier: Int = -1

	private var _localPositionIsValid = false
	private val _localPosition: Vector2 = Vector2()

	/**
	 * The position of the mouse event relative to the [target].
	 */
	private fun localPosition(): Vector2Ro {
		if (!_localPositionIsValid) {
			_localPositionIsValid = true
			_localPosition.set(canvasX, canvasY)
			currentTarget!!.windowToLocal(_localPosition)
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

	fun localize(currentTarget: InteractiveElementRo) {
		this.currentTarget = currentTarget
		_localPositionIsValid = false
	}

	/**
	 * The distance of this touch point to the other touch point (in canvas coordinates)
	 */
	fun dst(other: Touch): Float {
		val xD = other.canvasX - canvasX
		val yD = other.canvasY - canvasY
		return sqrt((xD * xD + yD * yD))
	}

	override fun clear() {
		canvasX = 0f
		canvasY = 0f
		radiusX = 0f
		radiusY = 0f
		rotationAngle = 0f
		target = null
		currentTarget = null
		identifier = -1
		_localPositionIsValid = false
	}

	fun set(otherTouch: TouchRo) {
		canvasX = otherTouch.canvasX
		canvasY = otherTouch.canvasY
		radiusX = otherTouch.radiusX
		radiusY = otherTouch.radiusY
		rotationAngle = otherTouch.rotationAngle
		target = otherTouch.target
		currentTarget = otherTouch.currentTarget
		identifier = otherTouch.identifier
		_localPositionIsValid = false
	}

	fun free() {
		pool.free(this)
	}

	companion object {

		private val pool = ClearableObjectPool { Touch() }
		fun obtain(): Touch = pool.obtain()
	}
}