package com.acornui.serialization

import com.acornui.graphic.Color
import com.acornui.graphic.ColorRo
import com.acornui.math.*
import kotlinx.serialization.modules.SerializersModule

val dataModule = SerializersModule {
	polymorphic(ColorRo::class) {
		Color::class with Color.serializer()
	}
	polymorphic(CornersRo::class) {
		Corners::class with Corners.serializer()
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