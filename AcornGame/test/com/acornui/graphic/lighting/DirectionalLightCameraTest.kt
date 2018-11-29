package com.acornui.graphic.lighting

import com.acornui.core.graphic.OrthographicCamera
import com.acornui.core.graphic.PerspectiveCamera
import com.acornui.math.Box
import com.acornui.math.Frustum
import com.acornui.math.Vector3
import com.acornui.test.assertListEquals
import org.junit.Test
import kotlin.test.assertTrue
import kotlin.test.fail

class DirectionalLightCameraTest {

	@Test fun testUpdate() {

		val d = DirectionalLightCamera()
		val viewCamera = PerspectiveCamera()

		_testUpdate(d, Vector3(0f, 0f, 1f), viewCamera)
		_testUpdate(d, Vector3(1f, 0f, 0f), viewCamera)
		_testUpdate(d, Vector3(1f, 1f, 1f), viewCamera)
		_testUpdate(d, Vector3(1f, 1f, -1f), viewCamera)
		_testUpdate(d, Vector3(-1f, 1f, -1f), viewCamera)
		_testUpdate(d, Vector3(-1f, -1f, -1f), viewCamera)
		_testUpdate(d, Vector3(0.5f, -1.5f, -1f), viewCamera)

		viewCamera.setPosition(200f, 300f, -200f)

		_testUpdate(d, Vector3(0f, 0f, 1f), viewCamera)
		_testUpdate(d, Vector3(1f, 0f, 0f), viewCamera)
		_testUpdate(d, Vector3(1f, 1f, 1f), viewCamera)
		_testUpdate(d, Vector3(1f, 1f, -1f), viewCamera)
		_testUpdate(d, Vector3(-1f, 1f, -1f), viewCamera)
		_testUpdate(d, Vector3(-1f, -1f, -1f), viewCamera)
		_testUpdate(d, Vector3(0.5f, -1.5f, -1f), viewCamera)
	}

	private fun _testUpdate(d: DirectionalLightCamera, direction: Vector3, viewCamera: PerspectiveCamera) {
		d.update(direction, viewCamera)

		var isGood = true
		val bounds = Box()
		val tmp = Vector3()
		bounds.inf()
		val m = 1.000001f
		for (i in 0..7) {
			viewCamera.invCombined.prj(tmp.set(Frustum.clipSpacePlanePoints[i])) // Clip to world coords
			d.combined.prj(tmp) // World to directional cam.

			isGood = isGood && (tmp.x <= m && tmp.x >= -m && tmp.y <= m && tmp.y >= -m && tmp.z <= m && tmp.z >= -m) // No clipping
			if (!isGood)
				fail("clip space not in view.")
			bounds.ext(tmp)
			bounds.update()
		}
		isGood = isGood && (bounds.dimensions.x > 1.99f && bounds.dimensions.y > 1.99f && bounds.dimensions.z > 1.99f)
		assertTrue(isGood)
	}

	@Test fun testClipSpace() {
		val d = DirectionalLightCamera()
		val viewCamera = OrthographicCamera()
		viewCamera.near = 1000f
		viewCamera.far = 5000f
		viewCamera.setViewport(400f, 300f)
		viewCamera.moveToLookAtRect(0f, 0f, 400f, 300f)

		d.setClipSpaceFromWorld(100f, 300f, 150f, 0f, 2000f, 4000f, viewCamera)
		println(d.clipSpace.joinToString())
		assertListEquals(arrayOf(
				Vector3(-0.5f, 0.0f, -0.5f), Vector3(0.5f, 0.0f, -0.5f), Vector3(0.5f, 1.0f, -0.5f), Vector3(-0.5f, 1.0f, -0.5f), Vector3(-0.5f, 0.0f, 0.5f), Vector3(0.5f, 0.0f, 0.5f), Vector3(0.5f, 1.0f, 0.5f), Vector3(-0.5f, 1.0f, 0.5f)
		), d.clipSpace)
	}
}
