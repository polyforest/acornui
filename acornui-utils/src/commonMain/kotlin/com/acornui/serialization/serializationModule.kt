package com.acornui.serialization

import com.acornui.math.*
import kotlinx.serialization.modules.SerializersModule

val dataModule = SerializersModule {
	polymorphic(RayRo::class) {
		Ray::class with Ray.serializer()
	}
	polymorphic(PadRo::class) {
		Pad::class with Pad.serializer()
	}
	polymorphic(Vector2Ro::class) {
		Vector2::class with Vector2.serializer()
	}
	polymorphic(Vector3Ro::class) {
		Vector3::class with Vector3.serializer()
	}
}