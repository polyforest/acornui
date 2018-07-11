package com.acornui.graphics.lighting

import com.acornui._assert
import com.acornui.collection.equalsArray
import com.acornui.core.graphics.CameraRo
import com.acornui.math.*
import com.acornui.observe.ModTagWatch

/**
 * @author nbilyk
 */
class DirectionalLightCamera {

	/**
	 * The clip space dimensions, (in relation to the view camera) where shadows can be created.
	 * This can be set more easily with [setClipSpace] or [setClipSpaceFromWorld].
	 */
	val clipSpace = arrayOf(Vector3(-1f, -1f, -1f), Vector3(1f, -1f, -1f), Vector3(1f, 1f, -1f), Vector3(-1f, 1f, -1f), Vector3(-1f, -1f, 1f), Vector3(1f, -1f, 1f), Vector3(1f, 1f, 1f), Vector3(-1f, 1f, 1f))

	private val viewCamWatch = ModTagWatch()

	val view = Matrix4()
	val combined = Matrix4()

	private val direction = Vector3(0f, 0f, 1f)
	private val up = Vector3(0f, -1f, 0f)
	private val lastClipSpace = Array(8, { Vector3() })

	private val bounds = Box()
	private val tmp = Vector3()

	/**
	 * Sets the clip space based on world coordinates.
	 */
	fun setClipSpaceFromWorld(left: Float, right: Float, bottom: Float, top: Float, near: Float, far: Float, viewCamera: CameraRo) {
		setClipSpace(left, right, bottom, top, near, far)
		for (i in 0..clipSpace.lastIndex) {
			viewCamera.combined.prj(clipSpace[i])
		}
	}

	/**
	 * Sets the clip space dimensions, (in relation to the view camera) where shadows can be created.
	 * The default is the entire viewable area: -1f, 1f, -1f, 1f, -1f, 1f
	 */
	fun setClipSpace(left: Float, right: Float, bottom: Float, top: Float, near: Float, far: Float) {
		clipSpace[0].x = left
		clipSpace[0].y = bottom
		clipSpace[0].z = near

		clipSpace[1].x = right
		clipSpace[1].y = bottom
		clipSpace[1].z = near

		clipSpace[2].x = right
		clipSpace[2].y = top
		clipSpace[2].z = near

		clipSpace[3].x = left
		clipSpace[3].y = top
		clipSpace[3].z = near

		clipSpace[4].x = left
		clipSpace[4].y = bottom
		clipSpace[4].z = far

		clipSpace[5].x = right
		clipSpace[5].y = bottom
		clipSpace[5].z = far

		clipSpace[6].x = right
		clipSpace[6].y = top
		clipSpace[6].z = far

		clipSpace[7].x = left
		clipSpace[7].y = top
		clipSpace[7].z = far
	}

	/**
	 * Updates the [combined] matrix based on the light's direction.
	 */
	fun update(newDirection: Vector3Ro, viewCam: CameraRo): Boolean {
		if (!viewCamWatch.set(viewCam.modTag) &&
				direction == newDirection && lastClipSpace.equalsArray(clipSpace)) {
			// Up-to-date
			return false
		}
		_assert(!newDirection.isZero()) { "Direction may not be zero." }

		setDirection(newDirection)
		view.setToLookAt(direction, up)

		// Take the viewable box from the viewerCamera and make sure the Directional light camera can see the whole world.
		bounds.inf()
		for (i in 0..clipSpace.lastIndex) {
			lastClipSpace[i].set(clipSpace[i]) // For no-op check.
			viewCam.invCombined.prj(tmp.set(clipSpace[i])) // Convert the screen boundary to world space.
			view.prj(tmp) // Project with the light's direction.
			bounds.ext(tmp)
		}
		bounds.update()

		combined.idt()
		combined.scl(2f / bounds.dimensions.x, 2f / bounds.dimensions.y, -2f / bounds.dimensions.z)
		combined.translate(-bounds.center.x, -bounds.center.y, -bounds.center.z)
		combined.mul(view)
		return true
	}

	/**
	 * Sets the new direction, keeping the up vector orthonormal.
	 */
	private fun setDirection(newDirection: Vector3Ro) {
		tmp.set(newDirection)
		val dot = tmp.dot(up)
		if (MathUtils.isZero(dot - 1)) {
			// Collinear
			up.set(direction).scl(-1f)
		} else if (MathUtils.isZero(dot + 1)) {
			// Collinear opposite
			up.set(direction)
		}
		direction.set(tmp)
		// Normalize up
		tmp.set(direction).crs(up).nor()
		up.set(tmp).crs(direction).nor()
	}
}